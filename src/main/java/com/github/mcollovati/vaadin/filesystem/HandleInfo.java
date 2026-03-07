package com.github.mcollovati.vaadin.filesystem;

/**
 * Internal DTO for deserializing handle metadata returned from the
 * client-side registry.
 */
record HandleInfo(String id, String name, String kind) {}
