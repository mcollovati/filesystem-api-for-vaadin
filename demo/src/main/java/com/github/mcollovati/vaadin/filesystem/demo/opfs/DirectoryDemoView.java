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
 * Demo view showcasing directory operations with the Origin Private File System.
 */
@Route("opfs/directory")
public class DirectoryDemoView extends AbstractOpfsDemoView {

    public DirectoryDemoView() {
        super(
                "Directory Operations",
                "List and manage OPFS directories with path-based operations. "
                        + "Use list() for root entries, list(path) for subdirectories, "
                        + "removeEntry(path) for single entries, and clear() to remove everything.");
    }

    @Override
    String codeSnippet() {
        return """
                // List root entries
                opfs.list().thenAccept(entries ->
                    entries.forEach(e ->
                        log(e.getKind() + ": " + e.getName())));

                // List a subdirectory
                opfs.list("docs").thenAccept(entries -> { ... });

                // Remove a single entry
                opfs.removeEntry("docs/old.txt");

                // Clear everything
                opfs.clear();""";
    }

    @Override
    void addActions() {
        var listRoot = new Button("List Root", e -> onListRoot());
        var listSubdir = new Button("List Subdirectory", e -> onListSubdirectory());
        var removeEntry = new Button("Remove Entry", e -> onRemoveEntry());
        var clearAll = new Button("Clear All", e -> onClearAll());
        add(new HorizontalLayout(listRoot, listSubdir, removeEntry, clearAll));
    }

    private void onListRoot() {
        opfs().list().thenAccept(this::logEntries).exceptionally(error -> {
            logError(error);
            return null;
        });
    }

    private void onListSubdirectory() {
        opfs().writeFile("docs/readme.txt", "Sample content")
                .thenCompose(v -> opfs().list("docs"))
                .thenAccept(entries -> {
                    appendLog("Entries in docs/:");
                    logEntriesContent(entries);
                })
                .exceptionally(error -> {
                    logError(error);
                    return null;
                });
    }

    private void onRemoveEntry() {
        opfs().writeFile("temp.txt", "Temporary file")
                .thenCompose(v -> {
                    appendLog("Created temp.txt");
                    return opfs().removeEntry("temp.txt");
                })
                .thenRun(() -> appendLog("Removed temp.txt"))
                .exceptionally(error -> {
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
        appendLog("OPFS root entries:");
        logEntriesContent(entries);
    }

    private void logEntriesContent(List<FileSystemHandle> entries) {
        if (entries.isEmpty()) {
            appendLog("  (empty)");
            return;
        }
        for (var entry : entries) {
            appendLog("  " + entry.getKind() + ": " + entry.getName());
        }
    }
}
