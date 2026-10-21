package io.pyhoma.desktop;

import javafx.application.Application;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("java.awt.headless", "false");
        System.setProperty("file.encoding", "UTF-8");
        Application.launch(PyhomaApp.class, args);
    }
}
