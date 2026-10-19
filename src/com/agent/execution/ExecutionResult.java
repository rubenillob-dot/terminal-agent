package com.agent.execution;

import java.time.Duration;
import java.util.Objects;

/**
 * Encapsulates the complete result of executing a system command via the agent.
 *
 * <p>Stores the exit code, captured standard output, captured standard error,
 * executed command line, and process run duration.</p>
 */
public final class ExecutionResult {

    private final String command;
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final Duration duration;

    /**
     * Constructs a new {@code ExecutionResult} instance.
     *
     * @param command  the command string executed, must not be null
     * @param exitCode the integer exit status returned by the process
     * @param stdout   the standard output captured from the process, must not be null
     * @param stderr   the standard error captured from the process, must not be null
     * @param duration the wall-clock time elapsed during command execution, must not be null
     * @throws NullPointerException if any mandatory parameter is null
     */
    public ExecutionResult(String command, int exitCode, String stdout, String stderr, Duration duration) {
        this.command = Objects.requireNonNull(command, "command must not be null");
        this.exitCode = exitCode;
        this.stdout = Objects.requireNonNull(stdout, "stdout must not be null");
        this.stderr = Objects.requireNonNull(stderr, "stderr must not be null");
        this.duration = Objects.requireNonNull(duration, "duration must not be null");
    }

    /**
     * Retrieves the command line that was executed.
     *
     * @return the command string
     */
    public String getCommand() {
        return command;
    }

    /**
     * Retrieves the process exit code.
     *
     * @return 0 if normal termination without error, non-zero otherwise
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Retrieves the standard output stream content captured from the process.
     *
     * @return standard output string
     */
    public String getStdout() {
        return stdout;
    }

    /**
     * Retrieves the standard error stream content captured from the process.
     *
     * @return standard error string
     */
    public String getStderr() {
        return stderr;
    }

    /**
     * Retrieves the time taken for the command to finish.
     *
     * @return process elapsed {@link Duration}
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Determines whether the process finished with a successful exit status (code 0).
     *
     * @return {@code true} if exit code is 0; {@code false} otherwise
     */
    public boolean isSuccess() {
        return exitCode == 0;
    }

    /**
     * Combines stdout and stderr streams into a single unified diagnostic log.
     *
     * @return combined text output
     */
    public String getCombinedOutput() {
        if (stdout.isBlank() && stderr.isBlank()) {
            return "";
        }
        if (stderr.isBlank()) {
            return stdout;
        }
        if (stdout.isBlank()) {
            return stderr;
        }
        return stdout + System.lineSeparator() + "[STDERR]:" + System.lineSeparator() + stderr;
    }

    @Override
    public String toString() {
        return "ExecutionResult{" +
                "command='" + command + '\'' +
                ", exitCode=" + exitCode +
                ", duration=" + duration.toMillis() + "ms" +
                ", stdoutLength=" + stdout.length() +
                ", stderrLength=" + stderr.length() +
                '}';
    }
}
