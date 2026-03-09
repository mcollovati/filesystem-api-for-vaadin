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

/**
 * Options for {@link ClientFileSystem#openDirectory(DirectoryPickerOptions)}.
 *
 * <p>Use the {@link #builder()} to create an instance.
 *
 * <pre>{@code
 * var options = DirectoryPickerOptions.builder()
 *         .mode(PermissionMode.READWRITE)
 *         .startIn("documents")
 *         .build();
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DirectoryPickerOptions implements Serializable {

    private String startIn;
    private PermissionMode mode;

    private DirectoryPickerOptions() {}

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
     * Returns the permission mode.
     *
     * @return the mode, or {@code null} if not set (defaults to
     *         {@code "read"} in the browser)
     */
    public PermissionMode getMode() {
        return mode;
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

    /** Builder for {@link DirectoryPickerOptions}. */
    public static final class Builder {

        private final DirectoryPickerOptions options;

        private Builder() {
            this.options = new DirectoryPickerOptions();
        }

        private Builder(DirectoryPickerOptions source) {
            this.options = new DirectoryPickerOptions();
            this.options.startIn = source.startIn;
            this.options.mode = source.mode;
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
         * Sets the permission mode for the directory.
         *
         * @param mode the permission mode
         * @return this builder
         */
        public Builder mode(PermissionMode mode) {
            options.mode = mode;
            return this;
        }

        /**
         * Builds the options.
         *
         * @return the options instance
         */
        public DirectoryPickerOptions build() {
            return options;
        }
    }
}
