package com.github.mcollovati.vaadin.filesystem;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;
import java.io.Serializable;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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

    private final Component component;
    boolean initialized;
    private Registration detachRegistration;

    private JsBridge(Component component) {
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
        return executeJs(expression, params).toCompletableFuture(String.class).thenApply(ignored -> null);
    }

    CompletableFuture<Boolean> isSameEntry(String handleId1, String handleId2) {
        return executeJs(
                        """
                        const h1 = this.__fsApiHandles.get($0);
                        const h2 = this.__fsApiHandles.get($1);
                        return await h1.isSameEntry(h2);""",
                        handleId1,
                        handleId2)
                .toCompletableFuture(Boolean.class);
    }

    CompletableFuture<PermissionState> queryPermission(String handleId, PermissionMode mode) {
        return executeJs(
                        """
                        const h = this.__fsApiHandles.get($0);
                        return await h.queryPermission({mode: $1});""",
                        handleId,
                        mode.getJsValue())
                .toCompletableFuture(String.class)
                .thenApply(PermissionState::fromJsValue);
    }

    CompletableFuture<PermissionState> requestPermission(String handleId, PermissionMode mode) {
        return executeJs(
                        """
                        const h = this.__fsApiHandles.get($0);
                        return await h.requestPermission({mode: $1});""",
                        handleId,
                        mode.getJsValue())
                .toCompletableFuture(String.class)
                .thenApply(PermissionState::fromJsValue);
    }

    CompletableFuture<List<FileSystemFileHandle>> showOpenFilePicker(OpenFilePickerOptions options) {
        return executeJs(
                        RESOLVE_START_IN
                                + """

                        const handles = await window.showOpenFilePicker(opts);
                        """
                                + REGISTER_HANDLES,
                        options)
                .toCompletableFuture(new TypeReference<List<HandleInfo>>() {})
                .thenApply(infos -> infos.stream()
                        .map(info -> new FileSystemFileHandle(info.id(), info.name(), this))
                        .toList());
    }

    CompletableFuture<FileSystemFileHandle> showSaveFilePicker(SaveFilePickerOptions options) {
        return executeJs(
                        RESOLVE_START_IN
                                + """

                        const handles = [await window.showSaveFilePicker(opts)];
                        """
                                + REGISTER_HANDLES,
                        options)
                .toCompletableFuture(new TypeReference<List<HandleInfo>>() {})
                .thenApply(infos -> infos.stream()
                        .map(info -> new FileSystemFileHandle(info.id(), info.name(), this))
                        .findFirst()
                        .orElseThrow());
    }

    CompletableFuture<FileSystemDirectoryHandle> showDirectoryPicker(DirectoryPickerOptions options) {
        return executeJs(
                        RESOLVE_START_IN
                                + """

                        const handles = [await window.showDirectoryPicker(opts)];
                        """
                                + REGISTER_HANDLES,
                        options)
                .toCompletableFuture(new TypeReference<List<HandleInfo>>() {})
                .thenApply(infos -> infos.stream()
                        .map(info -> new FileSystemDirectoryHandle(info.id(), info.name(), this))
                        .findFirst()
                        .orElseThrow());
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
        return executeJs(
                        """
                        const dir = this.__fsApiHandles.get($0);
                        const handle = await dir.getFileHandle($1, $2);
                        """
                                + REGISTER_SINGLE_HANDLE,
                        dirHandleId,
                        name,
                        options)
                .toCompletableFuture(new TypeReference<HandleInfo>() {})
                .thenApply(info -> new FileSystemFileHandle(info.id(), info.name(), this));
    }

    CompletableFuture<FileSystemDirectoryHandle> getDirectoryHandle(
            String dirHandleId, String name, GetHandleOptions options) {
        return executeJs(
                        """
                        const dir = this.__fsApiHandles.get($0);
                        const handle = await dir.getDirectoryHandle($1, $2);
                        """
                                + REGISTER_SINGLE_HANDLE,
                        dirHandleId,
                        name,
                        options)
                .toCompletableFuture(new TypeReference<HandleInfo>() {})
                .thenApply(info -> new FileSystemDirectoryHandle(info.id(), info.name(), this));
    }

    CompletableFuture<Void> removeEntry(String dirHandleId, String name, RemoveEntryOptions options) {
        return executeVoidJs(
                """
                const dir = this.__fsApiHandles.get($0);
                await dir.removeEntry($1, $2);""",
                dirHandleId,
                name,
                options);
    }

    CompletableFuture<Optional<List<String>>> resolve(String dirHandleId, String childHandleId) {
        return executeJs(
                        """
                        const dir = this.__fsApiHandles.get($0);
                        const child = this.__fsApiHandles.get($1);
                        const path = await dir.resolve(child);
                        return path;""",
                        dirHandleId,
                        childHandleId)
                .toCompletableFuture(new TypeReference<List<String>>() {})
                .thenApply(Optional::ofNullable);
    }

    CompletableFuture<List<FileSystemHandle>> entries(String dirHandleId) {
        return executeJs(
                        """
                        const dir = this.__fsApiHandles.get($0);
                        const result = [];
                        for await (const [name, handle] of dir.entries()) {
                            const id = String(this.__fsApiNextId++);
                            this.__fsApiHandles.set(id, handle);
                            result.push({id: id, name: handle.name, kind: handle.kind});
                        }
                        return result;""",
                        dirHandleId)
                .toCompletableFuture(new TypeReference<List<HandleInfo>>() {})
                .thenApply(infos -> infos.stream()
                        .map(info -> {
                            if ("directory".equals(info.kind())) {
                                return (FileSystemHandle) new FileSystemDirectoryHandle(info.id(), info.name(), this);
                            }
                            return (FileSystemHandle) new FileSystemFileHandle(info.id(), info.name(), this);
                        })
                        .toList());
    }

    CompletableFuture<FileData> getFile(String handleId) {
        return executeJs(
                        """
                        const h = this.__fsApiHandles.get($0);
                        const file = await h.getFile();
                        const buf = await file.arrayBuffer();
                        const bytes = new Uint8Array(buf);
                        let binary = '';
                        for (let i = 0; i < bytes.length; i++) {
                            binary += String.fromCharCode(bytes[i]);
                        }
                        return {name: file.name, size: file.size, type: file.type,
                            lastModified: file.lastModified, content: btoa(binary)};""",
                        handleId)
                .toCompletableFuture(new TypeReference<FileInfo>() {})
                .thenApply(info -> new FileData(
                        info.name(),
                        info.size(),
                        info.type(),
                        info.lastModified(),
                        Base64.getDecoder().decode(info.content())));
    }

    CompletableFuture<FileSystemWritableFileStream> createWritable(String handleId, WritableOptions options) {
        return executeJs(
                        """
                        const h = this.__fsApiHandles.get($0);
                        const writable = await h.createWritable($1);
                        const id = String(this.__fsApiNextWritableId++);
                        this.__fsApiWritables.set(id, writable);
                        return id;""",
                        handleId,
                        options)
                .toCompletableFuture(String.class)
                .thenApply(streamId -> new FileSystemWritableFileStream(streamId, this));
    }

    CompletableFuture<Void> writableWriteText(String streamId, String text) {
        return executeVoidJs(
                """
                const w = this.__fsApiWritables.get($0);
                await w.write($1);""",
                streamId,
                text);
    }

    CompletableFuture<Void> writableWriteBytes(String streamId, byte[] data) {
        String base64 = Base64.getEncoder().encodeToString(data);
        return executeVoidJs(
                """
                const w = this.__fsApiWritables.get($0);
                const binary = atob($1);
                const bytes = new Uint8Array(binary.length);
                for (let i = 0; i < binary.length; i++) {
                    bytes[i] = binary.charCodeAt(i);
                }
                await w.write(bytes);""",
                streamId,
                base64);
    }

    CompletableFuture<Void> writableSeek(String streamId, long position) {
        return executeVoidJs(
                """
                const w = this.__fsApiWritables.get($0);
                await w.seek($1);""",
                streamId,
                (double) position);
    }

    CompletableFuture<Void> writableTruncate(String streamId, long size) {
        return executeVoidJs(
                """
                const w = this.__fsApiWritables.get($0);
                await w.truncate($1);""",
                streamId,
                (double) size);
    }

    CompletableFuture<Void> writableClose(String streamId) {
        return executeVoidJs(
                """
                const w = this.__fsApiWritables.get($0);
                await w.close();
                this.__fsApiWritables.delete($0);""",
                streamId);
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

    private Element element() {
        return component.getElement();
    }
}
