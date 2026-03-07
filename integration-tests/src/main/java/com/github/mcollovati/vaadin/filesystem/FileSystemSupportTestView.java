package com.github.mcollovati.vaadin.filesystem;

import com.vaadin.flow.router.Route;

@Route("test/support")
public class FileSystemSupportTestView extends AbstractOpfsTestView {

    @Override
    void addActions() {
        add(button("check-support", "Check Support", this::onCheckSupport));
    }

    private void onCheckSupport() {
        fs().isSupported()
                .thenAccept(supported -> appendLog("supported=" + supported))
                .exceptionally(this::logError);
    }
}
