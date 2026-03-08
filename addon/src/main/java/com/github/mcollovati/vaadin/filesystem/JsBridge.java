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
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.Registration;
import java.io.Serializable;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;
import tools.jackson.core.type.TypeReference;

/**
 * Internal bridge between Java and browser JavaScript for File System API
 * operations.
 *
 * <p>Manages a client-side handle registry stored on the host component's
 * DOM element and provides methods to execute JS operations on handles.
 * One instance exists per component, obtained via
 * {@link #getForComponent(Component)}.
 */
class JsBridge implements Serializable {

    /**
     * JS snippet that copies the options parameter ($0) and resolves
     * {@code startIn} if it refers to a registered handle.
     */
    private static final String RESOLVE_START_IN =
            """
            const opts = Object.assign({}, $0);
            if (opts.startIn && this.__fsApiHandles.has(opts.startIn)) {
                opts.startIn = this.__fsApiHandles.get(opts.startIn);
            }""";

    /**
     * JS snippet that registers an array of handles into the registry and
     * returns their metadata. Expects a {@code handles} variable to be in
     * scope.
     */
    private static final String REGISTER_HANDLES =
            """
            return handles.map(h => {
                const id = String(this.__fsApiNextId++);
                this.__fsApiHandles.set(id, h);
                return {id: id, name: h.name, kind: h.kind};
            });""";

    /**
     * JS template that wraps an expression in a try-catch block. Catches
     * {@code DOMException} errors and re-throws them with a structured
     * message containing the error name, allowing server-side mapping to
     * specific Java exception types. Use with {@link String#formatted}.
     */
    private static final String JS_TRY_CATCH =
            """
            try {
            %s
            } catch(__e) {
                throw new Error((__e.name || 'Error') + ': ' + __e.message);
            }""";

    /**
     * Returns a JS snippet that retrieves a handle from the registry and
     * throws a descriptive {@code NotFoundError} if the handle is missing
     * (e.g. after {@code release()} was called).
     */
    private static String requireHandle(String varName, String paramRef) {
        return """
                const %s = this.__fsApiHandles.get(%s);
                if (!%s) throw new DOMException('Handle not found (released or invalid)', 'NotFoundError');
                """
                .formatted(varName, paramRef, varName);
    }

    /**
     * Returns a JS snippet that retrieves a writable stream from the
     * registry and throws a descriptive {@code NotFoundError} if the
     * stream is missing (e.g. after it was closed).
     */
    private static String requireWritable(String varName, String paramRef) {
        return """
                const %s = this.__fsApiWritables.get(%s);
                if (!%s) throw new DOMException('Writable stream not found (closed or invalid)', 'NotFoundError');
                """
                .formatted(varName, paramRef, varName);
    }

    /**
     * Maps JS DOMException names to Java exception factory methods.
     */
    private static final Map<String, ExceptionFactory> ERROR_MAP = Map.of(
            "NotFoundError", FileSystemNotFoundException::new,
            "NotAllowedError", FileSystemNotAllowedException::new,
            "AbortError", FileSystemNotAllowedException::new,
            "TypeMismatchError", FileSystemTypeMismatchException::new,
            "SecurityError", FileSystemNotAllowedException::new);

    private final Component component;
    boolean initialized;
    private Registration detachRegistration;

    JsBridge(Component component) {
        this.component = component;
    }

    /**
     * Returns the bridge instance for the given component, creating one if
     * it does not yet exist.
     *
     * @param component the host component
     * @return the bridge for the component
     */
    static JsBridge getForComponent(Component component) {
        JsBridge bridge = ComponentUtil.getData(component, JsBridge.class);
        if (bridge == null) {
            bridge = new JsBridge(component);
            ComponentUtil.setData(component, JsBridge.class, bridge);
        }
        return bridge;
    }

    PendingJavaScriptResult executeJs(String expression, Object... params) {
        ensureInitialized();
        return element().executeJs(expression, params);
    }

    CompletableFuture<Void> executeVoidJs(String expression, Object... params) {
        return mapErrors(
                executeJs(expression, params).toCompletableFuture(String.class).thenApply(ignored -> null));
    }

    CompletableFuture<Boolean> isSameEntry(String handleId1, String handleId2) {
        return mapErrors(executeJs(
                        JS_TRY_CATCH.formatted(requireHandle("h1", "$0")
                                + requireHandle("h2", "$1")
                                + "return await h1.isSameEntry(h2);"),
                        handleId1,
                        handleId2)
                .toCompletableFuture(Boolean.class));
    }

    CompletableFuture<PermissionState> queryPermission(String handleId, PermissionMode mode) {
        return mapErrors(executeJs(
                        JS_TRY_CATCH.formatted(
                                requireHandle("h", "$0") + "return await h.queryPermission({mode: $1});"),
                        handleId,
                        mode.getJsValue())
                .toCompletableFuture(String.class)
                .thenApply(PermissionState::fromJsValue));
    }

    CompletableFuture<PermissionState> requestPermission(String handleId, PermissionMode mode) {
        return mapErrors(executeJs(
                        JS_TRY_CATCH.formatted(
                                requireHandle("h", "$0") + "return await h.requestPermission({mode: $1});"),
                        handleId,
                        mode.getJsValue())
                .toCompletableFuture(String.class)
                .thenApply(PermissionState::fromJsValue));
    }

    CompletableFuture<List<FileSystemFileHandle>> showOpenFilePicker(OpenFilePickerOptions options) {
        return mapErrors(executeJs(
                        JS_TRY_CATCH.formatted(RESOLVE_START_IN
                                + """

                        const handles = await window.showOpenFilePicker(opts);
                        """
                                + REGISTER_HANDLES),
                        options)
                .toCompletableFuture(new TypeReference<List<HandleInfo>>() {})
                .thenApply(infos -> infos.stream()
                        .map(info -> new FileSystemFileHandle(info.id(), info.name(), this))
                        .toList()));
    }

    CompletableFuture<FileSystemFileHandle> showSaveFilePicker(SaveFilePickerOptions options) {
        return mapErrors(executeJs(
                        JS_TRY_CATCH.formatted(RESOLVE_START_IN
                                + """

                        const handles = [await window.showSaveFilePicker(opts)];
                        """
                                + REGISTER_HANDLES),
                        options)
                .toCompletableFuture(new TypeReference<List<HandleInfo>>() {})
                .thenApply(infos -> infos.stream()
                        .map(info -> new FileSystemFileHandle(info.id(), info.name(), this))
                        .findFirst()
                        .orElseThrow()));
    }

    CompletableFuture<FileSystemDirectoryHandle> getOriginPrivateDirectory() {
        return mapErrors(executeJs(JS_TRY_CATCH.formatted(
                        """
                        const handle = await navigator.storage.getDirectory();
                        """
                                + REGISTER_SINGLE_HANDLE))
                .toCompletableFuture(new TypeReference<HandleInfo>() {})
                .thenApply(info -> new FileSystemDirectoryHandle(info.id(), info.name(), this)));
    }

    CompletableFuture<FileSystemDirectoryHandle> showDirectoryPicker(DirectoryPickerOptions options) {
        return mapErrors(executeJs(
                        JS_TRY_CATCH.formatted(RESOLVE_START_IN
                                + """

                        const handles = [await window.showDirectoryPicker(opts)];
                        """
                                + REGISTER_HANDLES),
                        options)
                .toCompletableFuture(new TypeReference<List<HandleInfo>>() {})
                .thenApply(infos -> infos.stream()
                        .map(info -> new FileSystemDirectoryHandle(info.id(), info.name(), this))
                        .findFirst()
                        .orElseThrow()));
    }

    /**
     * JS snippet that registers a single handle and returns its metadata.
     * Expects a {@code handle} variable to be in scope.
     */
    private static final String REGISTER_SINGLE_HANDLE =
            """
            const id = String(this.__fsApiNextId++);
            this.__fsApiHandles.set(id, handle);
            return {id: id, name: handle.name, kind: handle.kind};""";

    CompletableFuture<FileSystemFileHandle> getFileHandle(String dirHandleId, String name, GetHandleOptions options) {
        return mapErrors(executeJs(
                        JS_TRY_CATCH.formatted(requireHandle("dir", "$0")
                                + """
                                const handle = await dir.getFileHandle($1, $2);
                                """
                                + REGISTER_SINGLE_HANDLE),
                        dirHandleId,
                        name,
                        options)
                .toCompletableFuture(new TypeReference<HandleInfo>() {})
                .thenApply(info -> new FileSystemFileHandle(info.id(), info.name(), this)));
    }

    CompletableFuture<FileSystemDirectoryHandle> getDirectoryHandle(
            String dirHandleId, String name, GetHandleOptions options) {
        return mapErrors(executeJs(
                        JS_TRY_CATCH.formatted(requireHandle("dir", "$0")
                                + """
                                const handle = await dir.getDirectoryHandle($1, $2);
                                """
                                + REGISTER_SINGLE_HANDLE),
                        dirHandleId,
                        name,
                        options)
                .toCompletableFuture(new TypeReference<HandleInfo>() {})
                .thenApply(info -> new FileSystemDirectoryHandle(info.id(), info.name(), this)));
    }

    CompletableFuture<Void> removeEntry(String dirHandleId, String name, RemoveEntryOptions options) {
        return executeVoidJs(
                JS_TRY_CATCH.formatted(requireHandle("dir", "$0") + "await dir.removeEntry($1, $2);"),
                dirHandleId,
                name,
                options);
    }

    CompletableFuture<Optional<List<String>>> resolve(String dirHandleId, String childHandleId) {
        return mapErrors(executeJs(
                        JS_TRY_CATCH.formatted(
                                requireHandle("dir", "$0")
                                        + requireHandle("child", "$1")
                                        + """
                                const path = await dir.resolve(child);
                                return path;"""),
                        dirHandleId,
                        childHandleId)
                .toCompletableFuture(new TypeReference<List<String>>() {})
                .thenApply(Optional::ofNullable));
    }

    CompletableFuture<List<FileSystemHandle>> entries(String dirHandleId) {
        return mapErrors(executeJs(
                        JS_TRY_CATCH.formatted(
                                requireHandle("dir", "$0")
                                        + """
                                const result = [];
                                for await (const [name, handle] of dir.entries()) {
                                    const id = String(this.__fsApiNextId++);
                                    this.__fsApiHandles.set(id, handle);
                                    result.push({id: id, name: handle.name, kind: handle.kind});
                                }
                                return result;"""),
                        dirHandleId)
                .toCompletableFuture(new TypeReference<List<HandleInfo>>() {})
                .thenApply(infos -> infos.stream()
                        .map(info -> {
                            if ("directory".equals(info.kind())) {
                                return (FileSystemHandle) new FileSystemDirectoryHandle(info.id(), info.name(), this);
                            }
                            return (FileSystemHandle) new FileSystemFileHandle(info.id(), info.name(), this);
                        })
                        .toList()));
    }

    CompletableFuture<FileData> getFile(String handleId) {
        return mapErrors(executeJs(
                        JS_TRY_CATCH.formatted(
                                requireHandle("h", "$0")
                                        + """
                                const file = await h.getFile();
                                const buf = await file.arrayBuffer();
                                const bytes = new Uint8Array(buf);
                                let binary = '';
                                for (let i = 0; i < bytes.length; i++) {
                                    binary += String.fromCharCode(bytes[i]);
                                }
                                return {name: file.name, size: file.size, type: file.type,
                                    lastModified: file.lastModified, content: btoa(binary)};"""),
                        handleId)
                .toCompletableFuture(new TypeReference<FileInfo>() {})
                .thenApply(info -> new FileData(
                        info.name(),
                        info.size(),
                        info.type(),
                        info.lastModified(),
                        Base64.getDecoder().decode(info.content()))));
    }

    CompletableFuture<FileSystemWritableFileStream> createWritable(String handleId, WritableOptions options) {
        return mapErrors(executeJs(
                        JS_TRY_CATCH.formatted(
                                requireHandle("h", "$0")
                                        + """
                                const writable = await h.createWritable($1);
                                const id = String(this.__fsApiNextWritableId++);
                                this.__fsApiWritables.set(id, writable);
                                return id;"""),
                        handleId,
                        options)
                .toCompletableFuture(String.class)
                .thenApply(streamId -> new FileSystemWritableFileStream(streamId, this)));
    }

    CompletableFuture<Void> writableWriteText(String streamId, String text) {
        return executeVoidJs(JS_TRY_CATCH.formatted(requireWritable("w", "$0") + "await w.write($1);"), streamId, text);
    }

    CompletableFuture<Void> writableWriteBytes(String streamId, byte[] data) {
        String base64 = Base64.getEncoder().encodeToString(data);
        return executeVoidJs(
                JS_TRY_CATCH.formatted(
                        requireWritable("w", "$0")
                                + """
                        const binary = atob($1);
                        const bytes = new Uint8Array(binary.length);
                        for (let i = 0; i < binary.length; i++) {
                            bytes[i] = binary.charCodeAt(i);
                        }
                        await w.write(bytes);"""),
                streamId,
                base64);
    }

    CompletableFuture<Void> writableSeek(String streamId, long position) {
        return executeVoidJs(
                JS_TRY_CATCH.formatted(requireWritable("w", "$0") + "await w.seek($1);"), streamId, (double) position);
    }

    CompletableFuture<Void> writableTruncate(String streamId, long size) {
        return executeVoidJs(
                JS_TRY_CATCH.formatted(requireWritable("w", "$0") + "await w.truncate($1);"), streamId, (double) size);
    }

    CompletableFuture<Void> writableClose(String streamId) {
        return executeVoidJs(
                JS_TRY_CATCH.formatted(
                        requireWritable("w", "$0")
                                + """
                        await w.close();
                        this.__fsApiWritables.delete($0);"""),
                streamId);
    }

    private static final AtomicLong ATTR_COUNTER = new AtomicLong();

    CompletableFuture<Void> uploadTo(String handleId, UploadHandler handler) {
        String attr = "__fsApiUpload_" + ATTR_COUNTER.getAndIncrement();
        element().setAttribute(attr, handler);
        return executeVoidJs(
                        JS_TRY_CATCH.formatted(
                                requireHandle("h", "$0")
                                        + """
                                const file = await h.getFile();
                                const formData = new FormData();
                                formData.append('file', file, file.name);
                                const url = this.getAttribute($1);
                                const response = await fetch(url, {method: 'POST', body: formData});
                                if (!response.ok) {
                                    throw new Error('NotAllowedError: Upload failed with status ' + response.status);
                                }"""),
                        handleId,
                        attr)
                .whenComplete((result, error) -> element().removeAttribute(attr));
    }

    CompletableFuture<Void> downloadFrom(String handleId, DownloadHandler handler) {
        String attr = "__fsApiDownload_" + ATTR_COUNTER.getAndIncrement();
        element().setAttribute(attr, handler);
        return executeVoidJs(
                        JS_TRY_CATCH.formatted(
                                requireHandle("h", "$0")
                                        + """
                                const url = this.getAttribute($1);
                                const response = await fetch(url);
                                if (!response.ok) {
                                    throw new Error('NotAllowedError: Download failed with status ' + response.status);
                                }
                                const writable = await h.createWritable();
                                const reader = response.body.getReader();
                                while (true) {
                                    const {done, value} = await reader.read();
                                    if (done) break;
                                    await writable.write(value);
                                }
                                await writable.close();"""),
                        handleId,
                        attr)
                .whenComplete((result, error) -> element().removeAttribute(attr));
    }

    void releaseHandle(String handleId) {
        ensureInitialized();
        element().executeJs("this.__fsApiHandles.delete($0);", handleId);
    }

    String getHandleId(FileSystemHandle handle) {
        return ((AbstractFileSystemHandle) handle).handleId();
    }

    private void ensureInitialized() {
        if (!initialized) {
            element()
                    .executeJs(
                            """
                            this.__fsApiHandles = this.__fsApiHandles || new Map();
                            this.__fsApiNextId = this.__fsApiNextId || 0;
                            this.__fsApiWritables = this.__fsApiWritables || new Map();
                            this.__fsApiNextWritableId = this.__fsApiNextWritableId || 0;""");
            detachRegistration = component.addDetachListener(event -> cleanup());
            initialized = true;
        }
    }

    /**
     * Cleans up all client-side registries and resets the bridge state.
     *
     * <p>Closes all open writable streams (best-effort), then clears both
     * the handle and writable stream registries on the client side. Resets
     * the bridge so that it re-initializes on the next operation, supporting
     * component re-attach scenarios.
     */
    private void cleanup() {
        element()
                .executeJs(
                        """
                        if (this.__fsApiWritables) {
                            for (const w of this.__fsApiWritables.values()) {
                                try { w.close(); } catch(e) {}
                            }
                            this.__fsApiWritables.clear();
                        }
                        if (this.__fsApiHandles) { this.__fsApiHandles.clear(); }""");
        initialized = false;
        if (detachRegistration != null) {
            detachRegistration.remove();
            detachRegistration = null;
        }
    }

    /**
     * Maps a {@link CompletableFuture} failure to the appropriate
     * {@link FileSystemApiException} subclass based on the JS error name.
     *
     * @param future the future to map errors for
     * @param <T>    the future's result type
     * @return a new future with mapped exceptions
     */
    static <T> CompletableFuture<T> mapErrors(CompletableFuture<T> future) {
        return future.exceptionallyCompose(error -> {
            Throwable cause = error instanceof CompletionException ? error.getCause() : error;
            FileSystemApiException mapped = mapException(cause);
            return CompletableFuture.failedFuture(mapped);
        });
    }

    /**
     * Maps a throwable from a failed JS call to the appropriate
     * {@link FileSystemApiException} subclass.
     *
     * @param error the original error
     * @return the mapped exception
     */
    static FileSystemApiException mapException(Throwable error) {
        String message = error.getMessage();
        if (message != null) {
            String remaining = message;
            while (true) {
                int colonIndex = remaining.indexOf(':');
                if (colonIndex <= 0) break;
                String errorName = remaining.substring(0, colonIndex).trim();
                ExceptionFactory factory = ERROR_MAP.get(errorName);
                if (factory != null) {
                    String errorMessage = remaining.substring(colonIndex + 1).trim();
                    return factory.create(errorMessage);
                }
                remaining = remaining.substring(colonIndex + 1).trim();
            }
        }
        return new FileSystemApiException(message != null ? message : "Unknown File System API error", error);
    }

    private Element element() {
        return component.getElement();
    }

    /**
     * Functional interface for creating specific exception instances.
     */
    @FunctionalInterface
    interface ExceptionFactory extends Serializable {
        /**
         * Creates an exception with the given message.
         *
         * @param message the error message
         * @return the exception
         */
        FileSystemApiException create(String message);
    }
}
