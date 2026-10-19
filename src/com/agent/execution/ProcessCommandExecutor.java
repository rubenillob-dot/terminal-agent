package com.agent.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Robust implementation of {@link CommandExecutor} utilizing {@link ProcessBuilder}.
 *
 * <p>Safely handles standard I/O stream consumption on dedicated virtual/cached background threads
 * to avoid pipe buffer deadlocks, enforces strict timeout deadlines with graceful destruction,
 * and detects the host operating system for shell wrapping.</p>
 */
public class ProcessCommandExecutor implements CommandExecutor {

    private final ExecutorService ioExecutor;

    /**
     * Default constructor initializing an asynchronous thread pool for stream consumption.
     */
    public ProcessCommandExecutor() {
        this.ioExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "ProcessIO-Worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param commandTokens    the list of arguments, starting with the executable, must not be null or empty
     * @param workingDirectory the directory in which the command will run, must not be null
     * @param timeout          the maximum time allowed for the command to finish, must not be null
     * @return an {@link ExecutionResult} containing exit code, output, and duration
     * @throws IOException          if an I/O error occurs while starting or reading from the process
     * @throws InterruptedException if the current thread is interrupted while waiting for the process
     * @throws IllegalArgumentException if {@code commandTokens} is null or empty
     * @throws NullPointerException     if any parameter is null
     */
    @Override
    public ExecutionResult execute(List<String> commandTokens, Path workingDirectory, Duration timeout)
            throws IOException, InterruptedException {
        Objects.requireNonNull(commandTokens, "commandTokens must not be null");
        Objects.requireNonNull(workingDirectory, "workingDirectory must not be null");
        Objects.requireNonNull(timeout, "timeout must not be null");

        if (commandTokens.isEmpty()) {
            throw new IllegalArgumentException("commandTokens must contain at least one element.");
        }

        File workDirFile = workingDirectory.toFile();
        if (!workDirFile.exists() || !workDirFile.isDirectory()) {
            throw new IllegalArgumentException("Working directory does not exist or is not a directory: " + workingDirectory);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(commandTokens);
        processBuilder.directory(workDirFile);

        long startNanos = System.nanoTime();
        Process process = processBuilder.start();

        CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(
                () -> readStream(process.getInputStream()), ioExecutor
        );
        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(
                () -> readStream(process.getErrorStream()), ioExecutor
        );

        boolean finishedNormally = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finishedNormally) {
            process.destroyForcibly();
            long elapsedNanos = System.nanoTime() - startNanos;
            return new ExecutionResult(
                    String.join(" ", commandTokens),
                    -1,
                    "",
                    "Execution timed out after " + timeout.toMillis() + " ms. Process forcibly killed.",
                    Duration.ofNanos(elapsedNanos)
            );
        }

        int exitCode = process.exitValue();
        long elapsedNanos = System.nanoTime() - startNanos;

        String stdout = safeGet(stdoutFuture, timeout);
        String stderr = safeGet(stderrFuture, timeout);

        return new ExecutionResult(
                String.join(" ", commandTokens),
                exitCode,
                stdout,
                stderr,
                Duration.ofNanos(elapsedNanos)
        );
    }

    /**
     * {@inheritDoc}
     *
     * @param command          the raw shell command string, must not be null or blank
     * @param workingDirectory the directory in which the shell command will run, must not be null
     * @param timeout          the maximum time allowed for the command to finish, must not be null
     * @return an {@link ExecutionResult} containing exit code, output, and duration
     * @throws IOException          if an I/O error occurs while starting or reading from the process
     * @throws InterruptedException if the current thread is interrupted while waiting for the process
     * @throws IllegalArgumentException if {@code command} is null or blank
     * @throws NullPointerException     if any parameter is null
     */
    @Override
    public ExecutionResult execute(String command, Path workingDirectory, Duration timeout)
            throws IOException, InterruptedException {
        Objects.requireNonNull(command, "command must not be null");
        if (command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank.");
        }

        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        List<String> shellTokens = isWindows
                ? List.of("cmd.exe", "/c", command)
                : List.of("bash", "-c", command);

        return execute(shellTokens, workingDirectory, timeout);
    }

    /**
     * Safely reads all characters from an input stream into a single String.
     *
     * @param inputStream the stream to read, must not be null
     * @return the string content of the stream
     */
    private String readStream(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append(System.lineSeparator());
            }
            return builder.toString().stripTrailing();
        } catch (IOException e) {
            return "Error reading process stream: " + e.getMessage();
        }
    }

    /**
     * Awaits completion of an asynchronous stream reader with a safety timeout.
     *
     * @param future  the future to await
     * @param timeout maximum duration to wait
     * @return stream text or fallback string
     */
    private String safeGet(CompletableFuture<String> future, Duration timeout) {
        try {
            return future.get(Math.min(timeout.toMillis(), 2000), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "[Error: Stream read interrupted]";
        } catch (ExecutionException | TimeoutException e) {
            return "[Error: Stream read failed or timed out: " + e.getMessage() + "]";
        }
    }
}
