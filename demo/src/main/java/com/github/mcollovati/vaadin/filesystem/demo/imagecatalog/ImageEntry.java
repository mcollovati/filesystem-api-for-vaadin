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
package com.github.mcollovati.vaadin.filesystem.demo.imagecatalog;

/**
 * An image entry with original metadata and generated thumbnail.
 */
record ImageEntry(
        String fileName,
        byte[] originalBytes,
        int originalWidth,
        int originalHeight,
        long originalSize,
        byte[] thumbnailBytes,
        String thumbnailMimeType) {

    String thumbnailFileName() {
        var dot = fileName.lastIndexOf('.');
        var base = dot > 0 ? fileName.substring(0, dot) : fileName;
        return base + "_thumb.png";
    }

    String dimensionsText() {
        return originalWidth + " x " + originalHeight;
    }
}
