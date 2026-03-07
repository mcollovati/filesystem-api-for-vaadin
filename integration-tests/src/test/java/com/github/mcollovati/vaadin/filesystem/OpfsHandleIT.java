package com.github.mcollovati.vaadin.filesystem;

import org.junit.jupiter.api.Test;

class OpfsHandleIT extends AbstractIT {

    @Test
    void isSameEntry() {
        navigateTo("test/opfs-handles");
        clickButton("is-same-entry");
        waitForLog("same=true");
    }

    @Test
    void queryPermission() {
        navigateTo("test/opfs-handles");
        clickButton("query-permission");
        waitForLog("permission=");
    }

    @Test
    void requestPermission() {
        navigateTo("test/opfs-handles");
        clickButton("request-permission");
        waitForLog("permission=");
    }
}
