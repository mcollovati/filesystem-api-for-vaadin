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
import com.vaadin.flow.component.ComponentUtil;
import java.util.List;
import java.util.function.Consumer;

/**
 * Entry point for setting up browserless tests with
 * {@link ClientFileSystem}.
 *
 * <p>Installs a {@link FakeJsBridge} on a component so that all
 * {@link ClientFileSystem} operations run against an in-memory file system
 * instead of browser JavaScript.
 *
 * <pre>{@code
 * FileSystemTester tester = FileSystemTester.forComponent(view)
 *         .withFile("notes.txt", "Hello!")
 *         .onOpenFilePicker(PickerResponse.returning("notes.txt"))
 *         .install();
 *
 * // ... interact with view ...
 *
 * assertEquals("Hello!", tester.fileSystem().file("notes.txt").contentAsString());
 * }</pre>
 *
 * @see PickerResponse
 */
public final class FileSystemTester {

    private final InMemoryFileSystem fs;

    private FileSystemTester(InMemoryFileSystem fs) {
        this.fs = fs;
    }

    /**
     * Returns a new builder for the given component.
     *
     * @param component the component to install the fake bridge on
     * @return a new builder
     */
    public static Builder forComponent(Component component) {
        return new Builder(component);
    }

    /**
     * Returns the in-memory file system for assertions.
     *
     * @return the file system
     */
    public InMemoryFileSystem fileSystem() {
        return fs;
    }

    /**
     * Builder for {@link FileSystemTester}.
     */
    public static final class Builder {

        private final Component component;
        private final InMemoryFileSystem fs = new InMemoryFileSystem();
        private FsDirectory currentDir;
        private PickerResponse<List<FileSystemFileHandle>> openFilePickerResponse;
        private PickerResponse<FileSystemFileHandle> saveFilePickerResponse;
        private PickerResponse<FileSystemDirectoryHandle> directoryPickerResponse;
        private PermissionState permissionState;

        private Builder(Component component) {
            this.component = component;
            this.currentDir = fs.root();
        }

        /**
         * Adds a file with text content to the current directory.
         *
         * @param name        the file name
         * @param textContent the text content
         * @return this builder
         */
        public Builder withFile(String name, String textContent) {
            return withFile(name, textContent.getBytes(java.nio.charset.StandardCharsets.UTF_8), "text/plain");
        }

        /**
         * Adds a file with binary content to the current directory.
         *
         * @param name    the file name
         * @param content the binary content
         * @return this builder
         */
        public Builder withFile(String name, byte[] content) {
            return withFile(name, content, "application/octet-stream");
        }

        /**
         * Adds a file with binary content and a specific MIME type to
         * the current directory.
         *
         * @param name     the file name
         * @param content  the binary content
         * @param mimeType the MIME type
         * @return this builder
         */
        public Builder withFile(String name, byte[] content, String mimeType) {
            currentDir.putChild(name, new FsFile(content, mimeType));
            return this;
        }

        /**
         * Adds an empty subdirectory to the current directory.
         *
         * @param name the directory name
         * @return this builder
         */
        public Builder withDirectory(String name) {
            currentDir.putChild(name, new FsDirectory());
            return this;
        }

        /**
         * Adds a subdirectory and populates it using the given consumer.
         *
         * @param name   the directory name
         * @param nested a consumer that configures the subdirectory contents
         * @return this builder
         */
        public Builder withDirectory(String name, Consumer<Builder> nested) {
            FsDirectory dir = new FsDirectory();
            currentDir.putChild(name, dir);
            FsDirectory previousDir = currentDir;
            currentDir = dir;
            nested.accept(this);
            currentDir = previousDir;
            return this;
        }

        /**
         * Configures the response for open file picker calls.
         *
         * @param response the picker response
         * @return this builder
         */
        public Builder onOpenFilePicker(PickerResponse<List<FileSystemFileHandle>> response) {
            this.openFilePickerResponse = response;
            return this;
        }

        /**
         * Configures the response for save file picker calls.
         *
         * @param response the picker response
         * @return this builder
         */
        public Builder onSaveFilePicker(PickerResponse<FileSystemFileHandle> response) {
            this.saveFilePickerResponse = response;
            return this;
        }

        /**
         * Configures the response for directory picker calls.
         *
         * @param response the picker response
         * @return this builder
         */
        public Builder onDirectoryPicker(PickerResponse<FileSystemDirectoryHandle> response) {
            this.directoryPickerResponse = response;
            return this;
        }

        /**
         * Configures the permission state returned by
         * {@code queryPermission} and {@code requestPermission}.
         *
         * @param state the permission state
         * @return this builder
         */
        public Builder withPermissionState(PermissionState state) {
            this.permissionState = state;
            return this;
        }

        /**
         * Installs the fake bridge on the component and returns the
         * tester instance.
         *
         * @return the installed tester
         */
        public FileSystemTester install() {
            FakeJsBridge bridge = new FakeJsBridge(component, fs);
            if (openFilePickerResponse != null) {
                bridge.setOpenFilePickerResponse(openFilePickerResponse);
            }
            if (saveFilePickerResponse != null) {
                bridge.setSaveFilePickerResponse(saveFilePickerResponse);
            }
            if (directoryPickerResponse != null) {
                bridge.setDirectoryPickerResponse(directoryPickerResponse);
            }
            if (permissionState != null) {
                bridge.setPermissionState(permissionState);
            }
            ComponentUtil.setData(component, JsBridge.class, bridge);
            return new FileSystemTester(fs);
        }
    }
}
