package com.agent.healing;

import java.util.Objects;

/**
 * Encapsulates the diagnostic outcome produced by analyzing a failed command execution.
 *
 * <p>Identifies the failure classification, target source file, line number,
 * root cause analysis, and suggested remedy.</p>
 */
public final class DiagnosticResult {

    private final boolean healable;
    private final String rootCause;
    private final String suggestedFix;
    private final String targetFile;
    private final int lineNumber;
    private final String rawError;

    /**
     * Constructs a new {@code DiagnosticResult}.
     *
     * @param healable     whether the identified issue is eligible for automatic self-healing
     * @param rootCause    detailed technical analysis of why the failure occurred, must not be null
     * @param suggestedFix recommended modification or repair strategy, must not be null
     * @param targetFile   relative or absolute path to the faulty source file, can be empty or null
     * @param lineNumber   approximate line number of the failure (or -1 if unavailable)
     * @param rawError     the raw stderr or compiler output captured, must not be null
     * @throws NullPointerException if {@code rootCause}, {@code suggestedFix}, or {@code rawError} is null
     */
    public DiagnosticResult(boolean healable,
                            String rootCause,
                            String suggestedFix,
                            String targetFile,
                            int lineNumber,
                            String rawError) {
        this.healable = healable;
        this.rootCause = Objects.requireNonNull(rootCause, "rootCause must not be null");
        this.suggestedFix = Objects.requireNonNull(suggestedFix, "suggestedFix must not be null");
        this.targetFile = targetFile != null ? targetFile : "";
        this.lineNumber = lineNumber;
        this.rawError = Objects.requireNonNull(rawError, "rawError must not be null");
    }

    /**
     * Checks if this diagnosis provides an actionable self-healing patch.
     *
     * @return {@code true} if self-healing can be attempted; {@code false} otherwise
     */
    public boolean isHealable() {
        return healable;
    }

    /**
     * Retrieves the root cause diagnosis.
     *
     * @return explanation string
     */
    public String getRootCause() {
        return rootCause;
    }

    /**
     * Retrieves the suggested fix or remedy instructions.
     *
     * @return fix recommendation
     */
    public String getSuggestedFix() {
        return suggestedFix;
    }

    /**
     * Retrieves the identified source file where the error occurred.
     *
     * @return file path string or empty
     */
    public String getTargetFile() {
        return targetFile;
    }

    /**
     * Retrieves the line number associated with the error.
     *
     * @return positive line number, or -1 if unknown
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Retrieves the verbatim raw compiler or runtime error stream.
     *
     * @return raw error string
     */
    public String getRawError() {
        return rawError;
    }

    @Override
    public String toString() {
        return "DiagnosticResult{" +
                "healable=" + healable +
                ", targetFile='" + targetFile + '\'' +
                ", line=" + lineNumber +
                ", rootCause='" + rootCause + '\'' +
                '}';
    }
}
