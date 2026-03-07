package com.github.mcollovati.vaadin.filesystem.unit;

import static org.junit.jupiter.api.Assertions.*;

import com.github.mcollovati.vaadin.filesystem.FileTypeFilter;
import com.github.mcollovati.vaadin.filesystem.SaveFilePickerOptions;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SaveFilePickerOptionsTest {

    @Test
    void defaultOptionsHaveNullFields() {
        var options = SaveFilePickerOptions.builder().build();
        assertNull(options.getTypes());
        assertNull(options.getExcludeAcceptAllOption());
        assertNull(options.getSuggestedName());
        assertNull(options.getStartIn());
    }

    @Test
    void builderSetsSuggestedName() {
        var options =
                SaveFilePickerOptions.builder().suggestedName("report.pdf").build();
        assertEquals("report.pdf", options.getSuggestedName());
    }

    @Test
    void builderSetsTypes() {
        var filter = new FileTypeFilter("PDF", Map.of("application/pdf", List.of(".pdf")));
        var options = SaveFilePickerOptions.builder().types(List.of(filter)).build();
        assertEquals(1, options.getTypes().size());
    }

    @Test
    void builderSetsStartInString() {
        var options = SaveFilePickerOptions.builder().startIn("downloads").build();
        assertEquals("downloads", options.getStartIn());
    }
}
