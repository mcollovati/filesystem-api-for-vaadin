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

import static com.github.mcollovati.vaadin.filesystem.JsExpressions.OPFS_NAVIGATE_SEGMENTS;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.OPFS_NAVIGATE_TO_PARENT;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.OPFS_NAVIGATE_TO_PARENT_CREATE;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.OPFS_NAVIGATE_TO_PARENT_EMPTY;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.REGISTER_HANDLES;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.REGISTER_SINGLE_HANDLE;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.REQUIRE_HANDLE_CHILD;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.REQUIRE_HANDLE_DIR;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.REQUIRE_HANDLE_H;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.REQUIRE_HANDLE_H1;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.REQUIRE_HANDLE_H2;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.REQUIRE_WRITABLE;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.RESOLVE_START_IN;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.SAVE_TO_DEVICE;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.TRY_CATCH_CLOSE;
import static com.github.mcollovati.vaadin.filesystem.JsExpressions.TRY_CATCH_OPEN;

import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import java.io.Serializable;

/**
 * JavaScript definitions for the browser File System API operations used by
 * this add-on.
 *
 * <p>Each nested interface declares the JavaScript executed by one area of the
 * API. They are collected into the frontend bundle at build time, so the
 * client never needs to evaluate JavaScript from a string.
 */
public final class FileSystemJS {

    private FileSystemJS() {
        // definitions only
    }

    /**
     * JavaScript definition for checking browser support of the File System
     * API.
     */
    @JsDefinition
    public interface Support extends Serializable {

        /**
         * Checks whether the browser supports the File System API.
         *
         * @return a pending result that completes with {@code true} if
         *         supported
         */
        @JsExpression("return typeof window.showOpenFilePicker === 'function';")
        PendingJavaScriptResult isSupported();
    }

    /**
     * JavaScript definition for lifecycle operations on the client-side handle
     * and writable stream registries.
     */
    @JsDefinition
    public interface Lifecycle extends Serializable {

        /**
         * Initializes the client-side registries on the host element.
         */
        @JsExpression(
                """
                this.__fsApiHandles = this.__fsApiHandles || new Map();
                this.__fsApiNextId = this.__fsApiNextId || 0;
                this.__fsApiWritables = this.__fsApiWritables || new Map();
                this.__fsApiNextWritableId = this.__fsApiNextWritableId || 0;""")
        void initialize();

        /**
         * Cleans up all client-side registries and closes open writable
         * streams best-effort.
         */
        @JsExpression(
                """
                if (this.__fsApiWritables) {
                    for (const w of this.__fsApiWritables.values()) {
                        try { w.close(); } catch(e) {}
                    }
                    this.__fsApiWritables.clear();
                }
                if (this.__fsApiHandles) { this.__fsApiHandles.clear(); }""")
        void cleanup();
    }

    /**
     * JavaScript definition for picker and origin-private directory
     * operations.
     */
    @JsDefinition
    public interface Picker extends Serializable {

        @JsExpression(TRY_CATCH_OPEN
                + RESOLVE_START_IN
                + "\nconst handles = await window.showOpenFilePicker(opts);\n"
                + REGISTER_HANDLES
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult showOpenFilePicker(OpenFilePickerOptions options);

        @JsExpression(TRY_CATCH_OPEN
                + RESOLVE_START_IN
                + "\nconst handles = [await window.showSaveFilePicker(opts)];\n"
                + REGISTER_HANDLES
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult showSaveFilePicker(SaveFilePickerOptions options);

        @JsExpression(TRY_CATCH_OPEN
                + RESOLVE_START_IN
                + "\nconst handles = [await window.showDirectoryPicker(opts)];\n"
                + REGISTER_HANDLES
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult showDirectoryPicker(DirectoryPickerOptions options);

        @JsExpression(TRY_CATCH_OPEN
                + "\nconst handle = await navigator.storage.getDirectory();\n"
                + REGISTER_SINGLE_HANDLE
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult getOriginPrivateDirectory();
    }

    /**
     * JavaScript definition for operations on file and directory handles.
     */
    @JsDefinition
    public interface Handle extends Serializable {

        @JsExpression(TRY_CATCH_OPEN
                + REQUIRE_HANDLE_H1
                + REQUIRE_HANDLE_H2
                + "\nreturn await h1.isSameEntry(h2);\n"
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult isSameEntry(String handleId1, String handleId2);

        @JsExpression(
                TRY_CATCH_OPEN + REQUIRE_HANDLE_H + "\nreturn await h.queryPermission({mode: $1});\n" + TRY_CATCH_CLOSE)
        PendingJavaScriptResult queryPermission(String handleId, PermissionMode mode);

        @JsExpression(TRY_CATCH_OPEN
                + REQUIRE_HANDLE_H
                + "\nreturn await h.requestPermission({mode: $1});\n"
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult requestPermission(String handleId, PermissionMode mode);

        @JsExpression(TRY_CATCH_OPEN
                + REQUIRE_HANDLE_DIR
                + "\nconst handle = await dir.getFileHandle($1, $2);\n"
                + REGISTER_SINGLE_HANDLE
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult getFileHandle(String dirHandleId, String name, GetHandleOptions options);

        @JsExpression(TRY_CATCH_OPEN
                + REQUIRE_HANDLE_DIR
                + "\nconst handle = await dir.getDirectoryHandle($1, $2);\n"
                + REGISTER_SINGLE_HANDLE
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult getDirectoryHandle(String dirHandleId, String name, GetHandleOptions options);

        @JsExpression(TRY_CATCH_OPEN + REQUIRE_HANDLE_DIR + "\nawait dir.removeEntry($1, $2);\n" + TRY_CATCH_CLOSE)
        PendingJavaScriptResult removeEntry(String dirHandleId, String name, RemoveEntryOptions options);

        @JsExpression(TRY_CATCH_OPEN
                + REQUIRE_HANDLE_DIR
                + REQUIRE_HANDLE_CHILD
                + "\nconst path = await dir.resolve(child);\n"
                + "return path;\n"
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult resolve(String dirHandleId, String childHandleId);

        @JsExpression(TRY_CATCH_OPEN
                + REQUIRE_HANDLE_DIR
                + """

                const result = [];
                for await (const [name, handle] of dir.entries()) {
                    const id = String(this.__fsApiNextId++);
                    this.__fsApiHandles.set(id, handle);
                    result.push({id: id, name: handle.name, kind: handle.kind});
                }
                return result;"""
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult entries(String dirHandleId);

        @JsExpression(TRY_CATCH_OPEN
                + REQUIRE_HANDLE_H
                + """

                const file = await h.getFile();
                const buf = await file.arrayBuffer();
                const bytes = new Uint8Array(buf);
                let binary = '';
                for (let i = 0; i < bytes.length; i++) {
                    binary += String.fromCharCode(bytes[i]);
                }
                return {name: file.name, size: file.size, type: file.type,
                    lastModified: file.lastModified, content: btoa(binary)};"""
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult getFile(String handleId);

        @JsExpression(TRY_CATCH_OPEN + REQUIRE_HANDLE_H + "\n" + SAVE_TO_DEVICE + TRY_CATCH_CLOSE)
        PendingJavaScriptResult saveToDevice(String handleId, String downloadName);

        @JsExpression(TRY_CATCH_OPEN
                + REQUIRE_HANDLE_H
                + """

                const file = await h.getFile();
                const formData = new FormData();
                formData.append('file', file, file.name);
                const url = this.getAttribute($1);
                const response = await fetch(url, {method: 'POST', body: formData});
                if (!response.ok) {
                    throw new Error('NotAllowedError: Upload failed with status ' + response.status);
                }"""
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult uploadTo(String handleId, String attributeName);

        @JsExpression(TRY_CATCH_OPEN
                + REQUIRE_HANDLE_H
                + """

                const url = this.getAttribute($1);
                const response = await fetch(url);
                if (!response.ok) {
                    throw new Error('NotAllowedError: Download failed with status ' + response.status);
                }
                const writable = await h.createWritable();
                const reader = response.body.getReader();
                while (true) {
                    const {done, value} = await reader.read();
                    if (done) break;
                    await writable.write(value);
                }
                await writable.close();"""
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult downloadFrom(String handleId, String attributeName);

        @JsExpression("this.__fsApiHandles.delete($0);")
        void releaseHandle(String handleId);
    }

    /**
     * JavaScript definition for origin-private file system (OPFS) path-based
     * operations.
     */
    @JsDefinition
    public interface Opfs extends Serializable {

        @JsExpression(TRY_CATCH_OPEN
                + OPFS_NAVIGATE_TO_PARENT
                + "\nconst handle = await dir.getFileHandle(leafName, $1);\n"
                + REGISTER_SINGLE_HANDLE
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult opfsGetFileHandle(String path, GetHandleOptions options);

        @JsExpression(TRY_CATCH_OPEN
                + OPFS_NAVIGATE_TO_PARENT
                + "\nconst handle = await dir.getDirectoryHandle(leafName, $1);\n"
                + REGISTER_SINGLE_HANDLE
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult opfsGetDirectoryHandle(String path, GetHandleOptions options);

        @JsExpression(TRY_CATCH_OPEN
                + OPFS_NAVIGATE_TO_PARENT_EMPTY
                + """

                const h = await dir.getFileHandle(leafName);
                const file = await h.getFile();
                const buf = await file.arrayBuffer();
                const bytes = new Uint8Array(buf);
                let binary = '';
                for (let i = 0; i < bytes.length; i++) {
                    binary += String.fromCharCode(bytes[i]);
                }
                return {name: file.name, size: file.size, type: file.type,
                    lastModified: file.lastModified, content: btoa(binary)};"""
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult opfsReadFile(String path);

        @JsExpression(TRY_CATCH_OPEN
                + OPFS_NAVIGATE_TO_PARENT_CREATE
                + """

                const h = await dir.getFileHandle(leafName, {create:true});
                const w = await h.createWritable();
                await w.write($1);
                await w.close();"""
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult opfsWriteText(String path, String text);

        @JsExpression(TRY_CATCH_OPEN
                + OPFS_NAVIGATE_TO_PARENT_CREATE
                + """

                const h = await dir.getFileHandle(leafName, {create:true});
                const w = await h.createWritable();
                const binary = atob($1);
                const bytes = new Uint8Array(binary.length);
                for (let i = 0; i < binary.length; i++) {
                    bytes[i] = binary.charCodeAt(i);
                }
                await w.write(bytes);
                await w.close();"""
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult opfsWriteBytes(String path, String base64);

        @JsExpression(TRY_CATCH_OPEN
                + OPFS_NAVIGATE_SEGMENTS
                + """

                const result = [];
                for await (const [name, handle] of dir.entries()) {
                    const id = String(this.__fsApiNextId++);
                    this.__fsApiHandles.set(id, handle);
                    result.push({id: id, name: handle.name, kind: handle.kind});
                }
                return result;"""
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult opfsEntries(String path, GetHandleOptions options);

        @JsExpression(TRY_CATCH_OPEN
                + OPFS_NAVIGATE_TO_PARENT_EMPTY
                + "\nawait dir.removeEntry(leafName, $1);\n"
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult opfsRemoveEntry(String path, RemoveEntryOptions options);

        @JsExpression(TRY_CATCH_OPEN
                + """

                const root = await navigator.storage.getDirectory();
                for await (const [name] of root.entries()) {
                    await root.removeEntry(name, {recursive: true});
                }"""
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult opfsClear();

        @JsExpression(TRY_CATCH_OPEN
                + OPFS_NAVIGATE_TO_PARENT_EMPTY
                + "\nconst h = await dir.getFileHandle(leafName);\n"
                + SAVE_TO_DEVICE
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult opfsSaveToDevice(String path, String downloadName);
    }

    /**
     * JavaScript definition for writable stream operations.
     */
    @JsDefinition
    public interface Writable extends Serializable {

        @JsExpression(TRY_CATCH_OPEN
                + REQUIRE_HANDLE_H
                + """

                const writable = await h.createWritable($1);
                const id = String(this.__fsApiNextWritableId++);
                this.__fsApiWritables.set(id, writable);
                return id;"""
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult createWritable(String handleId, WritableOptions options);

        @JsExpression(TRY_CATCH_OPEN + REQUIRE_WRITABLE + "\nawait w.write($1);\n" + TRY_CATCH_CLOSE)
        PendingJavaScriptResult writableWriteText(String streamId, String text);

        @JsExpression(TRY_CATCH_OPEN
                + REQUIRE_WRITABLE
                + """

                const binary = atob($1);
                const bytes = new Uint8Array(binary.length);
                for (let i = 0; i < binary.length; i++) {
                    bytes[i] = binary.charCodeAt(i);
                }
                await w.write(bytes);"""
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult writableWriteBytes(String streamId, String base64);

        @JsExpression(TRY_CATCH_OPEN + REQUIRE_WRITABLE + "\nawait w.seek($1);\n" + TRY_CATCH_CLOSE)
        PendingJavaScriptResult writableSeek(String streamId, double position);

        @JsExpression(TRY_CATCH_OPEN + REQUIRE_WRITABLE + "\nawait w.truncate($1);\n" + TRY_CATCH_CLOSE)
        PendingJavaScriptResult writableTruncate(String streamId, double size);

        @JsExpression(TRY_CATCH_OPEN
                + REQUIRE_WRITABLE
                + """

                await w.close();
                this.__fsApiWritables.delete($0);"""
                + TRY_CATCH_CLOSE)
        PendingJavaScriptResult writableClose(String streamId);
    }
}
