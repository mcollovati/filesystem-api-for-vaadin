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

@Route("test/support")
public class FileSystemSupportTestView extends VerticalLayout {

    private final ClientFileSystem fs;
    private final Pre log;

    public FileSystemSupportTestView() {
        fs = new ClientFileSystem(this);
        log = new Pre();
        log.setId("log");
        log.setWidthFull();
        var btn = new NativeButton("Check Support", e -> onCheckSupport());
        btn.setId("check-support");
        add(btn, log);
    }

    private void onCheckSupport() {
        fs.isSupported()
                .thenAccept(supported -> appendLog("supported=" + supported))
                .exceptionally(error -> {
                    appendLog("Error: " + error.getMessage());
                    return null;
                });
    }

    private void appendLog(String message) {
        getUI().ifPresent(ui -> ui.access(() -> {
            String current = log.getText();
            log.setText(current + message + "\n");
        }));
    }
}
