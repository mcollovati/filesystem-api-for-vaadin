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

import com.github.mcollovati.vaadin.filesystem.DirectoryPickerOptions;
import com.github.mcollovati.vaadin.filesystem.FileSystemHandle;
import com.github.mcollovati.vaadin.filesystem.PermissionMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import java.util.List;

/**
 * Demo view showcasing directory operations with the high-level API.
 */
@Route("directory")
public class DirectoryDemoView extends AbstractDemoView {

    public DirectoryDemoView() {
        super(
                "Directory Operations",
                "Open a directory with openDirectory() or list entries directly "
                        + "with listDirectory(). The high-level API provides convenient "
                        + "one-step methods for common directory workflows.");
    }

    @Override
    protected String codeSnippet() {
        return """
                // Open and get a directory handle
                fs.openDirectory().thenAccept(dir -> { ... });

                // List entries in one step
                var opts = DirectoryPickerOptions.builder()
                        .mode(PermissionMode.READWRITE).build();
                fs.listDirectory(opts).thenAccept(entries -> {
                    for (var entry : entries) {
                        log(entry.getKind() + ": " + entry.getName());
                    }
                });""";
    }

    @Override
    protected void addActions() {
        var openDir = new Button("Open Directory", e -> onOpenDirectory());
        var listDir = new Button("List Directory", e -> onListDirectory());
        add(new HorizontalLayout(openDir, listDir));
    }

    private void onOpenDirectory() {
        fs().openDirectory()
                .thenAccept(handle -> appendLog("Opened: " + handle.getName() + " (" + handle.getKind() + ")"))
                .exceptionally(this::logError);
    }

    private void onListDirectory() {
        var options =
                DirectoryPickerOptions.builder().mode(PermissionMode.READWRITE).build();
        fs().listDirectory(options).thenAccept(this::logEntries).exceptionally(this::logError);
    }

    private void logEntries(List<FileSystemHandle> entries) {
        if (entries.isEmpty()) {
            appendLog("  (empty directory)");
            return;
        }
        appendLog("Directory entries:");
        for (var entry : entries) {
            appendLog("  " + entry.getKind() + ": " + entry.getName());
        }
    }
}
