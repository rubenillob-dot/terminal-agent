package com.agent.core;

import com.agent.execution.ExecutionResult;
import com.agent.healing.DiagnosticResult;
import com.agent.model.Task;
import com.agent.model.TaskResult;
import com.agent.model.TaskStatus;
import com.agent.planner.ExecutionStep;
import com.agent.planner.TaskPlan;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Core autonomous orchestrator responsible for end-to-end task resolution.
 *
 * <p>Directs the lifecycle transitions:
 * {@code PENDING} &rarr; {@code PLANNING} &rarr; {@code EXECUTING} &rarr;
 * {@code VALIDATING} (&rarr; {@code HEALING}) &rarr; {@code COMPLETED} / {@code FAILED}.</p>
 *
 * <p>Implements autonomous self-healing loops when compiler errors or runtime exceptions occur.</p>
 */
public class AutonomousAgent {

    private final AgentContext context;
    private final Consumer<String> eventLogger;

    /**
     * Constructs an {@code AutonomousAgent} using the provided context and standard stdout logger.
     *
     * @param context the {@link AgentContext} holding dependencies and configurations, must not be null
     * @throws NullPointerException if {@code context} is null
     */
    public AutonomousAgent(AgentContext context) {
        this(context, message -> System.out.println("[Agent] " + message));
    }

    /**
     * Constructs an {@code AutonomousAgent} with a custom event listener.
     *
     * @param context     the {@link AgentContext} holding dependencies and configurations, must not be null
     * @param eventLogger callback consumer for operational log events, must not be null
     * @throws NullPointerException if any parameter is null
     */
    public AutonomousAgent(AgentContext context, Consumer<String> eventLogger) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.eventLogger = Objects.requireNonNull(eventLogger, "eventLogger must not be null");
    }

    /**
     * Executes the given technical task autonomously from planning to final validation.
     *
     * @param task the {@link Task} containing the problem statement and context, must not be null
     * @return the definitive {@link TaskResult} reflecting success, duration, and artifacts
     * @throws NullPointerException if {@code task} is null
     */
    public TaskResult executeTask(Task task) {
        Objects.requireNonNull(task, "task must not be null");

        Instant startInstant = Instant.now();
        List<Path> createdArtifacts = new ArrayList<>();
        int stepsExecuted = 0;

        eventLogger.accept("Received Task [" + task.getId() + "]: " + task.getDescription());

        // 1. Planning Phase
        TaskPlan plan;
        try {
            eventLogger.accept("Phase: PLANNING - Generating execution strategy...");
            plan = context.getTaskPlanner().createPlan(task);
            eventLogger.accept("Generated plan with " + plan.size() + " operational steps.");
        } catch (Exception e) {
            Duration elapsed = Duration.between(startInstant, Instant.now());
            return new TaskResult(
                    task,
                    TaskStatus.FAILED,
                    false,
                    "Planning failed with exception: " + e.getMessage(),
                    0,
                    elapsed,
                    createdArtifacts
            );
        }

        // 2. Execution and Validation Phase
        for (ExecutionStep step : plan.getSteps()) {
            stepsExecuted++;
            eventLogger.accept("Executing Step " + step.getStepNumber() + "/" + plan.size() + ": " + step.getName());

            try {
                switch (step.getActionType()) {
                    case WRITE_FILE -> {
                        Path writtenPath = context.getFileManager().createFile(
                                Path.of(step.getTargetPath()),
                                step.getPayload()
                        );
                        createdArtifacts.add(writtenPath);
                        eventLogger.accept("Artifact created: " + step.getTargetPath());
                    }

                    case COMPILE -> {
                        boolean compileOk = executeWithHealing(step, context.getMaxHealingAttempts());
                        if (!compileOk) {
                            Duration elapsed = Duration.between(startInstant, Instant.now());
                            return new TaskResult(
                                    task,
                                    TaskStatus.FAILED,
                                    false,
                                    "Compilation failed after maximum self-healing attempts.",
                                    stepsExecuted,
                                    elapsed,
                                    createdArtifacts
                            );
                        }
                    }

                    case TEST -> {
                        ExecutionResult testResult = context.getCommandExecutor().execute(
                                step.getPayload(),
                                context.getFileManager().getRootDirectory(),
                                context.getCommandTimeout()
                        );
                        if (!testResult.isSuccess()) {
                            eventLogger.accept("Test execution failed. Output:\n" + testResult.getCombinedOutput());
                            Duration elapsed = Duration.between(startInstant, Instant.now());
                            return new TaskResult(
                                    task,
                                    TaskStatus.FAILED,
                                    false,
                                    "Test suite failed with exit code " + testResult.getExitCode() + ":\n" + testResult.getCombinedOutput(),
                                    stepsExecuted,
                                    elapsed,
                                    createdArtifacts
                            );
                        }
                        eventLogger.accept("All test assertions passed successfully.");
                    }

                    case VERIFY -> {
                        ExecutionResult verifyResult = context.getCommandExecutor().execute(
                                step.getPayload(),
                                context.getFileManager().getRootDirectory(),
                                context.getCommandTimeout()
                        );
                        eventLogger.accept("Verification step output: " + verifyResult.getStdout());
                    }
                }
            } catch (Exception e) {
                Duration elapsed = Duration.between(startInstant, Instant.now());
                eventLogger.accept("Fatal error on step " + step.getStepNumber() + ": " + e.getMessage());
                return new TaskResult(
                        task,
                        TaskStatus.FAILED,
                        false,
                        "Execution aborted due to unexpected error: " + e.getMessage(),
                        stepsExecuted,
                        elapsed,
                        createdArtifacts
                );
            }
        }

        Duration totalDuration = Duration.between(startInstant, Instant.now());
        eventLogger.accept("Task [" + task.getId() + "] completed successfully in " + totalDuration.toMillis() + "ms.");

        return new TaskResult(
                task,
                TaskStatus.COMPLETED,
                true,
                "All " + stepsExecuted + " steps executed, compiled, and verified autonomously.",
                stepsExecuted,
                totalDuration,
                createdArtifacts
        );
    }

    /**
     * Executes a command step wrapped in a self-healing diagnostic and correction loop.
     *
     * @param step        the {@link ExecutionStep} to execute, must not be null
     * @param maxAttempts maximum retry and patching iterations
     * @return {@code true} if execution succeeded initially or after self-healing; {@code false} otherwise
     * @throws IOException          if reading or writing files fails
     * @throws InterruptedException if interrupted while executing process
     */
    private boolean executeWithHealing(ExecutionStep step, int maxAttempts) throws IOException, InterruptedException {
        int attempt = 0;
        while (attempt < maxAttempts) {
            attempt++;
            eventLogger.accept("Executing command (attempt " + attempt + "/" + maxAttempts + "): " + step.getPayload());

            ExecutionResult result = context.getCommandExecutor().execute(
                    step.getPayload(),
                    context.getFileManager().getRootDirectory(),
                    context.getCommandTimeout()
            );

            if (result.isSuccess()) {
                eventLogger.accept("Command completed with exit code 0.");
                return true;
            }

            eventLogger.accept("Command failed with exit code " + result.getExitCode() + ". Initiating diagnosis...");
            DiagnosticResult diagnostic = context.getHealingEngine().diagnose(result, step);
            eventLogger.accept("Diagnosis: " + diagnostic.getRootCause() + " | Remedy: " + diagnostic.getSuggestedFix());

            if (!diagnostic.isHealable() || diagnostic.getTargetFile().isBlank()) {
                eventLogger.accept("Error is not automatically healable. Aborting loop.");
                return false;
            }

            // Attempt to patch the offending file
            Path targetPath = Path.of(diagnostic.getTargetFile());
            if (context.getFileManager().exists(targetPath)) {
                String originalCode = context.getFileManager().readFile(targetPath);
                String patchedCode = context.getHealingEngine().applyFix(originalCode, diagnostic);

                if (!originalCode.equals(patchedCode)) {
                    context.getFileManager().createFile(targetPath, patchedCode);
                    eventLogger.accept("Applied self-healing patch to: " + targetPath);
                } else {
                    eventLogger.accept("Patch yielded identical content. Cannot self-heal further.");
                    return false;
                }
            } else {
                eventLogger.accept("Target file for repair not found: " + targetPath);
                return false;
            }
        }
        return false;
    }
}
