package com.github.mcollovati.vaadin.filesystem;

import com.vaadin.flow.router.Route;
import java.nio.charset.StandardCharsets;

@Route("test/opfs-writable")
public class OpfsWritableStreamTestView extends AbstractOpfsTestView {

    @Override
    void addActions() {
        add(button("write-via-stream", "Write via Stream", this::onWriteViaStream));
        add(button("seek-and-write", "Seek and Write", this::onSeekAndWrite));
        add(button("truncate", "Truncate", this::onTruncate));
        add(button("keep-existing-data", "Keep Existing Data", this::onKeepExistingData));
    }

    private void onWriteViaStream() {
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getFileHandle("stream.txt", GetHandleOptions.creating()))
                .thenCompose(file -> file.createWritable()
                        .thenCompose(w -> w.write("Hello Stream").thenCompose(v -> w.close()))
                        .thenCompose(v -> file.getFile()))
                .thenAccept(data -> {
                    String content = new String(data.getContent(), StandardCharsets.UTF_8);
                    appendLog("content=" + content);
                })
                .exceptionally(this::logError);
    }

    private void onSeekAndWrite() {
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getFileHandle("seek.txt", GetHandleOptions.creating()))
                .thenCompose(file -> file.createWritable()
                        .thenCompose(w -> w.write("AAABBB")
                                .thenCompose(v -> w.seek(3))
                                .thenCompose(v -> w.write("CCC"))
                                .thenCompose(v -> w.close()))
                        .thenCompose(v -> file.getFile()))
                .thenAccept(data -> {
                    String content = new String(data.getContent(), StandardCharsets.UTF_8);
                    appendLog("content=" + content);
                })
                .exceptionally(this::logError);
    }

    private void onTruncate() {
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getFileHandle("trunc.txt", GetHandleOptions.creating()))
                .thenCompose(file -> file.createWritable()
                        .thenCompose(w -> w.write("Hello World")
                                .thenCompose(v -> w.truncate(5))
                                .thenCompose(v -> w.close()))
                        .thenCompose(v -> file.getFile()))
                .thenAccept(data -> {
                    String content = new String(data.getContent(), StandardCharsets.UTF_8);
                    appendLog("content=" + content);
                    appendLog("size=" + data.getSize());
                })
                .exceptionally(this::logError);
    }

    private void onKeepExistingData() {
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getFileHandle("keep.txt", GetHandleOptions.creating()))
                .thenCompose(file -> file.writeString("Original")
                        .thenCompose(v -> file.createWritable(WritableOptions.keepingExistingData()))
                        .thenCompose(w ->
                                w.seek(0).thenCompose(v -> w.write("Modified")).thenCompose(v -> w.close()))
                        .thenCompose(v -> file.getFile()))
                .thenAccept(data -> {
                    String content = new String(data.getContent(), StandardCharsets.UTF_8);
                    appendLog("content=" + content);
                })
                .exceptionally(this::logError);
    }
}
