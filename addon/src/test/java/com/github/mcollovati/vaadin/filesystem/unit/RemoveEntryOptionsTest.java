package com.github.mcollovati.vaadin.filesystem.unit;

import static org.junit.jupiter.api.Assertions.*;

import com.github.mcollovati.vaadin.filesystem.RemoveEntryOptions;
import org.junit.jupiter.api.Test;

class RemoveEntryOptionsTest {

    @Test
    void defaultOptions() {
        var options = RemoveEntryOptions.builder().build();
        assertFalse(options.recursive());
    }

    @Test
    void recursiveTrue() {
        var options = RemoveEntryOptions.builder().recursive(true).build();
        assertTrue(options.recursive());
    }

    @Test
    void defaultConstructor() {
        var options = new RemoveEntryOptions();
        assertFalse(options.recursive());
    }
}
