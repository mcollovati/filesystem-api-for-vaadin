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

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.assertions.LocatorAssertions.ContainsTextOptions;
import org.junit.jupiter.api.Test;

class TriggeredPickersIT extends AbstractIT {

    @Test
    void openFileTriggeredByReadsFileOnClick() {
        stubPickers();
        navigateTo("test/triggered-pickers");
        clickButton("open-file");
        waitForLog("open=Alpha.txt");
    }

    @Test
    void openFileTriggeredByStaysArmedForRepeatedClicks() {
        stubPickers();
        navigateTo("test/triggered-pickers");
        clickButton("open-file");
        waitForLog("open=Alpha.txt");
        clickButton("open-file");
        waitForLogCount("open=Alpha.txt", 2);
    }

    @Test
    void openFilesTriggeredByReturnsAllFiles() {
        stubPickers();
        navigateTo("test/triggered-pickers");
        clickButton("open-files");
        waitForLog("open-files-count=2");
    }

    @Test
    void saveTextTriggeredByWritesAndCompletes() {
        stubPickers();
        navigateTo("test/triggered-pickers");
        clickButton("save-text");
        waitForLog("save-text=ok");
    }

    @Test
    void saveBytesTriggeredByWritesAndCompletes() {
        stubPickers();
        navigateTo("test/triggered-pickers");
        clickButton("save-bytes");
        waitForLog("save-bytes=ok");
    }

    @Test
    void openDirectoryTriggeredByReportsDirectory() {
        stubPickers();
        navigateTo("test/triggered-pickers");
        clickButton("open-directory");
        waitForLog("open-directory=docs");
    }

    @Test
    void listDirectoryTriggeredByReportsEntries() {
        stubPickers();
        navigateTo("test/triggered-pickers");
        clickButton("list-directory");
        waitForLog("list-directory-count=2");
    }

    @Test
    void readContentTriggeredByReadsFileData() {
        stubPickers();
        navigateTo("test/triggered-pickers");
        clickButton("read-content");
        waitForLog("read-content=HELLO");
    }

    @Test
    void requestPermissionTriggeredBy() {
        stubPickers();
        navigateTo("test/triggered-pickers");
        clickButton("pick-directory");
        waitForLog("picked-directory=docs");
        clickButton("request-permission");
        waitForLog("permission=granted");
    }

    private void stubPickers() {
        page.addInitScript(
                """
                window.showOpenFilePicker = async () => [
                    { name: 'Alpha.txt', kind: 'file',
                      getFile: async () => new File(['HELLO'], 'Alpha.txt', { type: 'text/plain' }) },
                    { name: 'Beta.txt', kind: 'file',
                      getFile: async () => new File(['WORLD'], 'Beta.txt', { type: 'text/plain' }) }
                ];
                window.showSaveFilePicker = async () => ({
                    name: 'Saved.txt', kind: 'file',
                    createWritable: async () => ({ write: async () => {}, close: async () => {} })
                });
                window.showDirectoryPicker = async () => ({
                    name: 'docs', kind: 'directory',
                    requestPermission: async (options) => ({ state: options.mode === 'readwrite' ? 'granted' : 'prompt' }),
                    entries: async function* () {
                        yield ['b.txt', { name: 'b.txt', kind: 'file',
                                         getFile: async () => new File(['BEE'], 'b.txt') }];
                        yield ['sub', { name: 'sub', kind: 'directory' }];
                    }
                });
                """);
    }

    private void waitForLogCount(String text, int count) {
        page.waitForFunction(
                "([t, c]) => (document.querySelector('#log').textContent.split(t).length - 1) >= c",
                new Object[] {text, count});
        assertThat(page.locator("#log")).containsText(text, new ContainsTextOptions().setTimeout(1000));
    }
}
