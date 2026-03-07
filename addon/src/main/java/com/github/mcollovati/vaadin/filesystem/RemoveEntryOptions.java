package com.github.mcollovati.vaadin.filesystem;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * Options for {@link FileSystemDirectoryHandle#removeEntry(String, RemoveEntryOptions)}.
 *
 * <p>This is the Java counterpart of the browser's
 * {@code FileSystemRemoveOptions} dictionary.
 *
 * @see FileSystemDirectoryHandle#removeEntry(String, RemoveEntryOptions)
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record RemoveEntryOptions(boolean recursive) implements Serializable {

    /**
     * Creates options with default values ({@code recursive = false}).
     */
    public RemoveEntryOptions() {
        this(false);
    }

    /**
     * Returns a new builder for {@link RemoveEntryOptions}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RemoveEntryOptions}.
     */
    public static final class Builder {

        private boolean recursive;

        private Builder() {}

        /**
         * Sets whether to remove the entry recursively. When {@code true}
         * and the entry is a directory, its contents are removed as well.
         *
         * @param recursive {@code true} for recursive removal
         * @return this builder
         */
        public Builder recursive(boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        /**
         * Builds the options.
         *
         * @return a new {@link RemoveEntryOptions} instance
         */
        public RemoveEntryOptions build() {
            return new RemoveEntryOptions(recursive);
        }
    }
}
