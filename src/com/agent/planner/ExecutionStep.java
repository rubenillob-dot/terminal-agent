package com.agent.planner;

import java.util.Objects;

/**
 * Represents a single discrete operational step within an autonomous execution plan.
 *
 * <p>Each step defines an action type (e.g., writing source files, compiling, testing),
 * an associated target path or command payload, and a descriptive label.</p>
 */
public final class ExecutionStep {

    /**
     * Enumerates the possible action types performed during an execution step.
     */
    public enum ActionType {
        /**
         * Create or update a source code or resource file.
         */
        WRITE_FILE,

        /**
         * Invoke the Java compiler (`javac`) to build bytecode.
         */
        COMPILE,

        /**
         * Execute a test suite or verification binary (`java`).
         */
        TEST,

        /**
         * Validate outputs, inspect file tree, or finalize artifacts.
         */
        VERIFY
    }

    private final int stepNumber;
    private final String name;
    private final String description;
    private final ActionType actionType;
    private final String targetPath;
    private final String payload;

    /**
     * Constructs a new {@code ExecutionStep} instance.
     *
     * @param stepNumber  1-based sequence index of the step, must be positive
     * @param name        concise human-readable name of the step, must not be null or blank
     * @param description detailed description of what the step performs, must not be null
     * @param actionType  the {@link ActionType} category of this step, must not be null
     * @param targetPath  file or directory path target (if applicable, can be empty or null)
     * @param payload     the content to write or command line to execute, must not be null
     * @throws IllegalArgumentException if {@code stepNumber <= 0} or {@code name} is blank
     * @throws NullPointerException     if {@code description}, {@code actionType}, or {@code payload} is null
     */
    public ExecutionStep(int stepNumber,
                         String name,
                         String description,
                         ActionType actionType,
                         String targetPath,
                         String payload) {
        if (stepNumber <= 0) {
            throw new IllegalArgumentException("stepNumber must be positive, given: " + stepNumber);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank.");
        }
        this.stepNumber = stepNumber;
        this.name = name;
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.actionType = Objects.requireNonNull(actionType, "actionType must not be null");
        this.targetPath = targetPath != null ? targetPath : "";
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
    }

    /**
     * Retrieves the sequential position of this step.
     *
     * @return 1-indexed step number
     */
    public int getStepNumber() {
        return stepNumber;
    }

    /**
     * Retrieves the short name of this step.
     *
     * @return step title or name
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the functional explanation of this step.
     *
     * @return step description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Retrieves the classification of this step.
     *
     * @return {@link ActionType}
     */
    public ActionType getActionType() {
        return actionType;
    }

    /**
     * Retrieves the file target or relative path associated with this action.
     *
     * @return target path string, or empty string if not applicable
     */
    public String getTargetPath() {
        return targetPath;
    }

    /**
     * Retrieves the payload string (file contents or command line string).
     *
     * @return payload content
     */
    public String getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "ExecutionStep{" +
                "step=" + stepNumber +
                ", name='" + name + '\'' +
                ", actionType=" + actionType +
                ", targetPath='" + targetPath + '\'' +
                '}';
    }
}
