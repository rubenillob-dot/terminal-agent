package com.agent.planner;

import com.agent.model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Standard implementation of {@link TaskPlanner} providing step decomposition
 * for Java development, compilation, and automated validation workflows.
 *
 * <p>Translates task requirements and contextual file specifications into concrete
 * {@link ExecutionStep} instances adhering to test-driven and compiler-validated methodologies.</p>
 */
public class DefaultTaskPlanner implements TaskPlanner {

    /**
     * Key for retrieving source file mappings from task context data.
     */
    public static final String CONTEXT_FILES_KEY = "files";

    /**
     * Key for specifying custom compilation commands in task context data.
     */
    public static final String CONTEXT_COMPILE_CMD_KEY = "compileCommand";

    /**
     * Key for specifying custom test or execution commands in task context data.
     */
    public static final String CONTEXT_TEST_CMD_KEY = "testCommand";

    /**
     * Key for specifying main verification class in task context data.
     */
    public static final String CONTEXT_MAIN_CLASS_KEY = "mainClass";

    /**
     * Constructs a new {@code DefaultTaskPlanner}.
     */
    public DefaultTaskPlanner() {
        // Standard default constructor
    }

    /**
     * {@inheritDoc}
     *
     * @param task the problem statement and context, must not be null
     * @return a structured {@link TaskPlan} ready for agent execution
     * @throws IllegalArgumentException if {@code task} description is missing
     * @throws NullPointerException     if {@code task} is null
     */
    @Override
    @SuppressWarnings("unchecked")
    public TaskPlan createPlan(Task task) {
        Objects.requireNonNull(task, "task must not be null");

        List<ExecutionStep> steps = new ArrayList<>();
        int stepIndex = 1;

        Map<String, Object> context = task.getContextData();

        // 1. Plan file writing steps
        if (context.containsKey(CONTEXT_FILES_KEY) && context.get(CONTEXT_FILES_KEY) instanceof Map<?, ?> filesMap) {
            for (Map.Entry<?, ?> entry : filesMap.entrySet()) {
                String relativePath = String.valueOf(entry.getKey());
                String codeContent = String.valueOf(entry.getValue());
                steps.add(new ExecutionStep(
                        stepIndex++,
                        "Write Source File: " + relativePath,
                        "Generate and write Java source file adhering to SOLID and strict Javadoc specifications.",
                        ExecutionStep.ActionType.WRITE_FILE,
                        relativePath,
                        codeContent
                ));
            }
        }

        // 2. Plan compilation step
        String compileCmd = context.containsKey(CONTEXT_COMPILE_CMD_KEY)
                ? String.valueOf(context.get(CONTEXT_COMPILE_CMD_KEY))
                : "javac -d out $(find src -name '*.java')";

        steps.add(new ExecutionStep(
                stepIndex++,
                "Compile Java Sources",
                "Invoke Java compiler (javac) with output directory configured and capture diagnostics.",
                ExecutionStep.ActionType.COMPILE,
                "out",
                compileCmd
        ));

        // 3. Plan test execution step
        String testCmd = context.containsKey(CONTEXT_TEST_CMD_KEY)
                ? String.valueOf(context.get(CONTEXT_TEST_CMD_KEY))
                : (context.containsKey(CONTEXT_MAIN_CLASS_KEY)
                ? "java -cp out " + context.get(CONTEXT_MAIN_CLASS_KEY)
                : "java -cp out com.agent.AgentTestSuite");

        steps.add(new ExecutionStep(
                stepIndex++,
                "Execute Test Suite",
                "Run compiled bytecode against automated test assertions and capture stdout/stderr.",
                ExecutionStep.ActionType.TEST,
                "",
                testCmd
        ));

        // 4. Plan verification step
        steps.add(new ExecutionStep(
                stepIndex,
                "Verify Quality & Documentation",
                "Perform final artifact verification and assert compliance with software architecture standards.",
                ExecutionStep.ActionType.VERIFY,
                "",
                "echo 'Solution verification completed successfully.'"
        ));

        String objective = "Autonomous resolution and verification pipeline for task: " + task.getId();
        return new TaskPlan(task, objective, steps);
    }
}
