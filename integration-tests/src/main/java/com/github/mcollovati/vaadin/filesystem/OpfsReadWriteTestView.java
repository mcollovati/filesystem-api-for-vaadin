package com.github.mcollovati.vaadin.filesystem;

import com.vaadin.flow.router.Route;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Route("test/opfs-read-write")
public class OpfsReadWriteTestView extends AbstractOpfsTestView {

    @Override
    void addActions() {
        add(button("write-read-text", "Write & Read Text", this::onWriteReadText));
        add(button("write-read-bytes", "Write & Read Bytes", this::onWriteReadBytes));
    }

    private void onWriteReadText() {
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getFileHandle("test.txt", GetHandleOptions.creating()))
                .thenCompose(file -> file.writeString("Hello OPFS").thenApply(v -> file))
                .thenCompose(FileSystemFileHandle::getFile)
                .thenAccept(data -> {
                    String content = new String(data.getContent(), StandardCharsets.UTF_8);
                    appendLog("text=" + content);
                    appendLog("size=" + data.getSize());
                })
                .exceptionally(this::logError);
    }

    private void onWriteReadBytes() {
        byte[] testData = new byte[] {0x01, 0x02, 0x03, (byte) 0xFF};
        getOpfsRoot()
                .thenCompose(root -> cleanupOpfs(root).thenApply(v -> root))
                .thenCompose(root -> root.getFileHandle("test.bin", GetHandleOptions.creating()))
                .thenCompose(file -> file.writeBytes(testData).thenApply(v -> file))
                .thenCompose(FileSystemFileHandle::getFile)
                .thenAccept(data -> {
                    appendLog("bytes=" + Arrays.toString(data.getContent()));
                    appendLog("size=" + data.getSize());
                })
                .exceptionally(this::logError);
    }
}
