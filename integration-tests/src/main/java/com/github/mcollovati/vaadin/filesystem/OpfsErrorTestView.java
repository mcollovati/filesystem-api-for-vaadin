package com.github.mcollovati.vaadin.filesystem;

import com.vaadin.flow.router.Route;

@Route("test/opfs-errors")
public class OpfsErrorTestView extends AbstractOpfsTestView {

    @Override
    void addActions() {
        add(button("not-found-error", "Not Found Error", this::onNotFoundError));
        add(button("type-mismatch-error", "Type Mismatch Error", this::onTypeMismatchError));
    }

    private void onNotFoundError() {
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getFileHandle("nonexistent.txt"))
                .thenAccept(ignore -> {})
                .exceptionally(this::logError);
    }

    private void onTypeMismatchError() {
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getDirectoryHandle("mismatch", GetHandleOptions.creating())
                        .thenCompose(dir -> root.getFileHandle("mismatch")))
                .thenAccept(ignore -> {})
                .exceptionally(this::logError);
    }
}
