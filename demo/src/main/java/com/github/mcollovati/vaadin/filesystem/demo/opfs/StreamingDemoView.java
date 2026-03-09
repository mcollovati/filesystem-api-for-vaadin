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
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Demo view showcasing streaming transfers with the Origin Private File System.
 */
@Route("opfs/streaming")
public class StreamingDemoView extends AbstractOpfsDemoView {

    public StreamingDemoView() {
        super(
                "Streaming Transfers",
                "Stream files between OPFS and the server with uploadFile(path, handler) "
                        + "and downloadFile(path, handler). Uses HTTP streaming for efficient "
                        + "transfer of large files without loading them fully into memory.");
    }

    @Override
    String codeSnippet() {
        return """
                // Upload: stream OPFS file to server
                var handler = UploadHandler.inMemory(
                    (metadata, bytes) -> log("Received " + bytes.length));
                opfs.uploadFile("data.txt", handler);

                // Download: stream server content to OPFS file
                var handler = DownloadHandler.fromInputStream(event ->
                    new DownloadResponse(inputStream, "file.txt",
                        "text/plain", content.length));
                opfs.downloadFile("data.txt", handler);""";
    }

    @Override
    void addActions() {
        var upload = new Button("Upload File to Server", e -> onUpload());
        var download = new Button("Download Content to File", e -> onDownload());
        add(new HorizontalLayout(upload, download));
    }

    private void onUpload() {
        var handler = UploadHandler.inMemory((metadata, bytes) -> appendLog(
                "Uploaded: " + metadata.fileName() + " (" + bytes.length + " bytes, " + metadata.contentType() + ")"));
        opfs().writeFile("upload-sample.txt", "Sample content for upload demo")
                .thenCompose(v -> opfs().uploadFile("upload-sample.txt", handler))
                .thenRun(() -> appendLog("Upload complete"))
                .exceptionally(error -> {
                    logError(error);
                    return null;
                });
    }

    private void onDownload() {
        byte[] content = "Hello from Vaadin DownloadHandler!\nThis content was streamed to OPFS."
                .getBytes(StandardCharsets.UTF_8);
        var handler = DownloadHandler.fromInputStream(event ->
                new DownloadResponse(new ByteArrayInputStream(content), "streamed.txt", "text/plain", content.length));
        opfs().downloadFile("streamed.txt", handler)
                .thenRun(() -> appendLog("Download complete: streamed.txt"))
                .exceptionally(error -> {
                    logError(error);
                    return null;
                });
    }
}
