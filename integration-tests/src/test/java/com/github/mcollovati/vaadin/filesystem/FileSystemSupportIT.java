package com.github.mcollovati.vaadin.filesystem;

import org.junit.jupiter.api.Test;

class FileSystemSupportIT extends AbstractIT {

    @Test
    void fileSystemApiIsSupported() {
        navigateTo("test/support");
        clickButton("check-support");
        waitForLog("supported=true");
    }
}
