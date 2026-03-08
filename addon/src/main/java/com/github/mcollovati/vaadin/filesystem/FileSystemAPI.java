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
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Java API for the browser's File System API.
 *
 * <p>Provides convenient high-level methods that combine picker dialogs
 * with follow-up operations (read, write, stream) so that common
 * workflows can be expressed in a single call, as well as lower-level
 * access to the Origin Private File System (OPFS).
 *
 * <pre>{@code
 * var fs = new FileSystemAPI(myView);
 *
 * // Pick and read a file in one step
 * fs.openFile().thenAccept(fileData ->
 *     log(fileData.getName() + ": " + fileData.getSize() + " bytes"));
 *
 * // Pick and write text in one step
 * fs.saveFile("Hello, world!");
 * }</pre>
 *
 * @see FileSystemCallbackAPI
 */
public final class FileSystemAPI implements Serializable {

    private final Component component;
    private JsBridge bridge;

    /**
     * Creates a new instance bound to the given component.
     *
     * @param component the component to bind to, not {@code null}
     */
    public FileSystemAPI(Component component) {
        this.component = component;
    }

    /**
     * Checks whether the browser supports the File System API.
     *
     * @return a future that completes with {@code true} if the File System
     *         API is available in the browser
     */
    public CompletableFuture<Boolean> isSupported() {
        return getBridge()
                .executeJs("return typeof window.showOpenFilePicker === 'function';")
                .toCompletableFuture(Boolean.class);
    }

    // -- Open & Read --

    /**
     * Opens a file picker and reads the selected file's content.
     *
     * <p>The entire file is read into memory and base64-encoded for
     * transfer to the server. For large files, prefer the streaming
     * overload {@link #openFile(UploadHandler)}.
     *
     * @return a future that completes with the file data
     */
    public CompletableFuture<FileData> openFile() {
        return openFile(OpenFilePickerOptions.builder().build());
    }

    /**
     * Opens a file picker with the given options and reads the selected
     * file's content.
     *
     * <p>The entire file is read into memory and base64-encoded for
     * transfer to the server. For large files, prefer the streaming
     * overload {@link #openFile(OpenFilePickerOptions, UploadHandler)}.
     *
     * @param options the picker options
     * @return a future that completes with the file data
     */
    public CompletableFuture<FileData> openFile(OpenFilePickerOptions options) {
        return showOpenFilePicker(options).thenCompose(handles -> handles.get(0).getFile());
    }

    /**
     * Opens a file picker allowing multiple selection and reads all
     * selected files' content.
     *
     * <p>Each file is read entirely into memory. For large or numerous
     * files, consider using {@link #openFile(UploadHandler)} to process
     * files individually via streaming.
     *
     * @return a future that completes with the list of file data
     */
    public CompletableFuture<List<FileData>> openFiles() {
        return openFiles(OpenFilePickerOptions.builder().build());
    }

    /**
     * Opens a file picker with the given options and reads all selected
     * files' content.
     *
     * <p>{@code multiple(true)} is forced on the effective options
     * regardless of the value in the provided options, so the picker
     * always allows selecting more than one file.
     *
     * <p>Each file is read entirely into memory. For large or numerous
     * files, consider using
     * {@link #openFile(OpenFilePickerOptions, UploadHandler)} to process
     * files individually via streaming.
     *
     * @param options the picker options
     * @return a future that completes with the list of file data
     */
    public CompletableFuture<List<FileData>> openFiles(OpenFilePickerOptions options) {
        OpenFilePickerOptions effective = options.rebuild().multiple(true).build();
        return showOpenFilePicker(effective).thenCompose(handles -> {
            List<CompletableFuture<FileData>> futures =
                    handles.stream().map(FileSystemFileHandle::getFile).toList();
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .thenApply(
                            v -> futures.stream().map(CompletableFuture::join).toList());
        });
    }

    // -- Save & Write --

    /**
     * Opens a save file picker and writes the given text to the
     * selected file.
     *
     * @param text the text to write
     * @return a future that completes when the write is done
     */
    public CompletableFuture<Void> saveFile(String text) {
        return saveFile(SaveFilePickerOptions.builder().build(), text);
    }

    /**
     * Opens a save file picker with the given options and writes the
     * given text to the selected file.
     *
     * @param options the picker options
     * @param text    the text to write
     * @return a future that completes when the write is done
     */
    public CompletableFuture<Void> saveFile(SaveFilePickerOptions options, String text) {
        return showSaveFilePicker(options).thenCompose(handle -> handle.writeString(text));
    }

    /**
     * Opens a save file picker and writes the given bytes to the
     * selected file.
     *
     * @param data the bytes to write
     * @return a future that completes when the write is done
     */
    public CompletableFuture<Void> saveFile(byte[] data) {
        return saveFile(SaveFilePickerOptions.builder().build(), data);
    }

    /**
     * Opens a save file picker with the given options and writes the
     * given bytes to the selected file.
     *
     * @param options the picker options
     * @param data    the bytes to write
     * @return a future that completes when the write is done
     */
    public CompletableFuture<Void> saveFile(SaveFilePickerOptions options, byte[] data) {
        return showSaveFilePicker(options).thenCompose(handle -> handle.writeBytes(data));
    }

    // -- Streaming transfers --

    /**
     * Opens a file picker and uploads the selected file to the server
     * using the given handler.
     *
     * @param handler the upload handler to receive the file content
     * @return a future that completes when the upload is done
     */
    public CompletableFuture<Void> openFile(UploadHandler handler) {
        return openFile(OpenFilePickerOptions.builder().build(), handler);
    }

    /**
     * Opens a file picker with the given options and uploads the selected
     * file to the server using the given handler.
     *
     * @param options the picker options
     * @param handler the upload handler to receive the file content
     * @return a future that completes when the upload is done
     */
    public CompletableFuture<Void> openFile(OpenFilePickerOptions options, UploadHandler handler) {
        return showOpenFilePicker(options).thenCompose(handles -> handles.get(0).uploadTo(handler));
    }

    /**
     * Opens a save file picker and downloads content from the server
     * into the selected file using the given handler.
     *
     * @param handler the download handler providing the content
     * @return a future that completes when the download is done
     */
    public CompletableFuture<Void> saveFile(DownloadHandler handler) {
        return saveFile(SaveFilePickerOptions.builder().build(), handler);
    }

    /**
     * Opens a save file picker with the given options and downloads
     * content from the server into the selected file using the given
     * handler.
     *
     * @param options the picker options
     * @param handler the download handler providing the content
     * @return a future that completes when the download is done
     */
    public CompletableFuture<Void> saveFile(SaveFilePickerOptions options, DownloadHandler handler) {
        return showSaveFilePicker(options).thenCompose(handle -> handle.downloadFrom(handler));
    }

    // -- Directory --

    /**
     * Opens a directory picker and returns the selected directory handle.
     *
     * @return a future that completes with the directory handle
     */
    public CompletableFuture<FileSystemDirectoryHandle> openDirectory() {
        return openDirectory(DirectoryPickerOptions.builder().build());
    }

    /**
     * Opens a directory picker with the given options and returns the
     * selected directory handle.
     *
     * @param options the picker options
     * @return a future that completes with the directory handle
     */
    public CompletableFuture<FileSystemDirectoryHandle> openDirectory(DirectoryPickerOptions options) {
        return showDirectoryPicker(options);
    }

    /**
     * Opens a directory picker and lists all entries in the selected
     * directory.
     *
     * @return a future that completes with the list of handles
     */
    public CompletableFuture<List<FileSystemHandle>> listDirectory() {
        return listDirectory(DirectoryPickerOptions.builder().build());
    }

    /**
     * Opens a directory picker with the given options and lists all
     * entries in the selected directory.
     *
     * @param options the picker options
     * @return a future that completes with the list of handles
     */
    public CompletableFuture<List<FileSystemHandle>> listDirectory(DirectoryPickerOptions options) {
        return showDirectoryPicker(options).thenCompose(FileSystemDirectoryHandle::entries);
    }

    // -- OPFS --

    /**
     * Returns a handle to the origin private file system (OPFS) root
     * directory.
     *
     * <p>OPFS is a sandboxed file system private to the page's origin,
     * accessed via {@code navigator.storage.getDirectory()}. Unlike the
     * picker methods, this does not show a dialog and does not require
     * user interaction.
     *
     * @return a future that completes with the OPFS root directory handle
     */
    public CompletableFuture<FileSystemDirectoryHandle> getOriginPrivateDirectory() {
        return getBridge().getOriginPrivateDirectory();
    }

    // -- Private picker methods (delegate to JsBridge) --

    private CompletableFuture<List<FileSystemFileHandle>> showOpenFilePicker(OpenFilePickerOptions options) {
        return getBridge().showOpenFilePicker(options);
    }

    private CompletableFuture<FileSystemFileHandle> showSaveFilePicker(SaveFilePickerOptions options) {
        return getBridge().showSaveFilePicker(options);
    }

    private CompletableFuture<FileSystemDirectoryHandle> showDirectoryPicker(DirectoryPickerOptions options) {
        return getBridge().showDirectoryPicker(options);
    }

    JsBridge getBridge() {
        if (bridge == null) {
            bridge = JsBridge.getForComponent(component);
        }
        return bridge;
    }
}
