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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;

/**
 * Wrapper for file content returned by
 * {@link FileSystemFileHandle#getFile()}.
 *
 * <p>This is the Java counterpart of the browser's {@code File} object.
 * It holds the file's metadata (name, size, MIME type, last-modified
 * timestamp) together with the raw byte content.
 */
public final class FileData implements Serializable {

    private final String name;
    private final long size;
    private final String type;
    private final long lastModified;
    private final byte[] content;

    /**
     * Creates a new file data instance.
     *
     * @param name         the file name
     * @param size         the file size in bytes
     * @param type         the MIME type (e.g. {@code "text/plain"})
     * @param lastModified the last-modified timestamp in milliseconds
     *                     since the Unix epoch
     * @param content      the raw file content
     */
    public FileData(String name, long size, String type, long lastModified, byte[] content) {
        this.name = name;
        this.size = size;
        this.type = type;
        this.lastModified = lastModified;
        this.content = content;
    }

    /**
     * Returns the file name.
     *
     * @return the file name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the file size in bytes.
     *
     * @return the file size
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the MIME type of the file.
     *
     * @return the MIME type, or an empty string if unknown
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the last-modified timestamp.
     *
     * @return milliseconds since the Unix epoch
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Returns the raw file content as a byte array.
     *
     * @return the file content
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Returns the file content as an {@link InputStream}.
     *
     * @return an input stream over the file content
     */
    public InputStream getContentAsInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public String toString() {
        return "FileData{name='" + name + "', size=" + size + ", type='" + type + "'}";
    }
}
