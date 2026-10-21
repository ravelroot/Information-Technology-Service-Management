package io.pyhoma.desktop.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class Toasts {

    public enum Kind { SUCCESS, INFO, WARNING, ERROR }

    private static VBox host;

    private Toasts() {
    }

    public static void attach(StackPane root) {
        host = new VBox(8);
        host.setAlignment(Pos.BOTTOM_RIGHT);
        host.setPickOnBounds(false);
        host.getStyleClass().add("toast-host");
        host.setMouseTransparent(true);
        StackPane.setAlignment(host, Pos.BOTTOM_RIGHT);
        root.getChildren().add(host);
    }

    public static void success(String text) {
        show(text, Kind.SUCCESS);
    }

    public static void info(String text) {
        show(text, Kind.INFO);
    }

    public static void warning(String text) {
        show(text, Kind.WARNING);
    }

    public static void show(String text, Kind kind) {
        if (host == null) {
            return;
        }
        String icon = switch (kind) {
            case SUCCESS -> "circle-check";
            case INFO -> "info";
            case WARNING -> "circle-alert";
            case ERROR -> "circle-x";
        };
        HBox toast = new HBox(10, Icons.of(icon, 16), new Label(text));
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.getStyleClass().addAll("toast", "toast-" + kind.name().toLowerCase());
        toast.setMaxWidth(420);
        host.getChildren().add(toast);
        FadeTransition in = new FadeTransition(Duration.millis(140), toast);
        in.setFromValue(0);
        in.setToValue(1);
        FadeTransition out = new FadeTransition(Duration.millis(220), toast);
        out.setToValue(0);
        SequentialTransition seq = new SequentialTransition(in, new PauseTransition(Duration.seconds(3.2)), out);
        seq.setOnFinished(e -> host.getChildren().remove(toast));
        seq.play();
    }
}
