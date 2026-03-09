# Filesystem API for Vaadin — Documentation

Full API reference and usage guide. For a quick introduction, see [README.md](README.md).

---

## Table of Contents

- [Core Concepts](#core-concepts)
- [API Styles](#api-styles)
- [File Pickers](#file-pickers)
- [Reading Files](#reading-files)
- [Writing Files](#writing-files)
- [Directory Operations](#directory-operations)
- [Origin Private File System (OPFS)](#origin-private-file-system-opfs)
- [Streaming Transfers](#streaming-transfers)
- [Permissions](#permissions)
- [Error Handling](#error-handling)
- [API Reference](#api-reference)
- [Handle Lifecycle](#handle-lifecycle)
- [Browserless Testing](#browserless-testing)

---

## Core Concepts

The add-on mirrors the browser's
[File System API](https://developer.mozilla.org/en-US/docs/Web/API/File_System_API)
as a server-side Java API. Key concepts:

- **`FileSystemAPI`** — the entry point, bound to a Vaadin `Component`. All
  operations execute JavaScript in the browser and return results as
  `CompletableFuture`s.
- **Handles** — `FileSystemFileHandle` and `FileSystemDirectoryHandle` represent
  references to browser-side file system entries. They are kept in a client-side
  registry and referenced by opaque IDs on the server.
- **Options** — picker and operation options are modeled as immutable objects
  created via builders (e.g. `OpenFilePickerOptions.builder().multiple(true).build()`).

## API Styles

### CompletableFuture-based (`FileSystemAPI`)

Every operation returns a `CompletableFuture`. Chain operations naturally:

```java
var fs = new FileSystemAPI(this);

fs.openFile()
    .thenAccept(data -> log("Got " + data.getName()))
    .exceptionally(error -> { log("Failed: " + error); return null; });
```

### Callback-based (`FileSystemCallbackAPI`)

Wraps `FileSystemAPI` with simple `onSuccess` / `onError` callbacks. Errors are
logged at `FINE` level when no error callback is provided.

```java
var fs = new FileSystemCallbackAPI(this);

fs.openFile(
    data -> log("Got " + data.getName()),
    error -> log("Failed: " + error));
```

Access the underlying future-based API at any time via `fs.api()`.

---

## File Pickers

### Open file picker

```java
var fs = new FileSystemAPI(this);

// Single file — default options
fs.openFile().thenAccept(fileData -> { ... });

// Multiple files
fs.openFiles().thenAccept(fileDataList -> { ... });

// With options
var options = OpenFilePickerOptions.builder()
        .multiple(true)
        .types(FileTypeFilter.of("Images", "image/*", ".png", ".jpg", ".gif"))
        .excludeAcceptAllOption(true)
        .startIn(WellKnownDirectory.PICTURES)
        .build();
fs.openFiles(options).thenAccept(files -> { ... });
```

### Save file picker

```java
var fs = new FileSystemAPI(this);

// Simple text save
fs.saveFile("Hello, world!");

// With options
var options = SaveFilePickerOptions.builder()
        .suggestedName("report.csv")
        .types(FileTypeFilter.of("CSV", "text/csv", ".csv"))
        .startIn(WellKnownDirectory.DOCUMENTS)
        .build();
fs.saveFile(options, csvContent);

// Binary save
fs.saveFile(pngBytes);
```

### Directory picker

```java
var fs = new FileSystemAPI(this);

// Get a directory handle
fs.openDirectory().thenAccept(dir -> { ... });

// List entries directly
fs.listDirectory().thenAccept(entries -> { ... });

// With read-write access
var options = DirectoryPickerOptions.builder()
        .mode(PermissionMode.READWRITE)
        .build();
fs.openDirectory(options).thenAccept(dir -> { ... });
```

### File type filters

Use `FileTypeFilter` to restrict which files appear in picker dialogs:

```java
// Convenience factory
FileTypeFilter.of("Images", "image/*", ".png", ".jpg")

// Full constructor for multiple MIME types
new FileTypeFilter("Web images", Map.of(
    "image/png", List.of(".png"),
    "image/jpeg", List.of(".jpg", ".jpeg")))
```

### Start-in directory

All picker options support `startIn` to suggest an initial directory:

```java
// Well-known directory
.startIn(WellKnownDirectory.DOWNLOADS)

// From an existing handle
.startIn(existingDirectoryHandle)
```

---

## Reading Files

### In-memory read

`openFile()` reads the entire file into a `FileData` object:

```java
fs.openFile().thenAccept(fileData -> {
    String name = fileData.getName();         // "photo.jpg"
    long size = fileData.getSize();           // 1048576
    String type = fileData.getType();         // "image/jpeg"
    long modified = fileData.getLastModified(); // epoch millis
    byte[] bytes = fileData.getContent();
    InputStream stream = fileData.getContentAsInputStream();
});
```

This transfers the file content via base64 encoding. Suitable for small to
medium files. For large files, use [streaming transfers](#streaming-transfers).

### Low-level read via handle

For more control, work with handles directly:

```java
fs.openDirectory().thenCompose(dir ->
    dir.getFileHandle("config.json")
).thenCompose(FileSystemFileHandle::getFile)
 .thenAccept(data -> { ... });
```

---

## Writing Files

### Simple write

```java
// Text
fs.saveFile("Hello, world!");
fs.saveFile(options, "Hello, world!");

// Binary
fs.saveFile(imageBytes);
fs.saveFile(options, imageBytes);
```

### Low-level write via writable stream

For fine-grained control, create a `FileSystemWritableFileStream`:

```java
fileHandle.createWritable().thenCompose(writable ->
    writable.write("Line 1\n")
        .thenCompose(v -> writable.write("Line 2\n"))
        .thenCompose(v -> writable.seek(0))
        .thenCompose(v -> writable.write("REPLACED\n"))
        .thenCompose(v -> writable.close())  // must close to commit!
);
```

Writable stream operations:

| Method | Description |
|--------|-------------|
| `write(String text)` | Write text at the current position |
| `write(byte[] data)` | Write binary data at the current position |
| `seek(long position)` | Move the cursor to a byte offset |
| `truncate(long size)` | Resize the file |
| `close()` | **Commit changes** — must be called |

The `keepExistingData` option preserves current file content:

```java
var options = WritableOptions.builder().keepExistingData(true).build();
fileHandle.createWritable(options).thenCompose(writable -> { ... });
```

---

## Directory Operations

Given a `FileSystemDirectoryHandle`:

```java
// Get a file handle (optionally create if missing)
dir.getFileHandle("notes.txt");
dir.getFileHandle("notes.txt", GetHandleOptions.builder().create(true).build());

// Get a subdirectory handle
dir.getDirectoryHandle("subdir");
dir.getDirectoryHandle("subdir", GetHandleOptions.builder().create(true).build());

// List all entries
dir.entries().thenAccept(entries -> {
    for (FileSystemHandle entry : entries) {
        if (entry.getKind() == HandleKind.FILE) {
            FileSystemFileHandle file = (FileSystemFileHandle) entry;
            // ...
        }
    }
});

// Remove an entry
dir.removeEntry("old-file.txt");
dir.removeEntry("old-dir", RemoveEntryOptions.builder().recursive(true).build());

// Resolve path from directory to a descendant handle
dir.resolve(childHandle).thenAccept(path -> {
    // path is Optional<List<String>>, e.g. Optional.of(["subdir", "file.txt"])
    // empty if the handle is not a descendant
});
```

---

## Origin Private File System (OPFS)

OPFS is a sandboxed filesystem private to the page's origin. Unlike picker
methods, it does not show dialogs and does not require user gestures. It has
broader browser support than the picker APIs.

```java
var fs = new FileSystemAPI(this);

fs.getOriginPrivateDirectory().thenCompose(root -> {
    // Create a file in OPFS
    var create = GetHandleOptions.builder().create(true).build();
    return root.getFileHandle("app-data.json", create);
}).thenCompose(file ->
    file.writeString("{\"setting\": true}")
);
```

OPFS is useful for:
- Application storage without user prompts
- Caching downloaded content
- Integration tests (no native dialogs needed)

---

## Streaming Transfers

For large files, base64 encoding is inefficient (~33% size inflation, entire
content in memory). The streaming API uses Vaadin's `UploadHandler` and
`DownloadHandler` for HTTP-based transfer.

### Upload (browser to server)

```java
var fs = new FileSystemAPI(this);

// In-memory
fs.openFile(UploadHandler.inMemory((meta, data) -> {
    byte[] content = data.asBytes();
}));

// To temporary file
fs.openFile(UploadHandler.toTempFile((meta, path) -> {
    Files.copy(path, destination);
}));

// With picker options
var options = OpenFilePickerOptions.builder()
        .types(FileTypeFilter.of("CSV", "text/csv", ".csv"))
        .build();
fs.openFile(options, UploadHandler.inMemory((meta, data) -> { ... }));
```

### Download (server to browser)

```java
var fs = new FileSystemAPI(this);

// Fixed content
fs.saveFile(DownloadHandler.forFixedContent(
    "report.csv", csvBytes, "text/csv"));

// Dynamic content from InputStream
fs.saveFile(DownloadHandler.forInputStream(
    "data.bin", () -> new FileInputStream(largeFile), "application/octet-stream"));

// With picker options
var options = SaveFilePickerOptions.builder()
        .suggestedName("export.csv")
        .build();
fs.saveFile(options, DownloadHandler.forFixedContent(
    "export.csv", data, "text/csv"));
```

---

## Permissions

Handles obtained from pickers already have the appropriate permission. For
handles that persist across sessions or for explicit checks:

```java
handle.queryPermission(PermissionMode.READWRITE).thenAccept(state -> {
    switch (state) {
        case GRANTED -> log("Access granted");
        case DENIED -> log("Access denied");
        case PROMPT -> {
            // Request permission (shows browser prompt)
            handle.requestPermission(PermissionMode.READWRITE)
                .thenAccept(newState -> log("New state: " + newState));
        }
    }
});
```

For the common case of "query first, then request if needed", use
`ensurePermission()` — a convenience method that combines both steps:

```java
handle.ensurePermission(PermissionMode.READWRITE).thenAccept(state -> {
    if (state == PermissionState.GRANTED) {
        // proceed with file operations
    } else {
        log("Permission denied");
    }
});
```

---

## Error Handling

All File System API exceptions extend `FileSystemApiException` (a
`RuntimeException`). When a browser-side `DOMException` occurs, it is mapped to
the appropriate Java exception:

| Browser Error | Java Exception | Typical Cause |
|---------------|---------------|---------------|
| `NotFoundError` | `FileSystemNotFoundException` | File or directory does not exist |
| `NotAllowedError` | `FileSystemNotAllowedException` | Permission denied |
| `AbortError` | `FileSystemNotAllowedException` | User cancelled the picker dialog |
| `TypeMismatchError` | `FileSystemTypeMismatchException` | Expected file but got directory, or vice versa |
| *(API unavailable)* | `FileSystemApiNotSupportedException` | Browser does not support the File System API |

### Handling errors with futures

```java
fs.openFile()
    .thenAccept(data -> { ... })
    .exceptionally(error -> {
        Throwable cause = error.getCause(); // unwrap CompletionException
        if (cause instanceof FileSystemNotAllowedException) {
            Notification.show("Picker was cancelled");
        } else if (cause instanceof FileSystemNotFoundException) {
            Notification.show("File not found");
        } else {
            Notification.show("Error: " + cause.getMessage());
        }
        return null;
    });
```

### Handling errors with callbacks

```java
var fs = new FileSystemCallbackAPI(this);

fs.openFile(
    data -> { ... },
    error -> {
        if (error.getCause() instanceof FileSystemNotAllowedException) {
            Notification.show("Cancelled");
        }
    });
```

---

## API Reference

### Entry Points

| Class | Description |
|-------|-------------|
| `FileSystemAPI` | Future-based API, bound to a `Component` |
| `FileSystemCallbackAPI` | Callback-based wrapper around `FileSystemAPI` |

### Handle Types

| Class | Description |
|-------|-------------|
| `FileSystemHandle` | Sealed interface for file system entries |
| `FileSystemFileHandle` | Handle to a file — read, write, upload, download |
| `FileSystemDirectoryHandle` | Handle to a directory — traverse, create, remove entries |
| `FileSystemWritableFileStream` | Writable stream for incremental file writes |
| `FileData` | File content + metadata returned by `getFile()` |

### Options

| Class | Used By |
|-------|---------|
| `OpenFilePickerOptions` | `FileSystemAPI.openFile()`, `openFiles()` |
| `SaveFilePickerOptions` | `FileSystemAPI.saveFile()` |
| `DirectoryPickerOptions` | `FileSystemAPI.openDirectory()`, `listDirectory()` |
| `FileTypeFilter` | File type restrictions in picker options |
| `WellKnownDirectory` | `startIn` values for picker options |
| `GetHandleOptions` | `getFileHandle()`, `getDirectoryHandle()` |
| `RemoveEntryOptions` | `removeEntry()` |
| `WritableOptions` | `createWritable()` |

### Enums

| Enum | Values |
|------|--------|
| `HandleKind` | `FILE`, `DIRECTORY` |
| `PermissionMode` | `READ`, `READWRITE` |
| `PermissionState` | `GRANTED`, `DENIED`, `PROMPT` |

### Exceptions

| Exception | Cause |
|-----------|-------|
| `FileSystemApiException` | Base exception |
| `FileSystemApiNotSupportedException` | API not available in browser |
| `FileSystemNotFoundException` | Entry not found |
| `FileSystemNotAllowedException` | Permission denied or picker cancelled |
| `FileSystemTypeMismatchException` | Wrong handle kind (file vs directory) |

### Browserless Test Support (`filesystem-api-browserless`)

| Class | Description |
|-------|-------------|
| `FileSystemTester` | Entry point — builder installs a fake bridge on a component |
| `FileSystemTester.Builder` | Fluent builder for file system state and picker responses |
| `InMemoryFileSystem` | In-memory file tree for assertions |
| `PickerResponse` | Functional interface defining fake picker behavior |
| `FsNode` | Sealed interface for in-memory file system nodes |
| `FsFile` | Mutable in-memory file node |
| `FsDirectory` | Mutable in-memory directory node |

---

## Handle Lifecycle

- Handles are stored in a client-side `Map` on the host component's DOM element
- Each handle gets a unique ID; Java objects hold the ID + bridge reference
- **Explicit release**: call `handle.release()` to remove from the registry
- **Auto-cleanup**: all handles and open writable streams are released when the
  bound component is detached from the UI
- Handles are invalidated on page navigation or refresh

---

## Browserless Testing

The `filesystem-api-browserless` module lets you write fast, deterministic unit
tests for views that use `FileSystemAPI` — without launching a browser. It
replaces the real JavaScript bridge with an in-memory fake so that picker
dialogs, file reads, and writes all resolve instantly in the JVM.

### Setup

Add both the test-support module and the browserless test runner:

```xml
<dependency>
    <groupId>com.github.mcollovati</groupId>
    <artifactId>filesystem-api-browserless</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>browserless-test-junit6</artifactId>
    <scope>test</scope>
</dependency>
```

### Writing a test view

Views are regular Vaadin components that use `FileSystemAPI`. The view contains
UI controls wired to the API — tests interact with these controls, not the API
directly.

```java
@Route("editor")
public class EditorView extends Div {
    final FileSystemAPI fs = new FileSystemAPI(this);
    final Input editor = new Input();
    final Span status = new Span();
    final NativeButton openBtn = new NativeButton("Open", e ->
        fs.openFile().thenAccept(data -> {
            editor.setValue(new String(data.getContent()));
            status.setText(data.getName());
        }));
    final NativeButton saveBtn = new NativeButton("Save", e ->
        fs.saveFile(editor.getValue()));

    public EditorView() {
        add(editor, openBtn, saveBtn, status);
    }
}
```

### FileSystemTester

`FileSystemTester` is the entry point. Its builder lets you populate an
in-memory file system and configure how pickers respond. Call `install()` to
wire the fake bridge into the component.

### Test pattern

The typical pattern is: navigate to the view, install the tester, interact with
UI components, and assert the resulting state.

**Open a file and assert UI state:**

```java
@ViewPackages(classes = EditorView.class)
class EditorTests extends BrowserlessTest {
    @Test
    void click_open_loads_file_content_into_editor() {
        EditorView view = navigate(EditorView.class);
        FileSystemTester.forComponent(view)
                .withFile("notes.txt", "Hello!")
                .onOpenFilePicker(PickerResponse.returning("notes.txt"))
                .install();

        test(view.openBtn).click();

        assertEquals("Hello!", view.editor.getValue());
        assertEquals("notes.txt", view.status.getText());
    }
}
```

**Save a file and assert file system state:**

```java
@Test
void click_save_writes_editor_content_to_file() {
    EditorView view = navigate(EditorView.class);
    FileSystemTester tester = FileSystemTester.forComponent(view)
            .onSaveFilePicker(PickerResponse.returningSingle("output.txt"))
            .install();

    view.editor.setValue("Saved!");
    test(view.saveBtn).click();

    assertEquals("Saved!", tester.fileSystem().file("output.txt").contentAsString());
}
```

### Configuring picker responses

Use `PickerResponse` factory methods to control what each picker returns:

| Method | Returns | Description |
|--------|---------|-------------|
| `returning(String...)` | `List<FileSystemFileHandle>` | Open picker returns named files from root |
| `returningSingle(String)` | `FileSystemFileHandle` | Save picker returns/creates a single file |
| `returningRoot()` | `FileSystemDirectoryHandle` | Directory picker returns root |
| `returningDirectory(String)` | `FileSystemDirectoryHandle` | Directory picker returns named subdirectory |
| `cancelling()` | *(fails)* | Simulates user cancelling the picker |

**Picker cancellation:**

```java
FileSystemTester.forComponent(view)
        .onOpenFilePicker(PickerResponse.cancelling())
        .install();

test(view.openBtn).click();
assertEquals("cancelled", view.status.getText());
```

### Populating the file system

The builder supports nested directories:

```java
FileSystemTester.forComponent(view)
        .withDirectory("docs", docs -> docs
            .withFile("readme.txt", "Read me")
            .withDirectory("images"))
        .install();
```

### Asserting file system state

After tests run, use `InMemoryFileSystem` to inspect what the view wrote:

- `tester.fileSystem().exists("path/to/file")` — check existence
- `tester.fileSystem().file("path")` — get an `FsFile` for content assertions
- `tester.fileSystem().directory("path")` — get an `FsDirectory` for children
- `fsFile.contentAsString()` — read content as UTF-8 string
- `fsFile.content()` — read raw bytes
- `fsDirectory.children()` — list child nodes

### Permission testing

Configure the permission state returned by `queryPermission()` and
`requestPermission()`:

```java
FileSystemTester.forComponent(view)
        .withFile("a.txt", "data")
        .withPermissionState(PermissionState.DENIED)
        .install();

test(view.checkBtn).click();
assertEquals("denied", view.permStatus.getText());
```
