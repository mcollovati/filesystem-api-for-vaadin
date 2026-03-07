package com.github.mcollovati.vaadin.filesystem;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the permission mode for a {@link FileSystemHandle}.
 *
 * <p>Used with {@link FileSystemHandle#queryPermission(PermissionMode)} and
 * {@link FileSystemHandle#requestPermission(PermissionMode)} to check or
 * request read or read-write access.
 */
public enum PermissionMode {
    /** Read-only access. */
    READ("read"),
    /** Read and write access. */
    READWRITE("readwrite");

    private final String jsValue;

    PermissionMode(String jsValue) {
        this.jsValue = jsValue;
    }

    /**
     * Returns the JavaScript string value for this mode.
     *
     * @return the JS value ({@code "read"} or {@code "readwrite"})
     */
    @JsonValue
    public String getJsValue() {
        return jsValue;
    }
}
