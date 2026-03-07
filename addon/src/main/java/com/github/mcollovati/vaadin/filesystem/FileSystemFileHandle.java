package com.github.mcollovati.vaadin.filesystem;

import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * A handle representing a file in the browser's file system.
 *
 * <p>This is the Java counterpart of the browser's
 * {@code FileSystemFileHandle} interface. Instances are obtained through
 * picker methods on {@link FileSystemAPI} or through
 * {@link FileSystemDirectoryHandle#getFileHandle(String, GetHandleOptions)}.
 *
 * @see FileSystemHandle
 * @see FileSystemDirectoryHandle
 */
public final class FileSystemFileHandle extends AbstractFileSystemHandle {

    FileSystemFileHandle(String handleId, String name, JsBridge bridge) {
        super(handleId, name, bridge);
    }

    @Override
    public HandleKind getKind() {
        return HandleKind.FILE;
    }

    /**
     * Reads the file content from the browser.
     *
     * <p>The file is read entirely into memory and transferred to the
     * server via base64 encoding. For very large files, consider the
     * memory implications on both the browser and server sides.
     *
     * @return a future that completes with the file data including
     *         metadata and content
     */
    public CompletableFuture<FileData> getFile() {
        return bridge().getFile(handleId());
    }

    /**
     * Creates a writable stream for this file with default options.
     *
     * <p>The returned stream can be used to write content to the file.
     * After writing, {@link FileSystemWritableFileStream#close()} must
     * be called to commit the changes.
     *
     * @return a future that completes with a writable stream
     * @see #createWritable(WritableOptions)
     */
    public CompletableFuture<FileSystemWritableFileStream> createWritable() {
        return createWritable(WritableOptions.builder().build());
    }

    /**
     * Creates a writable stream for this file with the given options.
     *
     * <p>The returned stream can be used to write content to the file.
     * After writing, {@link FileSystemWritableFileStream#close()} must
     * be called to commit the changes.
     *
     * @param options the writable stream options
     * @return a future that completes with a writable stream
     */
    public CompletableFuture<FileSystemWritableFileStream> createWritable(WritableOptions options) {
        return bridge().createWritable(handleId(), options);
    }

    /**
     * Uploads the file content to the server using the given
     * {@link UploadHandler}.
     *
     * <p>This transfers the file via HTTP streaming (multipart upload),
     * which is more efficient than {@link #getFile()} for large files
     * because it avoids base64 encoding overhead and does not require
     * the entire file content to be held in memory.
     *
     * <p>The {@link UploadHandler} receives the file as a standard
     * multipart upload. Built-in implementations such as
     * {@code UploadHandler.inMemory()}, {@code UploadHandler.toTempFile()},
     * and {@code UploadHandler.toFile()} can be used.
     *
     * @param handler the upload handler to receive the file content,
     *                not {@code null}
     * @return a future that completes when the upload is finished
     */
    public CompletableFuture<Void> uploadTo(UploadHandler handler) {
        Objects.requireNonNull(handler, "handler must not be null");
        return bridge().uploadTo(handleId(), handler);
    }

    /**
     * Downloads content from the server into this file using the given
     * {@link DownloadHandler}.
     *
     * <p>This transfers data via HTTP streaming, piping the response
     * body directly into a browser-side writable stream for this file.
     * This is more efficient than {@link FileSystemWritableFileStream#write(byte[])}
     * for large payloads because it avoids base64 encoding and streams
     * data in chunks without holding it entirely in memory.
     *
     * <p>The method creates a writable stream internally, writes all
     * received data, and closes the stream to commit the changes.
     *
     * @param handler the download handler providing the content to write,
     *                not {@code null}
     * @return a future that completes when the download and write are
     *         finished
     */
    public CompletableFuture<Void> downloadFrom(DownloadHandler handler) {
        Objects.requireNonNull(handler, "handler must not be null");
        return bridge().downloadFrom(handleId(), handler);
    }
}
