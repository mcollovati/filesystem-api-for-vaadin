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
package com.github.mcollovati.vaadin.filesystem.it;

import com.github.mcollovati.vaadin.filesystem.ClientFileSystem;
import com.github.mcollovati.vaadin.filesystem.FileSystemDirectoryHandle;
import com.github.mcollovati.vaadin.filesystem.PermissionMode;
import com.github.mcollovati.vaadin.filesystem.PermissionState;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;

@Route("test/triggered-pickers")
public class TriggeredPickersTestView extends VerticalLayout {

    private final ClientFileSystem fs = new ClientFileSystem(this);
    private final Pre log = new Pre();
    private NativeButton requestPermission;
    private boolean permissionArmed;

    public TriggeredPickersTestView() {
        log.setId("log");
        log.setWidthFull();

        NativeButton openFile = new NativeButton("Open File (triggered)");
        openFile.setId("open-file");
        fs.openFileTriggeredBy(
                openFile, data -> appendLog("open=" + data.getName() + "|"), error -> appendError("open", error));

        NativeButton openFiles = new NativeButton("Open Files (triggered)");
        openFiles.setId("open-files");
        fs.openFilesTriggeredBy(
                openFiles,
                files -> appendLog("open-files-count=" + files.size() + "|"),
                error -> appendError("open-files", error));

        NativeButton saveText = new NativeButton("Save Text (triggered)");
        saveText.setId("save-text");
        fs.saveFileTriggeredBy(
                saveText,
                "Hello Triggered",
                () -> appendLog("save-text=ok|"),
                error -> appendError("save-text", error));

        NativeButton saveBytes = new NativeButton("Save Bytes (triggered)");
        saveBytes.setId("save-bytes");
        fs.saveFileTriggeredBy(
                saveBytes,
                new byte[] {1, 2, 3},
                () -> appendLog("save-bytes=ok|"),
                error -> appendError("save-bytes", error));

        NativeButton openDirectory = new NativeButton("Open Directory (triggered)");
        openDirectory.setId("open-directory");
        fs.openDirectoryTriggeredBy(
                openDirectory,
                dir -> appendLog("open-directory=" + dir.getName() + "|"),
                error -> appendError("open-directory", error));

        NativeButton listDirectory = new NativeButton("List Directory (triggered)");
        listDirectory.setId("list-directory");
        fs.listDirectoryTriggeredBy(
                listDirectory,
                handles -> appendLog("list-directory-count=" + handles.size() + "|"),
                error -> appendError("list-directory", error));

        NativeButton readContent = new NativeButton("Open File & Log Content (triggered)");
        readContent.setId("read-content");
        fs.openFileTriggeredBy(
                readContent,
                data -> appendLog("read-content=" + new String(data.getContent(), StandardCharsets.UTF_8) + "|"),
                error -> appendError("read-content", error));

        NativeButton pickDirectory = new NativeButton("Pick Directory (for permission)");
        pickDirectory.setId("pick-directory");
        fs.openDirectory()
                .thenAccept(dir -> {
                    appendLog("picked-directory=" + dir.getName() + "|");
                    wirePermission(dir);
                })
                .exceptionally(error -> {
                    appendError("pick-directory", error);
                    return null;
                });

        requestPermission = new NativeButton("Request Permission (triggered)");
        requestPermission.setId("request-permission");
        requestPermission.setEnabled(false);

        add(
                openFile,
                openFiles,
                saveText,
                saveBytes,
                openDirectory,
                listDirectory,
                readContent,
                pickDirectory,
                requestPermission,
                log);
    }

    private void wirePermission(FileSystemDirectoryHandle dir) {
        if (!permissionArmed) {
            permissionArmed = true;
            dir.requestPermissionTriggeredBy(
                    requestPermission,
                    PermissionMode.READWRITE,
                    (PermissionState state) -> appendLog("permission=" + state.getJsValue() + "|"),
                    error -> appendError("request-permission", error));
        }
        requestPermission.setEnabled(true);
    }

    private void appendLog(String message) {
        getUI().ifPresent(ui -> ui.access(() -> log.setText(log.getText() + message)));
    }

    private void appendError(String operation, Throwable error) {
        Throwable cause = error instanceof CompletionException ce && ce.getCause() != null ? ce.getCause() : error;
        appendLog("err=" + operation + ":" + cause.getClass().getSimpleName() + "|");
    }
}
