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

/**
 * Shared JavaScript snippets used by the {@code JsDefinition} interfaces.
 *
 * <p>All values are compile-time constants so they can be referenced from
 * {@link com.vaadin.flow.js.JsExpression} annotations.
 */
final class JsExpressions {

    private JsExpressions() {
        // constants only
    }

    /**
     * JS snippet that copies the options parameter ({@code $0}) and resolves
     * {@code startIn} if it refers to a registered handle.
     */
    static final String RESOLVE_START_IN =
            """
            const opts = Object.assign({}, $0);
            if (opts.startIn && this.__fsApiHandles.has(opts.startIn)) {
                opts.startIn = this.__fsApiHandles.get(opts.startIn);
            }""";

    /**
     * JS snippet that registers an array of handles into the registry and
     * returns their metadata. Expects a {@code handles} variable to be in
     * scope.
     */
    static final String REGISTER_HANDLES =
            """
            return handles.map(h => {
                const id = String(this.__fsApiNextId++);
                this.__fsApiHandles.set(id, h);
                return {id: id, name: h.name, kind: h.kind};
            });""";

    /**
     * JS snippet that registers a single handle and returns its metadata.
     * Expects a {@code handle} variable to be in scope.
     */
    static final String REGISTER_SINGLE_HANDLE =
            """
            const id = String(this.__fsApiNextId++);
            this.__fsApiHandles.set(id, handle);
            return {id: id, name: handle.name, kind: handle.kind};""";

    /**
     * JS snippet that triggers a browser download from a file handle
     * variable {@code h}. Uses {@code $1} as the download name with a
     * fallback to the file's own name.
     */
    static final String SAVE_TO_DEVICE =
            """
            const file = await h.getFile();
            const url = URL.createObjectURL(file);
            const a = document.createElement('a');
            a.href = url;
            a.download = $1 || file.name;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);""";

    /**
     * Opening of a try-catch block. Used together with {@link #TRY_CATCH_CLOSE}
     * to wrap an expression and map {@code DOMException} errors to a
     * structured message that the server can parse.
     */
    static final String TRY_CATCH_OPEN = """
            try {""";

    /**
     * Close of the try-catch block started by {@link #TRY_CATCH_OPEN}.
     */
    static final String TRY_CATCH_CLOSE =
            """
            } catch(__e) {
                throw new Error((__e.name || 'Error') + ': ' + __e.message);
            }""";

    /**
     * JS snippet that retrieves a handle from the registry into variable
     * {@code h} and throws a descriptive {@code NotFoundError} if the handle
     * is missing.
     */
    static final String REQUIRE_HANDLE_H =
            """
            const h = this.__fsApiHandles.get($0);
            if (!h) throw new DOMException('Handle not found (released or invalid)', 'NotFoundError');
            """;

    /**
     * JS snippet that retrieves a handle from the registry into variable
     * {@code h1} and throws a descriptive {@code NotFoundError} if the handle
     * is missing.
     */
    static final String REQUIRE_HANDLE_H1 =
            """
            const h1 = this.__fsApiHandles.get($0);
            if (!h1) throw new DOMException('Handle not found (released or invalid)', 'NotFoundError');
            """;

    /**
     * JS snippet that retrieves a handle from the registry into variable
     * {@code h2} and throws a descriptive {@code NotFoundError} if the handle
     * is missing. Expects the handle id in {@code $1}.
     */
    static final String REQUIRE_HANDLE_H2 =
            """
            const h2 = this.__fsApiHandles.get($1);
            if (!h2) throw new DOMException('Handle not found (released or invalid)', 'NotFoundError');
            """;

    /**
     * JS snippet that retrieves a handle from the registry into variable
     * {@code dir} and throws a descriptive {@code NotFoundError} if the
     * handle is missing.
     */
    static final String REQUIRE_HANDLE_DIR =
            """
            const dir = this.__fsApiHandles.get($0);
            if (!dir) throw new DOMException('Handle not found (released or invalid)', 'NotFoundError');
            """;

    /**
     * JS snippet that retrieves a handle from the registry into variable
     * {@code child} and throws a descriptive {@code NotFoundError} if the
     * handle is missing. Expects the handle id in {@code $1}.
     */
    static final String REQUIRE_HANDLE_CHILD =
            """
            const child = this.__fsApiHandles.get($1);
            if (!child) throw new DOMException('Handle not found (released or invalid)', 'NotFoundError');
            """;

    /**
     * JS snippet that retrieves a writable stream from the registry and
     * throws a descriptive {@code NotFoundError} if the stream is missing.
     */
    static final String REQUIRE_WRITABLE =
            """
            const w = this.__fsApiWritables.get($0);
            if (!w) throw new DOMException('Writable stream not found (closed or invalid)', 'NotFoundError');
            """;

    /**
     * JS snippet that navigates from the OPFS root to the parent of the
     * last path segment. After execution, {@code dir} holds the parent
     * directory and {@code leafName} the final path segment.
     *
     * <p>{@code $0} is the path and {@code $1} are the options passed to
     * intermediate {@code getDirectoryHandle} calls.
     */
    static final String OPFS_NAVIGATE_TO_PARENT =
            """
            const root = await navigator.storage.getDirectory();
            const segments = $0.split('/').filter(s => s.length > 0);
            let dir = root;
            for (let i = 0; i < segments.length - 1; i++) {
                dir = await dir.getDirectoryHandle(segments[i], $1);
            }
            const leafName = segments[segments.length - 1];""";

    /**
     * JS snippet that navigates from the OPFS root to the parent of the
     * last path segment using empty options for intermediate directories.
     */
    static final String OPFS_NAVIGATE_TO_PARENT_EMPTY =
            """
            const root = await navigator.storage.getDirectory();
            const segments = $0.split('/').filter(s => s.length > 0);
            let dir = root;
            for (let i = 0; i < segments.length - 1; i++) {
                dir = await dir.getDirectoryHandle(segments[i], {});
            }
            const leafName = segments[segments.length - 1];""";

    /**
     * JS snippet that navigates from the OPFS root to the parent of the
     * last path segment creating intermediate directories as needed.
     */
    static final String OPFS_NAVIGATE_TO_PARENT_CREATE =
            """
            const root = await navigator.storage.getDirectory();
            const segments = $0.split('/').filter(s => s.length > 0);
            let dir = root;
            for (let i = 0; i < segments.length - 1; i++) {
                dir = await dir.getDirectoryHandle(segments[i], {create:true});
            }
            const leafName = segments[segments.length - 1];""";

    /**
     * JS snippet that navigates from the OPFS root through all segments of
     * the path in {@code $0}. After execution, {@code dir} holds the target
     * directory. {@code $1} are the options passed to intermediate
     * {@code getDirectoryHandle} calls.
     */
    static final String OPFS_NAVIGATE_SEGMENTS =
            """
            const root = await navigator.storage.getDirectory();
            const segments = $0.split('/').filter(s => s.length > 0);
            let dir = root;
            for (let i = 0; i < segments.length; i++) {
                dir = await dir.getDirectoryHandle(segments[i], $1);
            }""";
}
