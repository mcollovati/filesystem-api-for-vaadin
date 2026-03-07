package com.github.mcollovati.vaadin.filesystem;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * Options for {@link FileSystemAPI#showDirectoryPicker(DirectoryPickerOptions)}.
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

    /** Builder for {@link DirectoryPickerOptions}. */
    public static final class Builder {

        private final DirectoryPickerOptions options = new DirectoryPickerOptions();

        private Builder() {}

        /**
         * Sets the starting directory to a well-known directory name.
         *
         * <p>Valid values include {@code "desktop"}, {@code "documents"},
         * {@code "downloads"}, {@code "music"}, {@code "pictures"}, and
         * {@code "videos"}.
         *
         * @param wellKnownDirectory the directory name
         * @return this builder
         */
        public Builder startIn(String wellKnownDirectory) {
            options.startIn = wellKnownDirectory;
            return this;
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
