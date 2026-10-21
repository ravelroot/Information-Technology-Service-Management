package io.pyhoma.desktop.ui;

import java.util.function.DoubleConsumer;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

public final class Zoom {

    public static final double DEFAULT = 0.75;
    private static final double[] LEVELS = {0.5, 0.67, 0.75, 0.8, 0.9, 1.0, 1.1, 1.25, 1.5, 1.75};
    public static final double MIN = LEVELS[0];
    public static final double MAX = LEVELS[LEVELS.length - 1];
    private static final DoubleProperty LEVEL = new SimpleDoubleProperty(DEFAULT);
    private static DoubleConsumer onChange = v -> { };

    private Zoom() {
    }

    public static double level() {
        return LEVEL.get();
    }

    public static void setLevel(double value) {
        double best = DEFAULT;
        for (double l : LEVELS) {
            if (Math.abs(l - value) < Math.abs(best - value)) {
                best = l;
            }
        }
        LEVEL.set(best);
    }

    public static void parse(String value) {
        try {
            setLevel(value == null ? DEFAULT : Double.parseDouble(value));
        } catch (NumberFormatException e) {
            setLevel(DEFAULT);
        }
    }

    public static void onChange(DoubleConsumer listener) {
        onChange = listener == null ? v -> { } : listener;
    }

    public static Pane wrap(Region content) {
        Scale scale = new Scale(1, 1, 0, 0);
        scale.xProperty().bind(LEVEL);
        scale.yProperty().bind(LEVEL);
        content.getTransforms().add(scale);
        Pane host = new Pane(content) {
            @Override
            protected void layoutChildren() {
                double z = LEVEL.get();
                content.resizeRelocate(0, 0, getWidth() / z, getHeight() / z);
            }
        };
        host.setMinSize(0, 0);
        host.getStyleClass().add("zoom-host");
        LEVEL.addListener((o, a, b) -> host.requestLayout());
        return host;
    }

    public static void install(Scene scene, StackPane overlay) {
        Label badge = new Label();
        badge.getStyleClass().add("zoom-badge");
        badge.setMouseTransparent(true);
        badge.setOpacity(0);
        badge.setManaged(true);
        StackPane.setAlignment(badge, Pos.TOP_CENTER);
        overlay.getChildren().add(badge);
        Runnable show = () -> {
            badge.setText(Math.round(LEVEL.get() * 100) + "%");
            badge.toFront();
            FadeTransition in = new FadeTransition(Duration.millis(90), badge);
            in.setToValue(1);
            FadeTransition out = new FadeTransition(Duration.millis(260), badge);
            out.setToValue(0);
            SequentialTransition seq = new SequentialTransition(in, new PauseTransition(Duration.millis(800)), out);
            Object old = badge.getProperties().put("zoom.anim", seq);
            if (old instanceof SequentialTransition s) {
                s.stop();
            }
            seq.play();
        };
        scene.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isShortcutDown() && e.getDeltaY() != 0) {
                change(e.getDeltaY() > 0 ? 1 : -1, show);
                e.consume();
            }
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN), () -> change(1, show));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.ADD, KeyCombination.SHORTCUT_DOWN), () -> change(1, show));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.PLUS, KeyCombination.SHORTCUT_DOWN), () -> change(1, show));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN), () -> change(-1, show));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.SHORTCUT_DOWN), () -> change(-1, show));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN), () -> reset(show));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.NUMPAD0, KeyCombination.SHORTCUT_DOWN), () -> reset(show));
    }

    private static void change(int direction, Runnable show) {
        double before = LEVEL.get();
        int index = 0;
        for (int i = 0; i < LEVELS.length; i++) {
            if (Math.abs(LEVELS[i] - before) < 0.001) {
                index = i;
            }
        }
        int next = Math.max(0, Math.min(LEVELS.length - 1, index + direction));
        LEVEL.set(LEVELS[next]);
        if (LEVEL.get() != before) {
            onChange.accept(LEVEL.get());
        }
        show.run();
    }

    private static void reset(Runnable show) {
        setLevel(DEFAULT);
        onChange.accept(DEFAULT);
        show.run();
    }
}
