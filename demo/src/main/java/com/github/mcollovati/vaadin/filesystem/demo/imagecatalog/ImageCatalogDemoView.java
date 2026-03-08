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
package com.github.mcollovati.vaadin.filesystem.demo.imagecatalog;

import com.github.mcollovati.vaadin.filesystem.DirectoryPickerOptions;
import com.github.mcollovati.vaadin.filesystem.FileData;
import com.github.mcollovati.vaadin.filesystem.FileTypeFilter;
import com.github.mcollovati.vaadin.filesystem.GetHandleOptions;
import com.github.mcollovati.vaadin.filesystem.OpenFilePickerOptions;
import com.github.mcollovati.vaadin.filesystem.PermissionMode;
import com.github.mcollovati.vaadin.filesystem.SaveFilePickerOptions;
import com.github.mcollovati.vaadin.filesystem.demo.AbstractDemoView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;

/**
 * Demo view that imports images, generates thumbnails server-side,
 * and exports them individually or in bulk using the File System API.
 */
@Route("image-catalog")
public class ImageCatalogDemoView extends AbstractDemoView {

    private final List<ImageEntry> entries = new ArrayList<>();
    private final Grid<ImageEntry> grid = new Grid<>();

    public ImageCatalogDemoView() {
        super(
                "Image Catalog",
                "Import images, generate 200px-wide thumbnails server-side, "
                        + "preview them in a grid, and export thumbnails individually or in bulk.");
        grid.addColumn(new ComponentRenderer<>(this::createPreview))
                .setHeader("Preview")
                .setWidth("100px")
                .setFlexGrow(0);
        grid.addColumn(ImageEntry::fileName).setHeader("File Name");
        grid.addColumn(ImageEntry::dimensionsText).setHeader("Dimensions");
        grid.addColumn(entry -> formatSize(entry.originalSize())).setHeader("Size");
        grid.addColumn(new ComponentRenderer<>(entry -> new Button("Save Thumbnail", e -> onSaveThumbnail(entry))))
                .setHeader("Actions");
        grid.setWidthFull();
        addContent(grid);
    }

    @Override
    protected String codeSnippet() {
        return """
                // Import images
                var opts = OpenFilePickerOptions.builder()
                        .types(FileTypeFilter.of("Images", "image/*",
                            ".png", ".jpg", ".jpeg", ".gif", ".webp"))
                        .multiple(true).build();
                fs.openFiles(opts).thenAccept(files -> {
                    // Generate thumbnails with ImageIO
                });

                // Export thumbnail
                var saveOpts = SaveFilePickerOptions.builder()
                        .suggestedName("photo_thumb.png").build();
                fs.saveFile(saveOpts, thumbnailBytes);""";
    }

    @Override
    protected void addActions() {
        var importBtn = new Button("Import Images", e -> onImportImages());
        var exportAllBtn = new Button("Export All to Folder", e -> onExportAll());
        var clearBtn = new Button("Clear", e -> onClear());
        add(new HorizontalLayout(importBtn, exportAllBtn, clearBtn));
    }

    private void onImportImages() {
        var options = OpenFilePickerOptions.builder()
                .types(FileTypeFilter.of("Images", "image/*", ".png", ".jpg", ".jpeg", ".gif", ".webp"))
                .multiple(true)
                .build();
        fs().openFiles(options)
                .thenAccept(files -> {
                    for (FileData file : files) {
                        var entry = processImage(file);
                        if (entry != null) {
                            entries.add(entry);
                            appendLog("Imported " + file.getName() + " (" + entry.dimensionsText() + ")");
                        } else {
                            appendLog("Skipped " + file.getName() + " (unsupported format)");
                        }
                    }
                    getUI().ifPresent(ui -> ui.access(() -> grid.setItems(entries)));
                })
                .exceptionally(this::logError);
    }

    private ImageEntry processImage(FileData file) {
        try {
            var original = ImageIO.read(new ByteArrayInputStream(file.getContent()));
            if (original == null) {
                return null;
            }
            int thumbWidth = 200;
            int thumbHeight = (int) Math.round((double) original.getHeight() / original.getWidth() * thumbWidth);
            var thumbnail = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_ARGB);
            var g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(original, 0, 0, thumbWidth, thumbHeight, null);
            g2d.dispose();

            var baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "png", baos);

            return new ImageEntry(
                    file.getName(),
                    file.getContent(),
                    original.getWidth(),
                    original.getHeight(),
                    file.getSize(),
                    baos.toByteArray(),
                    "image/png");
        } catch (IOException e) {
            return null;
        }
    }

    private Image createPreview(ImageEntry entry) {
        var image = new Image(entry.thumbnailBytes(), entry.thumbnailFileName(), entry.thumbnailMimeType());
        image.setHeight("60px");
        return image;
    }

    private void onSaveThumbnail(ImageEntry entry) {
        var options = SaveFilePickerOptions.builder()
                .suggestedName(entry.thumbnailFileName())
                .types(FileTypeFilter.of("PNG Image", "image/png", ".png"))
                .build();
        fs().saveFile(options, entry.thumbnailBytes())
                .thenRun(() -> appendLog("Saved thumbnail for " + entry.fileName()))
                .exceptionally(this::logError);
    }

    private void onExportAll() {
        if (entries.isEmpty()) {
            appendLog("No images to export");
            return;
        }
        var options =
                DirectoryPickerOptions.builder().mode(PermissionMode.READWRITE).build();
        fs().openDirectory(options)
                .thenCompose(dir -> {
                    CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
                    for (ImageEntry entry : entries) {
                        chain = chain.thenCompose(
                                v -> dir.getFileHandle(entry.thumbnailFileName(), GetHandleOptions.creating())
                                        .thenCompose(fh -> fh.writeBytes(entry.thumbnailBytes())));
                    }
                    return chain;
                })
                .thenRun(() -> appendLog("Exported " + entries.size() + " thumbnails to folder"))
                .exceptionally(this::logError);
    }

    private void onClear() {
        entries.clear();
        grid.setItems(entries);
        appendLog("Cleared all images");
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
}
