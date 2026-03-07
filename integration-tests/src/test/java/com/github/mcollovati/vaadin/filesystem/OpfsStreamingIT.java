package com.github.mcollovati.vaadin.filesystem;

import org.junit.jupiter.api.Test;

class OpfsStreamingIT extends AbstractIT {

    @Test
    void uploadToServer() {
        navigateTo("test/opfs-streaming");
        clickButton("upload-to-server");
        waitForLog("uploaded=upload content", 15000);
        waitForLog("upload-complete", 15000);
    }

    @Test
    void downloadFromServer() {
        navigateTo("test/opfs-streaming");
        clickButton("download-from-server");
        waitForLog("downloaded=downloaded content", 15000);
    }
}
