package com.github.mcollovati.vaadin.filesystem.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Demo view showcasing streaming file transfers using Vaadin's
 * {@link UploadHandler} and {@link DownloadHandler}.
 *
 * <p>These methods avoid the base64 encoding overhead of
 * {@code getFile()} and {@code write(byte[])}, making them
 * suitable for large files.
 */
@Route("streaming")
public class StreamingDemoView extends AbstractDemoView {

    /**
     * Creates the streaming demo view.
     */
    public StreamingDemoView() {
        super(
                "Streaming Transfers",
                "Upload a file to the server with uploadTo(UploadHandler), or download "
                        + "server content into a file with downloadFrom(DownloadHandler). Both use "
                        + "HTTP streaming instead of base64 encoding, making them efficient for "
                        + "large files.");
    }

    @Override
    String codeSnippet() {
        return """
                // Upload: browser file → server via HTTP streaming
                fs.showOpenFilePicker().thenAccept(handles -> {
                    var handler = UploadHandler.inMemory(
                        (metadata, bytes) -> log("Received " + bytes.length + " bytes"));
                    handles.get(0).uploadTo(handler);
                });

                // Download: server content → browser file via HTTP streaming
                fs.showSaveFilePicker().thenAccept(handle -> {
                    var handler = DownloadHandler.fromInputStream(event ->
                        new DownloadResponse(inputStream, "file.txt",
                            "text/plain", content.length));
                    handle.downloadFrom(handler);
                });""";
    }

    @Override
    void addActions() {
        var upload = new Button("Upload File to Server", e -> onUpload());
        var download = new Button("Download Content to File", e -> onDownload());
        add(new HorizontalLayout(upload, download));
    }

    private void onUpload() {
        fs().showOpenFilePicker()
                .thenAccept(handles -> {
                    if (handles.isEmpty()) return;
                    var handle = handles.get(0);
                    var handler = UploadHandler.inMemory((metadata, bytes) -> appendLog("Uploaded: "
                            + metadata.fileName() + " (" + bytes.length + " bytes, " + metadata.contentType() + ")"));
                    handle.uploadTo(handler)
                            .thenRun(() -> appendLog("Upload complete for " + handle.getName()))
                            .exceptionally(this::logError);
                })
                .exceptionally(this::logError);
    }

    private void onDownload() {
        byte[] content = "Hello from Vaadin DownloadHandler!\nThis content was streamed to the file."
                .getBytes(StandardCharsets.UTF_8);
        fs().showSaveFilePicker()
                .thenAccept(handle -> {
                    var handler = DownloadHandler.fromInputStream(event -> new DownloadResponse(
                            new ByteArrayInputStream(content), handle.getName(), "text/plain", content.length));
                    handle.downloadFrom(handler)
                            .thenRun(() -> appendLog("Downloaded content to " + handle.getName()))
                            .exceptionally(this::logError);
                })
                .exceptionally(this::logError);
    }
}
