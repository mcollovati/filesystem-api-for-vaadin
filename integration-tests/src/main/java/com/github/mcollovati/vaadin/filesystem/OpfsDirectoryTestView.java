package com.github.mcollovati.vaadin.filesystem;

import com.vaadin.flow.router.Route;
import java.util.stream.Collectors;

@Route("test/opfs-directory")
public class OpfsDirectoryTestView extends AbstractOpfsTestView {

    @Override
    void addActions() {
        add(button("setup-and-list", "Setup & List", this::onSetupAndList));
        add(button("resolve-path", "Resolve Path", this::onResolvePath));
        add(button("remove-entry", "Remove Entry", this::onRemoveEntry));
        add(button("create-nested", "Create Nested", this::onCreateNested));
    }

    private void onSetupAndList() {
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getFileHandle("a.txt", GetHandleOptions.creating())
                        .thenCompose(f -> root.getFileHandle("b.txt", GetHandleOptions.creating()))
                        .thenCompose(f -> root.getDirectoryHandle("subdir", GetHandleOptions.creating()))
                        .thenCompose(d -> root.entries()))
                .thenAccept(entries -> {
                    String names = entries.stream()
                            .map(h -> h.getName() + "(" + h.getKind() + ")")
                            .sorted()
                            .collect(Collectors.joining(", "));
                    appendLog("entries=" + names);
                    appendLog("count=" + entries.size());
                })
                .exceptionally(this::logError);
    }

    private void onResolvePath() {
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getDirectoryHandle("sub", GetHandleOptions.creating())
                        .thenCompose(sub -> sub.getFileHandle("deep.txt", GetHandleOptions.creating())
                                .thenCompose(root::resolve)))
                .thenAccept(path -> appendLog("path=" + path.orElse(null)))
                .exceptionally(this::logError);
    }

    private void onRemoveEntry() {
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getFileHandle("to-delete.txt", GetHandleOptions.creating())
                        .thenCompose(f -> root.removeEntry("to-delete.txt"))
                        .thenCompose(v -> root.entries()))
                .thenAccept(entries -> appendLog("after-remove-count=" + entries.size()))
                .exceptionally(this::logError);
    }

    private void onCreateNested() {
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getDirectoryHandle("level1", GetHandleOptions.creating())
                        .thenCompose(l1 -> l1.getDirectoryHandle("level2", GetHandleOptions.creating()))
                        .thenCompose(l2 -> l2.getFileHandle("nested.txt", GetHandleOptions.creating()))
                        .thenCompose(file -> file.writeString("nested content").thenApply(v -> file))
                        .thenCompose(file -> root.resolve(file)))
                .thenAccept(path -> appendLog("nested-path=" + path.orElse(null)))
                .exceptionally(this::logError);
    }
}
