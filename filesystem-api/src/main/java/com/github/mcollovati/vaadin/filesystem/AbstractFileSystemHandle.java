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
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base implementation of {@link FileSystemHandle} providing shared state
 * and behavior for file and directory handles.
 */
abstract sealed class AbstractFileSystemHandle implements FileSystemHandle
        permits FileSystemFileHandle, FileSystemDirectoryHandle {

    private static final Logger logger = Logger.getLogger(AbstractFileSystemHandle.class.getName());

    private final String handleId;
    private final String name;
    private final JsBridge bridge;

    AbstractFileSystemHandle(String handleId, String name, JsBridge bridge) {
        this.handleId = handleId;
        this.name = name;
        this.bridge = bridge;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CompletableFuture<Boolean> isSameEntry(FileSystemHandle other) {
        return bridge.isSameEntry(handleId, bridge.getHandleId(other));
    }

    @Override
    public CompletableFuture<PermissionState> queryPermission(PermissionMode mode) {
        return bridge.queryPermission(handleId, mode);
    }

    @Override
    public CompletableFuture<PermissionState> requestPermission(PermissionMode mode) {
        return bridge.requestPermission(handleId, mode);
    }

    @Override
    public Registration requestPermissionTriggeredBy(
            Component source,
            PermissionMode mode,
            SerializableConsumer<PermissionState> onSuccess,
            SerializableConsumer<Throwable> onError) {
        bridge.ensureInitialized();
        PermissionPromiseAction action = new PermissionPromiseAction(
                bridge.element(),
                handleId,
                mode.getJsValue(),
                value -> {
                    if (onSuccess != null) {
                        onSuccess.accept(PermissionState.fromJsValue(value));
                    }
                },
                error -> {
                    Throwable mapped = JsBridge.mapError(error.name(), error.message());
                    if (onError != null) {
                        onError.accept(mapped);
                    } else {
                        logger.log(Level.FINE, "File System API operation failed", mapped);
                    }
                });
        ClickTrigger trigger = new ClickTrigger(source);
        trigger.triggers(action);
        return trigger::remove;
    }

    @Override
    public void release() {
        bridge.releaseHandle(handleId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractFileSystemHandle that)) return false;
        return Objects.equals(handleId, that.handleId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(handleId);
    }

    String handleId() {
        return handleId;
    }

    JsBridge bridge() {
        return bridge;
    }
}
