package com.github.mcollovati.vaadin.filesystem;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.Element;
import java.io.Serializable;
import java.util.List;
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
            "const opts = Object.assign({}, $0);" + "if (opts.startIn && this.__fsApiHandles.has(opts.startIn)) {"
                    + "  opts.startIn = this.__fsApiHandles.get(opts.startIn);" + "}";

    /**
     * JS snippet that registers an array of handles into the registry and
     * returns their metadata. Expects a {@code handles} variable to be in
     * scope.
     */
    private static final String REGISTER_HANDLES = "return handles.map(h => {"
            + "  const id = String(this.__fsApiNextId++);"
            + "  this.__fsApiHandles.set(id, h);"
            + "  return {id: id, name: h.name, kind: h.kind};"
            + "});";

    private final Component component;
    private boolean initialized;

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
                        "const h1 = this.__fsApiHandles.get($0);"
                                + "const h2 = this.__fsApiHandles.get($1);"
                                + "return await h1.isSameEntry(h2);",
                        handleId1,
                        handleId2)
                .toCompletableFuture(Boolean.class);
    }

    CompletableFuture<PermissionState> queryPermission(String handleId, PermissionMode mode) {
        return executeJs(
                        "const h = this.__fsApiHandles.get($0);" + "return await h.queryPermission({mode: $1});",
                        handleId,
                        mode.getJsValue())
                .toCompletableFuture(String.class)
                .thenApply(PermissionState::fromJsValue);
    }

    CompletableFuture<PermissionState> requestPermission(String handleId, PermissionMode mode) {
        return executeJs(
                        "const h = this.__fsApiHandles.get($0);" + "return await h.requestPermission({mode: $1});",
                        handleId,
                        mode.getJsValue())
                .toCompletableFuture(String.class)
                .thenApply(PermissionState::fromJsValue);
    }

    CompletableFuture<List<FileSystemFileHandle>> showOpenFilePicker(OpenFilePickerOptions options) {
        return executeJs(
                        RESOLVE_START_IN + "const handles = await window.showOpenFilePicker(opts);" + REGISTER_HANDLES,
                        options)
                .toCompletableFuture(new TypeReference<List<HandleInfo>>() {})
                .thenApply(infos -> infos.stream()
                        .map(info -> new FileSystemFileHandle(info.id(), info.name(), this))
                        .toList());
    }

    CompletableFuture<FileSystemFileHandle> showSaveFilePicker(SaveFilePickerOptions options) {
        return executeJs(
                        RESOLVE_START_IN + "const handles = [await window.showSaveFilePicker(opts)];"
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
                        RESOLVE_START_IN + "const handles = [await window.showDirectoryPicker(opts)];"
                                + REGISTER_HANDLES,
                        options)
                .toCompletableFuture(new TypeReference<List<HandleInfo>>() {})
                .thenApply(infos -> infos.stream()
                        .map(info -> new FileSystemDirectoryHandle(info.id(), info.name(), this))
                        .findFirst()
                        .orElseThrow());
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
                    .executeJs("this.__fsApiHandles = this.__fsApiHandles || new Map();"
                            + "this.__fsApiNextId = this.__fsApiNextId || 0;");
            initialized = true;
        }
    }

    private Element element() {
        return component.getElement();
    }
}
