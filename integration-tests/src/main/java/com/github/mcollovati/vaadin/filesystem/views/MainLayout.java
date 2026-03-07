package com.github.mcollovati.vaadin.filesystem.views;

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

    /**
     * Creates the main layout with navigation sidebar.
     */
    public MainLayout() {
        var toggle = new DrawerToggle();
        var title = new H1("File System API");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");

        addToNavbar(toggle, title);
        addToDrawer(new Scroller(createSideNav()));
    }

    private SideNav createSideNav() {
        var nav = new SideNav();
        nav.addItem(new SideNavItem("File Pickers", FilePickerDemoView.class));
        nav.addItem(new SideNavItem("Read File", ReadFileDemoView.class));
        nav.addItem(new SideNavItem("Write File", WriteFileDemoView.class));
        nav.addItem(new SideNavItem("Directory", DirectoryDemoView.class));
        nav.addItem(new SideNavItem("Streaming", StreamingDemoView.class));
        return nav;
    }

    /**
     * Helper to create a styled code block for displaying source snippets.
     *
     * @param code the source code text
     * @return a styled {@link Span} wrapping the code
     */
    static Span codeBlock(String code) {
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
