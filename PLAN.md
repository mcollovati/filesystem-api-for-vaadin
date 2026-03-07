# Implementation Plan: Vaadin File System API Add-on

A Vaadin add-on that provides a Java API to interact with the browser's
[File System API](https://developer.mozilla.org/en-US/docs/Web/API/File_System_API),
enabling server-side Java code to trigger file/directory pickers, read/write files,
and traverse directory structures through the browser's native filesystem access.

---

## How to Resume Work

To continue implementation in a new session, use one of these prompts:

```
Read PLAN.md and continue with the next step
```

```
Read PLAN.md and implement Step N
```

The assistant will read the plan, identify the next unchecked step, present a
detailed sub-plan for that step, and wait for your approval before writing code.

## Workflow Rules

- **Always ask confirmation before committing.** Show what will be committed and wait for approval.
- **When starting a new Step**, create a dedicated detailed plan for that step, summarize what will be done, and ask for confirmation or additions before writing any code.
- **Commit messages** should explain the reasons for the change, not implementation details.
- **NEVER** add co-authors to commit messages.
- **All classes must have proper Javadocs.** When updating code, Javadocs must be updated accordingly.
- **Always update the showcase/demo views** (or add new ones if necessary) whenever new features are added or existing ones are modified.

---

## API Design Overview

### JavaScript API to Java Mapping

| JS Interface / Method                  | Java Class / Method                                                  |
|----------------------------------------|----------------------------------------------------------------------|
| `FileSystemHandle`                     | `FileSystemHandle` (sealed interface, `kind`, `name`, `isSameEntry`) |
| `FileSystemFileHandle`                 | `FileSystemFileHandle` (extends `FileSystemHandle`)                  |
| `FileSystemDirectoryHandle`            | `FileSystemDirectoryHandle` (extends `FileSystemHandle`)             |
| `FileSystemWritableFileStream`         | `FileSystemWritableFileStream`                                       |
| `window.showOpenFilePicker(options)`   | `FileSystemAPI.showOpenFilePicker(UI, OpenFilePickerOptions)`        |
| `window.showSaveFilePicker(options)`   | `FileSystemAPI.showSaveFilePicker(UI, SaveFilePickerOptions)`        |
| `window.showDirectoryPicker(options)`  | `FileSystemAPI.showDirectoryPicker(UI, DirectoryPickerOptions)`      |
| `FileSystemHandle.kind`               | `FileSystemHandle.getKind()` returns `HandleKind` enum              |
| `FileSystemHandle.name`               | `FileSystemHandle.getName()`                                         |
| `FileSystemHandle.isSameEntry(other)`  | `FileSystemHandle.isSameEntry(FileSystemHandle)`                     |
| `FileSystemHandle.queryPermission()`   | `FileSystemHandle.queryPermission(PermissionMode)`                   |
| `FileSystemHandle.requestPermission()` | `FileSystemHandle.requestPermission(PermissionMode)`                 |
| `FileSystemFileHandle.getFile()`       | `FileSystemFileHandle.getFile()` -> `CompletableFuture<FileData>`    |
| `FileSystemFileHandle.createWritable()`| `FileSystemFileHandle.createWritable(WritableOptions)`               |
| `FileSystemDirectoryHandle.getFileHandle()`     | `FileSystemDirectoryHandle.getFileHandle(name, options)`  |
| `FileSystemDirectoryHandle.getDirectoryHandle()` | `FileSystemDirectoryHandle.getDirectoryHandle(name, opts)`|
| `FileSystemDirectoryHandle.removeEntry()`       | `FileSystemDirectoryHandle.removeEntry(name, options)`    |
| `FileSystemDirectoryHandle.resolve()`            | `FileSystemDirectoryHandle.resolve(handle)`              |
| `FileSystemDirectoryHandle.entries()`            | `FileSystemDirectoryHandle.entries()` -> callback-based  |
| `FileSystemWritableFileStream.write()` | `FileSystemWritableFileStream.write(data/options)`                   |
| `FileSystemWritableFileStream.seek()`  | `FileSystemWritableFileStream.seek(position)`                        |
| `FileSystemWritableFileStream.truncate()` | `FileSystemWritableFileStream.truncate(size)`                     |
| `FileSystemWritableFileStream.close()` | `FileSystemWritableFileStream.close()`                               |

### Key Design Decisions

1. **Async via `CompletableFuture`**: All JS-side async operations return
   `CompletableFuture<T>` on the Java side, bridged through Vaadin's
   `PendingJavaScriptResult`.

2. **Handle References**: JS handles are stored in a client-side registry
   (a `Map<id, handle>`) referenced by opaque IDs. Java-side `FileSystemHandle`
   objects hold the ID and the `UI` reference to call back into the browser.

3. **Sealed interface hierarchy**: `FileSystemHandle` is a sealed interface
   permitting `FileSystemFileHandle` and `FileSystemDirectoryHandle`.

4. **Options as records/builder**: Picker options and method options modeled as
   Java records or builder-pattern classes.

5. **Binary data transfer**: File reads transfer data via Vaadin's streaming
   mechanisms. File writes accept `byte[]`, `String`, or `InputStream`.

6. **Package**: `com.github.mcollovati.vaadin.filesystem`

### Enums

- `HandleKind`: `FILE`, `DIRECTORY`
- `PermissionMode`: `READ`, `READWRITE`
- `PermissionState`: `GRANTED`, `DENIED`, `PROMPT`

### Options Records

- `OpenFilePickerOptions`: `types`, `excludeAcceptAllOption`, `multiple`, `startIn`
- `SaveFilePickerOptions`: `types`, `excludeAcceptAllOption`, `suggestedName`, `startIn`
- `DirectoryPickerOptions`: `startIn`, `mode`
- `FileTypeFilter`: `description`, `accept` (Map<String, List<String>>)
- `GetHandleOptions`: `create` (boolean)
- `RemoveEntryOptions`: `recursive` (boolean)
- `WritableOptions`: `keepExistingData` (boolean)

---

## Implementation Steps

### Step 1: Project Setup and Build Configuration
**Goal**: Clean up the skeleton project, configure Spotless, update package names and dependencies.

- [x] Rename base package from `org.vaadin.addons.mygroup` to `com.github.mcollovati.vaadin.filesystem`
- [x] Update `pom.xml`:
  - Update `description` to a proper project description
  - Add Spotless Maven plugin (palantir-java-format)
  - Add `com.vaadin:browserless-test-junit6:1.0.0-beta1` dependency for unit tests
  - Add `com.microsoft.playwright:playwright` dependency for integration tests
  - Configure `maven-surefire-plugin` (unit tests, exclude `*IT.java`)
  - Configure `maven-failsafe-plugin` (integration tests, `*IT.java` in `it` profile)
- [x] Remove placeholder `TheAddon.java` and `AddonView.java`
- [x] Run `mvn spotless:apply` to verify formatting works
- [x] Commit: "chore: configure project structure and Spotless"

### Step 2: Core Handle Interfaces and Client-Side Registry
**Goal**: Implement the handle type hierarchy and the JS-side handle registry.

- [x] Handle registry stored on host component's DOM element via `Element.executeJs` (no JS module, no hidden elements, no globals)
- [x] Create Java enums: `HandleKind`, `PermissionMode`, `PermissionState`
- [x] Create Java `FileSystemHandle` sealed interface with `AbstractFileSystemHandle` base class
- [x] Create `FileSystemFileHandle` and `FileSystemDirectoryHandle`
- [x] `JsBridge` package-private utility class (singleton per component via `ComponentUtil.setData`)
- [x] `FileSystemAPI` instance-based entry point bound to any `Component`
- [x] Unit tests for enums
- [x] Commit: "feat: core handle interfaces and client-side registry"

### Step 3: File Picker Methods
**Goal**: Implement `showOpenFilePicker`, `showSaveFilePicker`, `showDirectoryPicker`.

- [x] Create options classes with builders: `OpenFilePickerOptions`, `SaveFilePickerOptions`, `DirectoryPickerOptions`
- [x] Create `FileTypeFilter` record
- [x] Add picker instance methods to `FileSystemAPI` with convenience overloads
- [x] Options passed as Jackson-serializable objects directly to `executeJs` (no manual JSON building)
- [x] JS handle registration with `startIn` handle resolution in `JsBridge`
- [x] `@JsonValue` on `PermissionMode.getJsValue()` for proper serialization
- [x] Unit tests for options builders
- [ ] Integration test (Playwright): `isSupported()`, picker error handling (deferred to Step 6)
- [x] Commit: "feat: file and directory picker methods"

### Step 4: FileSystemFileHandle Operations
**Goal**: Implement `getFile()` and `createWritable()` with read/write capabilities.

- [x] `FileData` class wrapping: `name` (String), `size` (long), `type` (String),
  `lastModified` (long), `content` (byte[] or InputStream)
- [x] `FileSystemFileHandle.getFile()` -> `CompletableFuture<FileData>`:
  - JS side: `handle.getFile()` -> read as ArrayBuffer -> base64 encode -> return to server
  - Server side: decode base64, wrap in `FileData`
  - Consider chunked transfer for large files
- [x] `WritableOptions` record: `keepExistingData` (boolean)
- [x] `FileSystemFileHandle.createWritable(WritableOptions)` -> `CompletableFuture<FileSystemWritableFileStream>`
- [x] `FileSystemWritableFileStream` class:
  - `write(String text)` -> `CompletableFuture<Void>`
  - `write(byte[] data)` -> `CompletableFuture<Void>`
  - `seek(long position)` -> `CompletableFuture<Void>`
  - `truncate(long size)` -> `CompletableFuture<Void>`
  - `close()` -> `CompletableFuture<Void>`
- [x] JS bridge: writable stream registry, write/seek/truncate/close operations
- [x] Unit tests (browserless):
  - `FileData` construction and accessors
  - `WritableOptions` defaults
- [ ] Integration test (Playwright):
  - OPFS-based test: create file in Origin Private File System, write content,
    read back and verify (avoids native file picker dialogs)
- [ ] Commit: "feat: file handle read/write operations"

### Step 5: FileSystemDirectoryHandle Operations
**Goal**: Implement directory traversal and manipulation methods.

- [x] `GetHandleOptions` record: `create` (boolean, default false)
- [x] `RemoveEntryOptions` record: `recursive` (boolean, default false)
- [x] `FileSystemDirectoryHandle` methods:
  - `getFileHandle(String name, GetHandleOptions options)` -> `CompletableFuture<FileSystemFileHandle>`
  - `getDirectoryHandle(String name, GetHandleOptions options)` -> `CompletableFuture<FileSystemDirectoryHandle>`
  - `removeEntry(String name, RemoveEntryOptions options)` -> `CompletableFuture<Void>`
  - `resolve(FileSystemHandle handle)` -> `CompletableFuture<Optional<List<String>>>`
  - `entries()` -> `CompletableFuture<List<FileSystemHandle>>`
  - Convenience overloads without options
- [x] JS bridge: directory operation functions
- [x] Unit tests (browserless):
  - Options records defaults
- [ ] Integration test (Playwright):
  - OPFS-based test: get root directory, create subdirectory, create file,
    list entries, resolve paths, remove entries
- [ ] Commit: "feat: directory handle operations"

### Step 6: Error Handling and Edge Cases
**Goal**: Robust error handling, browser support detection, and handle lifecycle.

- [x] Custom exception hierarchy:
  - `FileSystemApiException` (base)
  - `FileSystemApiNotSupportedException` (browser doesn't support the API)
  - `FileSystemNotFoundException` (file/directory not found)
  - `FileSystemNotAllowedException` (operation not allowed / cancelled)
  - `FileSystemTypeMismatchException` (expected file but got directory, or vice versa)
- [x] JS-side error mapping: catch `DOMException` by name and return structured
  error objects to the server:
  - `NotFoundError` -> `FileSystemNotFoundException`
  - `NotAllowedError` -> `FileSystemNotAllowedException`
  - `TypeMismatchError` -> `FileSystemTypeMismatchException`
  - `AbortError` -> `FileSystemNotAllowedException` (user cancellation)
- [x] Handle lifecycle management:
  - `FileSystemHandle.release()` to remove from client-side registry
  - Auto-cleanup on component detach (via detach listener in `JsBridge`)
- [x] Unit tests: exception mapping logic
- [ ] Integration test: verify error scenarios (access non-existent file, etc.)
- [ ] Commit: "feat: error handling and handle lifecycle"

### Step 7: UploadHandler and DownloadHandler Integration
**Goal**: Leverage Vaadin's `UploadHandler` and `DownloadHandler` for efficient large-file
transfer, avoiding the base64-over-`executeJs` bottleneck.

**Motivation**: The core `getFile()` and `write(byte[])` methods transfer data via base64-encoded
`executeJs` calls. This works well for small/medium files but is inefficient for large files
(~33% size inflation, entire content in memory). Vaadin's `UploadHandler` and `DownloadHandler`
use HTTP streaming, which is far more efficient and supports progress tracking.

- [x] **Upload integration** (`FileSystemFileHandle` → server via `UploadHandler`):
  - `FileSystemFileHandle.uploadTo(UploadHandler)` → `CompletableFuture<Void>`
  - JS side: get `File` from handle, build `FormData`, POST to `UploadHandler` endpoint via `fetch()`
  - Server side: `UploadHandler` receives the file as a standard multipart upload
  - Streaming transfer — no base64, no full content in memory
  - Complementary to `getFile()` (which remains for small files / when content is needed in Java)
- [x] **Download integration** (server → `FileSystemWritableFileStream` via `DownloadHandler`):
  - `FileSystemFileHandle.downloadFrom(DownloadHandler)` → `CompletableFuture<Void>`
  - JS side: `fetch()` the `DownloadHandler` URL, pipe response body into writable stream
  - Supports `DownloadHandler` progress tracking out of the box
  - Complementary to `write(byte[])` (which remains for small payloads)
- [x] Update demo view with large-file upload/download examples
- [ ] Unit tests for integration helper methods
- [ ] Integration test (OPFS-based): upload file content to server via `UploadHandler`,
  download content from `DownloadHandler` into OPFS file, verify round-trip
- [ ] Commit: "feat: `UploadHandler` and `DownloadHandler` integration for streaming transfers"

### Step 8: Documentation and README
**Goal**: Comprehensive documentation for users and contributors.

- [ ] `README.md` with sections:
  - Project description and motivation
  - Browser compatibility (Chromium-based browsers; Firefox/Safari partial support)
  - Maven dependency coordinates
  - Quick start examples:
    - Open file picker and read a file
    - Save file picker and write content
    - Browse directory contents
    - OPFS (Origin Private File System) usage
    - Streaming large files with `UploadHandler` / `DownloadHandler`
  - Full API reference summary table
  - Error handling guide
  - Building from source instructions
  - Running tests (`mvn test` for unit, `mvn verify -Pit` for integration)
  - Contributing guidelines
  - License (Apache 2.0)
- [ ] Javadoc on all public classes, methods, and enums
- [ ] Commit: "docs: README, Javadoc, and usage examples"

### Step 9: Polish and Release Preparation
**Goal**: Final cleanup, full test pass, CI readiness.

- [ ] Run full test suite: `mvn verify -Pit`
- [ ] Run `mvn spotless:check` to ensure consistent formatting
- [ ] Review all public API surface for consistency, naming, and usability
- [ ] Verify `pom.xml` metadata (description, SCM URL, developer info, license)
- [ ] Ensure assembly descriptor is updated for the new project
- [ ] Clean up any TODO comments or placeholder code
- [ ] Final Spotless formatting pass
- [ ] Commit: "chore: polish and release preparation"

**Note**: The plan now has 9 steps. Steps 1–6 cover the core File System API,
Step 7 adds Vaadin handler integration for efficient large-file transfer,
and Steps 8–9 handle documentation and release.

---

## Architecture Notes

### Client-Server Communication Pattern

```
Java (Server)                         JavaScript (Browser)
-----------------------------------------------------------
FileSystemAPI.showOpenFilePicker()
  -> UI.getPage().executeJs(...)  -->  window.showOpenFilePicker(options)
                                       -> register handles in registry
                                       -> return handle IDs + metadata
  <- PendingJavaScriptResult      <--  resolve({id, kind, name})
  -> create FileSystemFileHandle(id, ui)

FileSystemFileHandle.getFile()
  -> UI.getPage().executeJs(...)  -->  registry.get(id).getFile()
                                       -> read as ArrayBuffer
                                       -> base64 encode
  <- PendingJavaScriptResult      <--  resolve({name, size, type, lastModified, content})
  -> create FileData(...)
```

### Handle Lifecycle

- Handles are registered in a client-side `Map<string, FileSystemHandle>` when
  created by picker or directory operations
- Each handle gets a unique ID (incrementing counter)
- Java objects hold the handle ID + reference to `UI` for JS execution
- Handles can be explicitly released via `release()` method
- All handles are invalidated on page navigation/refresh
- Optional auto-cleanup via `UI.addDetachListener`

### Testing Strategy

**Unit tests** (`com.vaadin:browserless-test-junit6:1.0.0-beta1`):
- Extend `BrowserlessTest` from the browserless-test framework
- Use `@ViewPackages` to specify test views for route discovery
- Test Java-side logic: enums, records, options serialization, handle management
- Mock JS bridge for testing server-side behavior without a browser
- Test views can be navigated with `navigate(ViewClass.class)`
- Access `UI.getCurrent()`, `VaadinService.getCurrent()`, etc. in tests

**Integration tests** (Playwright + Jetty via `it` Maven profile):
- Jetty starts/stops around integration tests (existing `it` profile)
- Playwright connects to running Jetty instance at `http://localhost:8080`
- Test classes named `*IT.java`, run by `maven-failsafe-plugin`
- OPFS-based tests avoid native file picker dialogs (fully automatable)
- File picker tests verify API availability and error handling
- Test views in `src/test/java` under test-specific view packages

---

## Target File Structure

```
src/
  main/
    java/com/github/mcollovati/vaadin/filesystem/
      FileSystemAPI.java                        # Entry point (static picker methods + isSupported)
      FileSystemHandle.java                     # Sealed interface (kind, name, isSameEntry, permissions)
      FileSystemFileHandle.java                 # File handle (getFile, createWritable)
      FileSystemDirectoryHandle.java            # Directory handle (getFileHandle, getDirectoryHandle, etc.)
      FileSystemWritableFileStream.java         # Writable stream (write, seek, truncate, close)
      FileData.java                             # File content wrapper (name, size, type, content)
      HandleKind.java                           # FILE | DIRECTORY enum
      PermissionMode.java                       # READ | READWRITE enum
      PermissionState.java                      # GRANTED | DENIED | PROMPT enum
      OpenFilePickerOptions.java                # Options for showOpenFilePicker
      SaveFilePickerOptions.java                # Options for showSaveFilePicker
      DirectoryPickerOptions.java               # Options for showDirectoryPicker
      FileTypeFilter.java                       # File type filter (description + accept map)
      GetHandleOptions.java                     # Options for getFileHandle / getDirectoryHandle
      RemoveEntryOptions.java                   # Options for removeEntry
      WritableOptions.java                      # Options for createWritable
      FileSystemApiException.java               # Base exception
      FileSystemApiNotSupportedException.java   # API not available in browser
      FileSystemNotFoundException.java          # Entry not found
      FileSystemNotAllowedException.java        # Operation not allowed / cancelled
      FileSystemTypeMismatchException.java      # Type mismatch (file vs directory)
      internal/
        JsBridge.java                           # JS execution helpers, handle registry management
    resources/
      META-INF/resources/frontend/
        filesystem-api.js                       # Client-side JS module (registries + bridge functions)
  test/
    java/com/github/mcollovati/vaadin/filesystem/
      unit/                                     # Browserless unit tests
        HandleKindTest.java
        PermissionEnumsTest.java
        OpenFilePickerOptionsTest.java
        SaveFilePickerOptionsTest.java
        DirectoryPickerOptionsTest.java
        FileDataTest.java
        FileSystemAPITest.java                  # Tests with mocked JS bridge
      it/                                       # Playwright integration tests
        AbstractIT.java                         # Base IT class (Playwright setup/teardown)
        FileSystemSupportIT.java                # isSupported() test
        OpfsFileReadWriteIT.java                # OPFS read/write test
        OpfsDirectoryIT.java                    # OPFS directory operations test
      views/                                    # Test views for both unit and integration tests
        FilePickerTestView.java
        DirectoryTestView.java
        OpfsTestView.java
```

---

## Dependencies to Add

```xml
<!-- Spotless (in build/plugins) -->
<plugin>
  <groupId>com.diffplug.spotless</groupId>
  <artifactId>spotless-maven-plugin</artifactId>
  <version>2.44.4</version>
  <configuration>
    <java>
      <palantirJavaFormat>
        <version>2.50.0</version>
      </palantirJavaFormat>
      <importOrder/>
      <removeUnusedImports/>
    </java>
  </configuration>
</plugin>

<!-- Vaadin browserless test (in dependencies) -->
<dependency>
  <groupId>com.vaadin</groupId>
  <artifactId>browserless-test-junit6</artifactId>
  <version>1.0.0-beta1</version>
  <scope>test</scope>
</dependency>

<!-- Playwright for integration tests (in dependencies) -->
<dependency>
  <groupId>com.microsoft.playwright</groupId>
  <artifactId>playwright</artifactId>
  <version>1.50.0</version>
  <scope>test</scope>
</dependency>
```
