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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FileSystemTesterTest {

    // -- Views --

    @Route("editor")
    public static class EditorView extends Div {
        final ClientFileSystem fs = new ClientFileSystem(this);
        final Input editor = new Input();
        final Span status = new Span();
        final NativeButton openBtn = new NativeButton("Open", e -> fs.openFile().thenAccept(data -> {
            editor.setValue(new String(data.getContent()));
            status.setText(data.getName());
        }));
        final NativeButton saveBtn = new NativeButton("Save", e -> fs.saveFile(editor.getValue()));

        public EditorView() {
            add(editor, openBtn, saveBtn, status);
        }
    }

    @Route("multi-open")
    public static class MultiOpenView extends Div {
        final ClientFileSystem fs = new ClientFileSystem(this);
        final Span count = new Span();
        final Span contents = new Span();
        final NativeButton openBtn =
                new NativeButton("Open All", e -> fs.openFiles().thenAccept(files -> {
                    count.setText(String.valueOf(files.size()));
                    contents.setText(
                            files.stream().map(f -> new String(f.getContent())).reduce("", (a, b) -> a + b));
                }));

        public MultiOpenView() {
            add(openBtn, count, contents);
        }
    }

    @Route("dir-browser")
    public static class DirectoryBrowserView extends Div {
        final ClientFileSystem fs = new ClientFileSystem(this);
        final Span listing = new Span();
        final Span entryCount = new Span();
        final NativeButton listBtn =
                new NativeButton("List", e -> fs.listDirectory().thenAccept(entries -> {
                    entryCount.setText(String.valueOf(entries.size()));
                    listing.setText(entries.stream()
                            .map(h -> h.getName() + ":" + h.getKind().getJsValue())
                            .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b));
                }));

        public DirectoryBrowserView() {
            add(listBtn, entryCount, listing);
        }
    }

    @Route("cancel")
    public static class CancelView extends Div {
        final ClientFileSystem fs = new ClientFileSystem(this);
        final Span status = new Span("idle");
        final NativeButton openBtn = new NativeButton(
                "Open",
                e -> fs.openFile().thenAccept(data -> status.setText("loaded")).exceptionally(ex -> {
                    status.setText("cancelled");
                    return null;
                }));

        public CancelView() {
            add(openBtn, status);
        }
    }

    @Route("save-bytes")
    public static class SaveBytesView extends Div {
        final ClientFileSystem fs = new ClientFileSystem(this);
        final Span status = new Span();
        final NativeButton saveBtn = new NativeButton(
                "Save", e -> fs.saveFile(new byte[] {1, 2, 3, 4}).thenAccept(v -> status.setText("saved")));

        public SaveBytesView() {
            add(saveBtn, status);
        }
    }

    @Route("default-save")
    public static class DefaultSaveView extends Div {
        final ClientFileSystem fs = new ClientFileSystem(this);
        final Span status = new Span();
        final NativeButton saveBtn =
                new NativeButton("Save", e -> fs.saveFile("content").thenAccept(v -> status.setText("saved")));

        public DefaultSaveView() {
            add(saveBtn, status);
        }
    }

    // -- Test classes --

    @Nested
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
    }

    @Nested
    @ViewPackages(classes = MultiOpenView.class)
    class MultiOpenTests extends BrowserlessTest {

        @Test
        void click_open_all_reads_multiple_files() {
            MultiOpenView view = navigate(MultiOpenView.class);
            FileSystemTester.forComponent(view)
                    .withFile("a.txt", "AAA")
                    .withFile("b.txt", "BBB")
                    .onOpenFilePicker(PickerResponse.returning("a.txt", "b.txt"))
                    .install();

            test(view.openBtn).click();

            assertEquals("2", view.count.getText());
            assertEquals("AAABBB", view.contents.getText());
        }

        @Test
        void default_open_picker_returns_all_files() {
            MultiOpenView view = navigate(MultiOpenView.class);
            FileSystemTester.forComponent(view)
                    .withFile("a.txt", "A")
                    .withFile("b.txt", "B")
                    .withDirectory("dir")
                    .install();

            test(view.openBtn).click();

            assertEquals("2", view.count.getText());
        }
    }

    @Nested
    @ViewPackages(classes = DirectoryBrowserView.class)
    class DirectoryBrowserTests extends BrowserlessTest {

        @Test
        void click_list_shows_directory_entries() {
            DirectoryBrowserView view = navigate(DirectoryBrowserView.class);
            FileSystemTester.forComponent(view)
                    .withFile("a.txt", "aaa")
                    .withFile("b.txt", "bbb")
                    .withDirectory("sub")
                    .install();

            test(view.listBtn).click();

            assertEquals("3", view.entryCount.getText());
            assertTrue(view.listing.getText().contains("a.txt:file"));
            assertTrue(view.listing.getText().contains("sub:directory"));
        }
    }

    @Nested
    @ViewPackages(classes = CancelView.class)
    class CancelTests extends BrowserlessTest {

        @Test
        void cancelled_picker_triggers_error_handler() {
            CancelView view = navigate(CancelView.class);
            FileSystemTester.forComponent(view)
                    .onOpenFilePicker(PickerResponse.cancelling())
                    .install();

            test(view.openBtn).click();

            assertEquals("cancelled", view.status.getText());
        }
    }

    @Nested
    @ViewPackages(classes = SaveBytesView.class)
    class SaveBytesTests extends BrowserlessTest {

        @Test
        void click_save_writes_bytes() {
            SaveBytesView view = navigate(SaveBytesView.class);
            FileSystemTester tester = FileSystemTester.forComponent(view)
                    .onSaveFilePicker(PickerResponse.returningSingle("data.bin"))
                    .install();

            test(view.saveBtn).click();

            assertEquals("saved", view.status.getText());
            assertArrayEquals(
                    new byte[] {1, 2, 3, 4},
                    tester.fileSystem().file("data.bin").content());
        }
    }

    @Nested
    @ViewPackages(classes = DefaultSaveView.class)
    class DefaultSaveTests extends BrowserlessTest {

        @Test
        void default_save_picker_creates_untitled() {
            DefaultSaveView view = navigate(DefaultSaveView.class);
            FileSystemTester tester = FileSystemTester.forComponent(view).install();

            test(view.saveBtn).click();

            assertEquals("saved", view.status.getText());
            assertEquals("content", tester.fileSystem().file("untitled").contentAsString());
        }
    }

    @Nested
    class InMemoryFileSystemTests {

        @Test
        void nested_directories_via_builder() {
            Div dummy = new Div();
            FileSystemTester tester = FileSystemTester.forComponent(dummy)
                    .withDirectory("level1", b1 -> b1.withDirectory("level2", b2 -> b2.withFile("deep.txt", "deep")))
                    .install();

            assertTrue(tester.fileSystem().exists("level1/level2/deep.txt"));
            assertEquals(
                    "deep", tester.fileSystem().file("level1/level2/deep.txt").contentAsString());
        }

        @Test
        void file_metadata() {
            Div dummy = new Div();
            FileSystemTester tester = FileSystemTester.forComponent(dummy)
                    .withFile("test.txt", "content".getBytes(), "text/plain")
                    .install();

            FsFile file = tester.fileSystem().file("test.txt");
            assertEquals("content", file.contentAsString());
            assertEquals("text/plain", file.mimeType());
            assertTrue(file.lastModified() > 0);
        }
    }
}
