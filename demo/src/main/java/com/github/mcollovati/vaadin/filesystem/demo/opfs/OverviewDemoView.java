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

import com.github.mcollovati.vaadin.filesystem.FileSystemHandle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import java.util.List;

/**
 * Demo view showcasing basic Origin Private File System operations.
 */
@Route("opfs")
public class OverviewDemoView extends AbstractOpfsDemoView {

    public OverviewDemoView() {
        super(
                "Origin Private File System",
                "The Origin Private File System (OPFS) provides sandboxed, persistent "
                        + "storage accessible via path-based operations. No file picker dialogs "
                        + "are needed — the browser manages the storage automatically.");
    }

    @Override
    String codeSnippet() {
        return """
                var opfs = new OriginPrivateFileSystem(component);

                // Write and read back a text file
                opfs.writeFile("hello.txt", "Hello from OPFS!")
                    .thenCompose(v -> opfs.readFile("hello.txt"))
                    .thenAccept(data -> log(data.getName() + ": "
                        + new String(data.getContent())));

                // List root entries
                opfs.list().thenAccept(entries ->
                    entries.forEach(e -> log(e.getName())));

                // Clear all entries
                opfs.clear();""";
    }

    @Override
    void addActions() {
        var writeRead = new Button("Write & Read", e -> onWriteAndRead());
        var listRoot = new Button("List Root", e -> onListRoot());
        var clearAll = new Button("Clear OPFS", e -> onClearAll());
        add(new HorizontalLayout(writeRead, listRoot, clearAll));
    }

    private void onWriteAndRead() {
        opfs().writeFile("hello.txt", "Hello from OPFS!")
                .thenCompose(v -> opfs().readFile("hello.txt"))
                .thenAccept(data -> {
                    appendLog("Wrote and read back: " + data.getName());
                    appendLog("  Content: " + new String(data.getContent()));
                    appendLog("  Size: " + data.getSize() + " bytes");
                })
                .exceptionally(error -> {
                    logError(error);
                    return null;
                });
    }

    private void onListRoot() {
        opfs().list().thenAccept(this::logEntries).exceptionally(error -> {
            logError(error);
            return null;
        });
    }

    private void onClearAll() {
        opfs().clear().thenRun(() -> appendLog("OPFS cleared")).exceptionally(error -> {
            logError(error);
            return null;
        });
    }

    private void logEntries(List<FileSystemHandle> entries) {
        if (entries.isEmpty()) {
            appendLog("OPFS root is empty");
            return;
        }
        appendLog("OPFS root entries:");
        for (var entry : entries) {
            appendLog("  " + entry.getKind() + ": " + entry.getName());
        }
    }
}
