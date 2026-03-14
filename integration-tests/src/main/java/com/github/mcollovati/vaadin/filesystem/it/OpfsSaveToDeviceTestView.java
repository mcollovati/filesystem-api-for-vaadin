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
package com.github.mcollovati.vaadin.filesystem.it;

import com.github.mcollovati.vaadin.filesystem.FileSystemFileHandle;
import com.github.mcollovati.vaadin.filesystem.GetHandleOptions;
import com.vaadin.flow.router.Route;

@Route("test/opfs-save-to-device")
public class OpfsSaveToDeviceTestView extends AbstractOpfsTestView {

    @Override
    void addActions() {
        add(button("save-to-device", "Save to Device", this::onSaveToDevice));
        add(button("save-to-device-custom-name", "Save with Custom Name", this::onSaveToDeviceCustomName));
        add(button("save-to-device-handle", "Save via Handle", this::onSaveToDeviceHandle));
    }

    private void onSaveToDevice() {
        opfs().clear()
                .thenCompose(v -> opfs().writeFile("hello.txt", "hello from opfs"))
                .thenCompose(v -> opfs().saveToDevice("hello.txt"))
                .thenRun(() -> appendLog("save-complete"))
                .exceptionally(this::logError);
    }

    private void onSaveToDeviceCustomName() {
        opfs().clear()
                .thenCompose(v -> opfs().writeFile("data.txt", "custom name content"))
                .thenCompose(v -> opfs().saveToDevice("data.txt", "renamed.txt"))
                .thenRun(() -> appendLog("save-custom-complete"))
                .exceptionally(this::logError);
    }

    private void onSaveToDeviceHandle() {
        opfs().clear()
                .thenCompose(v -> opfs().writeFile("handle-test.txt", "handle save content"))
                .thenCompose(v -> opfs().getFileHandle(
                                "handle-test.txt", GetHandleOptions.builder().build()))
                .thenCompose(FileSystemFileHandle::saveToDevice)
                .thenRun(() -> appendLog("save-handle-complete"))
                .exceptionally(this::logError);
    }
}
