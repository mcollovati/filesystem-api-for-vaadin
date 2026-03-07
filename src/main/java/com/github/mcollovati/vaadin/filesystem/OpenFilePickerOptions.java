package com.github.mcollovati.vaadin.filesystem;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;

/**
 * Options for {@link FileSystemAPI#showOpenFilePicker(OpenFilePickerOptions)}.
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

    /** Builder for {@link OpenFilePickerOptions}. */
    public static final class Builder {

        private final OpenFilePickerOptions options = new OpenFilePickerOptions();

        private Builder() {}

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
         * Builds the options.
         *
         * @return the options instance
         */
        public OpenFilePickerOptions build() {
            return options;
        }
    }
}
