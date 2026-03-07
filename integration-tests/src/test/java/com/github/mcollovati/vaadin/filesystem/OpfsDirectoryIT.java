package com.github.mcollovati.vaadin.filesystem;

import org.junit.jupiter.api.Test;

class OpfsDirectoryIT extends AbstractIT {

    @Test
    void setupAndListEntries() {
        navigateTo("test/opfs-directory");
        clickButton("setup-and-list");
        waitForLog("count=3");
    }

    @Test
    void resolvePath() {
        navigateTo("test/opfs-directory");
        clickButton("resolve-path");
        waitForLog("path=[sub, deep.txt]");
    }

    @Test
    void removeEntry() {
        navigateTo("test/opfs-directory");
        clickButton("remove-entry");
        waitForLog("after-remove-count=0");
    }

    @Test
    void createNestedStructure() {
        navigateTo("test/opfs-directory");
        clickButton("create-nested");
        waitForLog("nested-path=[level1, level2, nested.txt]");
    }
}
