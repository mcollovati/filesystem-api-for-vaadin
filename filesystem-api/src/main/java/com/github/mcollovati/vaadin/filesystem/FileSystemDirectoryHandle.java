/*-
 * Copyright 2026 Marco Collovati
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.mcollovati.vaadin.filesystem;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A handle representing a directory in the browser's file system.
 *
 * <p>This is the Java counterpart of the browser's
 * {@code FileSystemDirectoryHandle} interface. Instances are obtained
 * through {@link FileSystemAPI#openDirectory()} or through
 * {@link #getDirectoryHandle(String, GetHandleOptions)}.
 *
 * @see FileSystemHandle
 * @see FileSystemFileHandle
 */
public final class FileSystemDirectoryHandle extends AbstractFileSystemHandle {

    FileSystemDirectoryHandle(String handleId, String name, JsBridge bridge) {
        super(handleId, name, bridge);
    }

    @Override
    public HandleKind getKind() {
        return HandleKind.DIRECTORY;
    }

    @Override
    public String toString() {
        return "FileSystemDirectoryHandle{name='" + name() + "'}";
    }

    /**
     * Returns a handle for a file with the given name in this directory.
     *
     * @param name the file name
     * @return a future that completes with the file handle
     * @see #getFileHandle(String, GetHandleOptions)
     */
    public CompletableFuture<FileSystemFileHandle> getFileHandle(String name) {
        return getFileHandle(name, GetHandleOptions.builder().build());
    }

    /**
     * Returns a handle for a file with the given name in this directory.
     *
     * <p>If the {@code create} option is {@code true} and the file does
     * not exist, it is created.
     *
     * @param name    the file name
     * @param options the options
     * @return a future that completes with the file handle
     */
    public CompletableFuture<FileSystemFileHandle> getFileHandle(String name, GetHandleOptions options) {
        return bridge().getFileHandle(handleId(), name, options);
    }

    /**
     * Returns a handle for a subdirectory with the given name.
     *
     * @param name the directory name
     * @return a future that completes with the directory handle
     * @see #getDirectoryHandle(String, GetHandleOptions)
     */
    public CompletableFuture<FileSystemDirectoryHandle> getDirectoryHandle(String name) {
        return getDirectoryHandle(name, GetHandleOptions.builder().build());
    }

    /**
     * Returns a handle for a subdirectory with the given name.
     *
     * <p>If the {@code create} option is {@code true} and the directory
     * does not exist, it is created.
     *
     * @param name    the directory name
     * @param options the options
     * @return a future that completes with the directory handle
     */
    public CompletableFuture<FileSystemDirectoryHandle> getDirectoryHandle(String name, GetHandleOptions options) {
        return bridge().getDirectoryHandle(handleId(), name, options);
    }

    /**
     * Removes a file or directory entry from this directory.
     *
     * @param name the entry name to remove
     * @return a future that completes when the entry is removed
     * @see #removeEntry(String, RemoveEntryOptions)
     */
    public CompletableFuture<Void> removeEntry(String name) {
        return removeEntry(name, RemoveEntryOptions.builder().build());
    }

    /**
     * Removes a file or directory entry from this directory.
     *
     * <p>If the {@code recursive} option is {@code true} and the entry
     * is a directory, its contents are removed recursively.
     *
     * @param name    the entry name to remove
     * @param options the options
     * @return a future that completes when the entry is removed
     */
    public CompletableFuture<Void> removeEntry(String name, RemoveEntryOptions options) {
        return bridge().removeEntry(handleId(), name, options);
    }

    /**
     * Resolves the path from this directory to the given handle.
     *
     * <p>If the handle is not a descendant of this directory, the
     * returned optional is empty.
     *
     * @param handle the handle to resolve
     * @return a future that completes with the path segments, or empty
     *         if the handle is not within this directory
     */
    public CompletableFuture<Optional<List<String>>> resolve(FileSystemHandle handle) {
        return bridge().resolve(handleId(), bridge().getHandleId(handle));
    }

    /**
     * Returns all entries (files and subdirectories) in this directory.
     *
     * <p>Each returned handle is registered in the client-side registry
     * and can be used for further operations.
     *
     * @return a future that completes with the list of child handles
     */
    public CompletableFuture<List<FileSystemHandle>> entries() {
        return bridge().entries(handleId());
    }
}
