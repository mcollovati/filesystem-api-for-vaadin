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
package com.github.mcollovati.vaadin.filesystem.it;

import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.Download;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class OpfsSaveToDeviceIT extends AbstractIT {

    @Test
    void saveToDevice() throws IOException {
        navigateTo("test/opfs-save-to-device");
        Download download = page.waitForDownload(() -> clickButton("save-to-device"));
        waitForLog("save-complete", 15000);

        assertEquals("hello.txt", download.suggestedFilename());
        try (InputStream is = download.createReadStream()) {
            assertEquals("hello from opfs", new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void saveToDeviceWithCustomName() throws IOException {
        navigateTo("test/opfs-save-to-device");
        Download download = page.waitForDownload(() -> clickButton("save-to-device-custom-name"));
        waitForLog("save-custom-complete", 15000);

        assertEquals("renamed.txt", download.suggestedFilename());
        try (InputStream is = download.createReadStream()) {
            assertEquals("custom name content", new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void saveToDeviceViaHandle() throws IOException {
        navigateTo("test/opfs-save-to-device");
        Download download = page.waitForDownload(() -> clickButton("save-to-device-handle"));
        waitForLog("save-handle-complete", 15000);

        assertEquals("handle-test.txt", download.suggestedFilename());
        try (InputStream is = download.createReadStream()) {
            assertEquals("handle save content", new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
