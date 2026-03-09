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
package com.github.mcollovati.vaadin.filesystem.demo.client;

import com.github.mcollovati.vaadin.filesystem.FileTypeFilter;
import com.github.mcollovati.vaadin.filesystem.OpenFilePickerOptions;
import com.github.mcollovati.vaadin.filesystem.demo.AbstractDemoView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

/**
 * Demo view showcasing the high-level file picker operations.
 */
@Route("")
public class FilePickerDemoView extends AbstractDemoView {

    public FilePickerDemoView() {
        super(
                "File Pickers",
                "Open files and read their content in a single call with openFile() "
                        + "and openFiles(). The high-level API combines picker + read into "
                        + "one step, returning FileData directly.");

        var status = new Span("Checking...");
        fs().isSupported().thenAccept(supported -> getUI().ifPresent(ui -> ui.access(() -> {
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
    protected String codeSnippet() {
        return """
                // Open and read a single file
                fs.openFile().thenAccept(fileData ->
                    log(fileData.getName() + ": " + fileData.getSize() + " bytes"));

                // Open and read multiple files
                fs.openFiles().thenAccept(files ->
                    files.forEach(f -> log(f.getName())));

                // Filtered by type
                var opts = OpenFilePickerOptions.builder()
                        .types(FileTypeFilter.of("Images", "image/*",
                            ".png", ".jpg"))
                        .build();
                fs.openFile(opts).thenAccept(fileData -> { ... });""";
    }

    @Override
    protected void addActions() {
        var openFile = new Button("Open File", e -> onOpenFile());
        var openMultiple = new Button("Open Multiple", e -> onOpenMultiple());
        var openImages = new Button("Open Images", e -> onOpenImages());
        add(new HorizontalLayout(openFile, openMultiple, openImages));
    }

    private void onOpenFile() {
        fs().openFile()
                .thenAccept(
                        fileData -> appendLog("Opened: " + fileData.getName() + " (" + fileData.getSize() + " bytes)"))
                .exceptionally(this::logError);
    }

    private void onOpenMultiple() {
        fs().openFiles()
                .thenAccept(files -> {
                    appendLog("Opened " + files.size() + " file(s):");
                    files.forEach(f -> appendLog("  " + f.getName() + " (" + f.getSize() + " bytes)"));
                })
                .exceptionally(this::logError);
    }

    private void onOpenImages() {
        var options = OpenFilePickerOptions.builder()
                .types(FileTypeFilter.of("Images", "image/*", ".png", ".jpg", ".gif"))
                .build();
        fs().openFile(options)
                .thenAccept(
                        fileData -> appendLog("Opened: " + fileData.getName() + " (" + fileData.getSize() + " bytes)"))
                .exceptionally(this::logError);
    }
}
