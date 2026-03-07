package com.github.mcollovati.vaadin.filesystem;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * Options for {@link FileSystemDirectoryHandle#getFileHandle(String, GetHandleOptions)}
 * and {@link FileSystemDirectoryHandle#getDirectoryHandle(String, GetHandleOptions)}.
 *
 * <p>This is the Java counterpart of the browser's
 * {@code FileSystemGetFileOptions} / {@code FileSystemGetDirectoryOptions}
 * dictionaries.
 *
 * @see FileSystemDirectoryHandle#getFileHandle(String, GetHandleOptions)
 * @see FileSystemDirectoryHandle#getDirectoryHandle(String, GetHandleOptions)
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record GetHandleOptions(boolean create) implements Serializable {

    /**
     * Creates options with default values ({@code create = false}).
     */
    public GetHandleOptions() {
        this(false);
    }

    /**
     * Returns a new builder for {@link GetHandleOptions}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GetHandleOptions}.
     */
    public static final class Builder {

        private boolean create;

        private Builder() {}

        /**
         * Sets whether to create the entry if it does not exist.
         *
         * @param create {@code true} to create the entry
         * @return this builder
         */
        public Builder create(boolean create) {
            this.create = create;
            return this;
        }

        /**
         * Builds the options.
         *
         * @return a new {@link GetHandleOptions} instance
         */
        public GetHandleOptions build() {
            return new GetHandleOptions(create);
        }
    }
}
