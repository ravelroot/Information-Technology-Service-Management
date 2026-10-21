package io.pyhoma.desktop.ui.components;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

public final class Logo {

    private static final double BASE = 500;

    private Logo() {
    }

    public static Node mark(double size) {
        Rectangle bg = new Rectangle(BASE, BASE);
        bg.getStyleClass().add("brand-logo-bg");
        SVGPath chevron = new SVGPath();
        chevron.setContent("M142 172 L244 222 L244 236 L142 287 L142 258 L206 229 L142 200 Z");
        chevron.getStyleClass().add("brand-logo-path");
        Rectangle cursor = new Rectangle(251, 290, 107, 28);
        cursor.setArcWidth(8);
        cursor.setArcHeight(8);
        cursor.getStyleClass().add("brand-logo-path");
        Group art = new Group(bg, chevron, cursor);
        double k = size / BASE;
        art.setScaleX(k);
        art.setScaleY(k);
        art.setTranslateX(-(BASE - size) / 2);
        art.setTranslateY(-(BASE - size) / 2);
        Pane box = new Pane(new Group(art));
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        box.getStyleClass().add("brand-logo");
        return box;
    }
}
