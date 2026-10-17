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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A fake {@link JsBridge} that operates against an {@link InMemoryFileSystem}
 * instead of executing JavaScript. All returned futures are already completed,
 * so {@code thenCompose}/{@code thenAccept} chains execute synchronously.
 */
class FakeJsBridge extends JsBridge {

    private final InMemoryFileSystem fs;
    private final Map<String, FakeWritableStream> writables = new LinkedHashMap<>();
    private long nextWritableId;

    private PickerResponse<List<FileSystemFileHandle>> openFilePickerResponse;
    private PickerResponse<FileSystemFileHandle> saveFilePickerResponse;
    private PickerResponse<FileSystemDirectoryHandle> directoryPickerResponse;
    private PermissionState permissionState = PermissionState.GRANTED;

    FakeJsBridge(Component component, InMemoryFileSystem fs) {
        super(component);
        this.fs = fs;
        this.initialized = true;
    }

    /**
     * Returns the in-memory file system backing this bridge.
     *
     * @return the file system
     */
    public InMemoryFileSystem fileSystem() {
        return fs;
    }

    void setOpenFilePickerResponse(PickerResponse<List<FileSystemFileHandle>> response) {
        this.openFilePickerResponse = response;
    }

    void setSaveFilePickerResponse(PickerResponse<FileSystemFileHandle> response) {
        this.saveFilePickerResponse = response;
    }

    void setDirectoryPickerResponse(PickerResponse<FileSystemDirectoryHandle> response) {
        this.directoryPickerResponse = response;
    }

    void setPermissionState(PermissionState state) {
        this.permissionState = state;
    }

    @Override
    CompletableFuture<Boolean> isSupported() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    void initialize() {
        this.initialized = true;
    }

    @Override
    void cleanup() {
        this.initialized = false;
    }

    @Override
    CompletableFuture<Boolean> isSameEntry(String handleId1, String handleId2) {
        FsNode node1 = fs.resolveHandle(handleId1);
        FsNode node2 = fs.resolveHandle(handleId2);
        return CompletableFuture.completedFuture(node1 == node2);
    }

    @Override
    CompletableFuture<PermissionState> queryPermission(String handleId, PermissionMode mode) {
        return CompletableFuture.completedFuture(permissionState);
    }

    @Override
    CompletableFuture<PermissionState> requestPermission(String handleId, PermissionMode mode) {
        return CompletableFuture.completedFuture(permissionState);
    }

    @Override
    CompletableFuture<List<FileSystemFileHandle>> showOpenFilePicker(OpenFilePickerOptions options) {
        if (openFilePickerResponse != null) {
            return openFilePickerResponse.respond(fs, this);
        }
        // Default: return all files in root
        List<FileSystemFileHandle> handles = new ArrayList<>();
        for (Map.Entry<String, FsNode> entry : fs.root().children().entrySet()) {
            if (entry.getValue() instanceof FsFile) {
                String id = fs.registerHandle(entry.getValue(), entry.getKey());
                handles.add(new FileSystemFileHandle(id, entry.getKey(), this));
            }
        }
        if (handles.isEmpty()) {
            return CompletableFuture.failedFuture(new FileSystemNotAllowedException("No files in root directory"));
        }
        return CompletableFuture.completedFuture(handles);
    }

    @Override
    CompletableFuture<FileSystemFileHandle> showSaveFilePicker(SaveFilePickerOptions options) {
        if (saveFilePickerResponse != null) {
            return saveFilePickerResponse.respond(fs, this);
        }
        // Default: create/return "untitled" file
        String name = "untitled";
        FsFile file;
        if (fs.root().hasChild(name) && fs.root().child(name) instanceof FsFile f) {
            file = f;
        } else {
            file = new FsFile(new byte[0], "");
            fs.root().putChild(name, file);
        }
        String id = fs.registerHandle(file, name);
        return CompletableFuture.completedFuture(new FileSystemFileHandle(id, name, this));
    }

    @Override
    CompletableFuture<FileSystemDirectoryHandle> showDirectoryPicker(DirectoryPickerOptions options) {
        if (directoryPickerResponse != null) {
            return directoryPickerResponse.respond(fs, this);
        }
        // Default: return root
        String id = fs.registerHandle(fs.root(), "");
        return CompletableFuture.completedFuture(new FileSystemDirectoryHandle(id, "", this));
    }

    @Override
    CompletableFuture<FileSystemDirectoryHandle> getOriginPrivateDirectory() {
        String id = fs.registerHandle(fs.root(), "");
        return CompletableFuture.completedFuture(new FileSystemDirectoryHandle(id, "", this));
    }

    @Override
    CompletableFuture<FileSystemFileHandle> getFileHandle(String dirHandleId, String name, GetHandleOptions options) {
        FsNode dirNode = fs.resolveHandle(dirHandleId);
        if (!(dirNode instanceof FsDirectory dir)) {
            return CompletableFuture.failedFuture(new FileSystemTypeMismatchException("Handle is not a directory"));
        }
        if (dir.hasChild(name)) {
            FsNode child = dir.child(name);
            if (!(child instanceof FsFile)) {
                return CompletableFuture.failedFuture(
                        new FileSystemTypeMismatchException("'" + name + "' is a directory, not a file"));
            }
            String id = fs.registerHandle(child, name);
            return CompletableFuture.completedFuture(new FileSystemFileHandle(id, name, this));
        }
        if (options.create()) {
            FsFile file = new FsFile(new byte[0], "");
            dir.putChild(name, file);
            String id = fs.registerHandle(file, name);
            return CompletableFuture.completedFuture(new FileSystemFileHandle(id, name, this));
        }
        return CompletableFuture.failedFuture(new FileSystemNotFoundException("File not found: " + name));
    }

    @Override
    CompletableFuture<FileSystemDirectoryHandle> getDirectoryHandle(
            String dirHandleId, String name, GetHandleOptions options) {
        FsNode dirNode = fs.resolveHandle(dirHandleId);
        if (!(dirNode instanceof FsDirectory dir)) {
            return CompletableFuture.failedFuture(new FileSystemTypeMismatchException("Handle is not a directory"));
        }
        if (dir.hasChild(name)) {
            FsNode child = dir.child(name);
            if (!(child instanceof FsDirectory)) {
                return CompletableFuture.failedFuture(
                        new FileSystemTypeMismatchException("'" + name + "' is a file, not a directory"));
            }
            String id = fs.registerHandle(child, name);
            return CompletableFuture.completedFuture(new FileSystemDirectoryHandle(id, name, this));
        }
        if (options.create()) {
            FsDirectory subDir = new FsDirectory();
            dir.putChild(name, subDir);
            String id = fs.registerHandle(subDir, name);
            return CompletableFuture.completedFuture(new FileSystemDirectoryHandle(id, name, this));
        }
        return CompletableFuture.failedFuture(new FileSystemNotFoundException("Directory not found: " + name));
    }

    @Override
    CompletableFuture<Void> removeEntry(String dirHandleId, String name, RemoveEntryOptions options) {
        FsNode dirNode = fs.resolveHandle(dirHandleId);
        if (!(dirNode instanceof FsDirectory dir)) {
            return CompletableFuture.failedFuture(new FileSystemTypeMismatchException("Handle is not a directory"));
        }
        if (!dir.hasChild(name)) {
            return CompletableFuture.failedFuture(new FileSystemNotFoundException("Entry not found: " + name));
        }
        FsNode child = dir.child(name);
        if (child instanceof FsDirectory childDir && !childDir.children().isEmpty() && !options.recursive()) {
            return CompletableFuture.failedFuture(
                    new FileSystemApiException("Directory is not empty and recursive is false"));
        }
        dir.removeChild(name);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    CompletableFuture<Optional<List<String>>> resolve(String dirHandleId, String childHandleId) {
        FsNode dirNode = fs.resolveHandle(dirHandleId);
        FsNode childNode = fs.resolveHandle(childHandleId);
        if (dirNode == childNode) {
            return CompletableFuture.completedFuture(Optional.of(List.of()));
        }
        if (!(dirNode instanceof FsDirectory dir)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        List<String> path = fs.resolvePath(dir, childNode);
        return CompletableFuture.completedFuture(Optional.ofNullable(path));
    }

    @Override
    CompletableFuture<List<FileSystemHandle>> entries(String dirHandleId) {
        FsNode dirNode = fs.resolveHandle(dirHandleId);
        if (!(dirNode instanceof FsDirectory dir)) {
            return CompletableFuture.failedFuture(new FileSystemTypeMismatchException("Handle is not a directory"));
        }
        List<FileSystemHandle> handles = new ArrayList<>();
        for (Map.Entry<String, FsNode> entry : dir.children().entrySet()) {
            String name = entry.getKey();
            FsNode node = entry.getValue();
            String id = fs.registerHandle(node, name);
            if (node instanceof FsFile) {
                handles.add(new FileSystemFileHandle(id, name, this));
            } else {
                handles.add(new FileSystemDirectoryHandle(id, name, this));
            }
        }
        return CompletableFuture.completedFuture(handles);
    }

    @Override
    CompletableFuture<FileData> getFile(String handleId) {
        FsNode node = fs.resolveHandle(handleId);
        if (!(node instanceof FsFile file)) {
            return CompletableFuture.failedFuture(new FileSystemTypeMismatchException("Handle is not a file"));
        }
        String name = fs.handleName(handleId);
        return CompletableFuture.completedFuture(
                new FileData(name, file.content().length, file.mimeType(), file.lastModified(), file.content()));
    }

    @Override
    CompletableFuture<FileSystemWritableFileStream> createWritable(String handleId, WritableOptions options) {
        FsNode node = fs.resolveHandle(handleId);
        if (!(node instanceof FsFile file)) {
            return CompletableFuture.failedFuture(new FileSystemTypeMismatchException("Handle is not a file"));
        }
        FakeWritableStream stream = new FakeWritableStream(file, options.keepExistingData());
        String streamId = String.valueOf(nextWritableId++);
        writables.put(streamId, stream);
        return CompletableFuture.completedFuture(new FileSystemWritableFileStream(streamId, this));
    }

    @Override
    CompletableFuture<Void> writableWriteText(String streamId, String text) {
        FakeWritableStream stream = writables.get(streamId);
        if (stream == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Unknown writable stream: " + streamId));
        }
        stream.writeText(text);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    CompletableFuture<Void> writableWriteBytes(String streamId, byte[] data) {
        FakeWritableStream stream = writables.get(streamId);
        if (stream == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Unknown writable stream: " + streamId));
        }
        stream.writeBytes(data);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    CompletableFuture<Void> writableSeek(String streamId, long position) {
        FakeWritableStream stream = writables.get(streamId);
        if (stream == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Unknown writable stream: " + streamId));
        }
        stream.seek(position);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    CompletableFuture<Void> writableTruncate(String streamId, long size) {
        FakeWritableStream stream = writables.get(streamId);
        if (stream == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Unknown writable stream: " + streamId));
        }
        stream.truncate(size);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    CompletableFuture<Void> writableClose(String streamId) {
        FakeWritableStream stream = writables.remove(streamId);
        if (stream == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Unknown writable stream: " + streamId));
        }
        stream.close();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    CompletableFuture<Void> uploadTo(String handleId, UploadHandler handler) {
        // Simplified: not simulating HTTP upload in browserless tests
        return CompletableFuture.completedFuture(null);
    }

    @Override
    CompletableFuture<Void> downloadFrom(String handleId, DownloadHandler handler) {
        // Simplified: not simulating HTTP download in browserless tests
        return CompletableFuture.completedFuture(null);
    }

    // -- OPFS single-roundtrip overrides --

    @Override
    CompletableFuture<FileSystemFileHandle> opfsGetFileHandle(String path, GetHandleOptions options) {
        return navigateToParent(path, options.create())
                .thenCompose(dirId -> getFileHandle(dirId, leafName(path), options));
    }

    @Override
    CompletableFuture<FileSystemDirectoryHandle> opfsGetDirectoryHandle(String path, GetHandleOptions options) {
        return navigateToParent(path, options.create())
                .thenCompose(dirId -> getDirectoryHandle(dirId, leafName(path), options));
    }

    @Override
    CompletableFuture<FileData> opfsReadFile(String path) {
        return opfsGetFileHandle(path, GetHandleOptions.builder().build()).thenCompose(FileSystemFileHandle::getFile);
    }

    @Override
    CompletableFuture<Void> opfsWriteText(String path, String text) {
        return opfsGetFileHandle(path, GetHandleOptions.creating())
                .thenCompose(file -> file.createWritable())
                .thenCompose(writable -> writable.write(text).thenCompose(v -> writable.close()));
    }

    @Override
    CompletableFuture<Void> opfsWriteBytes(String path, byte[] data) {
        return opfsGetFileHandle(path, GetHandleOptions.creating())
                .thenCompose(file -> file.createWritable())
                .thenCompose(writable -> writable.write(data).thenCompose(v -> writable.close()));
    }

    @Override
    CompletableFuture<List<FileSystemHandle>> opfsEntries(String path) {
        if (path == null || path.isEmpty()) {
            return getOriginPrivateDirectory().thenCompose(FileSystemDirectoryHandle::entries);
        }
        return opfsGetDirectoryHandle(path, GetHandleOptions.builder().build())
                .thenCompose(FileSystemDirectoryHandle::entries);
    }

    @Override
    CompletableFuture<Void> opfsRemoveEntry(String path, RemoveEntryOptions options) {
        return navigateToParent(path, false).thenCompose(dirId -> removeEntry(dirId, leafName(path), options));
    }

    @Override
    CompletableFuture<Void> opfsClear() {
        return getOriginPrivateDirectory().thenCompose(root -> root.entries().thenCompose(list -> {
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            for (FileSystemHandle entry : list) {
                chain = chain.thenCompose(v -> root.removeEntry(entry.getName(), RemoveEntryOptions.recursively()));
            }
            return chain;
        }));
    }

    private CompletableFuture<String> navigateToParent(String path, boolean create) {
        String[] segments = path.split("/");
        GetHandleOptions dirOptions = create
                ? GetHandleOptions.creating()
                : GetHandleOptions.builder().build();
        return getOriginPrivateDirectory().thenCompose(root -> {
            CompletableFuture<String> current = CompletableFuture.completedFuture(root.handleId());
            for (int i = 0; i < segments.length - 1; i++) {
                final String segment = segments[i];
                current = current.thenCompose(
                        dirId -> getDirectoryHandle(dirId, segment, dirOptions).thenApply(d -> d.handleId()));
            }
            return current;
        });
    }

    private static String leafName(String path) {
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    @Override
    void releaseHandle(String handleId) {
        fs.removeHandle(handleId);
    }
}
