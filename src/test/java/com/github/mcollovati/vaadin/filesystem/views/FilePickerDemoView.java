package com.github.mcollovati.vaadin.filesystem.views;

import com.github.mcollovati.vaadin.filesystem.DirectoryPickerOptions;
import com.github.mcollovati.vaadin.filesystem.FileSystemAPI;
import com.github.mcollovati.vaadin.filesystem.FileSystemFileHandle;
import com.github.mcollovati.vaadin.filesystem.FileTypeFilter;
import com.github.mcollovati.vaadin.filesystem.OpenFilePickerOptions;
import com.github.mcollovati.vaadin.filesystem.PermissionMode;
import com.github.mcollovati.vaadin.filesystem.SaveFilePickerOptions;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.Map;

@Route("")
public class FilePickerDemoView extends VerticalLayout {

    private final FileSystemAPI fs;
    private final Pre log;

    public FilePickerDemoView() {
        fs = new FileSystemAPI(this);
        log = new Pre();
        log.getStyle().set("background", "#f5f5f5").set("padding", "1em").set("min-height", "200px");

        add(new H2("File System API Demo"));
        add(createSupportCheck());
        add(createPickerButtons());
        add(new H2("Log"));
        add(log);
    }

    private Div createSupportCheck() {
        var status = new Span("Checking...");
        fs.isSupported().thenAccept(supported -> getUI().ifPresent(ui -> ui.access(() -> {
            if (supported) {
                status.setText("File System API is supported");
                status.getStyle().set("color", "green");
            } else {
                status.setText("File System API is NOT supported in this browser");
                status.getStyle().set("color", "red");
            }
        })));
        return new Div(new Span("Browser support: "), status);
    }

    private HorizontalLayout createPickerButtons() {
        var openFile = new Button("Open File", e -> onOpenFile());
        var openMultiple = new Button("Open Multiple", e -> onOpenMultiple());
        var openImages = new Button("Open Images", e -> onOpenImages());
        var saveFile = new Button("Save File", e -> onSaveFile());
        var openDir = new Button("Open Directory", e -> onOpenDirectory());
        return new HorizontalLayout(openFile, openMultiple, openImages, saveFile, openDir);
    }

    private void onOpenFile() {
        fs.showOpenFilePicker()
                .thenAccept(handles -> logHandles("Open File", handles))
                .exceptionally(this::logError);
    }

    private void onOpenMultiple() {
        var options = OpenFilePickerOptions.builder().multiple(true).build();
        fs.showOpenFilePicker(options)
                .thenAccept(handles -> logHandles("Open Multiple", handles))
                .exceptionally(this::logError);
    }

    private void onOpenImages() {
        var options = OpenFilePickerOptions.builder()
                .types(List.of(new FileTypeFilter("Images", Map.of("image/*", List.of(".png", ".jpg", ".gif")))))
                .build();
        fs.showOpenFilePicker(options)
                .thenAccept(handles -> logHandles("Open Images", handles))
                .exceptionally(this::logError);
    }

    private void onSaveFile() {
        var options = SaveFilePickerOptions.builder()
                .suggestedName("example.txt")
                .types(List.of(new FileTypeFilter("Text files", Map.of("text/plain", List.of(".txt")))))
                .build();
        fs.showSaveFilePicker(options)
                .thenAccept(handle -> logHandles("Save File", List.of(handle)))
                .exceptionally(this::logError);
    }

    private void onOpenDirectory() {
        var options =
                DirectoryPickerOptions.builder().mode(PermissionMode.READWRITE).build();
        fs.showDirectoryPicker(options)
                .thenAccept(handle -> appendLog("Open Directory: " + handle.getName() + " (" + handle.getKind() + ")"))
                .exceptionally(this::logError);
    }

    private void logHandles(String action, List<? extends FileSystemFileHandle> handles) {
        var sb = new StringBuilder(action + ": ");
        for (var handle : handles) {
            sb.append(handle.getName()).append(" (").append(handle.getKind()).append("), ");
        }
        appendLog(sb.toString());
    }

    private Void logError(Throwable error) {
        appendLog("Error: " + error.getMessage());
        return null;
    }

    private void appendLog(String message) {
        getUI().ifPresent(ui -> ui.access(() -> {
            String current = log.getText();
            log.setText(current + message + "\n");
        }));
    }
}
