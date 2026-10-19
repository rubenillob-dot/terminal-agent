package com.agent.test;

import com.agent.core.AgentContext;
import com.agent.core.AutonomousAgent;
import com.agent.execution.ExecutionResult;
import com.agent.execution.ProcessCommandExecutor;
import com.agent.healing.DiagnosticResult;
import com.agent.healing.SelfHealingEngine;
import com.agent.model.Task;
import com.agent.model.TaskResult;
import com.agent.model.TaskStatus;
import com.agent.planner.DefaultTaskPlanner;
import com.agent.planner.ExecutionStep;
import com.agent.planner.TaskPlan;
import com.agent.workspace.WorkspaceManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Automated test runner validating the Autonomous Agent components and self-healing cycle.
 *
 * <p>Executes independently without third-party test libraries, validating workspace sandboxing,
 * process execution, diagnostic engines, and end-to-end self-healing compilation workflows.</p>
 */
public final class AgentTestSuite {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static final List<String> failures = new ArrayList<>();

    private AgentTestSuite() {
        // Prevent instantiation of test runner utility
    }

    /**
     * Entry point executing all test suites and reporting final status.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("Running Autonomous Agent Test Suite (Java 25)");
        System.out.println("=================================================");

        runTest("WorkspaceManager Sandboxing and File Operations", AgentTestSuite::testWorkspaceManager);
        runTest("ProcessCommandExecutor Process Execution and Timeouts", AgentTestSuite::testProcessCommandExecutor);
        runTest("DefaultTaskPlanner Plan Generation", AgentTestSuite::testTaskPlanner);
        runTest("SelfHealingEngine Javac Error Diagnosis", AgentTestSuite::testSelfHealingEngineDiagnosis);
        runTest("AutonomousAgent End-to-End Self-Healing Workflow", AgentTestSuite::testAutonomousAgentSelfHealing);

        System.out.println("-------------------------------------------------");
        System.out.printf("Test Summary: %d/%d tests passed.%n", testsPassed, testsRun);

        if (!failures.isEmpty()) {
            System.err.println("Failures detected:");
            failures.forEach(f -> System.err.println(" - " + f));
            System.exit(1);
        } else {
            System.out.println("ALL TESTS PASSED WITH 100% SUCCESS.");
        }
    }

    /**
     * Executes a single test case wrapped with assertion capture.
     *
     * @param testName name of the test case
     * @param runnable test method reference
     */
    private static void runTest(String testName, TestCase runnable) {
        testsRun++;
        System.out.print("[TEST] " + testName + " ... ");
        try {
            runnable.run();
            testsPassed++;
            System.out.println("PASSED");
        } catch (Throwable t) {
            System.out.println("FAILED");
            failures.add(testName + ": " + t.getMessage());
            t.printStackTrace(System.err);
        }
    }

    /**
     * Validates workspace file creation, reading, and directory traversal security.
     *
     * @throws IOException if file operations fail unexpectedly
     */
    public static void testWorkspaceManager() throws IOException {
        Path tempDir = Files.createTempDirectory("agent-ws-test-");
        try {
            WorkspaceManager manager = new WorkspaceManager(tempDir);

            // Test normal creation and read
            Path testFile = manager.createFile(Path.of("sub/dir/test.txt"), "Hello Autonomous Agent");
            assertTrue(Files.exists(testFile), "File should physically exist");
            assertTrue(manager.exists(Path.of("sub/dir/test.txt")), "FileManager.exists should report true");
            assertEquals("Hello Autonomous Agent", manager.readFile(Path.of("sub/dir/test.txt")), "File content mismatch");

            // Test path traversal prevention
            boolean securityExceptionThrown = false;
            try {
                manager.resolve(Path.of("../../etc/passwd"));
            } catch (IllegalArgumentException e) {
                securityExceptionThrown = true;
            }
            assertTrue(securityExceptionThrown, "Sandboxed manager must prevent path traversal outside root");

        } finally {
            deleteRecursively(tempDir);
        }
    }

    /**
     * Validates command execution, exit codes, output streams, and timeout enforcement.
     *
     * @throws Exception if process execution fails
     */
    public static void testProcessCommandExecutor() throws Exception {
        ProcessCommandExecutor executor = new ProcessCommandExecutor();
        Path currentDir = Path.of(".").toAbsolutePath().normalize();

        // 1. Valid command execution
        ExecutionResult echoResult = executor.execute("echo 'JavaAgentTest'", currentDir, Duration.ofSeconds(5));
        assertTrue(echoResult.isSuccess(), "Echo command should succeed");
        assertTrue(echoResult.getStdout().contains("JavaAgentTest"), "Stdout should capture echoed text");

        // 2. Failed command execution
        ExecutionResult failResult = executor.execute("nonexistent_command_xyz_123", currentDir, Duration.ofSeconds(5));
        assertFalse(failResult.isSuccess(), "Nonexistent command should exit with failure status");

        // 3. Timeout handling
        ExecutionResult timeoutResult = executor.execute("sleep 3", currentDir, Duration.ofMillis(300));
        assertFalse(timeoutResult.isSuccess(), "Command should be killed when exceeding timeout");
        assertTrue(timeoutResult.getStderr().contains("timed out"), "Stderr should report timeout killing");
    }

    /**
     * Validates task planning decomposition and step ordering.
     */
    public static void testTaskPlanner() {
        DefaultTaskPlanner planner = new DefaultTaskPlanner();
        Map<String, Object> context = new HashMap<>();
        context.put(DefaultTaskPlanner.CONTEXT_FILES_KEY, Map.of(
                "src/com/example/Sample.java", "package com.example; public class Sample {}"
        ));

        Task task = new Task("T-001", "Create sample class", context);
        TaskPlan plan = planner.createPlan(task);

        assertNotNull(plan, "Plan must not be null");
        assertTrue(plan.size() >= 3, "Plan should contain write, compile, and test steps");
        assertEquals(ExecutionStep.ActionType.WRITE_FILE, plan.getSteps().get(0).getActionType(), "First step must write source");
    }

    /**
     * Validates compiler diagnosis and error line extraction.
     */
    public static void testSelfHealingEngineDiagnosis() {
        SelfHealingEngine engine = new SelfHealingEngine();
        String sampleCompilerError = "src/com/demo/Broken.java:4: error: ';' expected\n        int x = 10\n                  ^";

        ExecutionResult failedResult = new ExecutionResult(
                "javac Broken.java",
                1,
                "",
                sampleCompilerError,
                Duration.ofMillis(120)
        );

        ExecutionStep compileStep = new ExecutionStep(
                1,
                "Compile",
                "Compiling source",
                ExecutionStep.ActionType.COMPILE,
                "out",
                "javac Broken.java"
        );

        DiagnosticResult diag = engine.diagnose(failedResult, compileStep);

        assertTrue(diag.isHealable(), "Syntax semicolon error should be healable");
        assertEquals("src/com/demo/Broken.java", diag.getTargetFile(), "Target file must match parsed path");
        assertEquals(4, diag.getLineNumber(), "Line number must be parsed accurately");

        // Test auto-patch
        String brokenSource = "package com.demo;\npublic class Broken {\n    public void test() {\n        int x = 10\n    }\n}";
        String fixed = engine.applyFix(brokenSource, diag);
        assertTrue(fixed.contains("int x = 10;"), "Auto-fix should append missing semicolon");
    }

    /**
     * Validates an end-to-end autonomous resolution workflow including self-healing compilation.
     *
     * @throws IOException if workspace setup fails
     */
    public static void testAutonomousAgentSelfHealing() throws IOException {
        Path tempWs = Files.createTempDirectory("agent-heal-test-");
        try {
            AgentContext context = AgentContext.createDefault(tempWs);
            AutonomousAgent agent = new AutonomousAgent(context, msg -> { /* Silent during test */ });

            // Create task with an intentional syntax defect (missing semicolon)
            String faultyJavaCode = """
                    package com.calc;
                    
                    public class Calculator {
                        public int add(int a, int b) {
                            return a + b
                        }
                    }
                    """;

            String testJavaCode = """
                    package com.calc;
                    
                    public class CalculatorTest {
                        public static void main(String[] args) {
                            Calculator c = new Calculator();
                            if (c.add(2, 3) != 5) {
                                throw new AssertionError("Addition failed");
                            }
                            System.out.println("Calculator tests passed.");
                        }
                    }
                    """;

            Map<String, Object> contextData = new HashMap<>();
            contextData.put(DefaultTaskPlanner.CONTEXT_FILES_KEY, Map.of(
                    "src/com/calc/Calculator.java", faultyJavaCode,
                    "src/com/calc/CalculatorTest.java", testJavaCode
            ));
            contextData.put(DefaultTaskPlanner.CONTEXT_COMPILE_CMD_KEY, "javac -d out src/com/calc/*.java");
            contextData.put(DefaultTaskPlanner.CONTEXT_TEST_CMD_KEY, "java -cp out com.calc.CalculatorTest");

            Task task = new Task("HEAL-001", "Build and verify Calculator with automated self-healing", contextData);

            TaskResult result = agent.executeTask(task);

            assertTrue(result.isSuccess(), "Agent should self-heal compilation and succeed");
            assertEquals(TaskStatus.COMPLETED, result.getStatus(), "Terminal status must be COMPLETED");

            // Verify the fixed code on disk has the semicolon
            String fixedCode = context.getFileManager().readFile(Path.of("src/com/calc/Calculator.java"));
            assertTrue(fixedCode.contains("return a + b;"), "Self-healing must have patched missing semicolon on disk");

        } finally {
            deleteRecursively(tempWs);
        }
    }

    // Helper assertions
    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("Assertion failed: " + message);
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) throw new AssertionError("Assertion failed (expected false): " + message);
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj == null) throw new AssertionError("Assertion failed: " + message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + " [Expected: " + expected + ", Actual: " + actual + "]");
        }
    }

    private static void deleteRecursively(Path root) {
        try {
            if (Files.exists(root)) {
                try (var s = Files.walk(root)) {
                    s.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
                }
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Functional interface for test cases that can throw exceptions.
     */
    @FunctionalInterface
    public interface TestCase {
        /**
         * Executes the test logic.
         *
         * @throws Throwable if test assertions fail
         */
        void run() throws Throwable;
    }
}
