package com.github.mcollovati.vaadin.filesystem.unit;

import static org.junit.jupiter.api.Assertions.*;

import com.github.mcollovati.vaadin.filesystem.GetHandleOptions;
import org.junit.jupiter.api.Test;

class GetHandleOptionsTest {

    @Test
    void defaultOptions() {
        var options = GetHandleOptions.builder().build();
        assertFalse(options.create());
    }

    @Test
    void createTrue() {
        var options = GetHandleOptions.builder().create(true).build();
        assertTrue(options.create());
    }

    @Test
    void defaultConstructor() {
        var options = new GetHandleOptions();
        assertFalse(options.create());
    }
}
