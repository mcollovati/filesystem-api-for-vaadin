package com.github.mcollovati.vaadin.filesystem.unit;

import static org.junit.jupiter.api.Assertions.*;

import com.github.mcollovati.vaadin.filesystem.FileTypeFilter;
import com.github.mcollovati.vaadin.filesystem.OpenFilePickerOptions;
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
        var options =
                OpenFilePickerOptions.builder().excludeAcceptAllOption(true).build();
        assertTrue(options.getExcludeAcceptAllOption());
    }

    @Test
    void builderSetsStartInString() {
        var options = OpenFilePickerOptions.builder().startIn("documents").build();
        assertEquals("documents", options.getStartIn());
    }
}
