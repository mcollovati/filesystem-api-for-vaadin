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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Base implementation of {@link FileSystemHandle} providing shared state
 * and behavior for file and directory handles.
 */
abstract sealed class AbstractFileSystemHandle implements FileSystemHandle
        permits FileSystemFileHandle, FileSystemDirectoryHandle {

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
