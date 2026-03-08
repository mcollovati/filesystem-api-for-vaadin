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
import com.github.mcollovati.vaadin.filesystem.OpenFilePickerOptions;
import com.github.mcollovati.vaadin.filesystem.WellKnownDirectory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenFilePickerOptionsTest {

    @Test
    void defaultOptionsHaveNullFields() {
        var options = OpenFilePickerOptions.builder().build();
        assertNull(options.getTypes());
        assertNull(options.getExcludeAcceptAllOption());
        assertNull(options.getMultiple());
        assertNull(options.getStartIn());
    }

    @Test
    void builderSetsTypes() {
        var filter = new FileTypeFilter("Images", Map.of("image/*", List.of(".png", ".jpg")));
        var options = OpenFilePickerOptions.builder().types(List.of(filter)).build();
        assertEquals(1, options.getTypes().size());
        assertEquals("Images", options.getTypes().get(0).description());
    }

    @Test
    void builderSetsMultiple() {
        var options = OpenFilePickerOptions.builder().multiple(true).build();
        assertTrue(options.getMultiple());
    }

    @Test
    void builderSetsExcludeAcceptAllOption() {
        var filter = FileTypeFilter.of("Images", "image/*", ".png");
        var options = OpenFilePickerOptions.builder()
                .excludeAcceptAllOption(true)
                .types(filter)
                .build();
        assertTrue(options.getExcludeAcceptAllOption());
    }

    @Test
    void builderSetsStartInString() {
        var options = OpenFilePickerOptions.builder().startIn("documents").build();
        assertEquals("documents", options.getStartIn());
    }

    @Test
    void builderSetsStartInWellKnownDirectory() {
        var options = OpenFilePickerOptions.builder()
                .startIn(WellKnownDirectory.PICTURES)
                .build();
        assertEquals("pictures", options.getStartIn());
    }

    @Test
    void builderSetsTypesVarargs() {
        var filter1 = FileTypeFilter.of("Images", "image/*", ".png");
        var filter2 = FileTypeFilter.of("PDF", "application/pdf", ".pdf");
        var options = OpenFilePickerOptions.builder().types(filter1, filter2).build();
        assertEquals(2, options.getTypes().size());
    }

    @Test
    void rebuildCopiesAllFields() {
        var filter = new FileTypeFilter("Images", Map.of("image/*", List.of(".png")));
        var original = OpenFilePickerOptions.builder()
                .types(List.of(filter))
                .excludeAcceptAllOption(true)
                .multiple(true)
                .startIn("documents")
                .build();

        var copy = original.rebuild().build();

        assertEquals(original.getTypes(), copy.getTypes());
        assertEquals(original.getExcludeAcceptAllOption(), copy.getExcludeAcceptAllOption());
        assertEquals(original.getMultiple(), copy.getMultiple());
        assertEquals(original.getStartIn(), copy.getStartIn());
    }

    @Test
    void rebuildAllowsSelectiveOverride() {
        var original = OpenFilePickerOptions.builder()
                .multiple(false)
                .startIn("documents")
                .build();

        var modified = original.rebuild().multiple(true).build();

        assertTrue(modified.getMultiple());
        assertEquals("documents", modified.getStartIn());
    }

    @Test
    void rebuildDoesNotMutateOriginal() {
        var original = OpenFilePickerOptions.builder().multiple(false).build();

        original.rebuild().multiple(true).build();

        assertFalse(original.getMultiple());
    }

    @Test
    void buildThrowsWhenExcludeAcceptAllWithoutTypes() {
        var builder = OpenFilePickerOptions.builder().excludeAcceptAllOption(true);
        var ex = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(ex.getMessage().contains("excludeAcceptAllOption"));
    }

    @Test
    void buildThrowsWhenExcludeAcceptAllWithEmptyTypes() {
        var builder =
                OpenFilePickerOptions.builder().excludeAcceptAllOption(true).types(List.of());
        var ex = assertThrows(IllegalStateException.class, builder::build);
        assertTrue(ex.getMessage().contains("excludeAcceptAllOption"));
    }

    @Test
    void buildSucceedsWhenExcludeAcceptAllWithTypes() {
        var filter = FileTypeFilter.of("Images", "image/*", ".png");
        var options = OpenFilePickerOptions.builder()
                .excludeAcceptAllOption(true)
                .types(filter)
                .build();
        assertTrue(options.getExcludeAcceptAllOption());
        assertEquals(1, options.getTypes().size());
    }
}
