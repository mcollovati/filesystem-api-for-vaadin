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
package com.github.mcollovati.vaadin.filesystem;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import org.junit.jupiter.api.Test;

class PickerPromiseActionJsTest {

    private final ClickTrigger trigger = new ClickTrigger(new NativeButton());

    @Test
    void openFileGeneratesPickerJsAndRegistersHandles() {
        PickerPromiseAction action = new PickerPromiseAction(
                PickerPromiseAction.OPEN_FILE,
                OpenFilePickerOptions.builder().build(),
                new Element("div"),
                infos -> {},
                error -> {});
        JsFunction js = action.toPromiseJs(trigger);
        String body = js.getBody();
        assertTrue(body.contains("await window.showOpenFilePicker(opts)"), body);
        assertTrue(body.contains("el.__fsApiHandles.set(id, h)"), body);
        assertTrue(body.contains("return handles.map(h =>"), body);
    }

    @Test
    void saveFileWrapsPickerResultInSingleElementArray() {
        PickerPromiseAction action = new PickerPromiseAction(
                PickerPromiseAction.SAVE_FILE, null, new Element("div"), infos -> {}, error -> {});
        String body = action.toPromiseJs(trigger).getBody();
        assertTrue(body.contains("await window.showSaveFilePicker(opts)"), body);
        assertTrue(body.contains("handles = [await window.showSaveFilePicker(opts)]"), body);
    }

    @Test
    void openDirectoryInvokesDirectoryPicker() {
        PickerPromiseAction action = new PickerPromiseAction(
                PickerPromiseAction.OPEN_DIRECTORY, null, new Element("div"), infos -> {}, error -> {});
        String body = action.toPromiseJs(trigger).getBody();
        assertTrue(body.contains("await window.showDirectoryPicker(opts)"), body);
    }
}
