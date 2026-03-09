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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;

/**
 * Options for {@link ClientFileSystem#saveFile(SaveFilePickerOptions, String)}.
 *
 * <p>Use the {@link #builder()} to create an instance.
 *
 * <pre>{@code
 * var options = SaveFilePickerOptions.builder()
 *         .suggestedName("report.pdf")
 *         .types(List.of(new FileTypeFilter("PDF",
 *                 Map.of("application/pdf", List.of(".pdf")))))
 *         .build();
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SaveFilePickerOptions implements Serializable {

    private List<FileTypeFilter> types;
    private Boolean excludeAcceptAllOption;
    private String suggestedName;
    private String startIn;

    private SaveFilePickerOptions() {}

    /**
     * Returns the file type filters.
     *
     * @return the file type filters, or {@code null} if not set
     */
    public List<FileTypeFilter> getTypes() {
        return types;
    }

    /**
     * Returns whether the "accept all" option is excluded.
     *
     * @return {@code true} to exclude the generic file filter, or
     *         {@code null} if not set
     */
    public Boolean getExcludeAcceptAllOption() {
        return excludeAcceptAllOption;
    }

    /**
     * Returns the suggested file name.
     *
     * @return the suggested name, or {@code null} if not set
     */
    public String getSuggestedName() {
        return suggestedName;
    }

    /**
     * Returns the starting directory.
     *
     * @return a well-known directory name or a handle reference, or
     *         {@code null} if not set
     */
    public String getStartIn() {
        return startIn;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a builder initialized with the values of this instance,
     * allowing selective overrides.
     *
     * @return a pre-populated builder
     */
    public Builder rebuild() {
        return new Builder(this);
    }

    /** Builder for {@link SaveFilePickerOptions}. */
    public static final class Builder {

        private final SaveFilePickerOptions options;

        private Builder() {
            this.options = new SaveFilePickerOptions();
        }

        private Builder(SaveFilePickerOptions source) {
            this.options = new SaveFilePickerOptions();
            this.options.types = source.types;
            this.options.excludeAcceptAllOption = source.excludeAcceptAllOption;
            this.options.suggestedName = source.suggestedName;
            this.options.startIn = source.startIn;
        }

        /**
         * Sets the file type filters.
         *
         * @param types the filters
         * @return this builder
         */
        public Builder types(List<FileTypeFilter> types) {
            options.types = types;
            return this;
        }

        /**
         * Sets the file type filters (varargs convenience).
         *
         * @param types the filters
         * @return this builder
         */
        public Builder types(FileTypeFilter... types) {
            return types(List.of(types));
        }

        /**
         * Sets whether to exclude the generic "all files" filter option.
         *
         * @param excludeAcceptAllOption {@code true} to exclude
         * @return this builder
         */
        public Builder excludeAcceptAllOption(boolean excludeAcceptAllOption) {
            options.excludeAcceptAllOption = excludeAcceptAllOption;
            return this;
        }

        /**
         * Sets the suggested file name shown in the save dialog.
         *
         * @param suggestedName the suggested name
         * @return this builder
         */
        public Builder suggestedName(String suggestedName) {
            options.suggestedName = suggestedName;
            return this;
        }

        /**
         * Sets the starting directory to a well-known directory name.
         *
         * @param wellKnownDirectory the directory name
         * @return this builder
         * @see WellKnownDirectory
         */
        public Builder startIn(String wellKnownDirectory) {
            options.startIn = wellKnownDirectory;
            return this;
        }

        /**
         * Sets the starting directory to a well-known directory.
         *
         * @param directory the well-known directory
         * @return this builder
         */
        public Builder startIn(WellKnownDirectory directory) {
            return startIn(directory.getValue());
        }

        /**
         * Sets the starting directory to the directory of the given
         * handle.
         *
         * @param handle a file or directory handle
         * @return this builder
         */
        public Builder startIn(FileSystemHandle handle) {
            options.startIn = ((AbstractFileSystemHandle) handle).handleId();
            return this;
        }

        /**
         * Builds the options.
         *
         * @return the options instance
         * @throws IllegalStateException if {@code excludeAcceptAllOption}
         *         is {@code true} but no file type filters have been set
         */
        public SaveFilePickerOptions build() {
            if (Boolean.TRUE.equals(options.excludeAcceptAllOption)
                    && (options.types == null || options.types.isEmpty())) {
                throw new IllegalStateException("excludeAcceptAllOption requires at least one file type filter");
            }
            return options;
        }
    }
}
