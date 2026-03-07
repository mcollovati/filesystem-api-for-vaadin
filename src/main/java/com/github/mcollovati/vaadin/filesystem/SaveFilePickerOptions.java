package com.github.mcollovati.vaadin.filesystem;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;

/**
 * Options for {@link FileSystemAPI#showSaveFilePicker(SaveFilePickerOptions)}.
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

    /** Builder for {@link SaveFilePickerOptions}. */
    public static final class Builder {

        private final SaveFilePickerOptions options = new SaveFilePickerOptions();

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
        public SaveFilePickerOptions build() {
            return options;
        }
    }
}
