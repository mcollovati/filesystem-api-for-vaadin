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
import java.util.function.Supplier;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

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
     * Maps JS DOMException names to Java exception factory methods.
     */
    private static final Map<String, ExceptionFactory> ERROR_MAP = Map.of(
            "NotFoundError", FileSystemNotFoundException::new,
            "NotAllowedError", FileSystemNotAllowedException::new,
            "AbortError", FileSystemNotAllowedException::new,
            "TypeMismatchError", FileSystemTypeMismatchException::new,
            "SecurityError", FileSystemNotAllowedException::new,
            "TypeError", FileSystemApiNotSupportedException::new);

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

    CompletableFuture<Boolean> isSupported() {
        return element().executeJs(FileSystemJS.Support.class).isSupported().toCompletableFuture(Boolean.class);
    }

    private <T> T executeJs(Class<T> definitionType) {
        ensureInitialized();
        return element().executeJs(definitionType);
    }

    CompletableFuture<Boolean> isSameEntry(String handleId1, String handleId2) {
        return mapErrors(executeJs(FileSystemJS.Handle.class)
                .isSameEntry(handleId1, handleId2)
                .toCompletableFuture(Boolean.class));
    }

    CompletableFuture<PermissionState> queryPermission(String handleId, PermissionMode mode) {
        return mapErrors(executeJs(FileSystemJS.Handle.class)
                .queryPermission(handleId, mode)
                .toCompletableFuture(String.class)
                .thenApply(PermissionState::fromJsValue));
    }

    CompletableFuture<PermissionState> requestPermission(String handleId, PermissionMode mode) {
        return mapErrors(executeJs(FileSystemJS.Handle.class)
                .requestPermission(handleId, mode)
                .toCompletableFuture(String.class)
                .thenApply(PermissionState::fromJsValue));
    }

    CompletableFuture<List<FileSystemFileHandle>> showOpenFilePicker(OpenFilePickerOptions options) {
        return mapErrors(executeJs(FileSystemJS.Picker.class)
                .showOpenFilePicker(options)
                .toCompletableFuture(new TypeReference<List<HandleInfo>>() {})
                .thenApply(infos -> infos.stream()
                        .map(info -> new FileSystemFileHandle(info.id(), info.name(), this))
                        .toList()));
    }

    CompletableFuture<FileSystemFileHandle> showSaveFilePicker(SaveFilePickerOptions options) {
        return mapErrors(executeJs(FileSystemJS.Picker.class)
                .showSaveFilePicker(options)
                .toCompletableFuture(new TypeReference<List<HandleInfo>>() {})
                .thenApply(infos -> infos.stream()
                        .map(info -> new FileSystemFileHandle(info.id(), info.name(), this))
                        .findFirst()
                        .orElseThrow()));
    }

    CompletableFuture<FileSystemDirectoryHandle> getOriginPrivateDirectory() {
        return mapErrors(executeJs(FileSystemJS.Picker.class)
                .getOriginPrivateDirectory()
                .toCompletableFuture(new TypeReference<HandleInfo>() {})
                .thenApply(info -> new FileSystemDirectoryHandle(info.id(), info.name(), this)));
    }

    // -- OPFS path-based operations (single roundtrip) --

    CompletableFuture<FileSystemFileHandle> opfsGetFileHandle(String path, GetHandleOptions options) {
        return mapErrors(executeJs(FileSystemJS.Opfs.class)
                .opfsGetFileHandle(path, options)
                .toCompletableFuture(new TypeReference<HandleInfo>() {})
                .thenApply(info -> new FileSystemFileHandle(info.id(), info.name(), this)));
    }

    CompletableFuture<FileSystemDirectoryHandle> opfsGetDirectoryHandle(String path, GetHandleOptions options) {
        return mapErrors(executeJs(FileSystemJS.Opfs.class)
                .opfsGetDirectoryHandle(path, options)
                .toCompletableFuture(new TypeReference<HandleInfo>() {})
                .thenApply(info -> new FileSystemDirectoryHandle(info.id(), info.name(), this)));
    }

    CompletableFuture<FileData> opfsReadFile(String path) {
        return mapErrors(executeJs(FileSystemJS.Opfs.class)
                .opfsReadFile(path)
                .toCompletableFuture(JsonNode.class)
                .thenApply(node -> new FileData(
                        node.path("name").asString(),
                        node.path("size").longValue(),
                        node.path("type").asString(),
                        node.path("lastModified").longValue(),
                        Base64.getDecoder().decode(node.path("content").asString()))));
    }

    CompletableFuture<Void> opfsWriteText(String path, String text) {
        return runVoid(() -> executeJs(FileSystemJS.Opfs.class).opfsWriteText(path, text));
    }

    CompletableFuture<Void> opfsWriteBytes(String path, byte[] data) {
        String base64 = Base64.getEncoder().encodeToString(data);
        return runVoid(() -> executeJs(FileSystemJS.Opfs.class).opfsWriteBytes(path, base64));
    }

    CompletableFuture<List<FileSystemHandle>> opfsEntries(String path) {
        return mapErrors(executeJs(FileSystemJS.Opfs.class)
                .opfsEntries(path, GetHandleOptions.builder().build())
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

    CompletableFuture<Void> opfsRemoveEntry(String path, RemoveEntryOptions options) {
        return runVoid(() -> executeJs(FileSystemJS.Opfs.class).opfsRemoveEntry(path, options));
    }

    CompletableFuture<Void> opfsClear() {
        return runVoid(() -> executeJs(FileSystemJS.Opfs.class).opfsClear());
    }

    CompletableFuture<FileSystemDirectoryHandle> showDirectoryPicker(DirectoryPickerOptions options) {
        return mapErrors(executeJs(FileSystemJS.Picker.class)
                .showDirectoryPicker(options)
                .toCompletableFuture(new TypeReference<List<HandleInfo>>() {})
                .thenApply(infos -> infos.stream()
                        .map(info -> new FileSystemDirectoryHandle(info.id(), info.name(), this))
                        .findFirst()
                        .orElseThrow()));
    }

    CompletableFuture<FileSystemFileHandle> getFileHandle(String dirHandleId, String name, GetHandleOptions options) {
        return mapErrors(executeJs(FileSystemJS.Handle.class)
                .getFileHandle(dirHandleId, name, options)
                .toCompletableFuture(new TypeReference<HandleInfo>() {})
                .thenApply(info -> new FileSystemFileHandle(info.id(), info.name(), this)));
    }

    CompletableFuture<FileSystemDirectoryHandle> getDirectoryHandle(
            String dirHandleId, String name, GetHandleOptions options) {
        return mapErrors(executeJs(FileSystemJS.Handle.class)
                .getDirectoryHandle(dirHandleId, name, options)
                .toCompletableFuture(new TypeReference<HandleInfo>() {})
                .thenApply(info -> new FileSystemDirectoryHandle(info.id(), info.name(), this)));
    }

    CompletableFuture<Void> removeEntry(String dirHandleId, String name, RemoveEntryOptions options) {
        return runVoid(() -> executeJs(FileSystemJS.Handle.class).removeEntry(dirHandleId, name, options));
    }

    CompletableFuture<Optional<List<String>>> resolve(String dirHandleId, String childHandleId) {
        return mapErrors(executeJs(FileSystemJS.Handle.class)
                .resolve(dirHandleId, childHandleId)
                .toCompletableFuture(new TypeReference<List<String>>() {})
                .thenApply(Optional::ofNullable));
    }

    CompletableFuture<List<FileSystemHandle>> entries(String dirHandleId) {
        return mapErrors(executeJs(FileSystemJS.Handle.class)
                .entries(dirHandleId)
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
        return mapErrors(executeJs(FileSystemJS.Handle.class)
                .getFile(handleId)
                .toCompletableFuture(JsonNode.class)
                .thenApply(node -> new FileData(
                        node.path("name").asString(),
                        node.path("size").longValue(),
                        node.path("type").asString(),
                        node.path("lastModified").longValue(),
                        Base64.getDecoder().decode(node.path("content").asString()))));
    }

    CompletableFuture<FileSystemWritableFileStream> createWritable(String handleId, WritableOptions options) {
        return mapErrors(executeJs(FileSystemJS.Writable.class)
                .createWritable(handleId, options)
                .toCompletableFuture(String.class)
                .thenApply(streamId -> new FileSystemWritableFileStream(streamId, this)));
    }

    CompletableFuture<Void> writableWriteText(String streamId, String text) {
        return runVoid(() -> executeJs(FileSystemJS.Writable.class).writableWriteText(streamId, text));
    }

    CompletableFuture<Void> writableWriteBytes(String streamId, byte[] data) {
        String base64 = Base64.getEncoder().encodeToString(data);
        return runVoid(() -> executeJs(FileSystemJS.Writable.class).writableWriteBytes(streamId, base64));
    }

    CompletableFuture<Void> writableSeek(String streamId, long position) {
        return runVoid(() -> executeJs(FileSystemJS.Writable.class).writableSeek(streamId, (double) position));
    }

    CompletableFuture<Void> writableTruncate(String streamId, long size) {
        return runVoid(() -> executeJs(FileSystemJS.Writable.class).writableTruncate(streamId, (double) size));
    }

    CompletableFuture<Void> writableClose(String streamId) {
        return runVoid(() -> executeJs(FileSystemJS.Writable.class).writableClose(streamId));
    }

    private static final AtomicLong ATTR_COUNTER = new AtomicLong();

    CompletableFuture<Void> uploadTo(String handleId, UploadHandler handler) {
        String attr = "__fsApiUpload_" + ATTR_COUNTER.getAndIncrement();
        element().setAttribute(attr, handler);
        return runVoid(() -> executeJs(FileSystemJS.Handle.class).uploadTo(handleId, attr))
                .whenComplete((result, error) -> element().removeAttribute(attr));
    }

    CompletableFuture<Void> downloadFrom(String handleId, DownloadHandler handler) {
        String attr = "__fsApiDownload_" + ATTR_COUNTER.getAndIncrement();
        element().setAttribute(attr, handler);
        return runVoid(() -> executeJs(FileSystemJS.Handle.class).downloadFrom(handleId, attr))
                .whenComplete((result, error) -> element().removeAttribute(attr));
    }

    CompletableFuture<Void> saveToDevice(String handleId, String downloadName) {
        return runVoid(() -> executeJs(FileSystemJS.Handle.class).saveToDevice(handleId, downloadName));
    }

    CompletableFuture<Void> opfsSaveToDevice(String path, String downloadName) {
        return runVoid(() -> executeJs(FileSystemJS.Opfs.class).opfsSaveToDevice(path, downloadName));
    }

    void releaseHandle(String handleId) {
        executeJs(FileSystemJS.Handle.class).releaseHandle(handleId);
    }

    String getHandleId(FileSystemHandle handle) {
        return ((AbstractFileSystemHandle) handle).handleId();
    }

    private void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    /**
     * Initializes the client-side registries and registers the detach
     * listener. Subclasses may override this method to avoid executing
     * JavaScript.
     */
    void initialize() {
        element().executeJs(FileSystemJS.Lifecycle.class).initialize();
        detachRegistration = component.addDetachListener(event -> cleanup());
        initialized = true;
    }

    /**
     * Cleans up all client-side registries and resets the bridge state.
     *
     * <p>Closes all open writable streams (best-effort), then clears both
     * the handle and writable stream registries on the client side. Resets
     * the bridge so that it re-initializes on the next operation, supporting
     * component re-attach scenarios.
     */
    void cleanup() {
        executeJs(FileSystemJS.Lifecycle.class).cleanup();
        initialized = false;
        if (detachRegistration != null) {
            detachRegistration.remove();
            detachRegistration = null;
        }
    }

    /**
     * Runs a JavaScript call that returns no value and maps errors to the
     * appropriate Java exception type.
     *
     * @param call a supplier that returns the pending JS result to await
     * @return a future that completes when the call finishes
     */
    private CompletableFuture<Void> runVoid(Supplier<PendingJavaScriptResult> call) {
        return mapErrors(call.get().toCompletableFuture(String.class).thenApply(ignored -> null));
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
