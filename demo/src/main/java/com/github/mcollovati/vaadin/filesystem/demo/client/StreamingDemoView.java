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

import com.github.mcollovati.vaadin.filesystem.SaveFilePickerOptions;
import com.github.mcollovati.vaadin.filesystem.demo.AbstractDemoView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Demo view showcasing streaming transfers with the high-level API.
 */
@Route("streaming")
public class StreamingDemoView extends AbstractDemoView {

    public StreamingDemoView() {
        super(
                "Streaming Transfers",
                "Upload a file via openFile(UploadHandler) or download server content "
                        + "via saveFile(DownloadHandler). The high-level API combines picker "
                        + "and streaming in a single call.");
    }

    @Override
    protected String codeSnippet() {
        return """
                // Upload: pick file + stream to server in one call
                var handler = UploadHandler.inMemory(
                    (metadata, bytes) -> log("Received " + bytes.length));
                fs.openFile(handler);

                // Download: pick save location + stream from server
                var handler = DownloadHandler.fromInputStream(event ->
                    new DownloadResponse(inputStream, "file.txt",
                        "text/plain", content.length));
                fs.saveFile(handler);""";
    }

    @Override
    protected void addActions() {
        var upload = new Button("Upload File to Server", e -> onUpload());
        var download = new Button("Download Content to File", e -> onDownload());
        add(new HorizontalLayout(upload, download));
    }

    private void onUpload() {
        var handler = UploadHandler.inMemory((metadata, bytes) -> appendLog(
                "Uploaded: " + metadata.fileName() + " (" + bytes.length + " bytes, " + metadata.contentType() + ")"));
        fs().openFile(handler).thenRun(() -> appendLog("Upload complete")).exceptionally(this::logError);
    }

    private void onDownload() {
        byte[] content = "Hello from Vaadin DownloadHandler!\nThis content was streamed to the file."
                .getBytes(StandardCharsets.UTF_8);
        var options =
                SaveFilePickerOptions.builder().suggestedName("streamed.txt").build();
        var handler = DownloadHandler.fromInputStream(event ->
                new DownloadResponse(new ByteArrayInputStream(content), "streamed.txt", "text/plain", content.length));
        fs().saveFile(options, handler)
                .thenRun(() -> appendLog("Download complete"))
                .exceptionally(this::logError);
    }
}
