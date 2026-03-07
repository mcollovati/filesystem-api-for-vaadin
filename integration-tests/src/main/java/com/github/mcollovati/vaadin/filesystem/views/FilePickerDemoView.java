package com.github.mcollovati.vaadin.filesystem.views;

import com.github.mcollovati.vaadin.filesystem.FileSystemFileHandle;
import com.github.mcollovati.vaadin.filesystem.FileTypeFilter;
import com.github.mcollovati.vaadin.filesystem.OpenFilePickerOptions;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.Map;

/**
 * Demo view showcasing the file picker API: {@code showOpenFilePicker}
 * and {@code showSaveFilePicker} with various options.
 */
@Route("")
public class FilePickerDemoView extends AbstractDemoView {

    /**
     * Creates the file picker demo view.
     */
    public FilePickerDemoView() {
        super(
                "File Pickers",
                "Open and save file picker dialogs using showOpenFilePicker() and "
                        + "showSaveFilePicker(). Configure options like multiple selection "
                        + "and file type filters through builder-style option classes.");

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
    String codeSnippet() {
        return """
                // Single file
                fs.showOpenFilePicker()
                    .thenAccept(handles -> { ... });

                // Multiple files
                var opts = OpenFilePickerOptions.builder()
                        .multiple(true).build();
                fs.showOpenFilePicker(opts)
                    .thenAccept(handles -> { ... });

                // Filtered by type
                var opts = OpenFilePickerOptions.builder()
                        .types(List.of(new FileTypeFilter("Images",
                            Map.of("image/*", List.of(".png", ".jpg")))))
                        .build();
                fs.showOpenFilePicker(opts)
                    .thenAccept(handles -> { ... });""";
    }

    @Override
    void addActions() {
        var openFile = new Button("Open File", e -> onOpenFile());
        var openMultiple = new Button("Open Multiple", e -> onOpenMultiple());
        var openImages = new Button("Open Images", e -> onOpenImages());
        add(new HorizontalLayout(openFile, openMultiple, openImages));
    }

    private void onOpenFile() {
        fs().showOpenFilePicker()
                .thenAccept(handles -> logHandles("Open File", handles))
                .exceptionally(this::logError);
    }

    private void onOpenMultiple() {
        var options = OpenFilePickerOptions.builder().multiple(true).build();
        fs().showOpenFilePicker(options)
                .thenAccept(handles -> logHandles("Open Multiple", handles))
                .exceptionally(this::logError);
    }

    private void onOpenImages() {
        var options = OpenFilePickerOptions.builder()
                .types(List.of(new FileTypeFilter("Images", Map.of("image/*", List.of(".png", ".jpg", ".gif")))))
                .build();
        fs().showOpenFilePicker(options)
                .thenAccept(handles -> logHandles("Open Images", handles))
                .exceptionally(this::logError);
    }

    private void logHandles(String action, List<? extends FileSystemFileHandle> handles) {
        var sb = new StringBuilder(action + ": ");
        for (var handle : handles) {
            sb.append(handle.getName()).append(" (").append(handle.getKind()).append("), ");
        }
        appendLog(sb.toString());
    }
}
