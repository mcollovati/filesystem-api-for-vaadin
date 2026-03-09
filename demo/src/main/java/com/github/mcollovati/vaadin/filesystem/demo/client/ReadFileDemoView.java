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

import com.github.mcollovati.vaadin.filesystem.FileData;
import com.github.mcollovati.vaadin.filesystem.demo.AbstractDemoView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.Route;
import java.nio.charset.StandardCharsets;

/**
 * Demo view showcasing file reading with the high-level API.
 */
@Route("read-file")
public class ReadFileDemoView extends AbstractDemoView {

    public ReadFileDemoView() {
        super(
                "Read File",
                "Open and read a file in one call with fs.openFile(). "
                        + "The method picks a file and returns a FileData object "
                        + "containing name, size, MIME type, and byte content.");
    }

    @Override
    protected String codeSnippet() {
        return """
                fs.openFile().thenAccept(fileData -> {
                    String name = fileData.getName();
                    long size = fileData.getSize();
                    String type = fileData.getType();
                    byte[] content = fileData.getContent();
                    InputStream is = fileData.getContentAsInputStream();
                });""";
    }

    @Override
    protected void addActions() {
        add(new Button("Read File Content", e -> onReadFile()));
    }

    private void onReadFile() {
        fs().openFile().thenAccept(this::logFileData).exceptionally(this::logError);
    }

    private void logFileData(FileData fileData) {
        var sb = new StringBuilder("Read File: ")
                .append(fileData.getName())
                .append(" (")
                .append(fileData.getType())
                .append(", ")
                .append(fileData.getSize())
                .append(" bytes)");
        if (fileData.getType().startsWith("text/") && fileData.getSize() > 0) {
            var text = new String(fileData.getContent(), StandardCharsets.UTF_8);
            var preview = text.length() > 200 ? text.substring(0, 200) + "..." : text;
            sb.append("\n  Content: ").append(preview);
        }
        appendLog(sb.toString());
    }
}
