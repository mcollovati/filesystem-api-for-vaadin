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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Callback-based API for the browser's File System API.
 *
 * <p>Wraps the {@link FileSystemAPI} and converts
 * {@code CompletableFuture}-based results into simple callbacks
 * using {@link SerializableConsumer} and {@link SerializableRunnable}.
 *
 * <pre>{@code
 * var fs = new FileSystemCallbackAPI(myView);
 *
 * fs.openFile(
 *     fileData -> log(fileData.getName()),
 *     error -> log("Error: " + error.getMessage()));
 * }</pre>
 *
 * @see FileSystemAPI
 */
public final class FileSystemCallbackAPI implements Serializable {

    private static final Logger logger = Logger.getLogger(FileSystemCallbackAPI.class.getName());

    private final FileSystemAPI api;

    /**
     * Creates a new instance bound to the given component.
     *
     * @param component the component to bind to, not {@code null}
     */
    public FileSystemCallbackAPI(Component component) {
        this.api = new FileSystemAPI(component);
    }

    /**
     * Returns the underlying {@code CompletableFuture}-based API.
     *
     * @return the high-level API instance
     */
    public FileSystemAPI api() {
        return api;
    }

    // -- Support check --

    /**
     * Checks whether the browser supports the File System API.
     *
     * @param onResult called with {@code true} if supported
     */
    public void isSupported(SerializableConsumer<Boolean> onResult) {
        api.isSupported().thenAccept(onResult).exceptionally(this::logAndIgnore);
    }

    // -- Open & Read --

    /**
     * Opens a file picker and reads the selected file's content.
     *
     * <p>The entire file is read into memory and base64-encoded for
     * transfer to the server. For large files, prefer the streaming
     * overload {@link #openFile(UploadHandler)}.
     *
     * @param onSuccess called with the file data
     */
    public void openFile(SerializableConsumer<FileData> onSuccess) {
        openFile(onSuccess, null);
    }

    /**
     * Opens a file picker and reads the selected file's content.
     *
     * <p>The entire file is read into memory and base64-encoded for
     * transfer to the server. For large files, prefer the streaming
     * overload
     * {@link #openFile(UploadHandler, SerializableRunnable, SerializableConsumer)}.
     *
     * @param onSuccess called with the file data
     * @param onError   called if an error occurs, or {@code null}
     */
    public void openFile(SerializableConsumer<FileData> onSuccess, SerializableConsumer<Throwable> onError) {
        openFile(OpenFilePickerOptions.builder().build(), onSuccess, onError);
    }

    /**
     * Opens a file picker with the given options and reads the selected
     * file's content.
     *
     * <p>The entire file is read into memory and base64-encoded for
     * transfer to the server. For large files, prefer the streaming
     * overload
     * {@link #openFile(OpenFilePickerOptions, UploadHandler, SerializableRunnable, SerializableConsumer)}.
     *
     * @param options   the picker options
     * @param onSuccess called with the file data
     * @param onError   called if an error occurs, or {@code null}
     */
    public void openFile(
            OpenFilePickerOptions options,
            SerializableConsumer<FileData> onSuccess,
            SerializableConsumer<Throwable> onError) {
        api.openFile(options).thenAccept(onSuccess).exceptionally(errorHandler(onError));
    }

    /**
     * Opens a file picker allowing multiple selection and reads all
     * selected files.
     *
     * <p>Each file is read entirely into memory. For large or numerous
     * files, consider using {@link #openFile(UploadHandler)} to process
     * files individually via streaming.
     *
     * @param onSuccess called with the list of file data
     */
    public void openFiles(SerializableConsumer<List<FileData>> onSuccess) {
        openFiles(onSuccess, null);
    }

    /**
     * Opens a file picker allowing multiple selection and reads all
     * selected files.
     *
     * <p>Each file is read entirely into memory. For large or numerous
     * files, consider using
     * {@link #openFile(UploadHandler, SerializableRunnable, SerializableConsumer)}
     * to process files individually via streaming.
     *
     * @param onSuccess called with the list of file data
     * @param onError   called if an error occurs, or {@code null}
     */
    public void openFiles(SerializableConsumer<List<FileData>> onSuccess, SerializableConsumer<Throwable> onError) {
        openFiles(OpenFilePickerOptions.builder().build(), onSuccess, onError);
    }

    /**
     * Opens a file picker with the given options and reads all selected
     * files.
     *
     * <p>Each file is read entirely into memory. For large or numerous
     * files, consider using
     * {@link #openFile(OpenFilePickerOptions, UploadHandler, SerializableRunnable, SerializableConsumer)}
     * to process files individually via streaming.
     *
     * @param options   the picker options
     * @param onSuccess called with the list of file data
     * @param onError   called if an error occurs, or {@code null}
     */
    public void openFiles(
            OpenFilePickerOptions options,
            SerializableConsumer<List<FileData>> onSuccess,
            SerializableConsumer<Throwable> onError) {
        api.openFiles(options).thenAccept(onSuccess).exceptionally(errorHandler(onError));
    }

    // -- Save & Write (text) --

    /**
     * Opens a save file picker and writes the given text.
     *
     * @param text the text to write
     */
    public void saveFile(String text) {
        saveFile(text, null, null);
    }

    /**
     * Opens a save file picker and writes the given text.
     *
     * @param text      the text to write
     * @param onSuccess called when the write completes, or {@code null}
     */
    public void saveFile(String text, SerializableRunnable onSuccess) {
        saveFile(text, onSuccess, null);
    }

    /**
     * Opens a save file picker and writes the given text.
     *
     * @param text      the text to write
     * @param onSuccess called when the write completes, or {@code null}
     * @param onError   called if an error occurs, or {@code null}
     */
    public void saveFile(String text, SerializableRunnable onSuccess, SerializableConsumer<Throwable> onError) {
        saveFile(SaveFilePickerOptions.builder().build(), text, onSuccess, onError);
    }

    /**
     * Opens a save file picker with the given options and writes the
     * given text.
     *
     * @param options   the picker options
     * @param text      the text to write
     * @param onSuccess called when the write completes, or {@code null}
     * @param onError   called if an error occurs, or {@code null}
     */
    public void saveFile(
            SaveFilePickerOptions options,
            String text,
            SerializableRunnable onSuccess,
            SerializableConsumer<Throwable> onError) {
        api.saveFile(options, text).thenRun(runOrNoop(onSuccess)).exceptionally(errorHandler(onError));
    }

    // -- Save & Write (bytes) --

    /**
     * Opens a save file picker and writes the given bytes.
     *
     * @param data the bytes to write
     */
    public void saveFile(byte[] data) {
        saveFile(data, null, null);
    }

    /**
     * Opens a save file picker and writes the given bytes.
     *
     * @param data      the bytes to write
     * @param onSuccess called when the write completes, or {@code null}
     */
    public void saveFile(byte[] data, SerializableRunnable onSuccess) {
        saveFile(data, onSuccess, null);
    }

    /**
     * Opens a save file picker and writes the given bytes.
     *
     * @param data      the bytes to write
     * @param onSuccess called when the write completes, or {@code null}
     * @param onError   called if an error occurs, or {@code null}
     */
    public void saveFile(byte[] data, SerializableRunnable onSuccess, SerializableConsumer<Throwable> onError) {
        saveFile(SaveFilePickerOptions.builder().build(), data, onSuccess, onError);
    }

    /**
     * Opens a save file picker with the given options and writes the
     * given bytes.
     *
     * @param options   the picker options
     * @param data      the bytes to write
     * @param onSuccess called when the write completes, or {@code null}
     * @param onError   called if an error occurs, or {@code null}
     */
    public void saveFile(
            SaveFilePickerOptions options,
            byte[] data,
            SerializableRunnable onSuccess,
            SerializableConsumer<Throwable> onError) {
        api.saveFile(options, data).thenRun(runOrNoop(onSuccess)).exceptionally(errorHandler(onError));
    }

    // -- Streaming: Upload --

    /**
     * Opens a file picker and uploads the selected file to the server.
     *
     * @param handler the upload handler to receive the file content
     */
    public void openFile(UploadHandler handler) {
        openFile(handler, null, null);
    }

    /**
     * Opens a file picker and uploads the selected file to the server.
     *
     * @param handler   the upload handler to receive the file content
     * @param onSuccess called when the upload completes, or {@code null}
     * @param onError   called if an error occurs, or {@code null}
     */
    public void openFile(
            UploadHandler handler, SerializableRunnable onSuccess, SerializableConsumer<Throwable> onError) {
        openFile(OpenFilePickerOptions.builder().build(), handler, onSuccess, onError);
    }

    /**
     * Opens a file picker with the given options and uploads the selected
     * file to the server.
     *
     * @param options   the picker options
     * @param handler   the upload handler to receive the file content
     * @param onSuccess called when the upload completes, or {@code null}
     * @param onError   called if an error occurs, or {@code null}
     */
    public void openFile(
            OpenFilePickerOptions options,
            UploadHandler handler,
            SerializableRunnable onSuccess,
            SerializableConsumer<Throwable> onError) {
        api.openFile(options, handler).thenRun(runOrNoop(onSuccess)).exceptionally(errorHandler(onError));
    }

    // -- Streaming: Download --

    /**
     * Opens a save file picker and downloads content from the server
     * into the selected file.
     *
     * @param handler the download handler providing the content
     */
    public void saveFile(DownloadHandler handler) {
        saveFile(handler, null, null);
    }

    /**
     * Opens a save file picker and downloads content from the server
     * into the selected file.
     *
     * @param handler   the download handler providing the content
     * @param onSuccess called when the download completes, or {@code null}
     * @param onError   called if an error occurs, or {@code null}
     */
    public void saveFile(
            DownloadHandler handler, SerializableRunnable onSuccess, SerializableConsumer<Throwable> onError) {
        saveFile(SaveFilePickerOptions.builder().build(), handler, onSuccess, onError);
    }

    /**
     * Opens a save file picker with the given options and downloads
     * content from the server into the selected file.
     *
     * @param options   the picker options
     * @param handler   the download handler providing the content
     * @param onSuccess called when the download completes, or {@code null}
     * @param onError   called if an error occurs, or {@code null}
     */
    public void saveFile(
            SaveFilePickerOptions options,
            DownloadHandler handler,
            SerializableRunnable onSuccess,
            SerializableConsumer<Throwable> onError) {
        api.saveFile(options, handler).thenRun(runOrNoop(onSuccess)).exceptionally(errorHandler(onError));
    }

    // -- Directory --

    /**
     * Opens a directory picker.
     *
     * @param onSuccess called with the directory handle
     */
    public void openDirectory(SerializableConsumer<FileSystemDirectoryHandle> onSuccess) {
        openDirectory(onSuccess, null);
    }

    /**
     * Opens a directory picker.
     *
     * @param onSuccess called with the directory handle
     * @param onError   called if an error occurs, or {@code null}
     */
    public void openDirectory(
            SerializableConsumer<FileSystemDirectoryHandle> onSuccess, SerializableConsumer<Throwable> onError) {
        openDirectory(DirectoryPickerOptions.builder().build(), onSuccess, onError);
    }

    /**
     * Opens a directory picker with the given options.
     *
     * @param options   the picker options
     * @param onSuccess called with the directory handle
     * @param onError   called if an error occurs, or {@code null}
     */
    public void openDirectory(
            DirectoryPickerOptions options,
            SerializableConsumer<FileSystemDirectoryHandle> onSuccess,
            SerializableConsumer<Throwable> onError) {
        api.openDirectory(options).thenAccept(onSuccess).exceptionally(errorHandler(onError));
    }

    /**
     * Opens a directory picker and lists all entries.
     *
     * @param onSuccess called with the list of handles
     */
    public void listDirectory(SerializableConsumer<List<FileSystemHandle>> onSuccess) {
        listDirectory(onSuccess, null);
    }

    /**
     * Opens a directory picker and lists all entries.
     *
     * @param onSuccess called with the list of handles
     * @param onError   called if an error occurs, or {@code null}
     */
    public void listDirectory(
            SerializableConsumer<List<FileSystemHandle>> onSuccess, SerializableConsumer<Throwable> onError) {
        listDirectory(DirectoryPickerOptions.builder().build(), onSuccess, onError);
    }

    /**
     * Opens a directory picker with the given options and lists all
     * entries.
     *
     * @param options   the picker options
     * @param onSuccess called with the list of handles
     * @param onError   called if an error occurs, or {@code null}
     */
    public void listDirectory(
            DirectoryPickerOptions options,
            SerializableConsumer<List<FileSystemHandle>> onSuccess,
            SerializableConsumer<Throwable> onError) {
        api.listDirectory(options).thenAccept(onSuccess).exceptionally(errorHandler(onError));
    }

    private java.util.function.Function<Throwable, Void> errorHandler(SerializableConsumer<Throwable> onError) {
        return error -> {
            if (onError != null) {
                onError.accept(error);
            } else {
                logger.log(Level.FINE, "File System API operation failed", error);
            }
            return null;
        };
    }

    private Runnable runOrNoop(SerializableRunnable onSuccess) {
        return onSuccess != null ? onSuccess : () -> {};
    }

    private Void logAndIgnore(Throwable error) {
        logger.log(Level.FINE, "File System API operation failed", error);
        return null;
    }
}
