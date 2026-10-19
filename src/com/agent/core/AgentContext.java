package com.agent.core;

import com.agent.execution.CommandExecutor;
import com.agent.execution.ProcessCommandExecutor;
import com.agent.healing.SelfHealingEngine;
import com.agent.planner.DefaultTaskPlanner;
import com.agent.planner.TaskPlanner;
import com.agent.workspace.FileManager;
import com.agent.workspace.WorkspaceManager;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * Thread-safe configuration and dependency injection context for the autonomous agent.
 *
 * <p>Holds references to workspace management, system command execution, task planning,
 * and self-healing engine facilities, along with operational timeouts and retry limits.</p>
 */
public final class AgentContext {

    private final FileManager fileManager;
    private final CommandExecutor commandExecutor;
    private final TaskPlanner taskPlanner;
    private final SelfHealingEngine healingEngine;
    private final int maxHealingAttempts;
    private final Duration commandTimeout;

    /**
     * Constructs a fully initialized {@code AgentContext}.
     *
     * @param fileManager         sandboxed workspace file manager, must not be null
     * @param commandExecutor     command execution facility, must not be null
     * @param taskPlanner         planning strategy implementation, must not be null
     * @param healingEngine       self-healing diagnostic engine, must not be null
     * @param maxHealingAttempts  maximum consecutive healing attempts allowed per step, must be positive
     * @param commandTimeout      maximum execution duration per command, must not be null
     * @throws IllegalArgumentException if {@code maxHealingAttempts <= 0}
     * @throws NullPointerException     if any object parameter is null
     */
    public AgentContext(FileManager fileManager,
                        CommandExecutor commandExecutor,
                        TaskPlanner taskPlanner,
                        SelfHealingEngine healingEngine,
                        int maxHealingAttempts,
                        Duration commandTimeout) {
        this.fileManager = Objects.requireNonNull(fileManager, "fileManager must not be null");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor must not be null");
        this.taskPlanner = Objects.requireNonNull(taskPlanner, "taskPlanner must not be null");
        this.healingEngine = Objects.requireNonNull(healingEngine, "healingEngine must not be null");
        if (maxHealingAttempts <= 0) {
            throw new IllegalArgumentException("maxHealingAttempts must be greater than zero, given: " + maxHealingAttempts);
        }
        this.maxHealingAttempts = maxHealingAttempts;
        this.commandTimeout = Objects.requireNonNull(commandTimeout, "commandTimeout must not be null");
    }

    /**
     * Factory method creating a default production context rooted at a specified directory.
     *
     * @param workspaceRoot root directory path for the agent workspace, must not be null
     * @return configured {@code AgentContext} instance
     * @throws NullPointerException if {@code workspaceRoot} is null
     */
    public static AgentContext createDefault(Path workspaceRoot) {
        return new AgentContext(
                new WorkspaceManager(workspaceRoot),
                new ProcessCommandExecutor(),
                new DefaultTaskPlanner(),
                new SelfHealingEngine(),
                3,
                Duration.ofSeconds(30)
        );
    }

    /**
     * Retrieves the file manager component.
     *
     * @return {@link FileManager} instance
     */
    public FileManager getFileManager() {
        return fileManager;
    }

    /**
     * Retrieves the command executor component.
     *
     * @return {@link CommandExecutor} instance
     */
    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    /**
     * Retrieves the planning strategy component.
     *
     * @return {@link TaskPlanner} instance
     */
    public TaskPlanner getTaskPlanner() {
        return taskPlanner;
    }

    /**
     * Retrieves the self-healing diagnostic engine.
     *
     * @return {@link SelfHealingEngine} instance
     */
    public SelfHealingEngine getHealingEngine() {
        return healingEngine;
    }

    /**
     * Retrieves the configured maximum healing attempts.
     *
     * @return max retry limit
     */
    public int getMaxHealingAttempts() {
        return maxHealingAttempts;
    }

    /**
     * Retrieves the command timeout duration.
     *
     * @return {@link Duration} timeout
     */
    public Duration getCommandTimeout() {
        return commandTimeout;
    }
}
