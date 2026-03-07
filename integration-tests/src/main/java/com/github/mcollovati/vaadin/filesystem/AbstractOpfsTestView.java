package com.github.mcollovati.vaadin.filesystem;

import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

abstract class AbstractOpfsTestView extends VerticalLayout {

    private final FileSystemAPI fs;
    private final Pre log;

    AbstractOpfsTestView() {
        fs = new FileSystemAPI(this);
        log = new Pre();
        log.setId("log");
        log.setWidthFull();
        addActions();
        add(log);
    }

    FileSystemAPI fs() {
        return fs;
    }

    abstract void addActions();

    CompletableFuture<FileSystemDirectoryHandle> getOpfsRoot() {
        return fs.full().getOriginPrivateDirectory();
    }

    CompletableFuture<Void> cleanupOpfs(FileSystemDirectoryHandle root) {
        return root.entries().thenCompose(entries -> {
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            for (FileSystemHandle entry : entries) {
                chain = chain.thenCompose(v -> root.removeEntry(entry.getName(), RemoveEntryOptions.recursively()));
            }
            return chain;
        });
    }

    void appendLog(String message) {
        getUI().ifPresent(ui -> ui.access(() -> {
            String current = log.getText();
            log.setText(current + message + "\n");
        }));
    }

    Void logError(Throwable error) {
        Throwable cause = error instanceof CompletionException ? error.getCause() : error;
        String type = cause.getClass().getSimpleName();
        appendLog(type + ": " + cause.getMessage());
        return null;
    }

    NativeButton button(String id, String text, Runnable action) {
        var btn = new NativeButton(text, e -> action.run());
        btn.setId(id);
        return btn;
    }
}
