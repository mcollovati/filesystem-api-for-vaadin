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
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.Registration;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java API for the browser's File System API.
 *
 * <p>Provides convenient high-level methods that combine picker dialogs
 * with follow-up operations (read, write, stream) so that common
 * workflows can be expressed in a single call.
 *
 * <p>For Origin Private File System (OPFS) access, use
 * {@link OriginPrivateFileSystem} instead.
 *
 * <pre>{@code
 * var fs = new ClientFileSystem(myView);
 *
 * // Pick and read a file in one step
 * fs.openFile().thenAccept(fileData ->
 *     log(fileData.getName() + ": " + fileData.getSize() + " bytes"));
 *
 * // Pick and write text in one step
 * fs.saveFile("Hello, world!");
 * }</pre>
 *
 * @see CallbackClientFileSystem
 */
public final class ClientFileSystem implements Serializable {

    private static final Logger logger = Logger.getLogger(ClientFileSystem.class.getName());

    private final Component component;
    private JsBridge bridge;

    /**
     * Creates a new instance bound to the given component.
     *
     * @param component the component to bind to, not {@code null}
     */
    public ClientFileSystem(Component component) {
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

    // -- Triggered operations --

    /**
     * Arms a click trigger on the given component so that each click opens
     * a file picker and reads the selected file's content.
     *
     * <p>Unlike {@link #openFile(OpenFilePickerOptions)}, the picker is
     * invoked directly inside the browser's click handler, preserving the
     * user gesture. The operation runs once per click; further clicks
     * re-open the picker and report fresh results.
     *
     * @param source    the component whose clicks open the picker
     * @param options   the picker options
     * @param onSuccess called with the file data
     * @param onError   called if an error occurs, or {@code null}
     * @return a registration that inactivates the click trigger
     */
    public Registration openFileTriggeredBy(
            Component source,
            OpenFilePickerOptions options,
            SerializableConsumer<FileData> onSuccess,
            SerializableConsumer<Throwable> onError) {
        return armPicker(
                source,
                PickerPromiseAction.OPEN_FILE,
                options,
                infos -> {
                    FileSystemFileHandle handle = firstFileHandle(infos);
                    if (handle == null) {
                        reportError(onError, new FileSystemApiException("No file selected"));
                    } else if (onSuccess != null) {
                        handle.getFile().thenAccept(onSuccess).exceptionally(e -> {
                            reportError(onError, unwrap(e));
                            return null;
                        });
                    }
                },
                onError);
    }

    /**
     * Arms a click trigger on the given component so that each click opens
     * a file picker with default options and reads the selected file.
     *
     * @param source    the component on which the picker is triggered
     * @param onSuccess called with the file data
     * @param onError   called if an error occurs, or {@code null}
     * @return a registration that disarms the click trigger
     * @see #openFileTriggeredBy(Component, OpenFilePickerOptions, SerializableConsumer,
     *      SerializableConsumer)
     */
    public Registration openFileTriggeredBy(
            Component source, SerializableConsumer<FileData> onSuccess, SerializableConsumer<Throwable> onError) {
        return openFileTriggeredBy(source, OpenFilePickerOptions.builder().build(), onSuccess, onError);
    }

    /**
     * Arms a click trigger on the given component so that each click opens
     * a file picker allowing multiple selection and reads all selected files.
     *
     * <p>{@code multiple(true)} is forced on the effective options regardless
     * of the value in the provided options.
     *
     * @param source    the component on which the picker is triggered
     * @param options   the picker options
     * @param onSuccess called with the list of file data
     * @param onError   called if an error occurs, or {@code null}
     * @return a registration that disarms the click trigger
     */
    public Registration openFilesTriggeredBy(
            Component source,
            OpenFilePickerOptions options,
            SerializableConsumer<List<FileData>> onSuccess,
            SerializableConsumer<Throwable> onError) {
        OpenFilePickerOptions effective = options.rebuild().multiple(true).build();
        return armPicker(
                source,
                PickerPromiseAction.OPEN_FILE,
                effective,
                infos -> {
                    if (onSuccess == null) return;
                    List<FileSystemFileHandle> handles = toFileHandles(infos);
                    List<CompletableFuture<FileData>> futures =
                            handles.stream().map(FileSystemFileHandle::getFile).toList();
                    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                            .thenApply(v -> futures.stream()
                                    .map(CompletableFuture::join)
                                    .toList())
                            .thenAccept(onSuccess)
                            .exceptionally(e -> {
                                reportError(onError, unwrap(e));
                                return null;
                            });
                },
                onError);
    }

    /**
     * Arms a click trigger on the given component so that each click opens
     * a file picker allowing multiple selection with default options and
     * reads all selected files.
     *
     * @param source    the component on which the picker is triggered
     * @param onSuccess called with the list of file data
     * @param onError   called if an error occurs, or {@code null}
     * @return a registration that disarms the click trigger
     * @see #openFilesTriggeredBy(Component, OpenFilePickerOptions, SerializableConsumer,
     *      SerializableConsumer)
     */
    public Registration openFilesTriggeredBy(
            Component source, SerializableConsumer<List<FileData>> onSuccess, SerializableConsumer<Throwable> onError) {
        return openFilesTriggeredBy(source, OpenFilePickerOptions.builder().build(), onSuccess, onError);
    }

    /**
     * Arms a click trigger on the given component so that each click opens
     * a save file picker and writes the given text to the selected file.
     *
     * @param source    the component on which the picker is triggered
     * @param options   the picker options
     * @param text      the text to write
     * @param onSuccess called when the write completes, or {@code null}
     * @param onError   called if an error occurs, or {@code null}
     * @return a registration that disarms the click trigger
     */
    public Registration saveFileTriggeredBy(
            Component source,
            SaveFilePickerOptions options,
            String text,
            SerializableRunnable onSuccess,
            SerializableConsumer<Throwable> onError) {
        return armPicker(
                source,
                PickerPromiseAction.SAVE_FILE,
                options,
                infos -> {
                    FileSystemFileHandle handle = firstFileHandle(infos);
                    if (handle == null) {
                        reportError(onError, new FileSystemApiException("No file selected"));
                    } else {
                        handle.writeString(text)
                                .thenRun(onSuccess != null ? onSuccess : () -> {})
                                .exceptionally(e -> {
                                    reportError(onError, unwrap(e));
                                    return null;
                                });
                    }
                },
                onError);
    }

    /**
     * Arms a click trigger on the given component so that each click opens
     * a save file picker with default options and writes the given text.
     *
     * @param source    the component on which the picker is triggered
     * @param text      the text to write
     * @param onSuccess called when the write completes, or {@code null}
     * @param onError   called if an error occurs, or {@code null}
     * @return a registration that disarms the click trigger
     * @see #saveFileTriggeredBy(Component, SaveFilePickerOptions, String,
     *      SerializableRunnable, SerializableConsumer)
     */
    public Registration saveFileTriggeredBy(
            Component source, String text, SerializableRunnable onSuccess, SerializableConsumer<Throwable> onError) {
        return saveFileTriggeredBy(source, SaveFilePickerOptions.builder().build(), text, onSuccess, onError);
    }

    /**
     * Arms a click trigger on the given component so that each click opens
     * a save file picker and writes the given bytes to the selected file.
     *
     * @param source    the component on which the picker is triggered
     * @param options   the picker options
     * @param data      the bytes to write
     * @param onSuccess called when the write completes, or {@code null}
     * @param onError   called if an error occurs, or {@code null}
     * @return a registration that disarms the click trigger
     */
    public Registration saveFileTriggeredBy(
            Component source,
            SaveFilePickerOptions options,
            byte[] data,
            SerializableRunnable onSuccess,
            SerializableConsumer<Throwable> onError) {
        return armPicker(
                source,
                PickerPromiseAction.SAVE_FILE,
                options,
                infos -> {
                    FileSystemFileHandle handle = firstFileHandle(infos);
                    if (handle == null) {
                        reportError(onError, new FileSystemApiException("No file selected"));
                    } else {
                        handle.writeBytes(data)
                                .thenRun(onSuccess != null ? onSuccess : () -> {})
                                .exceptionally(e -> {
                                    reportError(onError, unwrap(e));
                                    return null;
                                });
                    }
                },
                onError);
    }

    /**
     * Arms a click trigger on the given component so that each click opens
     * a save file picker with default options and writes the given bytes.
     *
     * @param source    the component on which the picker is triggered
     * @param data      the bytes to write
     * @param onSuccess called when the write completes, or {@code null}
     * @param onError   called if an error occurs, or {@code null}
     * @return a registration that disarms the click trigger
     * @see #saveFileTriggeredBy(Component, SaveFilePickerOptions, byte[],
     *      SerializableRunnable, SerializableConsumer)
     */
    public Registration saveFileTriggeredBy(
            Component source, byte[] data, SerializableRunnable onSuccess, SerializableConsumer<Throwable> onError) {
        return saveFileTriggeredBy(source, SaveFilePickerOptions.builder().build(), data, onSuccess, onError);
    }

    /**
     * Arms a click trigger on the given component so that each click opens
     * a directory picker and reports the selected directory handle.
     *
     * @param source    the component on which the picker is triggered
     * @param options   the picker options
     * @param onSuccess called with the directory handle
     * @param onError   called if an error occurs, or {@code null}
     * @return a registration that disarms the click trigger
     */
    public Registration openDirectoryTriggeredBy(
            Component source,
            DirectoryPickerOptions options,
            SerializableConsumer<FileSystemDirectoryHandle> onSuccess,
            SerializableConsumer<Throwable> onError) {
        return armPicker(
                source,
                PickerPromiseAction.OPEN_DIRECTORY,
                options,
                infos -> {
                    FileSystemDirectoryHandle dir = firstDirectoryHandle(infos);
                    if (dir == null) {
                        reportError(onError, new FileSystemApiException("No directory selected"));
                    } else if (onSuccess != null) {
                        onSuccess.accept(dir);
                    }
                },
                onError);
    }

    /**
     * Arms a click trigger on the given component so that each click opens
     * a directory picker with default options.
     *
     * @param source    the component on which the picker is triggered
     * @param onSuccess called with the directory handle
     * @param onError   called if an error occurs, or {@code null}
     * @return a registration that disarms the click trigger
     * @see #openDirectoryTriggeredBy(Component, DirectoryPickerOptions, SerializableConsumer,
     *      SerializableConsumer)
     */
    public Registration openDirectoryTriggeredBy(
            Component source,
            SerializableConsumer<FileSystemDirectoryHandle> onSuccess,
            SerializableConsumer<Throwable> onError) {
        return openDirectoryTriggeredBy(source, DirectoryPickerOptions.builder().build(), onSuccess, onError);
    }

    /**
     * Arms a click trigger on the given component so that each click opens
     * a directory picker and lists all entries in the selected directory.
     *
     * @param source    the component on which the picker is triggered
     * @param options   the picker options
     * @param onSuccess called with the list of child handles
     * @param onError   called if an error occurs, or {@code null}
     * @return a registration that disarms the click trigger
     */
    public Registration listDirectoryTriggeredBy(
            Component source,
            DirectoryPickerOptions options,
            SerializableConsumer<List<FileSystemHandle>> onSuccess,
            SerializableConsumer<Throwable> onError) {
        return armPicker(
                source,
                PickerPromiseAction.OPEN_DIRECTORY,
                options,
                infos -> {
                    FileSystemDirectoryHandle dir = firstDirectoryHandle(infos);
                    if (dir == null) {
                        reportError(onError, new FileSystemApiException("No directory selected"));
                    } else if (onSuccess != null) {
                        dir.entries().thenAccept(onSuccess).exceptionally(e -> {
                            reportError(onError, unwrap(e));
                            return null;
                        });
                    }
                },
                onError);
    }

    /**
     * Arms a click trigger on the given component so that each click opens
     * a directory picker with default options and lists all entries.
     *
     * @param source    the component on which the picker is triggered
     * @param onSuccess called with the list of child handles
     * @param onError   called if an error occurs, or {@code null}
     * @return a registration that disarms the click trigger
     * @see #listDirectoryTriggeredBy(Component, DirectoryPickerOptions, SerializableConsumer,
     *      SerializableConsumer)
     */
    public Registration listDirectoryTriggeredBy(
            Component source,
            SerializableConsumer<List<FileSystemHandle>> onSuccess,
            SerializableConsumer<Throwable> onError) {
        return listDirectoryTriggeredBy(source, DirectoryPickerOptions.builder().build(), onSuccess, onError);
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

    /**
     * Arms a {@link ClickTrigger} on the given component that opens the
     * picker and reports picked handles each time the user clicks.
     *
     * @param source          the component whose clicks open the picker
     * @param pickerExpression the JS expression that binds a {@code handles}
     *                         array (see {@link PickerPromiseAction})
     * @param options         the picker options
     * @param onPicked        invoked with the picked handle metadata
     * @param onError         user error callback, or {@code null}
     * @return a registration that disarms the click trigger
     */
    private Registration armPicker(
            Component source,
            String pickerExpression,
            Object options,
            SerializableConsumer<HandleInfo[]> onPicked,
            SerializableConsumer<Throwable> onError) {
        JsBridge bridge = getBridge();
        bridge.ensureInitialized();
        PickerPromiseAction action = new PickerPromiseAction(
                pickerExpression,
                options,
                bridge.element(),
                onPicked,
                jsError -> reportError(onError, JsBridge.mapError(jsError.name(), jsError.message())));
        ClickTrigger trigger = new ClickTrigger(source);
        trigger.triggers(action);
        return trigger::remove;
    }

    private List<FileSystemFileHandle> toFileHandles(HandleInfo[] infos) {
        return Arrays.stream(infos)
                .map(info -> new FileSystemFileHandle(info.id(), info.name(), getBridge()))
                .toList();
    }

    private FileSystemFileHandle firstFileHandle(HandleInfo[] infos) {
        return infos.length == 0 ? null : new FileSystemFileHandle(infos[0].id(), infos[0].name(), getBridge());
    }

    private FileSystemDirectoryHandle firstDirectoryHandle(HandleInfo[] infos) {
        return infos.length == 0 ? null : new FileSystemDirectoryHandle(infos[0].id(), infos[0].name(), getBridge());
    }

    private void reportError(SerializableConsumer<Throwable> onError, Throwable error) {
        if (onError != null) {
            onError.accept(error);
        } else {
            logger.log(Level.FINE, "File System API operation failed", error);
        }
    }

    private Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }

    JsBridge getBridge() {
        if (bridge == null) {
            bridge = JsBridge.getForComponent(component);
        }
        return bridge;
    }
}
