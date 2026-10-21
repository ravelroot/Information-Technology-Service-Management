package io.pyhoma.desktop.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class Ui {

    private Ui() {
    }

    public static Button button(String text, String icon, Runnable action, String... styles) {
        Button b = new Button(text);
        if (icon != null) {
            b.setGraphic(Icons.of(icon, 15));
        }
        b.getStyleClass().addAll(styles);
        if (action != null) {
            b.setOnAction(e -> action.run());
        }
        return b;
    }

    public static Button primary(String text, String icon, Runnable action) {
        return button(text, icon, action, "primary");
    }

    public static Button ghost(String text, String icon, Runnable action) {
        return button(text, icon, action, "ghost");
    }

    public static Button danger(String text, String icon, Runnable action) {
        return button(text, icon, action, "danger");
    }

    public static Button iconButton(String icon, String tooltip, Runnable action) {
        Button b = new Button();
        b.setGraphic(Icons.of(icon, 16));
        b.getStyleClass().addAll("icon-button");
        if (tooltip != null) {
            b.setTooltip(new Tooltip(tooltip));
            b.setAccessibleText(tooltip);
        }
        if (action != null) {
            b.setOnAction(e -> action.run());
        }
        return b;
    }

    public static Label label(String text, String... styles) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styles);
        return l;
    }

    public static Label muted(String text) {
        return label(text, "muted");
    }

    public static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    public static HBox row(double spacing, Node... nodes) {
        HBox h = new HBox(spacing, nodes);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    public static VBox column(double spacing, Node... nodes) {
        return new VBox(spacing, nodes);
    }

    public static VBox panel(String title, Node content, Node... actions) {
        VBox box = new VBox();
        box.getStyleClass().add("panel");
        if (title != null) {
            HBox head = row(8, label(title, "panel-title"), spacer());
            head.getChildren().addAll(actions);
            head.getStyleClass().add("panel-head");
            box.getChildren().add(head);
        }
        VBox body = new VBox(content);
        body.getStyleClass().add("panel-body");
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox.setVgrow(content, Priority.ALWAYS);
        box.getChildren().add(body);
        return box;
    }

    public static Insets pad(double v) {
        return new Insets(v);
    }

    public static VBox fact(String label, Node value) {
        VBox v = new VBox(2, label(label, "fact-label"), value);
        v.getStyleClass().add("fact");
        return v;
    }

    public static VBox fact(String label, String value) {
        Label l = label(value == null || value.isBlank() ? "—" : value, "fact-value");
        l.setWrapText(true);
        return fact(label, l);
    }

    public static <T extends Node> T style(T node, String... classes) {
        node.getStyleClass().addAll(classes);
        return node;
    }

    public static javafx.util.StringConverter<Number> integerAxisFormatter() {
        return new javafx.util.StringConverter<>() {
            @Override
            public String toString(Number n) {
                if (n == null || Math.abs(n.doubleValue() - Math.rint(n.doubleValue())) > 1e-9) {
                    return "";
                }
                return java.text.NumberFormat.getIntegerInstance(I18n.locale()).format(n.longValue());
            }

            @Override
            public Number fromString(String s) {
                return Double.valueOf(s);
            }
        };
    }

    public static void showMaximized(javafx.stage.Stage stage) {
        if (!stage.isMaximized()) {
            stage.sizeToScene();
        }
        if (!stage.isShowing()) {
            stage.show();
        }
        if (!stage.isMaximized()) {
            center(stage);
            stage.setMaximized(true);
        } else if (!fillsScreen(stage)) {
            stage.setMaximized(false);
            stage.setMaximized(true);
        }
        javafx.application.Platform.runLater(() -> {
            if (stage.isShowing() && (!stage.isMaximized() || !fillsScreen(stage))) {
                stage.setMaximized(false);
                stage.setMaximized(true);
            }
        });
    }

    public static double[] sceneSize(javafx.stage.Stage stage, double width, double height) {
        javafx.scene.Scene current = stage.getScene();
        if (stage.isShowing() && stage.isMaximized() && current != null && current.getWidth() > 0 && current.getHeight() > 0) {
            return new double[] {current.getWidth(), current.getHeight()};
        }
        return new double[] {width, height};
    }

    private static boolean fillsScreen(javafx.stage.Stage stage) {
        javafx.collections.ObservableList<javafx.stage.Screen> screens = javafx.stage.Screen.getScreensForRectangle(
                stage.getX(), stage.getY(), Math.max(1, stage.getWidth()), Math.max(1, stage.getHeight()));
        javafx.geometry.Rectangle2D vb = (screens.isEmpty() ? javafx.stage.Screen.getPrimary() : screens.get(0)).getVisualBounds();
        return stage.getWidth() >= vb.getWidth() - 2 && stage.getHeight() >= vb.getHeight() - 2;
    }

    public static void showCentered(javafx.stage.Stage stage) {
        if (stage.isMaximized()) {
            stage.setMaximized(false);
        }
        stage.sizeToScene();
        if (!stage.isShowing()) {
            stage.show();
        }
        center(stage);
        javafx.application.Platform.runLater(() -> {
            if (!stage.isMaximized()) {
                center(stage);
            }
        });
    }

    public static void center(javafx.stage.Stage stage) {
        double w = stage.getWidth();
        double h = stage.getHeight();
        if (Double.isNaN(w) || Double.isNaN(h)) {
            stage.centerOnScreen();
            return;
        }
        javafx.geometry.Rectangle2D vb = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setX(vb.getMinX() + Math.max(0, (vb.getWidth() - w) / 2));
        stage.setY(vb.getMinY() + Math.max(0, (vb.getHeight() - h) / 2));
    }
}
