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

import static org.junit.jupiter.api.Assertions.*;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OriginPrivateFileSystemTest {

    @Test
    void constructorRejectsNull() {
        assertThrows(NullPointerException.class, () -> new OriginPrivateFileSystem(null));
    }

    @Test
    void getFileHandleRejectsNullPath() {
        var opfs = createOpfs(new FakeBridge());
        assertThrows(NullPointerException.class, () -> opfs.getFileHandle(null));
    }

    @Test
    void getFileHandleRejectsEmptyPath() {
        var opfs = createOpfs(new FakeBridge());
        assertThrows(IllegalArgumentException.class, () -> opfs.getFileHandle(""));
    }

    @Test
    void getFileHandleRejectsSlashOnlyPath() {
        var opfs = createOpfs(new FakeBridge());
        assertThrows(IllegalArgumentException.class, () -> opfs.getFileHandle("/"));
    }

    @Test
    void getDirectoryHandleRejectsNullPath() {
        var opfs = createOpfs(new FakeBridge());
        assertThrows(NullPointerException.class, () -> opfs.getDirectoryHandle(null));
    }

    @Test
    void getDirectoryHandleRejectsEmptyPath() {
        var opfs = createOpfs(new FakeBridge());
        assertThrows(IllegalArgumentException.class, () -> opfs.getDirectoryHandle(""));
    }

    @Test
    void removeEntryRejectsNullPath() {
        var opfs = createOpfs(new FakeBridge());
        assertThrows(NullPointerException.class, () -> opfs.removeEntry(null));
    }

    @Test
    void removeEntryRejectsEmptyPath() {
        var opfs = createOpfs(new FakeBridge());
        assertThrows(IllegalArgumentException.class, () -> opfs.removeEntry(""));
    }

    @Test
    void rootIsCached() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);

        CompletableFuture<FileSystemDirectoryHandle> first = opfs.root();
        CompletableFuture<FileSystemDirectoryHandle> second = opfs.root();

        assertSame(first, second);
        assertEquals(1, bridge.rootCallCount.get(), "getOriginPrivateDirectory should be called only once");
    }

    @Test
    void rootCacheClearedOnFailure() {
        var bridge = new FakeBridge();
        bridge.rootShouldFail = true;
        var opfs = createOpfs(bridge);

        CompletableFuture<FileSystemDirectoryHandle> first = opfs.root();
        assertTrue(first.isCompletedExceptionally());

        bridge.rootShouldFail = false;
        CompletableFuture<FileSystemDirectoryHandle> second = opfs.root();
        assertNotSame(first, second, "cache should be cleared after failure");
        assertEquals(2, bridge.rootCallCount.get(), "getOriginPrivateDirectory should retry after failure");
    }

    @Test
    void singleSegmentPathDelegatesToBridge() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);

        FileSystemFileHandle handle = opfs.getFileHandle("test.txt").join();

        assertEquals("test.txt", handle.getName());
        assertEquals(1, bridge.fileHandleRequests.size());
        assertEquals("test.txt", bridge.fileHandleRequests.get(0).path);
    }

    @Test
    void multiSegmentPathPassedToBridge() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);

        FileSystemFileHandle handle =
                opfs.getFileHandle("a/b/file.txt", GetHandleOptions.creating()).join();

        assertEquals("file.txt", handle.getName());
        assertEquals(1, bridge.fileHandleRequests.size());
        assertEquals("a/b/file.txt", bridge.fileHandleRequests.get(0).path);
    }

    @Test
    void writeFileDelegatesToBridge() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);

        opfs.writeFile("data.txt", "hello").join();

        assertEquals("data.txt", bridge.writeTextPath);
        assertEquals("hello", bridge.writeTextContent);
    }

    @Test
    void clearDelegatesToBridge() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);

        opfs.clear().join();

        assertTrue(bridge.clearCalled);
    }

    @Test
    void getDirectoryHandleDelegatesToBridge() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);

        FileSystemDirectoryHandle handle =
                opfs.getDirectoryHandle("a/b/sub", GetHandleOptions.creating()).join();

        assertEquals("sub", handle.getName());
        assertEquals(1, bridge.dirHandleRequests.size());
        assertEquals("a/b/sub", bridge.dirHandleRequests.get(0).path);
    }

    @Test
    void removeEntryDelegatesToBridge() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);

        opfs.removeEntry("a/b/old.txt", RemoveEntryOptions.recursively()).join();

        assertEquals(1, bridge.removeRequests.size());
        assertEquals("a/b/old.txt", bridge.removeRequests.get(0).path);
    }

    @Test
    void getDirectoryHandleDefaultOptionsDelegatesToBridge() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);

        FileSystemDirectoryHandle handle = opfs.getDirectoryHandle("docs").join();

        assertEquals("docs", handle.getName());
        assertEquals(1, bridge.dirHandleRequests.size());
        assertEquals("docs", bridge.dirHandleRequests.get(0).path);
    }

    @Test
    void readFileDelegatesToBridge() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);

        FileData data = opfs.readFile("notes.txt").join();

        assertNotNull(data);
        assertEquals("notes.txt", bridge.readFilePath);
    }

    @Test
    void writeFileBytesDelegatesToBridge() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);
        byte[] content = {1, 2, 3};

        opfs.writeFile("bin.dat", content).join();

        assertEquals("bin.dat", bridge.writeBytesPath);
        assertArrayEquals(content, bridge.writeBytesContent);
    }

    @Test
    void uploadFileDelegatesToBridge() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);
        UploadHandler handler = event -> {};

        opfs.uploadFile("doc.pdf", handler).join();

        assertEquals(1, bridge.fileHandleRequests.size());
        assertEquals("doc.pdf", bridge.fileHandleRequests.get(0).path);
        assertSame(handler, bridge.lastUploadHandler);
    }

    @Test
    void downloadFileDelegatesToBridge() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);
        DownloadHandler handler = event -> {};

        opfs.downloadFile("out.txt", handler).join();

        assertEquals(1, bridge.fileHandleRequests.size());
        assertEquals("out.txt", bridge.fileHandleRequests.get(0).path);
        assertSame(handler, bridge.lastDownloadHandler);
    }

    @Test
    void removeEntryDefaultOptionsDelegatesToBridge() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);

        opfs.removeEntry("stale.txt").join();

        assertEquals(1, bridge.removeRequests.size());
        assertEquals("stale.txt", bridge.removeRequests.get(0).path);
    }

    @Test
    void listWithPathDelegatesToBridge() {
        var bridge = new FakeBridge();
        bridge.entries.add(new FileSystemFileHandle("f1", "nested.txt", bridge));
        var opfs = createOpfs(bridge);

        List<FileSystemHandle> result = opfs.list("sub/dir").join();

        assertEquals(1, result.size());
        assertEquals("nested.txt", result.get(0).getName());
        assertEquals("sub/dir", bridge.lastEntriesPath);
    }

    @Test
    void saveToDeviceRejectsNullPath() {
        var opfs = createOpfs(new FakeBridge());
        assertThrows(NullPointerException.class, () -> opfs.saveToDevice(null));
    }

    @Test
    void saveToDeviceRejectsEmptyPath() {
        var opfs = createOpfs(new FakeBridge());
        assertThrows(IllegalArgumentException.class, () -> opfs.saveToDevice(""));
    }

    @Test
    void saveToDeviceDelegatesWithDefaultName() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);

        opfs.saveToDevice("archive.zip").join();

        assertEquals("archive.zip", bridge.saveToDevicePath);
        assertNull(bridge.saveToDeviceDownloadName);
    }

    @Test
    void saveToDeviceDelegatesWithCustomName() {
        var bridge = new FakeBridge();
        var opfs = createOpfs(bridge);

        opfs.saveToDevice("data/archive.zip", "my-download.zip").join();

        assertEquals("data/archive.zip", bridge.saveToDevicePath);
        assertEquals("my-download.zip", bridge.saveToDeviceDownloadName);
    }

    @Test
    void listDelegatesEntriesToRoot() {
        var bridge = new FakeBridge();
        bridge.entries.add(new FileSystemFileHandle("f1", "file.txt", bridge));
        var opfs = createOpfs(bridge);

        List<FileSystemHandle> result = opfs.list().join();

        assertEquals(1, result.size());
        assertEquals("file.txt", result.get(0).getName());
    }

    private static OriginPrivateFileSystem createOpfs(FakeBridge bridge) {
        var component = new FakeComponent();
        ComponentUtil.setData(component, JsBridge.class, bridge);
        return new OriginPrivateFileSystem(component);
    }

    @Tag("div")
    private static class FakeComponent extends Component {
        // minimal component stub for testing
    }

    record OpfsRequest(String path, Object options) {}

    private static class FakeBridge extends JsBridge {
        final List<OpfsRequest> fileHandleRequests = new ArrayList<>();
        final List<OpfsRequest> dirHandleRequests = new ArrayList<>();
        final List<OpfsRequest> removeRequests = new ArrayList<>();
        final List<FileSystemHandle> entries = new ArrayList<>();
        final AtomicInteger rootCallCount = new AtomicInteger();
        boolean rootShouldFail;
        boolean clearCalled;
        String writeTextPath;
        String writeTextContent;
        String writeBytesPath;
        byte[] writeBytesContent;
        String readFilePath;
        String lastEntriesPath;
        UploadHandler lastUploadHandler;
        DownloadHandler lastDownloadHandler;
        String saveToDevicePath;
        String saveToDeviceDownloadName;
        String handleSaveToDeviceId;
        String handleSaveToDeviceDownloadName;
        private final AtomicInteger idCounter = new AtomicInteger();

        FakeBridge() {
            super(null);
        }

        @Override
        CompletableFuture<FileSystemDirectoryHandle> getOriginPrivateDirectory() {
            rootCallCount.incrementAndGet();
            if (rootShouldFail) {
                return CompletableFuture.failedFuture(new FileSystemApiException("OPFS not available"));
            }
            return CompletableFuture.completedFuture(new FileSystemDirectoryHandle("root", "", this));
        }

        @Override
        CompletableFuture<FileSystemFileHandle> opfsGetFileHandle(String path, GetHandleOptions options) {
            fileHandleRequests.add(new OpfsRequest(path, options));
            String leafName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
            String id = "file-" + idCounter.incrementAndGet();
            return CompletableFuture.completedFuture(new FileSystemFileHandle(id, leafName, this));
        }

        @Override
        CompletableFuture<FileSystemDirectoryHandle> opfsGetDirectoryHandle(String path, GetHandleOptions options) {
            dirHandleRequests.add(new OpfsRequest(path, options));
            String leafName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
            String id = "dir-" + idCounter.incrementAndGet();
            return CompletableFuture.completedFuture(new FileSystemDirectoryHandle(id, leafName, this));
        }

        @Override
        CompletableFuture<Void> opfsWriteText(String path, String text) {
            writeTextPath = path;
            writeTextContent = text;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        CompletableFuture<Void> opfsWriteBytes(String path, byte[] data) {
            writeBytesPath = path;
            writeBytesContent = data;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        CompletableFuture<FileData> opfsReadFile(String path) {
            readFilePath = path;
            return CompletableFuture.completedFuture(new FileData("test", 0, "", 0, new byte[0]));
        }

        @Override
        CompletableFuture<List<FileSystemHandle>> opfsEntries(String path) {
            lastEntriesPath = path;
            return CompletableFuture.completedFuture(new ArrayList<>(entries));
        }

        @Override
        CompletableFuture<Void> uploadTo(String handleId, UploadHandler handler) {
            lastUploadHandler = handler;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        CompletableFuture<Void> downloadFrom(String handleId, DownloadHandler handler) {
            lastDownloadHandler = handler;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        CompletableFuture<Void> opfsRemoveEntry(String path, RemoveEntryOptions options) {
            removeRequests.add(new OpfsRequest(path, options));
            return CompletableFuture.completedFuture(null);
        }

        @Override
        CompletableFuture<Void> opfsClear() {
            clearCalled = true;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        CompletableFuture<Void> opfsSaveToDevice(String path, String downloadName) {
            saveToDevicePath = path;
            saveToDeviceDownloadName = downloadName;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        CompletableFuture<Void> saveToDevice(String handleId, String downloadName) {
            handleSaveToDeviceId = handleId;
            handleSaveToDeviceDownloadName = downloadName;
            return CompletableFuture.completedFuture(null);
        }
    }
}
