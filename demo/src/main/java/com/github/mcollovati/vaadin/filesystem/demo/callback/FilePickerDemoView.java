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
package com.github.mcollovati.vaadin.filesystem.demo.callback;

import com.github.mcollovati.vaadin.filesystem.FileTypeFilter;
import com.github.mcollovati.vaadin.filesystem.OpenFilePickerOptions;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

/**
 * Demo view showcasing file picker operations with the callback API.
 */
@Route("callback/file-pickers")
public class FilePickerDemoView extends AbstractCallbackDemoView {

    public FilePickerDemoView() {
        super(
                "File Pickers (Callback API)",
                "Open files and read their content using callbacks. "
                        + "No CompletableFuture chaining needed — just provide "
                        + "success and error handlers.");

        var status = new Span("Checking...");
        fs().isSupported(supported -> getUI().ifPresent(ui -> ui.access(() -> {
            if (supported) {
                status.setText("File System API is supported");
                status.getStyle().set("color", "var(--lumo-success-color)");
            } else {
                status.setText("File System API is NOT supported in this browser");
                status.getStyle().set("color", "var(--lumo-error-color)");
            }
        })));
        addContent(status);
    }

    @Override
    String codeSnippet() {
        return """
                // Open and read a single file
                fs.openFile(
                    fileData -> log(fileData.getName()),
                    error -> log(error.getMessage()));

                // Open and read multiple files
                fs.openFiles(
                    files -> files.forEach(f -> log(f.getName())),
                    error -> log(error.getMessage()));

                // Filtered by type
                var opts = OpenFilePickerOptions.builder()
                        .types(FileTypeFilter.of("Images", "image/*",
                            ".png", ".jpg"))
                        .build();
                fs.openFile(opts,
                    fileData -> log(fileData.getName()),
                    error -> log(error.getMessage()));""";
    }

    @Override
    void addActions() {
        var openFile = new Button("Open File", e -> onOpenFile());
        var openMultiple = new Button("Open Multiple", e -> onOpenMultiple());
        var openImages = new Button("Open Images", e -> onOpenImages());
        add(new HorizontalLayout(openFile, openMultiple, openImages));
    }

    private void onOpenFile() {
        fs().openFile(
                        fileData -> appendLog("Opened: " + fileData.getName() + " (" + fileData.getSize() + " bytes)"),
                        this::logError);
    }

    private void onOpenMultiple() {
        fs().openFiles(
                        files -> {
                            appendLog("Opened " + files.size() + " file(s):");
                            files.forEach(f -> appendLog("  " + f.getName() + " (" + f.getSize() + " bytes)"));
                        },
                        this::logError);
    }

    private void onOpenImages() {
        var options = OpenFilePickerOptions.builder()
                .types(FileTypeFilter.of("Images", "image/*", ".png", ".jpg", ".gif"))
                .build();
        fs().openFile(
                        options,
                        fileData -> appendLog("Opened: " + fileData.getName() + " (" + fileData.getSize() + " bytes)"),
                        this::logError);
    }
}
