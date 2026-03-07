package com.github.mcollovati.vaadin.filesystem;

import org.junit.jupiter.api.Test;

class OpfsReadWriteIT extends AbstractIT {

    @Test
    void writeAndReadText() {
        navigateTo("test/opfs-read-write");
        clickButton("write-read-text");
        waitForLog("text=Hello OPFS");
        waitForLog("size=10");
    }

    @Test
    void writeAndReadBytes() {
        navigateTo("test/opfs-read-write");
        clickButton("write-read-bytes");
        waitForLog("bytes=[1, 2, 3, -1]");
        waitForLog("size=4");
    }
}
