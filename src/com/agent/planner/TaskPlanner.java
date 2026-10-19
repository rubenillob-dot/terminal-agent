package com.agent.planner;

import com.agent.model.Task;

/**
 * Strategy interface for decomposing technical problem statements into structured execution plans.
 *
 * <p>Implementations analyze task descriptions and contextual metadata to formulate
 * concrete, ordered sequences of operational steps.</p>
 */
public interface TaskPlanner {

    /**
     * Formulates an executable {@link TaskPlan} for the specified technical task.
     *
     * @param task the problem statement and context, must not be null
     * @return a structured {@link TaskPlan} ready for agent execution
     * @throws IllegalArgumentException if {@code task} contains invalid or incomplete specifications
     * @throws NullPointerException     if {@code task} is null
     */
    TaskPlan createPlan(Task task);
}
