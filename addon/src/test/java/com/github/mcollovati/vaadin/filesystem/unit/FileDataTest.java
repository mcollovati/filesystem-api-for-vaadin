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
package com.github.mcollovati.vaadin.filesystem.unit;

import static org.junit.jupiter.api.Assertions.*;

import com.github.mcollovati.vaadin.filesystem.FileData;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class FileDataTest {

    @Test
    void constructorAndAccessors() {
        byte[] content = "hello".getBytes();
        var fileData = new FileData("test.txt", 5L, "text/plain", 1700000000000L, content);

        assertEquals("test.txt", fileData.getName());
        assertEquals(5L, fileData.getSize());
        assertEquals("text/plain", fileData.getType());
        assertEquals(1700000000000L, fileData.getLastModified());
        assertArrayEquals(content, fileData.getContent());
    }

    @Test
    void emptyContent() {
        var fileData = new FileData("empty.bin", 0L, "", 0L, new byte[0]);

        assertEquals(0L, fileData.getSize());
        assertEquals(0, fileData.getContent().length);
    }

    @Test
    void getContentAsInputStream() throws IOException {
        byte[] content = "stream test".getBytes();
        var fileData = new FileData("stream.txt", content.length, "text/plain", 0L, content);

        try (var is = fileData.getContentAsInputStream()) {
            assertArrayEquals(content, is.readAllBytes());
        }
    }

    @Test
    void getContentAsInputStreamEmpty() throws IOException {
        var fileData = new FileData("empty.txt", 0L, "", 0L, new byte[0]);

        try (var is = fileData.getContentAsInputStream()) {
            assertEquals(0, is.readAllBytes().length);
        }
    }

    @Test
    void toStringContainsNameSizeAndType() {
        var fileData = new FileData("report.pdf", 1024L, "application/pdf", 0L, new byte[0]);
        var str = fileData.toString();
        assertEquals("FileData{name='report.pdf', size=1024, type='application/pdf'}", str);
    }
}
