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

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class SaveToDeviceTest {

    @Test
    void saveToDevice_delegatesToBridgeWithHandleIdAndNullDownloadName() {
        var bridge = new FakeBridge();
        var handle = new FileSystemFileHandle("h-42", "report.pdf", bridge);

        handle.saveToDevice().join();

        assertEquals("h-42", bridge.handleId);
        assertNull(bridge.downloadName, "downloadName should be null when using file's own name");
    }

    @Test
    void saveToDeviceWithCustomName_delegatesToBridgeWithHandleIdAndDownloadName() {
        var bridge = new FakeBridge();
        var handle = new FileSystemFileHandle("h-7", "data.csv", bridge);

        handle.saveToDevice("export.csv").join();

        assertEquals("h-7", bridge.handleId);
        assertEquals("export.csv", bridge.downloadName);
    }

    @Test
    void saveToDeviceWithNullName_delegatesToBridgeWithNullDownloadName() {
        var bridge = new FakeBridge();
        var handle = new FileSystemFileHandle("h-99", "notes.txt", bridge);

        handle.saveToDevice(null).join();

        assertEquals("h-99", bridge.handleId);
        assertNull(bridge.downloadName);
    }

    private static class FakeBridge extends JsBridge {
        String handleId;
        String downloadName;

        FakeBridge() {
            super(null);
        }

        @Override
        CompletableFuture<Void> saveToDevice(String handleId, String downloadName) {
            this.handleId = handleId;
            this.downloadName = downloadName;
            return CompletableFuture.completedFuture(null);
        }
    }
}
