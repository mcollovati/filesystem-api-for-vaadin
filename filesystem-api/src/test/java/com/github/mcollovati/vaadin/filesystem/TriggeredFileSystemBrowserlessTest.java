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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import org.junit.jupiter.api.Test;

@ViewPackages(classes = TriggeredFileSystemBrowserlessTest.TriggeredView.class)
class TriggeredFileSystemBrowserlessTest extends BrowserlessTest {

    @Route("triggered-server-test")
    public static class TriggeredView extends VerticalLayout {}

    @Test
    void pickerTriggersAreArmedAndCanBeDisarmed() {
        TriggeredView view = navigate(TriggeredView.class);
        ClientFileSystem fs = new ClientFileSystem(view);

        NativeButton openFile = new NativeButton();
        NativeButton openFiles = new NativeButton();
        NativeButton saveText = new NativeButton();
        NativeButton saveBytes = new NativeButton();
        NativeButton openDir = new NativeButton();
        NativeButton listDir = new NativeButton();

        Registration regOpen = fs.openFileTriggeredBy(openFile, data -> {}, error -> {});
        Registration regOpenFiles = fs.openFilesTriggeredBy(openFiles, files -> {}, error -> {});
        Registration regSaveText = fs.saveFileTriggeredBy(saveText, "text", () -> {}, error -> {});
        Registration regSaveBytes = fs.saveFileTriggeredBy(saveBytes, new byte[] {1}, () -> {}, error -> {});
        Registration regOpenDir = fs.openDirectoryTriggeredBy(openDir, dir -> {}, error -> {});
        Registration regListDir = fs.listDirectoryTriggeredBy(listDir, entries -> {}, error -> {});

        assertNotNull(regOpen);
        assertNotNull(regOpenFiles);
        assertNotNull(regSaveText);
        assertNotNull(regSaveBytes);
        assertNotNull(regOpenDir);
        assertNotNull(regListDir);

        regOpen.remove();
        regOpenFiles.remove();
        regSaveText.remove();
        regSaveBytes.remove();
        regOpenDir.remove();
        regListDir.remove();
    }

    @Test
    void permissionTriggerIsArmedAndCanBeDisarmed() {
        TriggeredView view = navigate(TriggeredView.class);
        ClientFileSystem fs = new ClientFileSystem(view);
        FileSystemDirectoryHandle handle = new FileSystemDirectoryHandle("dir-1", "docs", fs.getBridge());

        NativeButton button = new NativeButton();
        Registration reg = handle.requestPermissionTriggeredBy(button, PermissionMode.READ, state -> {}, error -> {});
        assertNotNull(reg);
        reg.remove();
    }
}
