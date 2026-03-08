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
package com.github.mcollovati.vaadin.filesystem.demo.contactmanager;

import com.github.mcollovati.vaadin.filesystem.DirectoryPickerOptions;
import com.github.mcollovati.vaadin.filesystem.FileData;
import com.github.mcollovati.vaadin.filesystem.FileTypeFilter;
import com.github.mcollovati.vaadin.filesystem.GetHandleOptions;
import com.github.mcollovati.vaadin.filesystem.OpenFilePickerOptions;
import com.github.mcollovati.vaadin.filesystem.PermissionMode;
import com.github.mcollovati.vaadin.filesystem.SaveFilePickerOptions;
import com.github.mcollovati.vaadin.filesystem.demo.AbstractDemoView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Demo view that imports contacts from CSV files, displays them in an editable
 * Grid, and exports individual or bulk vCard files using the File System API.
 */
@Route("contact-manager")
public class ContactManagerDemoView extends AbstractDemoView {

    private final List<Contact> contacts = new ArrayList<>();
    private final Grid<Contact> grid = new Grid<>();

    public ContactManagerDemoView() {
        super(
                "Contact Manager",
                "Import contacts from CSV files, edit them inline, "
                        + "and export as vCard (.vcf) files individually or in bulk to a folder.");

        var binder = new Binder<>(Contact.class);
        Editor<Contact> editor = grid.getEditor();
        editor.setBinder(binder);
        editor.setBuffered(true);

        var nameField = new TextField();
        nameField.setWidthFull();
        var nameCol = grid.addColumn(Contact::getName).setHeader("Name");
        nameCol.setEditorComponent(nameField);
        binder.forField(nameField).bind(Contact::getName, Contact::setName);

        var emailField = new TextField();
        emailField.setWidthFull();
        var emailCol = grid.addColumn(Contact::getEmail).setHeader("Email");
        emailCol.setEditorComponent(emailField);
        binder.forField(emailField).bind(Contact::getEmail, Contact::setEmail);

        var phoneField = new TextField();
        phoneField.setWidthFull();
        var phoneCol = grid.addColumn(Contact::getPhone).setHeader("Phone");
        phoneCol.setEditorComponent(phoneField);
        binder.forField(phoneField).bind(Contact::getPhone, Contact::setPhone);

        var companyField = new TextField();
        companyField.setWidthFull();
        var companyCol = grid.addColumn(Contact::getCompany).setHeader("Company");
        companyCol.setEditorComponent(companyField);
        binder.forField(companyField).bind(Contact::getCompany, Contact::setCompany);

        Grid.Column<Contact> actionColumn = grid.addColumn(new ComponentRenderer<>(contact -> {
            var actions = new HorizontalLayout();
            actions.setSpacing(true);
            actions.setPadding(false);

            var saveVCard = new Button("Save vCard", ev -> onSaveVCard(contact));
            saveVCard.addThemeVariants(ButtonVariant.LUMO_SMALL);

            var editBtn = new Button("Edit", ev -> {
                if (editor.isOpen()) {
                    editor.cancel();
                }
                editor.editItem(contact);
            });
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);

            var removeBtn = new Button("Remove", ev -> {
                contacts.remove(contact);
                refreshGrid();
            });
            removeBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);

            actions.add(saveVCard, editBtn, removeBtn);
            return actions;
        }));
        actionColumn.setHeader("Actions").setAutoWidth(true);

        var saveBtn = new Button("Save", ev -> editor.save());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        var cancelBtn = new Button("Cancel", ev -> editor.cancel());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        var editorActions = new HorizontalLayout(saveBtn, cancelBtn);
        editorActions.setSpacing(true);
        editorActions.setPadding(false);

        actionColumn.setEditorComponent(editorActions);
        editor.addSaveListener(ev -> {
            refreshGrid();
        });

        grid.setWidthFull();
        addContent(grid);

        loadDemoData();
    }

    @Override
    protected String codeSnippet() {
        return """
                // Import CSV contacts
                var opts = OpenFilePickerOptions.builder()
                        .types(FileTypeFilter.of("CSV", "text/csv", ".csv"))
                        .multiple(true).build();
                fs.openFiles(opts).thenAccept(files -> { ... });

                // Export single vCard
                var saveOpts = SaveFilePickerOptions.builder()
                        .suggestedName("Name.vcf")
                        .types(FileTypeFilter.of("vCard", "text/vcard", ".vcf"))
                        .build();
                fs.saveFile(saveOpts, vcardText);

                // Bulk export to folder
                fs.openDirectory(dirOpts).thenAccept(dir -> {
                    dir.getFileHandle("Name.vcf", creating())
                       .thenCompose(fh -> fh.writeString(vcard));
                });""";
    }

    @Override
    protected void addActions() {
        var importBtn = new Button("Import CSV", e -> onImportCsv());
        var addBtn = new Button("Add Contact", e -> onAddContact());
        var exportAllBtn = new Button("Export All to Folder", e -> onExportAll());
        var clearBtn = new Button("Clear", e -> onClear());
        add(new HorizontalLayout(importBtn, addBtn, exportAllBtn, clearBtn));
    }

    private void loadDemoData() {
        contacts.add(new Contact("Alice Johnson", "alice@example.com", "+1-555-0101", "Acme Corp"));
        contacts.add(new Contact("Bob Smith", "bob.smith@example.com", "+1-555-0102", "Globex Inc"));
        contacts.add(new Contact("Carol Williams", "carol@example.com", "+1-555-0103", "Initech"));
        contacts.add(new Contact("Dave Brown", "dave.brown@example.com", "+1-555-0104", "Umbrella Ltd"));
        contacts.add(new Contact("Eve Davis", "eve@example.com", "+1-555-0105", "Stark Industries"));
        refreshGrid();
    }

    private void onAddContact() {
        var contact = new Contact();
        contacts.addFirst(contact);
        refreshGrid();
        grid.getEditor().editItem(contact);
    }

    private void onImportCsv() {
        var options = OpenFilePickerOptions.builder()
                .types(FileTypeFilter.of("CSV files", "text/csv", ".csv"))
                .multiple(true)
                .build();
        fs().openFiles(options)
                .thenAccept(files -> {
                    for (FileData file : files) {
                        var csv = new String(file.getContent(), StandardCharsets.UTF_8);
                        var parsed = parseCsv(csv);
                        contacts.addAll(parsed);
                        appendLog("Imported " + parsed.size() + " contacts from " + file.getName());
                    }
                    getUI().ifPresent(ui -> ui.access(this::refreshGrid));
                })
                .exceptionally(this::logError);
    }

    private List<Contact> parseCsv(String csv) {
        var result = new ArrayList<Contact>();
        var lines = csv.split("\\r?\\n");
        for (int i = 1; i < lines.length; i++) {
            var line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            var fields = line.split(",", -1);
            var name = field(fields, 0);
            var email = field(fields, 1);
            var phone = field(fields, 2);
            var company = field(fields, 3);
            result.add(new Contact(name, email, phone, company));
        }
        return result;
    }

    private static String field(String[] fields, int index) {
        return index < fields.length ? fields[index].trim() : "";
    }

    private void onSaveVCard(Contact contact) {
        var options = SaveFilePickerOptions.builder()
                .suggestedName(vcfFileName(contact))
                .types(FileTypeFilter.of("vCard", "text/vcard", ".vcf"))
                .build();
        fs().saveFile(options, toVCard(contact))
                .thenRun(() -> appendLog("Saved vCard for " + contact.getName()))
                .exceptionally(this::logError);
    }

    private void onExportAll() {
        if (contacts.isEmpty()) {
            appendLog("No contacts to export");
            return;
        }
        var options =
                DirectoryPickerOptions.builder().mode(PermissionMode.READWRITE).build();
        fs().openDirectory(options)
                .thenCompose(dir -> {
                    CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
                    for (Contact contact : contacts) {
                        var fileName = vcfFileName(contact);
                        var vcard = toVCard(contact);
                        chain = chain.thenCompose(v -> dir.getFileHandle(fileName, GetHandleOptions.creating())
                                .thenCompose(fh -> fh.writeString(vcard)));
                    }
                    return chain;
                })
                .thenRun(() -> appendLog("Exported " + contacts.size() + " vCards to folder"))
                .exceptionally(this::logError);
    }

    private void onClear() {
        contacts.clear();
        refreshGrid();
        appendLog("Cleared all contacts");
    }

    private void refreshGrid() {
        grid.setItems(contacts);
    }

    private static String vcfFileName(Contact contact) {
        return contact.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + ".vcf";
    }

    private static String toVCard(Contact contact) {
        var sb = new StringBuilder();
        sb.append("BEGIN:VCARD\r\n");
        sb.append("VERSION:3.0\r\n");
        sb.append("FN:").append(contact.getName()).append("\r\n");
        if (!contact.getEmail().isEmpty()) {
            sb.append("EMAIL:").append(contact.getEmail()).append("\r\n");
        }
        if (!contact.getPhone().isEmpty()) {
            sb.append("TEL:").append(contact.getPhone()).append("\r\n");
        }
        if (!contact.getCompany().isEmpty()) {
            sb.append("ORG:").append(contact.getCompany()).append("\r\n");
        }
        sb.append("END:VCARD\r\n");
        return sb.toString();
    }
}
