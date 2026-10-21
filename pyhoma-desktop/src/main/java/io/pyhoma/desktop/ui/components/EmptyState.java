package io.pyhoma.desktop.ui.components;

import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Ui;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class EmptyState extends VBox {

    public EmptyState(String icon, String title, String hint, Node action) {
        super(8);
        setAlignment(Pos.CENTER);
        getStyleClass().add("empty-state");
        Node i = Icons.of(icon, 28);
        Label t = Ui.label(title, "empty-title");
        getChildren().addAll(i, t);
        if (hint != null) {
            Label h = Ui.label(hint, "muted");
            h.setWrapText(true);
            h.setMaxWidth(360);
            getChildren().add(h);
        }
        if (action != null) {
            getChildren().add(action);
        }
    }
}
