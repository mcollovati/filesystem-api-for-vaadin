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
package com.github.mcollovati.vaadin.filesystem.demo.archivecreator;

import com.github.mcollovati.vaadin.filesystem.HandleKind;
import com.github.mcollovati.vaadin.filesystem.OriginPrivateFileSystem;
import com.github.mcollovati.vaadin.filesystem.demo.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Demo view that stages files in OPFS, signs and compresses them
 * server-side into a ZIP archive, streams the archive back to OPFS,
 * and triggers a browser download.
 */
@Route("archive-creator")
public class ArchiveCreatorDemoView extends VerticalLayout {

    private final OriginPrivateFileSystem opfs;
    private final Grid<StagedFile> grid = new Grid<>();
    private final Pre log;
    private final TextField archiveNameField;
    private final Button createArchiveBtn;

    public ArchiveCreatorDemoView() {
        opfs = new OriginPrivateFileSystem(this);

        log = new Pre();
        log.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("min-height", "150px")
                .set("margin", "0");
        log.setWidthFull();

        setPadding(true);
        setSpacing(true);

        add(new H2("Archive Creator"));
        add(new Paragraph(
                "Stage files in your browser's private storage (OPFS), select which ones to include, "
                        + "and create a signed ZIP archive. The server signs each file with a SHA-256 digest "
                        + "before compressing. Works in all modern browsers."));
        add(MainLayout.codeBlock(codeSnippet()));

        // Upload
        var upload = new Upload(UploadHandler.inMemory((metadata, bytes) -> {
            opfs.writeFile(metadata.fileName(), bytes)
                    .thenRun(() -> {
                        appendLog("Staged: " + metadata.fileName() + " (" + formatSize(bytes.length) + ")");
                        refreshFileList();
                    })
                    .exceptionally(this::logError);
        }));

        // Archive name
        archiveNameField = new TextField();
        archiveNameField.setValue("archive.zip");
        archiveNameField.setWidth("200px");

        // Create archive button
        createArchiveBtn = new Button("Create Signed Archive", e -> onCreateArchive());
        createArchiveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createArchiveBtn.setEnabled(false);

        var clearBtn = new Button("Clear All", e -> onClearAll());
        clearBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        var toolbar = new HorizontalLayout(upload, archiveNameField, createArchiveBtn, clearBtn);
        toolbar.setAlignItems(Alignment.BASELINE);
        toolbar.setFlexGrow(1, upload);
        add(toolbar);

        // Grid
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addColumn(StagedFile::name).setHeader("Name").setFlexGrow(1);
        grid.addColumn(StagedFile::type).setHeader("Type");
        grid.addColumn(StagedFile::displaySize).setHeader("Size");
        grid.addColumn(new ComponentRenderer<>(file -> {
                    var removeBtn = new Button("Remove", ev -> onRemoveFile(file));
                    removeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                    return removeBtn;
                }))
                .setHeader("Actions")
                .setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
        grid.addSelectionListener(e -> createArchiveBtn.setEnabled(!e.getAllSelectedItems().isEmpty()));
        add(grid);

        add(new H2("Log"));
        add(log);

        refreshFileList();
    }

    private String codeSnippet() {
        return """
                OriginPrivateFileSystem opfs = new OriginPrivateFileSystem(this);

                // Stage files in OPFS
                opfs.writeFile("report.pdf", uploadedBytes);

                // Stream each file to server for signing
                opfs.uploadFile("report.pdf", UploadHandler.inMemory(
                    (metadata, bytes) -> sign(bytes)));

                // Stream signed archive back to OPFS
                opfs.downloadFile("archive.zip", DownloadHandler.fromInputStream(
                    event -> new DownloadResponse(zipStream, "archive.zip",
                        "application/zip", zipBytes.length)));""";
    }

    private void refreshFileList() {
        opfs.list()
                .thenCompose(handles -> {
                    List<CompletableFuture<StagedFile>> futures = handles.stream()
                            .filter(h -> h.getKind() == HandleKind.FILE)
                            .filter(h -> !h.getName().endsWith(".zip"))
                            .map(h -> opfs.readFile(h.getName())
                                    .thenApply(fd -> new StagedFile(fd.getName(), fd.getSize(), fd.getType())))
                            .toList();
                    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                            .thenApply(v -> futures.stream()
                                    .map(CompletableFuture::join)
                                    .toList());
                })
                .thenAccept(files -> getUI().ifPresent(ui -> ui.access(() -> grid.setItems(files))))
                .exceptionally(this::logError);
    }

    private void onCreateArchive() {
        Set<StagedFile> selected = grid.getSelectedItems();
        if (selected.isEmpty()) {
            return;
        }

        String archiveName = archiveNameField.getValue().trim();
        if (archiveName.isBlank()) {
            archiveName = "archive.zip";
        }
        if (!archiveName.endsWith(".zip")) {
            archiveName += ".zip";
        }

        createArchiveBtn.setEnabled(false);
        appendLog("Creating signed archive: " + archiveName + " (" + selected.size() + " files)...");

        var zipOut = new ByteArrayOutputStream();
        var zos = new ZipOutputStream(zipOut);
        var manifest = new ArrayList<ManifestEntry>();
        var timestamp = Instant.now().toString();

        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (StagedFile file : selected) {
            chain = chain.thenCompose(v -> opfs.uploadFile(
                    file.name(),
                    UploadHandler.inMemory((metadata, bytes) -> {
                        try {
                            String digest = sha256(bytes);

                            // Add original file
                            zos.putNextEntry(new ZipEntry(metadata.fileName()));
                            zos.write(bytes);
                            zos.closeEntry();

                            // Add detached signature
                            byte[] sig = createSignature(metadata.fileName(), digest, timestamp);
                            zos.putNextEntry(new ZipEntry(metadata.fileName() + ".sig"));
                            zos.write(sig);
                            zos.closeEntry();

                            manifest.add(new ManifestEntry(metadata.fileName(), bytes.length, digest));
                            appendLog("  Signed: " + metadata.fileName() + " [" + digest.substring(0, 16) + "...]");
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    })));
        }

        String finalArchiveName = archiveName;
        chain.thenRun(() -> {
                    try {
                        // Write manifest
                        zos.putNextEntry(new ZipEntry("manifest.json"));
                        zos.write(buildManifest(manifest, timestamp));
                        zos.closeEntry();
                        zos.close();
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                })
                .thenCompose(v -> {
                    byte[] zipBytes = zipOut.toByteArray();
                    appendLog("Archive created: " + formatSize(zipBytes.length) + " (" + manifest.size()
                            + " files signed)");
                    return opfs.downloadFile(
                            finalArchiveName,
                            DownloadHandler.fromInputStream(event -> new DownloadResponse(
                                    new ByteArrayInputStream(zipBytes),
                                    finalArchiveName,
                                    "application/zip",
                                    zipBytes.length)));
                })
                .thenRun(() -> appendLog("Archive saved to OPFS: " + finalArchiveName))
                .thenCompose(v -> opfs.saveToDevice(finalArchiveName))
                .thenRun(() -> {
                    appendLog("Download started: " + finalArchiveName);
                    refreshFileList();
                })
                .exceptionally(this::logError);
    }

    private void onRemoveFile(StagedFile file) {
        opfs.removeEntry(file.name())
                .thenRun(() -> {
                    appendLog("Removed: " + file.name());
                    refreshFileList();
                })
                .exceptionally(this::logError);
    }

    private void onClearAll() {
        opfs.clear()
                .thenRun(() -> {
                    appendLog("Cleared all staged files");
                    refreshFileList();
                })
                .exceptionally(this::logError);
    }

    // -- Signing helpers --

    private static byte[] createSignature(String fileName, String digest, String timestamp) {
        return """
                -----BEGIN FILE SIGNATURE-----
                File: %s
                Algorithm: SHA-256
                Digest: %s
                Signed-By: Vaadin Archive Creator
                Timestamp: %s
                -----END FILE SIGNATURE-----
                """
                .formatted(fileName, digest, timestamp)
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] buildManifest(List<ManifestEntry> entries, String timestamp) {
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"signedBy\": \"Vaadin Archive Creator\",\n");
        sb.append("  \"algorithm\": \"SHA-256\",\n");
        sb.append("  \"timestamp\": \"").append(timestamp).append("\",\n");
        sb.append("  \"files\": [\n");
        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            sb.append("    { \"name\": \"")
                    .append(escapeJson(e.name()))
                    .append("\", \"size\": ")
                    .append(e.size())
                    .append(", \"digest\": \"")
                    .append(e.digest())
                    .append("\" }");
            if (i < entries.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // -- UI helpers --

    private void appendLog(String message) {
        getUI().ifPresent(ui -> ui.access(() -> {
            String current = log.getText();
            log.setText(current + message + "\n");
        }));
    }

    private Void logError(Throwable error) {
        Throwable cause = error instanceof CompletionException ? error.getCause() : error;
        String type = cause.getClass().getSimpleName();
        appendLog("ERROR " + type + ": " + cause.getMessage());
        getUI().ifPresent(ui -> ui.access(() -> createArchiveBtn.setEnabled(true)));
        return null;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    private record ManifestEntry(String name, long size, String digest) {}
}
