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

import static org.junit.jupiter.api.Assertions.*;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.Test;

@ViewPackages(classes = JsBridgeCleanupTest.CleanupTestView.class)
class JsBridgeCleanupTest extends BrowserlessTest {

    @Tag("div")
    @Route("cleanup-test")
    public static class CleanupTestView extends Component {}

    @Test
    void detach_resets_initialized_flag() {
        CleanupTestView view = navigate(CleanupTestView.class);

        ClientFileSystem api = new ClientFileSystem(view);
        JsBridge bridge = api.getBridge();
        // Trigger initialization
        bridge.executeJs("return true;");
        assertTrue(bridge.initialized, "bridge should be initialized after executeJs");

        // Simulate detach by removing from parent
        view.getElement().removeFromTree();

        assertFalse(bridge.initialized, "bridge should reset initialized flag on detach");
    }

    @Test
    void re_attach_re_initializes_bridge() {
        CleanupTestView view = navigate(CleanupTestView.class);

        ClientFileSystem api = new ClientFileSystem(view);
        JsBridge bridge = api.getBridge();
        bridge.executeJs("return true;");

        // Detach
        view.getElement().removeFromTree();
        assertFalse(bridge.initialized);

        // Re-attach and use again
        bridge.executeJs("return true;");
        assertTrue(bridge.initialized, "bridge should re-initialize after re-attach");
    }

    @Test
    void cleanup_clears_detach_registration() {
        CleanupTestView view = navigate(CleanupTestView.class);

        ClientFileSystem api = new ClientFileSystem(view);
        JsBridge bridge = api.getBridge();
        bridge.executeJs("return true;");

        // First detach triggers cleanup
        view.getElement().removeFromTree();
        assertFalse(bridge.initialized);

        // Re-attach, re-initialize and detach again — should not fail
        UI.getCurrent().getElement().appendChild(view.getElement());
        bridge.executeJs("return true;");
        assertTrue(bridge.initialized);

        view.getElement().removeFromTree();
        assertFalse(bridge.initialized);
    }
}
