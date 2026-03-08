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
package com.github.mcollovati.vaadin.filesystem.demo;

import com.github.mcollovati.vaadin.filesystem.demo.contactmanager.ContactManagerDemoView;
import com.github.mcollovati.vaadin.filesystem.demo.imagecatalog.ImageCatalogDemoView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;

/**
 * Main application layout with a side navigation menu for the
 * File System API demo views.
 */
@Layout
public class MainLayout extends AppLayout {

    public MainLayout() {
        var toggle = new DrawerToggle();
        var title = new H1("File System API");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");

        addToNavbar(toggle, title);
        addToDrawer(new Scroller(createSideNav()));
    }

    private SideNav createSideNav() {
        var nav = new SideNav();

        var highLevel = new SideNavItem("High-level API");
        highLevel.addItem(new SideNavItem("File Pickers", FilePickerDemoView.class));
        highLevel.addItem(new SideNavItem("Read File", ReadFileDemoView.class));
        highLevel.addItem(new SideNavItem("Write File", WriteFileDemoView.class));
        highLevel.addItem(new SideNavItem("Directory", DirectoryDemoView.class));
        highLevel.addItem(new SideNavItem("Streaming", StreamingDemoView.class));
        nav.addItem(highLevel);

        var callback = new SideNavItem("Callback API");
        callback.addItem(new SideNavItem(
                "File Pickers", com.github.mcollovati.vaadin.filesystem.demo.callback.FilePickerDemoView.class));
        callback.addItem(new SideNavItem(
                "Read File", com.github.mcollovati.vaadin.filesystem.demo.callback.ReadFileDemoView.class));
        callback.addItem(new SideNavItem(
                "Write File", com.github.mcollovati.vaadin.filesystem.demo.callback.WriteFileDemoView.class));
        callback.addItem(new SideNavItem(
                "Directory", com.github.mcollovati.vaadin.filesystem.demo.callback.DirectoryDemoView.class));
        callback.addItem(new SideNavItem(
                "Streaming", com.github.mcollovati.vaadin.filesystem.demo.callback.StreamingDemoView.class));
        nav.addItem(callback);

        var examples = new SideNavItem("Examples");
        examples.addItem(new SideNavItem("Contact Manager", ContactManagerDemoView.class));
        examples.addItem(new SideNavItem("Image Catalog", ImageCatalogDemoView.class));
        nav.addItem(examples);

        return nav;
    }

    /**
     * Helper to create a styled code block for displaying source snippets.
     *
     * @param code the source code text
     * @return a styled {@link Span} wrapping the code
     */
    public static Span codeBlock(String code) {
        var span = new Span(code);
        span.getElement()
                .setAttribute(
                        "style",
                        "display:block; white-space:pre; font-family:monospace;"
                                + " font-size:var(--lumo-font-size-s); background:var(--lumo-contrast-5pct);"
                                + " padding:var(--lumo-space-m); border-radius:var(--lumo-border-radius-m);"
                                + " overflow-x:auto;");
        return span;
    }
}
