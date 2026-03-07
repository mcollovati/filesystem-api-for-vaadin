package com.github.mcollovati.vaadin.filesystem.views;

import com.github.mcollovati.vaadin.filesystem.DirectoryPickerOptions;
import com.github.mcollovati.vaadin.filesystem.FileSystemHandle;
import com.github.mcollovati.vaadin.filesystem.PermissionMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.Route;
import java.util.List;

/**
 * Demo view showcasing directory operations: open a directory with
 * {@code showDirectoryPicker()}, list entries, and navigate the
 * directory tree.
 */
@Route("directory")
public class DirectoryDemoView extends AbstractDemoView {

    /**
     * Creates the directory demo view.
     */
    public DirectoryDemoView() {
        super(
                "Directory Operations",
                "Open a directory with showDirectoryPicker() and explore its contents. "
                        + "Use entries() to list files and subdirectories, getFileHandle() and "
                        + "getDirectoryHandle() to access children, resolve() to find paths, "
                        + "and removeEntry() to delete entries.");
    }

    @Override
    String codeSnippet() {
        return """
                var opts = DirectoryPickerOptions.builder()
                        .mode(PermissionMode.READWRITE).build();

                fs.showDirectoryPicker(opts).thenAccept(dir -> {
                    // List entries
                    dir.entries().thenAccept(entries -> {
                        for (var entry : entries) {
                            // entry.getName(), entry.getKind()
                        }
                    });

                    // Get or create a file
                    var create = GetHandleOptions.builder()
                            .create(true).build();
                    dir.getFileHandle("notes.txt", create)
                        .thenAccept(file -> { ... });

                    // Resolve path to a child
                    dir.resolve(childHandle)
                        .thenAccept(path -> { ... });
                });""";
    }

    @Override
    void addActions() {
        add(new Button("Open Directory", e -> onOpenDirectory()));
    }

    private void onOpenDirectory() {
        var options =
                DirectoryPickerOptions.builder().mode(PermissionMode.READWRITE).build();
        fs().showDirectoryPicker(options)
                .thenAccept(handle -> {
                    appendLog("Open Directory: " + handle.getName() + " (" + handle.getKind() + ")");
                    handle.entries().thenAccept(this::logEntries).exceptionally(this::logError);
                })
                .exceptionally(this::logError);
    }

    private void logEntries(List<FileSystemHandle> entries) {
        if (entries.isEmpty()) {
            appendLog("  (empty directory)");
            return;
        }
        for (var entry : entries) {
            appendLog("  " + entry.getKind() + ": " + entry.getName());
        }
    }
}
