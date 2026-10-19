package com.agent.workspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Contract for managing workspace files, directories, and code artifacts.
 *
 * <p>Provides sandboxed file system operations, preventing unintended access
 * outside the designated project root directory.</p>
 */
public interface FileManager {

    /**
     * Creates or overwrites a file relative to the root directory with specified content.
     *
     * @param relativePath the target path relative to the workspace root, must not be null
     * @param content      the string content to write, must not be null
     * @return the absolute {@link Path} of the created file
     * @throws IOException              if writing fails or parent directories cannot be created
     * @throws IllegalArgumentException if the path attempts directory traversal outside root
     * @throws NullPointerException     if any parameter is null
     */
    Path createFile(Path relativePath, String content) throws IOException;

    /**
     * Reads the entire string content of a file relative to the workspace root.
     *
     * @param relativePath the target path relative to the workspace root, must not be null
     * @return string content of the file
     * @throws IOException              if reading fails or file is not found
     * @throws IllegalArgumentException if the path attempts directory traversal outside root
     * @throws NullPointerException     if parameter is null
     */
    String readFile(Path relativePath) throws IOException;

    /**
     * Verifies whether a given path exists within the workspace.
     *
     * @param relativePath the target path relative to the workspace root, must not be null
     * @return {@code true} if the file or directory exists; {@code false} otherwise
     * @throws IllegalArgumentException if the path attempts directory traversal outside root
     * @throws NullPointerException     if parameter is null
     */
    boolean exists(Path relativePath);

    /**
     * Deletes a file or empty directory relative to the workspace root.
     *
     * @param relativePath the target path relative to the workspace root, must not be null
     * @throws IOException              if deletion fails
     * @throws IllegalArgumentException if the path attempts directory traversal outside root
     * @throws NullPointerException     if parameter is null
     */
    void delete(Path relativePath) throws IOException;

    /**
     * Recursively or shallowly lists all regular files within a relative directory.
     *
     * @param relativePath the sub-directory path to list, must not be null
     * @return list of relative {@link Path} instances found
     * @throws IOException              if directory inspection fails
     * @throws IllegalArgumentException if the path attempts directory traversal outside root
     * @throws NullPointerException     if parameter is null
     */
    List<Path> listFiles(Path relativePath) throws IOException;

    /**
     * Retrieves the absolute root directory of this workspace manager.
     *
     * @return absolute root {@link Path}
     */
    Path getRootDirectory();

    /**
     * Resolves and normalizes a relative path against the workspace root with boundary checks.
     *
     * @param relativePath the relative path to resolve, must not be null
     * @return normalized absolute {@link Path}
     * @throws IllegalArgumentException if the resolved path falls outside the root directory
     * @throws NullPointerException     if parameter is null
     */
    Path resolve(Path relativePath);
}
