# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Vaadin add-on providing a Java API for the browser's File System API. Multimodule Maven project (Java 21, Vaadin 25.1).

- `filesystem-api/` — the publishable library JAR (`artifactId: filesystem-api`)
- `integration-tests/` — demo views, Jetty runner, Playwright tests (`artifactId: filesystem-api-integration-tests`)

Package: `com.github.mcollovati.vaadin.filesystem`

## Build Commands

```bash
mvn clean verify              # compile both modules, run unit tests
mvn spotless:apply             # format all Java files (palantir-java-format)
mvn spotless:check             # check formatting without modifying

# Run a single unit test (in filesystem-api module)
mvn -pl filesystem-api test -Dtest=HandleKindTest

# Start demo app on port 8080
cd integration-tests && mvn jetty:run

# Run integration tests (Jetty start/stop + Failsafe)
mvn verify -Pit

# Build for Vaadin Directory release
mvn install -Pdirectory -pl filesystem-api

# Semver analysis (compare current build against baseline JARs)
mvn -pl filesystem-api,filesystem-api-browserless verify -DskipTests -Psemver \
  -Djapicmp.baseline.dir=/tmp/baseline
```

## Architecture

Two entry points, both instantiated with any Vaadin `Component` and bound to its DOM element:

- `FileSystemAPI` — `CompletableFuture`-based (async)
- `FileSystemCallbackAPI` — callback-based wrapper around `FileSystemAPI`

```
FileSystemAPI / FileSystemCallbackAPI
  → JsBridge (package-private, singleton per component via ComponentUtil.setData)
    → inline JS via PendingJavaScriptResult → CompletableFuture
```

Handle hierarchy (sealed):
```
FileSystemHandle (sealed interface)
  └─ AbstractFileSystemHandle (abstract, package-private)
       ├─ FileSystemFileHandle
       └─ FileSystemDirectoryHandle
```

Picker options are records with builders: `OpenFilePickerOptions`, `SaveFilePickerOptions`, `DirectoryPickerOptions`.

Client-side handle registry: JS `Map` stored on the host component's DOM element (no hidden elements or globals). All JavaScript is inline in `JsBridge` — there are no external JS resource files.

## Testing

- **Unit tests** (`filesystem-api/src/test/`): JUnit Jupiter. Some tests extend `BrowserlessTest` with `@ViewPackages` from `browserless-test-junit6`.
- **Integration tests** (`integration-tests/src/test/`): `*IT.java` naming, Playwright + Jetty, activated via `-Pit` profile. OPFS-based tests avoid native file picker dialogs.

## Conventions

- Formatting enforced by Spotless (palantir-java-format) — runs on all modules.
- Apache 2.0 license header required on all Java files (template: `etc/license-header.txt`).
- Commit messages explain **why**, not what. Never add co-authored-by lines. Wrap `@annotations` and class names in backticks.
- Changes to `.github/workflows/` use the `chore(actions):` commit message prefix.
