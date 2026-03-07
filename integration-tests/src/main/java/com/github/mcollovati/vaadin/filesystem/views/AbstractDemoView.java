package com.github.mcollovati.vaadin.filesystem.views;

import com.github.mcollovati.vaadin.filesystem.FileSystemAPI;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Base class for demo views. Provides a description area, a code
 * snippet section, action components, and a log panel.
 */
abstract class AbstractDemoView extends VerticalLayout {

    private final FileSystemAPI fs;
    private final Pre log;

    /**
     * Creates the view with the given title and description.
     *
     * @param title       the view title
     * @param description a short description of the showcased API
     */
    AbstractDemoView(String title, String description) {
        fs = new FileSystemAPI(this);
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

    /**
     * Returns the {@link FileSystemAPI} instance bound to this view.
     *
     * @return the file system API
     */
    FileSystemAPI fs() {
        return fs;
    }

    /**
     * Returns the source code snippet to display. Override in
     * subclasses to provide the relevant code example.
     *
     * @return the code snippet text
     */
    abstract String codeSnippet();

    /**
     * Adds the action buttons or interactive components to the view.
     * Called during construction, after the description and code snippet.
     */
    abstract void addActions();

    /**
     * Appends a message to the log panel.
     *
     * @param message the message to append
     */
    void appendLog(String message) {
        getUI().ifPresent(ui -> ui.access(() -> {
            String current = log.getText();
            log.setText(current + message + "\n");
        }));
    }

    /**
     * Error handler that logs the error message and returns {@code null}.
     * Suitable for use with {@link java.util.concurrent.CompletableFuture#exceptionally}.
     *
     * @param error the throwable
     * @return {@code null}
     */
    Void logError(Throwable error) {
        appendLog("Error: " + error.getMessage());
        return null;
    }

    /**
     * Adds components to this view. Convenience pass-through to make
     * subclass code cleaner.
     *
     * @param components the components to add
     */
    void addContent(Component... components) {
        // Insert before the "Log" heading (last 2 elements: H2 + Pre)
        for (var component : components) {
            addComponentAtIndex(getComponentCount() - 2, component);
        }
    }
}
