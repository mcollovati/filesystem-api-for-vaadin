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
package com.github.mcollovati.vaadin.filesystem.demo;

import com.github.mcollovati.vaadin.filesystem.FileTypeFilter;
import com.github.mcollovati.vaadin.filesystem.SaveFilePickerOptions;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

/**
 * Demo view showcasing file writing with the high-level API.
 */
@Route("write-file")
public class WriteFileDemoView extends AbstractDemoView {

    public WriteFileDemoView() {
        super(
                "Write File",
                "Pick a save location and write content in one call with "
                        + "fs.saveFile(options, text). The method handles picker + "
                        + "writable stream creation + write + close internally.");
    }

    @Override
    protected String codeSnippet() {
        return """
                var opts = SaveFilePickerOptions.builder()
                        .suggestedName("hello.txt")
                        .types(FileTypeFilter.of("Text files",
                            "text/plain", ".txt"))
                        .build();

                fs.saveFile(opts, "Hello from Vaadin!");""";
    }

    @Override
    protected void addActions() {
        var writeText = new Button("Write Text", e -> onWriteText());
        var writeBytes = new Button("Write Bytes", e -> onWriteBytes());
        add(new HorizontalLayout(writeText, writeBytes));
    }

    private void onWriteText() {
        var options = SaveFilePickerOptions.builder()
                .suggestedName("hello.txt")
                .types(FileTypeFilter.of("Text files", "text/plain", ".txt"))
                .build();
        fs().saveFile(options, "Hello from Vaadin File System API!")
                .thenRun(() -> appendLog("Write complete"))
                .exceptionally(this::logError);
    }

    private void onWriteBytes() {
        var options = SaveFilePickerOptions.builder().suggestedName("data.bin").build();
        fs().saveFile(options, new byte[] {0x48, 0x65, 0x6C, 0x6C, 0x6F})
                .thenRun(() -> appendLog("Write complete"))
                .exceptionally(this::logError);
    }
}
