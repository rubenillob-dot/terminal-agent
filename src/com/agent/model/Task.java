package com.agent.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable domain representation of a technical task assigned to the autonomous agent.
 *
 * <p>Encapsulates an identifier, functional description, contextual metadata,
 * and registration timestamp required for autonomous resolution.</p>
 */
public final class Task {

    private final String id;
    private final String description;
    private final Map<String, Object> contextData;
    private final Instant createdAt;

    /**
     * Constructs a new {@code Task} instance with immutable properties.
     *
     * @param id          unique identifier for the task, must not be null or blank
     * @param description human-readable problem statement or technical requirements, must not be null or blank
     * @param contextData contextual key-value properties or parameters, must not be null
     * @throws IllegalArgumentException if {@code id} or {@code description} is null or blank
     * @throws NullPointerException     if {@code contextData} is null
     */
    public Task(String id, String description, Map<String, Object> contextData) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Task id must not be null or blank.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Task description must not be null or blank.");
        }
        this.id = id;
        this.description = description;
        this.contextData = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(contextData, "contextData cannot be null")));
        this.createdAt = Instant.now();
    }

    /**
     * Retrieves the unique identifier of the task.
     *
     * @return the unique task identifier string
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves the problem statement or technical description of the task.
     *
     * @return the description text
     */
    public String getDescription() {
        return description;
    }

    /**
     * Retrieves an unmodifiable view of contextual metadata associated with this task.
     *
     * @return unmodifiable map of context properties
     */
    public Map<String, Object> getContextData() {
        return contextData;
    }

    /**
     * Retrieves the timestamp when this task was created.
     *
     * @return creation {@link Instant}
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
