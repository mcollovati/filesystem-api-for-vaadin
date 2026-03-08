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
 * Options for {@link FileSystemAPI#openFile(OpenFilePickerOptions)}.
 *
 * <p>Use the {@link #builder()} to create an instance.
 *
 * <pre>{@code
 * var options = OpenFilePickerOptions.builder()
 *         .multiple(true)
 *         .types(List.of(new FileTypeFilter("Images",
 *                 Map.of("image/*", List.of(".png", ".jpg")))))
 *         .build();
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class OpenFilePickerOptions implements Serializable {

    private List<FileTypeFilter> types;
    private Boolean excludeAcceptAllOption;
    private Boolean multiple;
    private String startIn;

    private OpenFilePickerOptions() {}

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
     * Returns whether multiple files can be selected.
     *
     * @return {@code true} to allow multi-selection, or {@code null} if
     *         not set
     */
    public Boolean getMultiple() {
        return multiple;
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

    /** Builder for {@link OpenFilePickerOptions}. */
    public static final class Builder {

        private final OpenFilePickerOptions options;

        private Builder() {
            this.options = new OpenFilePickerOptions();
        }

        private Builder(OpenFilePickerOptions source) {
            this.options = new OpenFilePickerOptions();
            this.options.types = source.types;
            this.options.excludeAcceptAllOption = source.excludeAcceptAllOption;
            this.options.multiple = source.multiple;
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
         * Sets whether multiple files can be selected.
         *
         * @param multiple {@code true} to allow multi-selection
         * @return this builder
         */
        public Builder multiple(boolean multiple) {
            options.multiple = multiple;
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
        public OpenFilePickerOptions build() {
            if (Boolean.TRUE.equals(options.excludeAcceptAllOption)
                    && (options.types == null || options.types.isEmpty())) {
                throw new IllegalStateException("excludeAcceptAllOption requires at least one file type filter");
            }
            return options;
        }
    }
}
