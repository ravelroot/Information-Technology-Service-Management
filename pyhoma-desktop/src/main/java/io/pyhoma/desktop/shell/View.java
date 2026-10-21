package io.pyhoma.desktop.shell;

import javafx.scene.Node;

public abstract class View {

    private Node root;

    protected abstract Node build();

    public final Node root() {
        if (root == null) {
            root = build();
            root.getStyleClass().add("view");
        }
        return root;
    }

    public void onShow() {
    }

    public abstract String title();
}
