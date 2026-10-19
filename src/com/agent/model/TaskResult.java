package com.agent.model;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the complete outcome of an autonomous agent execution lifecycle.
 *
 * <p>Contains final execution status, success indicator, detailed execution summary,
 * execution duration, step count, and any produced file artifacts.</p>
 */
public final class TaskResult {

    private final Task task;
    private final TaskStatus status;
    private final boolean success;
    private final String summary;
    private final int stepsExecuted;
    private final Duration duration;
    private final List<Path> generatedArtifacts;

    /**
     * Constructs a new {@code TaskResult} representing an execution conclusion.
     *
     * @param task               the task that was executed, must not be null
     * @param status             final {@link TaskStatus} reached by the agent, must not be null
     * @param success            boolean flag indicating overall task success
     * @param summary            concise human-readable outcome summary, must not be null
     * @param stepsExecuted      total number of execution steps performed
     * @param duration           total elapsed duration of the lifecycle, must not be null
     * @param generatedArtifacts list of paths created or modified during the task, must not be null
     * @throws NullPointerException if any mandatory parameter is null
     */
    public TaskResult(Task task,
                      TaskStatus status,
                      boolean success,
                      String summary,
                      int stepsExecuted,
                      Duration duration,
                      List<Path> generatedArtifacts) {
        this.task = Objects.requireNonNull(task, "task must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.success = success;
        this.summary = Objects.requireNonNull(summary, "summary must not be null");
        this.stepsExecuted = stepsExecuted;
        this.duration = Objects.requireNonNull(duration, "duration must not be null");
        this.generatedArtifacts = Collections.unmodifiableList(List.copyOf(
                Objects.requireNonNull(generatedArtifacts, "generatedArtifacts must not be null")
        ));
    }

    /**
     * Retrieves the original task associated with this result.
     *
     * @return the {@link Task} instance
     */
    public Task getTask() {
        return task;
    }

    /**
     * Retrieves the terminal status of the task.
     *
     * @return final {@link TaskStatus}
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * Checks if the task achieved a successful terminal state.
     *
     * @return {@code true} if successful; {@code false} otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Retrieves the execution summary narrative.
     *
     * @return summary string
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Retrieves the total number of plan steps executed.
     *
     * @return steps executed count
     */
    public int getStepsExecuted() {
        return stepsExecuted;
    }

    /**
     * Retrieves the total wall-clock duration of the task execution.
     *
     * @return elapsed {@link Duration}
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Retrieves the list of file paths generated or modified by the agent.
     *
     * @return unmodifiable list of {@link Path} instances
     */
    public List<Path> getGeneratedArtifacts() {
        return generatedArtifacts;
    }

    @Override
    public String toString() {
        return "TaskResult{" +
                "taskId=" + task.getId() +
                ", status=" + status +
                ", success=" + success +
                ", stepsExecuted=" + stepsExecuted +
                ", duration=" + duration.toMillis() + "ms" +
                ", artifacts=" + generatedArtifacts.size() +
                '}';
    }
}
