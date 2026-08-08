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
 * Action that requests permission for a registered handle synchronously
 * inside the user gesture for the trigger that fires it.
 *
 * <p>Resolves with the resulting permission state as a JavaScript string
 * ({@code "granted"}, {@code "denied"} or {@code "prompt"}).
 */
final class PermissionPromiseAction extends PromiseAction<String> {

    /**
     * JS template for the permission request. {@code $0} is the element
     * hosting the handle registry, {@code $1} the handle id and {@code $2}
     * the permission mode.
     */
    private static final String REQUEST_PERMISSION_JS =
            """
            const el = $0;
            const h = el.__fsApiHandles.get($1);
            if (!h) throw new DOMException('Handle not found (released or invalid)', 'NotFoundError');
            return h.requestPermission({mode: $2});""";

    private final Element registryElement;
    private final String handleId;
    private final String mode;

    PermissionPromiseAction(
            Element registryElement,
            String handleId,
            String mode,
            SerializableConsumer<String> onSuccess,
            SerializableConsumer<Error> onError) {
        super(String.class, onSuccess, onError);
        this.registryElement = registryElement;
        this.handleId = handleId;
        this.mode = mode;
    }

    @Override
    protected JsFunction toPromiseJs(Trigger trigger) {
        return JsFunction.of(REQUEST_PERMISSION_JS, registryElement, handleId, mode)
                .withArguments("event");
    }
}
