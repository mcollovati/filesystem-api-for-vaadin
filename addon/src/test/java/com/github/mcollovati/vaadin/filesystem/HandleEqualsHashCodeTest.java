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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HandleEqualsHashCodeTest {

    @Test
    void sameHandleIdMeansEqual() {
        var a = new FileSystemFileHandle("1", "file.txt", null);
        var b = new FileSystemFileHandle("1", "other.txt", null);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentHandleIdMeansNotEqual() {
        var a = new FileSystemFileHandle("1", "file.txt", null);
        var b = new FileSystemFileHandle("2", "file.txt", null);
        assertNotEquals(a, b);
    }

    @Test
    void fileAndDirectoryWithSameIdAreEqual() {
        var file = new FileSystemFileHandle("1", "name", null);
        var dir = new FileSystemDirectoryHandle("1", "name", null);
        assertEquals(file, dir);
    }

    @Test
    void notEqualToNull() {
        var handle = new FileSystemFileHandle("1", "file.txt", null);
        assertNotEquals(null, handle);
    }

    @Test
    void notEqualToOtherType() {
        var handle = new FileSystemFileHandle("1", "file.txt", null);
        assertNotEquals("1", handle);
    }

    @Test
    void equalToSelf() {
        var handle = new FileSystemDirectoryHandle("1", "dir", null);
        assertEquals(handle, handle);
    }

    @Test
    void fileHandleToString() {
        var handle = new FileSystemFileHandle("1", "photo.png", null);
        assertEquals("FileSystemFileHandle{name='photo.png'}", handle.toString());
    }

    @Test
    void directoryHandleToString() {
        var handle = new FileSystemDirectoryHandle("1", "docs", null);
        assertEquals("FileSystemDirectoryHandle{name='docs'}", handle.toString());
    }
}
