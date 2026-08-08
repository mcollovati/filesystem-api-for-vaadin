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

import com.github.mcollovati.vaadin.filesystem.ClientFileSystem;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import java.util.concurrent.CompletionException;

@Route("test/triggered-cancel")
public class TriggeredCancelTestView extends VerticalLayout {

    private final Pre log = new Pre();

    public TriggeredCancelTestView() {
        ClientFileSystem fs = new ClientFileSystem(this);

        log.setId("log");
        log.setWidthFull();

        NativeButton cancel = new NativeButton("Cancel picker (triggered)");
        cancel.setId("cancel");
        fs.openFileTriggeredBy(
                cancel,
                data -> appendLog("unexpected-success=" + data.getName() + "|"),
                error -> appendError("cancel", error));

        add(cancel, log);
    }

    private void appendLog(String message) {
        getUI().ifPresent(ui -> ui.access(() -> log.setText(log.getText() + message)));
    }

    private void appendError(String operation, Throwable error) {
        Throwable cause = error instanceof CompletionException ce && ce.getCause() != null ? ce.getCause() : error;
        appendLog("err=" + operation + ":" + cause.getClass().getSimpleName() + "|");
    }
}
