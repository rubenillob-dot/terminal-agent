package com.agent.healing;

import com.agent.execution.ExecutionResult;
import com.agent.planner.ExecutionStep;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Diagnostic and self-healing engine responsible for parsing compilation and runtime failures,
 * locating offending source code lines, and generating remediation patches.
 *
 * <p>Implements the core Self-Healing feedback loop required for autonomous agent stability.</p>
 */
public class SelfHealingEngine {

    private static final Pattern JAVAC_ERROR_PATTERN = Pattern.compile(
            "^(.*?\\.java):(\\d+):\\s+error:\\s+(.*)$",
            Pattern.MULTILINE
    );

    private static final Pattern STACKTRACE_PATTERN = Pattern.compile(
            "at\\s+([\\w.$]+)\\.([\\w$]+)\\(([\\w.-]+\\.java):(\\d+)\\)"
    );

    private static final Pattern EXCEPTION_HEADER_PATTERN = Pattern.compile(
            "(?:Exception in thread \"[^\"]+\"|Caused by:)\\s+([\\w.$]+)(?::\\s*(.*))?"
    );

    /**
     * Constructs a new {@code SelfHealingEngine}.
     */
    public SelfHealingEngine() {
        // Default constructor
    }

    /**
     * Diagnoses a failed execution step by parsing standard error and standard output streams.
     *
     * @param failureResult the {@link ExecutionResult} of the failed command, must not be null
     * @param failedStep    the {@link ExecutionStep} that failed, must not be null
     * @return a structured {@link DiagnosticResult} detailing root cause and fix recommendation
     * @throws NullPointerException if any parameter is null
     */
    public DiagnosticResult diagnose(ExecutionResult failureResult, ExecutionStep failedStep) {
        Objects.requireNonNull(failureResult, "failureResult must not be null");
        Objects.requireNonNull(failedStep, "failedStep must not be null");

        String combinedOutput = failureResult.getCombinedOutput();

        if (failedStep.getActionType() == ExecutionStep.ActionType.COMPILE) {
            return diagnoseCompilationError(combinedOutput);
        } else if (failedStep.getActionType() == ExecutionStep.ActionType.TEST) {
            return diagnoseRuntimeError(combinedOutput);
        }

        return new DiagnosticResult(
                false,
                "Generic step failure: Exit code " + failureResult.getExitCode(),
                "Review step payload and execution environment.",
                failedStep.getTargetPath(),
                -1,
                combinedOutput
        );
    }

    /**
     * Analyzes compiler output to extract file, line number, and javac error classification.
     *
     * @param output compiler standard output/error
     * @return compilation diagnostic
     */
    private DiagnosticResult diagnoseCompilationError(String output) {
        Matcher matcher = JAVAC_ERROR_PATTERN.matcher(output);
        if (matcher.find()) {
            String file = matcher.group(1).trim();
            int line = Integer.parseInt(matcher.group(2));
            String errorMsg = matcher.group(3).trim();

            String suggestedFix = categorizeCompilerError(errorMsg);

            return new DiagnosticResult(
                    true,
                    "Compilation Error at line " + line + ": " + errorMsg,
                    suggestedFix,
                    file,
                    line,
                    output
            );
        }

        return new DiagnosticResult(
                true,
                "Compilation failure without standard javac pattern: " + output.lines().findFirst().orElse("Unknown error"),
                "Verify file paths, imports, and package declarations.",
                "",
                -1,
                output
        );
    }

    /**
     * Analyzes runtime stacktraces and assertion failures from test executions.
     *
     * @param output test run standard output/error
     * @return runtime diagnostic
     */
    private DiagnosticResult diagnoseRuntimeError(String output) {
        String exceptionType = "RuntimeException";
        String exceptionDetail = "Uncaught exception or assertion failure";

        Matcher excMatcher = EXCEPTION_HEADER_PATTERN.matcher(output);
        if (excMatcher.find()) {
            exceptionType = excMatcher.group(1);
            if (excMatcher.group(2) != null) {
                exceptionDetail = excMatcher.group(2).trim();
            }
        }

        Matcher stackMatcher = STACKTRACE_PATTERN.matcher(output);
        String targetFile = "";
        int line = -1;
        if (stackMatcher.find()) {
            targetFile = stackMatcher.group(3);
            line = Integer.parseInt(stackMatcher.group(4));
        }

        String remedy = "Handle exception [" + exceptionType + "] and fix boundary condition: " + exceptionDetail;

        return new DiagnosticResult(
                true,
                "Runtime Failure [" + exceptionType + "]: " + exceptionDetail,
                remedy,
                targetFile,
                line,
                output
        );
    }

    /**
     * Classifies typical Java compiler errors into actionable remedies.
     *
     * @param errorMessage raw error message from javac
     * @return actionable fix suggestion
     */
    private String categorizeCompilerError(String errorMessage) {
        if (errorMessage.contains("cannot find symbol")) {
            return "Check missing import, typo in method/variable identifier, or missing dependency.";
        }
        if (errorMessage.contains("';' expected")) {
            return "Insert missing semicolon ';' at the end of the statement.";
        }
        if (errorMessage.contains("incompatible types")) {
            return "Correct variable type declaration or add explicit casting / conversion.";
        }
        if (errorMessage.contains("unreported exception")) {
            return "Surround with try-catch block or declare exception in method 'throws' signature.";
        }
        if (errorMessage.contains("reached end of file while parsing")) {
            return "Close all unclosed curly braces, parentheses, or string literals.";
        }
        return "Review syntax and method contract around the error line.";
    }

    /**
     * Applies an automated string correction strategy to source code based on diagnosis.
     *
     * @param sourceCode original source code string, must not be null
     * @param diagnostic diagnosis containing root cause and error line, must not be null
     * @return patched source code string
     * @throws NullPointerException if any parameter is null
     */
    public String applyFix(String sourceCode, DiagnosticResult diagnostic) {
        Objects.requireNonNull(sourceCode, "sourceCode must not be null");
        Objects.requireNonNull(diagnostic, "diagnostic must not be null");

        if (diagnostic.getLineNumber() <= 0) {
            return sourceCode;
        }

        String[] lines = sourceCode.split("\\R");
        int targetIdx = diagnostic.getLineNumber() - 1;

        if (targetIdx < lines.length) {
            String offendingLine = lines[targetIdx];
            if (diagnostic.getRootCause().contains("';' expected") && !offendingLine.trim().endsWith(";")) {
                lines[targetIdx] = offendingLine + ";";
                return String.join(System.lineSeparator(), lines);
            }
        }

        return sourceCode;
    }
}
