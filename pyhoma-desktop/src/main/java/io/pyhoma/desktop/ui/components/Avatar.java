package io.pyhoma.desktop.ui.components;

import io.pyhoma.domain.support.Texts;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;

public final class Avatar extends StackPane {

    public Avatar(String name, double size) {
        getStyleClass().add("avatar");
        String n = name == null ? "?" : name;
        Label l = new Label(Texts.initials(n));
        l.getStyleClass().add("avatar-text");
        l.setStyle("-fx-font-size: " + Math.round(size * 0.4) + "px;");
        getChildren().add(l);
        setStyle("-fx-background-radius: " + size + ";");
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
        if (name != null) {
            Tooltip.install(this, new Tooltip(name));
        }
    }
}
