package com.github.mcollovati.vaadin.filesystem.unit;

import static org.junit.jupiter.api.Assertions.*;

import com.github.mcollovati.vaadin.filesystem.DirectoryPickerOptions;
import com.github.mcollovati.vaadin.filesystem.PermissionMode;
import org.junit.jupiter.api.Test;

class DirectoryPickerOptionsTest {

    @Test
    void defaultOptionsHaveNullFields() {
        var options = DirectoryPickerOptions.builder().build();
        assertNull(options.getStartIn());
        assertNull(options.getMode());
    }

    @Test
    void builderSetsMode() {
        var options =
                DirectoryPickerOptions.builder().mode(PermissionMode.READWRITE).build();
        assertEquals(PermissionMode.READWRITE, options.getMode());
    }

    @Test
    void builderSetsStartInString() {
        var options = DirectoryPickerOptions.builder().startIn("desktop").build();
        assertEquals("desktop", options.getStartIn());
    }
}
