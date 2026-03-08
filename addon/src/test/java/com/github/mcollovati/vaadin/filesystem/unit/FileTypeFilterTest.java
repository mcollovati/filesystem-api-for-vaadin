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

import com.github.mcollovati.vaadin.filesystem.FileTypeFilter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FileTypeFilterTest {

    @Test
    void ofFactory() {
        var filter = FileTypeFilter.of("Images", "image/*", ".png", ".jpg");
        assertEquals("Images", filter.description());
        assertEquals(1, filter.accept().size());
        assertEquals(List.of(".png", ".jpg"), filter.accept().get("image/*"));
    }

    @Test
    void ofFactoryNoExtensions() {
        var filter = FileTypeFilter.of("All Images", "image/*");
        assertEquals("All Images", filter.description());
        assertEquals(List.of(), filter.accept().get("image/*"));
    }

    @Test
    void nullDescription_defaultsToEmpty() {
        var filter = new FileTypeFilter(null, Map.of("image/*", List.of()));
        assertEquals("", filter.description());
    }

    @Test
    void nullAccept_throws() {
        assertThrows(NullPointerException.class, () -> new FileTypeFilter("Images", null));
    }

    @Test
    void emptyAccept_throws() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new FileTypeFilter("Images", Map.of()));
        assertTrue(ex.getMessage().contains("at least one"));
    }

    @Test
    void invalidMimeType_throws() {
        var ex =
                assertThrows(IllegalArgumentException.class, () -> FileTypeFilter.of("Bad", "not-a-mime-type", ".txt"));
        assertTrue(ex.getMessage().contains("Invalid MIME type"));
    }

    @Test
    void extensionWithoutDot_throws() {
        var ex = assertThrows(IllegalArgumentException.class, () -> FileTypeFilter.of("Bad", "text/plain", "txt"));
        assertTrue(ex.getMessage().contains("Invalid file extension"));
    }

    @Test
    void validMimeTypes() {
        assertDoesNotThrow(() -> FileTypeFilter.of("Text", "text/plain", ".txt"));
        assertDoesNotThrow(() -> FileTypeFilter.of("Images", "image/*", ".png"));
        assertDoesNotThrow(() -> FileTypeFilter.of("Custom", "application/vnd.ms-excel", ".xls"));
    }
}
