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

import com.vaadin.flow.component.trigger.internal.PromiseAction;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;

/**
 * Action that opens a file system picker synchronously inside the
 * user gesture for the trigger that fires it.
 *
 * <p>Runs {@code window.showOpenFilePicker}, {@code showSaveFilePicker} or
 * {@code showDirectoryPicker} (depending on the picker expression), registers
 * the returned handles in the client-side registry and resolves with their
 * metadata. The picker invocation happens within the browser's event handler,
 * so the transient user activation is guaranteed.
 */
final class PickerPromiseAction extends PromiseAction<HandleInfo[]> {

    /**
     * JS template for the picker action. {@code $0} is the picker options
     * object, {@code $1} the element that hosts the handle registry. The
     * {@code %s} placeholder receives the picker invocation expression, which
     * must evaluate to a Promise resolving to an array of picked handles.
     *
     * <p>Flow materialises the function with {@code new Function(...)}, so the
     * body must not use {@code await}; the promise is chained with
     * {@code .then()} instead.
     */
    private static final String PICKER_JS_TEMPLATE =
            """
            const el = $1;
            const opts = $0;
            if (opts.startIn && el.__fsApiHandles.has(opts.startIn)) {
                opts.startIn = el.__fsApiHandles.get(opts.startIn);
            }
            return %s.then(handles => handles.map(h => {
                const id = String(el.__fsApiNextId++);
                el.__fsApiHandles.set(id, h);
                return {id: id, name: h.name, kind: h.kind};
            }));""";

    /** Picker expression for {@code window.showOpenFilePicker}. */
    static final String OPEN_FILE = "window.showOpenFilePicker(opts)";

    /** Picker expression for {@code window.showSaveFilePicker}. */
    static final String SAVE_FILE = "window.showSaveFilePicker(opts).then(handle => [handle])";

    /** Picker expression for {@code window.showDirectoryPicker}. */
    static final String OPEN_DIRECTORY = "window.showDirectoryPicker(opts).then(handle => [handle])";

    private final String js;
    private final Object options;
    private final Element registryElement;

    PickerPromiseAction(
            String pickerExpression,
            Object options,
            Element registryElement,
            SerializableConsumer<HandleInfo[]> onSuccess,
            SerializableConsumer<Error> onError) {
        super(HandleInfo[].class, onSuccess, onError);
        this.options = options;
        this.registryElement = registryElement;
        this.js = PICKER_JS_TEMPLATE.formatted(pickerExpression);
    }

    @Override
    protected JsFunction toPromiseJs(Trigger trigger) {
        return JsFunction.of(js, options, registryElement).withArguments("event");
    }
}
