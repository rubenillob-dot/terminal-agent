package com.agent.planner;

import com.agent.model.Task;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable representation of a generated execution plan formulated to solve a technical task.
 *
 * <p>Contains the original task reference, an overarching strategy objective,
 * and the ordered sequence of {@link ExecutionStep} instances to execute.</p>
 */
public final class TaskPlan {

    private final Task task;
    private final String objective;
    private final List<ExecutionStep> steps;

    /**
     * Constructs a new {@code TaskPlan}.
     *
     * @param task      the source {@link Task}, must not be null
     * @param objective high-level architectural goal or summary of the plan, must not be null
     * @param steps     ordered list of execution steps, must not be null or empty
     * @throws IllegalArgumentException if {@code steps} is empty or {@code objective} is blank
     * @throws NullPointerException     if any parameter is null
     */
    public TaskPlan(Task task, String objective, List<ExecutionStep> steps) {
        this.task = Objects.requireNonNull(task, "task must not be null");
        if (objective == null || objective.isBlank()) {
            throw new IllegalArgumentException("objective must not be null or blank.");
        }
        this.objective = objective;
        Objects.requireNonNull(steps, "steps list must not be null");
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("steps list must not be empty.");
        }
        this.steps = Collections.unmodifiableList(List.copyOf(steps));
    }

    /**
     * Retrieves the technical task this plan addresses.
     *
     * @return the associated {@link Task}
     */
    public Task getTask() {
        return task;
    }

    /**
     * Retrieves the strategic summary or objective of this plan.
     *
     * @return the plan objective
     */
    public String getObjective() {
        return objective;
    }

    /**
     * Retrieves an unmodifiable ordered list of steps to execute.
     *
     * @return list of {@link ExecutionStep} objects
     */
    public List<ExecutionStep> getSteps() {
        return steps;
    }

    /**
     * Retrieves the total number of steps in this plan.
     *
     * @return step count
     */
    public int size() {
        return steps.size();
    }

    @Override
    public String toString() {
        return "TaskPlan{" +
                "taskId=" + task.getId() +
                ", objective='" + objective + '\'' +
                ", totalSteps=" + steps.size() +
                '}';
    }
}
