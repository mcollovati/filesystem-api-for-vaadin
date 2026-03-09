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

import com.github.mcollovati.vaadin.filesystem.FileData;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.Route;
import java.nio.charset.StandardCharsets;

/**
 * Demo view showcasing file reading with the Origin Private File System.
 */
@Route("opfs/read-file")
public class ReadFileDemoView extends AbstractOpfsDemoView {

    private static final String SAMPLE_PATH = "sample.txt";
    private static final String SAMPLE_CONTENT = "Hello from OPFS!\nThis is a sample file for the read demo.";

    public ReadFileDemoView() {
        super(
                "Read File",
                "Read a file from OPFS with opfs.readFile(path). "
                        + "The method returns a FileData object containing name, size, "
                        + "MIME type, and byte content. A sample file is written first "
                        + "to demonstrate read-back.");
    }

    @Override
    String codeSnippet() {
        return """
                opfs.readFile("sample.txt").thenAccept(fileData -> {
                    String name = fileData.getName();
                    long size = fileData.getSize();
                    String type = fileData.getType();
                    byte[] content = fileData.getContent();
                    InputStream is = fileData.getContentAsInputStream();
                });""";
    }

    @Override
    void addActions() {
        add(new Button("Read File Content", e -> onReadFile()));
    }

    private void onReadFile() {
        opfs().writeFile(SAMPLE_PATH, SAMPLE_CONTENT)
                .thenCompose(v -> opfs().readFile(SAMPLE_PATH))
                .thenAccept(this::logFileData)
                .exceptionally(error -> {
                    logError(error);
                    return null;
                });
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
