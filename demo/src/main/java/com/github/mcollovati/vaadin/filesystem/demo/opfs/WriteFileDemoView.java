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
package com.github.mcollovati.vaadin.filesystem.demo.opfs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

/**
 * Demo view showcasing file writing with the Origin Private File System.
 */
@Route("opfs/write-file")
public class WriteFileDemoView extends AbstractOpfsDemoView {

    public WriteFileDemoView() {
        super(
                "Write File",
                "Write content to OPFS with opfs.writeFile(path, content). "
                        + "Intermediate directories are created automatically. "
                        + "Supports both text and binary content.");
    }

    @Override
    String codeSnippet() {
        return """
                // Write text content
                opfs.writeFile("docs/hello.txt",
                    "Hello from Vaadin!");

                // Write binary content
                opfs.writeFile("data/output.bin",
                    new byte[] {0x48, 0x65, 0x6C, 0x6C, 0x6F});""";
    }

    @Override
    void addActions() {
        var writeText = new Button("Write Text", e -> onWriteText());
        var writeBytes = new Button("Write Bytes", e -> onWriteBytes());
        add(new HorizontalLayout(writeText, writeBytes));
    }

    private void onWriteText() {
        opfs().writeFile("docs/hello.txt", "Hello from Vaadin File System API!")
                .thenRun(() -> appendLog("Write complete: docs/hello.txt"))
                .exceptionally(error -> {
                    logError(error);
                    return null;
                });
    }

    private void onWriteBytes() {
        opfs().writeFile("data/output.bin", new byte[] {0x48, 0x65, 0x6C, 0x6C, 0x6F})
                .thenRun(() -> appendLog("Write complete: data/output.bin"))
                .exceptionally(error -> {
                    logError(error);
                    return null;
                });
    }
}
