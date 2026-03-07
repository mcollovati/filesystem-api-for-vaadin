package com.github.mcollovati.vaadin.filesystem;

import com.vaadin.flow.router.Route;

@Route("test/opfs-handles")
public class OpfsHandleTestView extends AbstractOpfsTestView {

    @Override
    void addActions() {
        add(button("is-same-entry", "Is Same Entry", this::onIsSameEntry));
        add(button("query-permission", "Query Permission", this::onQueryPermission));
        add(button("request-permission", "Request Permission", this::onRequestPermission));
    }

    private void onIsSameEntry() {
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getFileHandle("same.txt", GetHandleOptions.creating())
                        .thenCompose(f1 -> root.getFileHandle("same.txt").thenCompose(f2 -> f1.isSameEntry(f2))))
                .thenAccept(same -> appendLog("same=" + same))
                .exceptionally(this::logError);
    }

    private void onQueryPermission() {
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getFileHandle("perm.txt", GetHandleOptions.creating()))
                .thenCompose(file -> file.queryPermission(PermissionMode.READ))
                .thenAccept(state -> appendLog("permission=" + state))
                .exceptionally(this::logError);
    }

    private void onRequestPermission() {
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getFileHandle("perm.txt", GetHandleOptions.creating()))
                .thenCompose(file -> file.requestPermission(PermissionMode.READWRITE))
                .thenAccept(state -> appendLog("permission=" + state))
                .exceptionally(this::logError);
    }
}
