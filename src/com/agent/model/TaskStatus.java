package com.agent.model;

/**
 * Enumeration representing the distinct lifecycle states of an autonomous agent task.
 *
 * <p>These states govern transitions during planning, code generation, compilation,
 * validation, and potential self-healing iterations.</p>
 */
public enum TaskStatus {

    /**
     * The task has been received and registered but has not yet started execution.
     */
    PENDING,

    /**
     * The task is currently being analyzed and broken down into execution steps.
     */
    PLANNING,

    /**
     * The plan steps (code writing, directory setups) are currently executing.
     */
    EXECUTING,

    /**
     * The solution is undergoing compilation, static analysis, or automated testing.
     */
    VALIDATING,

    /**
     * A failure was detected; the self-healing engine is diagnosing and patching code.
     */
    HEALING,

    /**
     * The task has successfully completed all steps, compilation, and validation.
     */
    COMPLETED,

    /**
     * The task failed to achieve its goal and exceeded maximum self-healing retries.
     */
    FAILED
}
