package io.talos.desktop.ui.components;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

public final class Logo {

    private static final double BASE = 500;

    // Abaixo deste tamanho a gaveta perde a fenda e fica mais alta, para continuar legível.
    private static final double COMPACT_BELOW = 56;

    private static final String RACK =
            "M135 159h230v50h-230z M160 179h100v10h-100z M321 184a9 9 0 1 0 18 0a9 9 0 1 0 -18 0z "
            + "M135 225h230v50h-230z M160 245h100v10h-100z M321 250a9 9 0 1 0 18 0a9 9 0 1 0 -18 0z "
            + "M135 291h230v50h-230z M160 311h100v10h-100z M321 316a9 9 0 1 0 18 0a9 9 0 1 0 -18 0z";

    private static final String RACK_COMPACT =
            "M125 125h250v62h-250z M321 156a14 14 0 1 0 28 0a14 14 0 1 0 -28 0z "
            + "M125 219h250v62h-250z M321 250a14 14 0 1 0 28 0a14 14 0 1 0 -28 0z "
            + "M125 313h250v62h-250z M321 344a14 14 0 1 0 28 0a14 14 0 1 0 -28 0z";

    private Logo() {
    }

    public static Node mark(double size) {
        Rectangle bg = new Rectangle(BASE, BASE);
        bg.getStyleClass().add("brand-logo-bg");
        SVGPath rack = new SVGPath();
        rack.setContent(size < COMPACT_BELOW ? RACK_COMPACT : RACK);
        rack.setFillRule(FillRule.EVEN_ODD);
        rack.getStyleClass().add("brand-logo-path");
        Group art = new Group(bg, rack);
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
