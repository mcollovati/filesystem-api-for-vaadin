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
package com.github.mcollovati.vaadin.filesystem.demo.client;

import com.github.mcollovati.vaadin.filesystem.demo.AbstractDemoView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

/**
 * Demo view showcasing pickers that run directly inside the browser's
 * click handler.
 *
 * <p>Unlike the future-based methods, the {@code *TriggeredBy} methods arm a
 * click trigger on a component so that the picker is opened synchronously
 * inside the click event. This preserves the transient user activation that
 * some browsers require for showing file pickers, and re-opens the picker on
 * every click without extra server round-trips.
 */
@Route("triggered")
public class TriggeredDemoView extends AbstractDemoView {

    private Registration disarmingRegistration;

    public TriggeredDemoView() {
        super(
                "Triggered Operations",
                "Open pickers directly from the click handler with openFileTriggeredBy() "
                        + "and friends. The picker is invoked synchronously inside the browser's "
                        + "click event, so the user gesture is preserved, and stays armed so that "
                        + "every click re-opens the picker without a server round-trip.");

        addContent(disarmStatus());
    }

    @Override
    protected String codeSnippet() {
        return """
                // Arm a button so each click opens the picker in the click handler
                fs.openFileTriggeredBy(button, fileData -> {
                    appendLog(fileData.getName());
                }, this::logError);

                // Triggers stay armed and can be disarmed
                Registration registration = fs.saveFileTriggeredBy(button,
                    "Hello", () -> appendLog("saved"), this::logError);
                registration.remove();

                // Other operations support the same pattern
                fs.openFilesTriggeredBy(button, files -> { ... }, this::logError);
                fs.openDirectoryTriggeredBy(button, dir -> { ... }, this::logError);
                fs.listDirectoryTriggeredBy(button, entries -> { ... }, this::logError);""";
    }

    @Override
    protected void addActions() {
        var openFile = new Button("Open File (each click)");
        disarmingRegistration = fs().openFileTriggeredBy(
                openFile,
                fileData -> appendLog("Opened: " + fileData.getName() + " (" + fileData.getSize() + " bytes)"),
                this::logError);

        var openFiles = new Button("Open Files (each click)");
        fs().openFilesTriggeredBy(
                openFiles,
                files -> {
                    appendLog("Opened " + files.size() + " file(s):");
                    files.forEach(f -> appendLog("  " + f.getName() + " (" + f.getSize() + " bytes)"));
                },
                this::logError);

        var saveText = new Button("Save Text (each click)");
        fs().saveFileTriggeredBy(
                saveText, "Hello from the trigger!", () -> appendLog("Text saved"), this::logError);

        var saveBytes = new Button("Save Bytes (each click)");
        fs().saveFileTriggeredBy(
                saveBytes, new byte[] {1, 2, 3, 4, 5}, () -> appendLog("Bytes saved"), this::logError);

        var openDirectory = new Button("Open Directory (each click)");
        fs().openDirectoryTriggeredBy(
                openDirectory, dir -> appendLog("Opened directory: " + dir.getName()), this::logError);

        var listDirectory = new Button("List Directory (each click)");
        fs().listDirectoryTriggeredBy(
                listDirectory,
                entries -> {
                    appendLog("Directory contains " + entries.size() + " entries:");
                    entries.forEach(handle ->
                            appendLog("  " + handle.getName() + " [" + handle.getKind().getJsValue() + "]"));
                },
                this::logError);

        var disarm = new Button("Disarm Open File Trigger", e -> disarm());
        disarm.getStyle().set("color", "var(--lumo-error-text-color)");

        add(new HorizontalLayout(openFile, openFiles, saveText, saveBytes));
        add(new HorizontalLayout(openDirectory, listDirectory, disarm));
    }

    private Span disarmStatus() {
        var status = new Span("The Open File trigger is armed. Click the button repeatedly to open the picker again.");
        status.setId("disarm-status");
        return status;
    }

    private void disarm() {
        if (disarmingRegistration != null) {
            disarmingRegistration.remove();
            disarmingRegistration = null;
            appendLog("Open File trigger disarmed");
        } else {
            appendLog("Open File trigger is not armed");
        }
    }
}