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
package com.github.mcollovati.vaadin.filesystem;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

/**
 * A writable stream for writing content to a file in the browser's
 * file system.
 *
 * <p>This is the Java counterpart of the browser's
 * {@code FileSystemWritableFileStream} interface. Instances are obtained
 * through {@link FileSystemFileHandle#createWritable()}.
 *
 * <p>After writing, the stream must be closed with {@link #close()} to
 * commit the changes to the file. If the stream is not closed, the
 * changes are discarded.
 *
 * @see FileSystemFileHandle#createWritable()
 */
public final class FileSystemWritableFileStream implements Serializable {

    private final String streamId;
    private final JsBridge bridge;

    FileSystemWritableFileStream(String streamId, JsBridge bridge) {
        this.streamId = streamId;
        this.bridge = bridge;
    }

    /**
     * Writes a text string to the stream at the current position.
     *
     * @param text the text to write
     * @return a future that completes when the write is done
     */
    public CompletableFuture<Void> write(String text) {
        return bridge.writableWriteText(streamId, text);
    }

    /**
     * Writes binary data to the stream at the current position.
     *
     * <p>The data is transferred to the browser via base64 encoding.
     *
     * @param data the bytes to write
     * @return a future that completes when the write is done
     */
    public CompletableFuture<Void> write(byte[] data) {
        return bridge.writableWriteBytes(streamId, data);
    }

    /**
     * Moves the current file cursor position to the specified offset.
     *
     * @param position the byte offset from the beginning of the file
     * @return a future that completes when the seek is done
     */
    public CompletableFuture<Void> seek(long position) {
        return bridge.writableSeek(streamId, position);
    }

    /**
     * Resizes the file to the specified size. If the new size is smaller
     * than the current size, the file is truncated. If larger, the file
     * is padded with zero bytes.
     *
     * @param size the new file size in bytes
     * @return a future that completes when the truncate is done
     */
    public CompletableFuture<Void> truncate(long size) {
        return bridge.writableTruncate(streamId, size);
    }

    /**
     * Closes the stream and commits the written data to the file.
     *
     * <p>This must be called after writing to persist changes. If
     * the stream is not closed, written data is discarded.
     *
     * <p>Open streams are also automatically closed (best-effort) when the
     * component bound to the {@link ClientFileSystem} is detached from the UI.
     *
     * @return a future that completes when the stream is closed
     */
    public CompletableFuture<Void> close() {
        return bridge.writableClose(streamId);
    }
}
