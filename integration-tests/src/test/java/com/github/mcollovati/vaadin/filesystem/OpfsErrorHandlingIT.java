package com.github.mcollovati.vaadin.filesystem;

import org.junit.jupiter.api.Test;

class OpfsErrorHandlingIT extends AbstractIT {

    @Test
    void notFoundError() {
        navigateTo("test/opfs-errors");
        clickButton("not-found-error");
        waitForLog("FileSystemNotFoundException:");
    }

    @Test
    void typeMismatchError() {
        navigateTo("test/opfs-errors");
        clickButton("type-mismatch-error");
        waitForLog("FileSystemTypeMismatchException:");
    }
}
