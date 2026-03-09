# Filesystem API for Vaadin

A Vaadin add-on that provides a Java API for the browser's
[File System API](https://developer.mozilla.org/en-US/docs/Web/API/File_System_API),
enabling server-side Java code to trigger file/directory pickers, read and write
files, and traverse directory structures through native browser filesystem access.

A public demo is available at https://filesystem-api-for-vaadin.up.railway.app/.

## Features

- **File pickers** — open, save, and directory picker dialogs from server-side Java
- **Read and write files** — transfer file content between browser and server
- **Directory traversal** — list entries, create/remove files and subdirectories
- **Origin Private File System (OPFS)** — sandboxed storage without user prompts via `OriginPrivateFileSystem`
- **Streaming transfers** — efficient large-file upload/download via Vaadin's
  `UploadHandler` and `DownloadHandler`
- **Two API styles** — `CompletableFuture`-based and callback-based
- **Handle lifecycle management** — automatic cleanup on component detach

## Browser Compatibility

The File System API (file pickers) is supported in **Chromium-based browsers**
(Chrome, Edge, Opera). Firefox and Safari have partial support limited to OPFS.

See [MDN browser compatibility](https://developer.mozilla.org/en-US/docs/Web/API/File_System_API#browser_compatibility)
for details.

## Requirements

- Java 21+
- Vaadin 25+

## Modules

| Module | Description |
|--------|-------------|
| `filesystem-api` | The add-on library — the only runtime dependency you need |
| `filesystem-api-browserless` | Test-time companion for unit tests without a browser (see [Browserless Testing](DOCUMENTATION.md#browserless-testing)) |

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.github.mcollovati</groupId>
    <artifactId>filesystem-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Getting Started

Create a `ClientFileSystem` instance bound to any Vaadin component (typically your
view) and use it to interact with the browser's file system.

### Read a file

```java
var fs = new ClientFileSystem(this);

fs.openFile().thenAccept(fileData -> {
    String name = fileData.getName();
    long size = fileData.getSize();
    byte[] content = fileData.getContent();
    Notification.show("Read " + name + " (" + size + " bytes)");
});
```

### Write a file

```java
var fs = new ClientFileSystem(this);

var options = SaveFilePickerOptions.builder()
        .suggestedName("hello.txt")
        .build();
fs.saveFile(options, "Hello, world!");
```

### Browse a directory

```java
var fs = new ClientFileSystem(this);

fs.listDirectory().thenAccept(entries -> {
    for (FileSystemHandle entry : entries) {
        System.out.println(entry.getName() + " (" + entry.getKind() + ")");
    }
});
```

### Callback-based API

If you prefer callbacks over futures:

```java
var fs = new CallbackClientFileSystem(this);

fs.openFile(
    fileData -> Notification.show("Read: " + fileData.getName()),
    error -> Notification.show("Error: " + error.getMessage()));
```

### Origin Private File System (OPFS)

For programmatic storage without user prompts, use `OriginPrivateFileSystem`.
It provides path-based convenience methods and creates intermediate directories
automatically:

```java
var opfs = new OriginPrivateFileSystem(this);

// Write a file (creates intermediate dirs)
opfs.writeFile("data/config.json", "{\"key\": true}");

// Read it back
opfs.readFile("data/config.json").thenAccept(data ->
    Notification.show(new String(data.getContent())));

// List root entries
opfs.list().thenAccept(entries ->
    entries.forEach(e -> System.out.println(e.getName())));

// Clean up
opfs.clear();
```

### Streaming large files

For large files, use Vaadin's `UploadHandler` / `DownloadHandler` to avoid
base64 overhead:

```java
var fs = new ClientFileSystem(this);

// Upload: browser file -> server
fs.openFile(UploadHandler.inMemory((meta, data) -> {
    byte[] content = data.asBytes();
    // process content...
}));

// Download: server -> browser file
fs.saveFile(DownloadHandler.forFixedContent("report.csv",
        csvContent.getBytes(), "text/csv"));
```

For the full API reference, detailed examples, error handling guide, and
browserless testing setup, see [DOCUMENTATION.md](DOCUMENTATION.md).

## Building from Source

```bash
# Compile and run unit tests
mvn clean verify

# Run integration tests (Playwright + Jetty)
mvn verify -Pit

# Format code
mvn spotless:apply

# Build for Vaadin Directory
mvn install -Pdirectory -pl filesystem-api
```

## License

Apache License 2.0 — see [LICENSE](LICENSE) for details.
