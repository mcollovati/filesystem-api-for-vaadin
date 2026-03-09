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
package com.github.mcollovati.vaadin.filesystem.demo.callback;

import com.github.mcollovati.vaadin.filesystem.CallbackClientFileSystem;
import com.github.mcollovati.vaadin.filesystem.demo.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.concurrent.CompletionException;

/**
 * Base class for callback API demo views.
 */
abstract class AbstractCallbackDemoView extends VerticalLayout {

    private final CallbackClientFileSystem fs;
    private final Pre log;

    AbstractCallbackDemoView(String title, String description) {
        fs = new CallbackClientFileSystem(this);
        log = new Pre();
        log.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("min-height", "150px")
                .set("margin", "0");
        log.setWidthFull();

        setPadding(true);
        setSpacing(true);

        add(new H2(title));
        add(new Paragraph(description));
        add(MainLayout.codeBlock(codeSnippet()));
        addActions();
        add(new H2("Log"));
        add(log);
    }

    CallbackClientFileSystem fs() {
        return fs;
    }

    abstract String codeSnippet();

    abstract void addActions();

    void appendLog(String message) {
        getUI().ifPresent(ui -> ui.access(() -> {
            String current = log.getText();
            log.setText(current + message + "\n");
        }));
    }

    void logError(Throwable error) {
        Throwable cause = error instanceof CompletionException ? error.getCause() : error;
        String type = cause.getClass().getSimpleName();
        appendLog(type + ": " + cause.getMessage());
    }

    void addContent(Component... components) {
        for (var component : components) {
            addComponentAtIndex(getComponentCount() - 2, component);
        }
    }
}
