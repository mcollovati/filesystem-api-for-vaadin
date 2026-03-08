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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Describes a file type filter for use in file picker options.
 *
 * <p>Each filter has a user-visible description and an accept map that
 * maps MIME types to lists of file extensions.
 *
 * <pre>{@code
 * var filter = new FileTypeFilter("Images",
 *         Map.of("image/*", List.of(".png", ".jpg")));
 * }</pre>
 *
 * @param description an optional human-readable description of the filter;
 *                    {@code null} is normalized to an empty string
 * @param accept a map of MIME types to accepted file extensions
 * @see OpenFilePickerOptions
 * @see SaveFilePickerOptions
 */
public record FileTypeFilter(String description, Map<String, List<String>> accept) implements Serializable {

    private static final Pattern MIME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9!#$&\\-^_.+]*/(\\*|[a-zA-Z0-9][a-zA-Z0-9!#$&\\-^_.+]*)$");

    /**
     * Validates MIME types and file extensions. A {@code null} description
     * is normalized to an empty string.
     */
    public FileTypeFilter {
        description = description != null ? description : "";
        Objects.requireNonNull(accept, "accept must not be null");
        if (accept.isEmpty()) {
            throw new IllegalArgumentException("accept must contain at least one MIME type entry");
        }
        for (var entry : accept.entrySet()) {
            String mimeType = entry.getKey();
            if (mimeType == null || !MIME_PATTERN.matcher(mimeType).matches()) {
                throw new IllegalArgumentException("Invalid MIME type: '" + mimeType
                        + "'. Expected format: type/subtype (e.g. 'image/*', 'text/plain')");
            }
            for (String ext : entry.getValue()) {
                if (ext == null || !ext.startsWith(".")) {
                    throw new IllegalArgumentException(
                            "Invalid file extension: '" + ext + "'. Extensions must start with '.'");
                }
            }
        }
    }

    /**
     * Creates a filter for a single MIME type with optional extensions.
     *
     * <pre>{@code
     * FileTypeFilter.of("Images", "image/*", ".png", ".jpg")
     * }</pre>
     *
     * @param description a human-readable description
     * @param mimeType    the MIME type (e.g. {@code "image/*"})
     * @param extensions  file extensions (e.g. {@code ".png"})
     * @return a new filter
     */
    public static FileTypeFilter of(String description, String mimeType, String... extensions) {
        return new FileTypeFilter(description, Map.of(mimeType, List.of(extensions)));
    }
}
