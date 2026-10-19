package com.agent.workspace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Sandboxed file manager implementation that confines file operations strictly
 * within a configured root workspace directory.
 *
 * <p>Prevents directory traversal attacks using path normalization and prefix checks,
 * while managing directories and source artifacts cleanly.</p>
 */
public class WorkspaceManager implements FileManager {

    private final Path rootDirectory;

    /**
     * Constructs a {@code WorkspaceManager} anchored at the given root directory.
     *
     * @param rootDirectory the directory representing the workspace root, must not be null
     * @throws IllegalArgumentException if {@code rootDirectory} cannot be resolved or does not exist
     * @throws NullPointerException     if {@code rootDirectory} is null
     */
    public WorkspaceManager(Path rootDirectory) {
        Objects.requireNonNull(rootDirectory, "rootDirectory must not be null");
        this.rootDirectory = rootDirectory.toAbsolutePath().normalize();
        if (!Files.exists(this.rootDirectory)) {
            try {
                Files.createDirectories(this.rootDirectory);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to initialize workspace root: " + rootDirectory, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param relativePath the target path relative to the workspace root, must not be null
     * @param content      the string content to write, must not be null
     * @return the absolute {@link Path} of the created file
     * @throws IOException              if writing fails or parent directories cannot be created
     * @throws IllegalArgumentException if the path attempts directory traversal outside root
     * @throws NullPointerException     if any parameter is null
     */
    @Override
    public Path createFile(Path relativePath, String content) throws IOException {
        Objects.requireNonNull(content, "content must not be null");
        Path resolved = resolve(relativePath);

        Path parent = resolved.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Files.writeString(
                resolved,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
        return resolved;
    }

    /**
     * {@inheritDoc}
     *
     * @param relativePath the target path relative to the workspace root, must not be null
     * @return string content of the file
     * @throws IOException              if reading fails or file is not found
     * @throws IllegalArgumentException if the path attempts directory traversal outside root
     * @throws NullPointerException     if parameter is null
     */
    @Override
    public String readFile(Path relativePath) throws IOException {
        Path resolved = resolve(relativePath);
        if (!Files.exists(resolved)) {
            throw new IOException("File does not exist: " + relativePath);
        }
        return Files.readString(resolved, StandardCharsets.UTF_8);
    }

    /**
     * {@inheritDoc}
     *
     * @param relativePath the target path relative to the workspace root, must not be null
     * @return {@code true} if the file or directory exists; {@code false} otherwise
     * @throws IllegalArgumentException if the path attempts directory traversal outside root
     * @throws NullPointerException     if parameter is null
     */
    @Override
    public boolean exists(Path relativePath) {
        Path resolved = resolve(relativePath);
        return Files.exists(resolved);
    }

    /**
     * {@inheritDoc}
     *
     * @param relativePath the target path relative to the workspace root, must not be null
     * @throws IOException              if deletion fails
     * @throws IllegalArgumentException if the path attempts directory traversal outside root
     * @throws NullPointerException     if parameter is null
     */
    @Override
    public void delete(Path relativePath) throws IOException {
        Path resolved = resolve(relativePath);
        Files.deleteIfExists(resolved);
    }

    /**
     * {@inheritDoc}
     *
     * @param relativePath the sub-directory path to list, must not be null
     * @return list of relative {@link Path} instances found
     * @throws IOException              if directory inspection fails
     * @throws IllegalArgumentException if the path attempts directory traversal outside root
     * @throws NullPointerException     if parameter is null
     */
    @Override
    public List<Path> listFiles(Path relativePath) throws IOException {
        Path resolved = resolve(relativePath);
        if (!Files.exists(resolved)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(resolved)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(rootDirectory::relativize)
                    .toList();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return absolute root {@link Path}
     */
    @Override
    public Path getRootDirectory() {
        return rootDirectory;
    }

    /**
     * {@inheritDoc}
     *
     * @param relativePath the relative path to resolve, must not be null
     * @return normalized absolute {@link Path}
     * @throws IllegalArgumentException if the resolved path falls outside the root directory
     * @throws NullPointerException     if parameter is null
     */
    @Override
    public Path resolve(Path relativePath) {
        Objects.requireNonNull(relativePath, "relativePath must not be null");
        Path resolved = rootDirectory.resolve(relativePath).normalize().toAbsolutePath();
        if (!resolved.startsWith(rootDirectory)) {
            throw new IllegalArgumentException(
                    "Path traversal violation: Target path '" + resolved + "' is outside root '" + rootDirectory + "'"
            );
        }
        return resolved;
    }
}
