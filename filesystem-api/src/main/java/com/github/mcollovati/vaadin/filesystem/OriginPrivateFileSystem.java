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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Simplified API for the browser's
 * <a href="https://developer.mozilla.org/en-US/docs/Web/API/File_System_API#origin_private_file_system">Origin
 * Private File System</a> (OPFS).
 *
 * <p>OPFS is a sandboxed file system private to the page's origin, accessed
 * via
 * <a href="https://developer.mozilla.org/en-US/docs/Web/API/StorageManager/getDirectory">{@code navigator.storage.getDirectory()}</a>.
 * Unlike the picker methods on {@link ClientFileSystem}, OPFS does not show
 * dialogs and does not require user interaction. It also has broader
 * browser support (Firefox and Safari support OPFS but not the picker API).
 *
 * <p>This class provides path-based convenience methods that eliminate the
 * handle-by-handle chaining typically needed when working with the File
 * System API. Paths use {@code /} as separator; intermediate directories
 * are created automatically for write operations.
 *
 * <p>Each method executes as a single client-server roundtrip — path
 * navigation happens entirely in the browser.
 *
 * <pre>{@code
 * var opfs = new OriginPrivateFileSystem(myView);
 *
 * // Write a file (creates intermediate directories)
 * opfs.writeFile("a/b/config.json", "{\"key\": true}");
 *
 * // Read it back
 * opfs.readFile("a/b/config.json").thenAccept(data ->
 *     log(new String(data.getContent())));
 *
 * // List root entries
 * opfs.list().thenAccept(entries ->
 *     entries.forEach(e -> log(e.getName())));
 * }</pre>
 *
 * <p><b>When to use this vs {@link ClientFileSystem}:</b>
 * <ul>
 *   <li>Use {@link ClientFileSystem} when the user picks files or directories
 *       via native OS dialogs (open, save, browse). Works only in Chromium.
 *   <li>Use {@code OriginPrivateFileSystem} for programmatic app-managed
 *       storage without user prompts. Broader browser support. Persistent
 *       sandboxed storage. Path-based convenience.
 * </ul>
 *
 * @see ClientFileSystem
 * @see FileSystemDirectoryHandle
 */
public final class OriginPrivateFileSystem implements Serializable {

    private final Component component;
    private transient volatile CompletableFuture<FileSystemDirectoryHandle> cachedRoot;

    /**
     * Creates a new instance bound to the given component.
     *
     * @param component the component to bind to, not {@code null}
     */
    public OriginPrivateFileSystem(Component component) {
        this.component = Objects.requireNonNull(component, "component must not be null");
    }

    // -- Root access (cached) --

    /**
     * Returns a handle to the OPFS root directory.
     *
     * <p>The result is cached so that concurrent and subsequent calls share
     * the same in-flight request. The cache is cleared on failure so that
     * the next call retries.
     *
     * @return a future that completes with the OPFS root directory handle
     */
    public CompletableFuture<FileSystemDirectoryHandle> root() {
        CompletableFuture<FileSystemDirectoryHandle> result = cachedRoot;
        if (result != null) {
            return result;
        }
        synchronized (this) {
            if (cachedRoot != null) {
                return cachedRoot;
            }
            CompletableFuture<FileSystemDirectoryHandle> future = getBridge().getOriginPrivateDirectory();
            cachedRoot = future;
            future.whenComplete((handle, error) -> {
                if (error != null) {
                    synchronized (this) {
                        cachedRoot = null;
                    }
                }
            });
            return future;
        }
    }

    // -- Path-based handle access --

    /**
     * Returns a file handle at the given path relative to the OPFS root.
     *
     * <p>The path is split on {@code /}. Intermediate directories must
     * already exist; use {@link #getFileHandle(String, GetHandleOptions)}
     * with {@link GetHandleOptions#creating()} to create them.
     *
     * @param path the {@code /}-separated path to the file
     * @return a future that completes with the file handle
     */
    public CompletableFuture<FileSystemFileHandle> getFileHandle(String path) {
        return getFileHandle(path, GetHandleOptions.builder().build());
    }

    /**
     * Returns a file handle at the given path relative to the OPFS root.
     *
     * <p>The path is split on {@code /}. When the {@code create} option is
     * {@code true}, intermediate directories are also created.
     *
     * @param path    the {@code /}-separated path to the file
     * @param options the options (e.g. {@link GetHandleOptions#creating()})
     * @return a future that completes with the file handle
     */
    public CompletableFuture<FileSystemFileHandle> getFileHandle(String path, GetHandleOptions options) {
        Objects.requireNonNull(options, "options must not be null");
        return getBridge().opfsGetFileHandle(normalizePath(path), options);
    }

    /**
     * Returns a directory handle at the given path relative to the OPFS root.
     *
     * <p>The path is split on {@code /}. Intermediate directories must
     * already exist; use
     * {@link #getDirectoryHandle(String, GetHandleOptions)} with
     * {@link GetHandleOptions#creating()} to create them.
     *
     * @param path the {@code /}-separated path to the directory
     * @return a future that completes with the directory handle
     */
    public CompletableFuture<FileSystemDirectoryHandle> getDirectoryHandle(String path) {
        return getDirectoryHandle(path, GetHandleOptions.builder().build());
    }

    /**
     * Returns a directory handle at the given path relative to the OPFS root.
     *
     * <p>The path is split on {@code /}. When the {@code create} option is
     * {@code true}, intermediate directories are also created.
     *
     * @param path    the {@code /}-separated path to the directory
     * @param options the options (e.g. {@link GetHandleOptions#creating()})
     * @return a future that completes with the directory handle
     */
    public CompletableFuture<FileSystemDirectoryHandle> getDirectoryHandle(String path, GetHandleOptions options) {
        Objects.requireNonNull(options, "options must not be null");
        return getBridge().opfsGetDirectoryHandle(normalizePath(path), options);
    }

    // -- Direct read/write --

    /**
     * Reads a file at the given path.
     *
     * <p>The entire file is read into memory and transferred to the server
     * via base64 encoding. For large files, prefer
     * {@link #uploadFile(String, UploadHandler)}.
     *
     * @param path the {@code /}-separated path to the file
     * @return a future that completes with the file data
     */
    public CompletableFuture<FileData> readFile(String path) {
        return getBridge().opfsReadFile(normalizePath(path));
    }

    /**
     * Writes text to a file at the given path, creating intermediate
     * directories and the file itself if they do not exist.
     *
     * @param path the {@code /}-separated path to the file
     * @param text the text to write
     * @return a future that completes when the write is done
     */
    public CompletableFuture<Void> writeFile(String path, String text) {
        return getBridge().opfsWriteText(normalizePath(path), text);
    }

    /**
     * Writes binary data to a file at the given path, creating intermediate
     * directories and the file itself if they do not exist.
     *
     * @param path the {@code /}-separated path to the file
     * @param data the bytes to write
     * @return a future that completes when the write is done
     */
    public CompletableFuture<Void> writeFile(String path, byte[] data) {
        return getBridge().opfsWriteBytes(normalizePath(path), data);
    }

    // -- Streaming --

    /**
     * Uploads a file from OPFS to the server using the given handler.
     *
     * <p>This transfers the file via HTTP streaming (multipart upload),
     * which is more efficient than {@link #readFile(String)} for large files.
     *
     * <p>This method requires two roundtrips: one to resolve the file
     * handle and one for the HTTP transfer.
     *
     * @param path    the {@code /}-separated path to the file
     * @param handler the upload handler to receive the file content,
     *                not {@code null}
     * @return a future that completes when the upload is finished
     */
    public CompletableFuture<Void> uploadFile(String path, UploadHandler handler) {
        Objects.requireNonNull(handler, "handler must not be null");
        return getFileHandle(path).thenCompose(file -> file.uploadTo(handler));
    }

    /**
     * Downloads content from the server into an OPFS file using the given
     * handler, creating intermediate directories and the file itself if
     * they do not exist.
     *
     * <p>This transfers data via HTTP streaming, which is more efficient
     * than {@link #writeFile(String, byte[])} for large payloads.
     *
     * <p>This method requires two roundtrips: one to resolve the file
     * handle and one for the HTTP transfer.
     *
     * @param path    the {@code /}-separated path to the file
     * @param handler the download handler providing the content to write,
     *                not {@code null}
     * @return a future that completes when the download and write are
     *         finished
     */
    public CompletableFuture<Void> downloadFile(String path, DownloadHandler handler) {
        Objects.requireNonNull(handler, "handler must not be null");
        return getFileHandle(path, GetHandleOptions.creating()).thenCompose(file -> file.downloadFrom(handler));
    }

    // -- Save to device --

    /**
     * Triggers a browser download of a file from OPFS to the user's device.
     *
     * <p>The leaf filename from the path is used as the download filename.
     *
     * @param path the {@code /}-separated path to the file
     * @return a future that completes when the download is triggered
     * @see #saveToDevice(String, String)
     */
    public CompletableFuture<Void> saveToDevice(String path) {
        return saveToDevice(path, null);
    }

    /**
     * Triggers a browser download of a file from OPFS to the user's device.
     *
     * @param path         the {@code /}-separated path to the file
     * @param downloadName custom filename for the browser download,
     *                     or {@code null} to use the leaf filename
     * @return a future that completes when the download is triggered
     */
    public CompletableFuture<Void> saveToDevice(String path, String downloadName) {
        return getBridge().opfsSaveToDevice(normalizePath(path), downloadName);
    }

    // -- Directory listing --

    /**
     * Lists all entries in the OPFS root directory.
     *
     * @return a future that completes with the list of handles
     */
    public CompletableFuture<List<FileSystemHandle>> list() {
        return getBridge().opfsEntries("");
    }

    /**
     * Lists all entries in the directory at the given path.
     *
     * @param path the {@code /}-separated path to the directory
     * @return a future that completes with the list of handles
     */
    public CompletableFuture<List<FileSystemHandle>> list(String path) {
        return getBridge().opfsEntries(normalizePath(path));
    }

    // -- Removal --

    /**
     * Removes a file or directory entry at the given path.
     *
     * <p>Only the leaf entry is removed. To remove a non-empty directory,
     * use {@link #removeEntry(String, RemoveEntryOptions)} with
     * {@link RemoveEntryOptions#recursively()}.
     *
     * @param path the {@code /}-separated path to the entry
     * @return a future that completes when the entry is removed
     */
    public CompletableFuture<Void> removeEntry(String path) {
        return removeEntry(path, RemoveEntryOptions.builder().build());
    }

    /**
     * Removes a file or directory entry at the given path.
     *
     * @param path    the {@code /}-separated path to the entry
     * @param options the options (e.g. {@link RemoveEntryOptions#recursively()})
     * @return a future that completes when the entry is removed
     */
    public CompletableFuture<Void> removeEntry(String path, RemoveEntryOptions options) {
        Objects.requireNonNull(options, "options must not be null");
        return getBridge().opfsRemoveEntry(normalizePath(path), options);
    }

    /**
     * Removes all entries from the OPFS root directory.
     *
     * @return a future that completes when all entries are removed
     */
    public CompletableFuture<Void> clear() {
        return getBridge().opfsClear();
    }

    // -- Internal helpers --

    private String normalizePath(String path) {
        Objects.requireNonNull(path, "path must not be null");
        String trimmed = path.replaceAll("^/+|/+$", "");
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("path must not be empty");
        }
        return trimmed;
    }

    JsBridge getBridge() {
        return JsBridge.getForComponent(component);
    }
}
