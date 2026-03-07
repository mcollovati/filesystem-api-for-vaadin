package com.github.mcollovati.vaadin.filesystem.views;

import com.github.mcollovati.vaadin.filesystem.FileTypeFilter;
import com.github.mcollovati.vaadin.filesystem.SaveFilePickerOptions;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.Map;

/**
 * Demo view showcasing file writing: pick a save location with
 * {@code showSaveFilePicker()}, create a writable stream, write
 * content, and close the stream.
 */
@Route("write-file")
public class WriteFileDemoView extends AbstractDemoView {

    /**
     * Creates the write file demo view.
     */
    public WriteFileDemoView() {
        super(
                "Write File",
                "Pick a save location with showSaveFilePicker(), then write content "
                        + "using createWritable(). The writable stream supports write(String), "
                        + "write(byte[]), seek(), and truncate(). Always call close() to "
                        + "commit changes to the file.");
    }

    @Override
    String codeSnippet() {
        return """
                var opts = SaveFilePickerOptions.builder()
                        .suggestedName("hello.txt")
                        .types(List.of(new FileTypeFilter("Text files",
                            Map.of("text/plain", List.of(".txt")))))
                        .build();

                fs.showSaveFilePicker(opts).thenCompose(handle ->
                    handle.createWritable().thenCompose(writable ->
                        writable.write("Hello from Vaadin!")
                            .thenCompose(v -> writable.close())
                    )
                );""";
    }

    @Override
    void addActions() {
        var saveHandle = new Button("Save File Handle", e -> onSaveFile());
        var writeFile = new Button("Write to File", e -> onWriteFile());
        add(new HorizontalLayout(saveHandle, writeFile));
    }

    private void onSaveFile() {
        var options = SaveFilePickerOptions.builder()
                .suggestedName("example.txt")
                .types(List.of(new FileTypeFilter("Text files", Map.of("text/plain", List.of(".txt")))))
                .build();
        fs().showSaveFilePicker(options)
                .thenAccept(handle -> appendLog("Save File: " + handle.getName() + " (" + handle.getKind() + ")"))
                .exceptionally(this::logError);
    }

    private void onWriteFile() {
        var options = SaveFilePickerOptions.builder()
                .suggestedName("hello.txt")
                .types(List.of(new FileTypeFilter("Text files", Map.of("text/plain", List.of(".txt")))))
                .build();
        fs().showSaveFilePicker(options)
                .thenCompose(handle -> handle.createWritable()
                        .thenCompose(writable -> writable.write("Hello from Vaadin File System API!")
                                .thenCompose(v -> writable.close()))
                        .thenRun(() -> appendLog("Write File: wrote content to " + handle.getName())))
                .exceptionally(this::logError);
    }
}
