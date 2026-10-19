package com.agent.execution;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Contract for executing shell commands and child operating system processes.
 *
 * <p>Implementations must handle asynchronous stream reading, timeout enforcement,
 * and working directory context safely.</p>
 */
public interface CommandExecutor {

    /**
     * Executes a command represented as an array or list of command tokens.
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
    ExecutionResult execute(List<String> commandTokens, Path workingDirectory, Duration timeout)
            throws IOException, InterruptedException;

    /**
     * Executes a single command string by invoking the platform's default shell (e.g. bash / sh).
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
    ExecutionResult execute(String command, Path workingDirectory, Duration timeout)
            throws IOException, InterruptedException;
}
