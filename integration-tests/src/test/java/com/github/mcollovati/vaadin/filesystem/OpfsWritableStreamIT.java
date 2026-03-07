package com.github.mcollovati.vaadin.filesystem;

import org.junit.jupiter.api.Test;

class OpfsWritableStreamIT extends AbstractIT {

    @Test
    void writeViaStream() {
        navigateTo("test/opfs-writable");
        clickButton("write-via-stream");
        waitForLog("content=Hello Stream");
    }

    @Test
    void seekAndWrite() {
        navigateTo("test/opfs-writable");
        clickButton("seek-and-write");
        waitForLog("content=AAACCC");
    }

    @Test
    void truncate() {
        navigateTo("test/opfs-writable");
        clickButton("truncate");
        waitForLog("content=Hello");
        waitForLog("size=5");
    }

    @Test
    void keepExistingData() {
        navigateTo("test/opfs-writable");
        clickButton("keep-existing-data");
        waitForLog("content=Modified");
    }
}
