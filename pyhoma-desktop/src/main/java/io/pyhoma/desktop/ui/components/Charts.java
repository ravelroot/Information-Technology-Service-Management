package io.pyhoma.desktop.ui.components;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Motion;
import io.pyhoma.desktop.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;

public final class Charts {

    public enum Mode {
        BARS("chart-bar"), COLUMNS("chart-column"), DONUT("chart-pie"), LINE("chart-line");

        private final String icon;

        Mode(String icon) {
            this.icon = icon;
        }

        public String icon() {
            return icon;
        }
    }

    public record Slice(String label, long value, String style) {
    }

    private Charts() {
    }

    public static Mode remembered(String key, Mode fallback, List<Mode> allowed) {
        String v = App.config() == null ? null : App.config().get("ui.chart." + key, null);
        if (v != null) {
            try {
                Mode m = Mode.valueOf(v);
                if (allowed.contains(m)) {
                    return m;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return fallback;
    }

    public static Node switchable(String title, String key, List<Mode> modes, Mode fallback, Function<Mode, Node> render) {
        StackPane body = new StackPane();
        body.setAlignment(Pos.TOP_LEFT);
        Mode initial = remembered(key, fallback, modes);
        body.getChildren().setAll(render.apply(initial));
        HBox toggles = new HBox(2);
        toggles.getStyleClass().add("chart-toggle");
        toggles.setAlignment(Pos.CENTER_RIGHT);
        ToggleGroup group = new ToggleGroup();
        for (Mode m : modes) {
            ToggleButton b = new ToggleButton();
            b.setGraphic(Icons.of(m.icon(), 14));
            b.setUserData(m);
            b.setToggleGroup(group);
            b.setTooltip(new Tooltip(I18n.t("chart.mode." + m.name().toLowerCase(Locale.ROOT))));
            b.getStyleClass().add("chart-toggle-button");
            b.setFocusTraversable(false);
            if (m == initial) {
                b.setSelected(true);
            }
            toggles.getChildren().add(b);
        }
        group.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                group.selectToggle(a);
                return;
            }
            Mode m = (Mode) b.getUserData();
            if (App.config() != null) {
                App.config().set("ui.chart." + key, m.name());
                App.config().save();
            }
            Node next = render.apply(m);
            body.getChildren().setAll(next);
            Motion.fadeIn(next, 240);
        });
        return Ui.panel(title, body, toggles);
    }

    public static Node bars(List<Slice> data) {
        VBox box = new VBox(10);
        long max = data.stream().mapToLong(Slice::value).max().orElse(1);
        for (Slice s : data) {
            box.getChildren().add(meter(s.label(), s.value(), max, s.style()));
        }
        return box;
    }

    public static Node meter(String label, long value, long max, String style) {
        Label l = Ui.label(label, "meter-label");
        Label v = Ui.label(String.valueOf(value), "meter-value");
        HBox top = new HBox(l, Ui.spacer(), v);
        Region track = new Region();
        track.getStyleClass().add("meter-track");
        Region fill = new Region();
        fill.getStyleClass().addAll("meter-fill", style);
        StackPane bar = new StackPane(track, fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        bar.setMinHeight(6);
        bar.setPrefHeight(6);
        fill.maxWidthProperty().bind(bar.widthProperty().multiply(Motion.grow(max == 0 ? 0 : (double) value / max)));
        return new VBox(4, top, bar);
    }

    public static Node columns(List<Slice> data, double height) {
        long max = data.stream().mapToLong(Slice::value).max().orElse(1);
        HBox row = new HBox(8);
        row.getStyleClass().add("column-chart");
        row.setAlignment(Pos.BOTTOM_CENTER);
        for (Slice s : data) {
            Label value = Ui.label(String.valueOf(s.value()), "column-value");
            Region bar = new Region();
            bar.getStyleClass().addAll("column-bar", "meter-fill", s.style());
            bar.setMaxWidth(46);
            bar.setPrefWidth(46);
            bar.setMinHeight(0);
            DoubleProperty grow = Motion.grow(max == 0 ? 0 : (double) s.value() / max);
            bar.prefHeightProperty().bind(grow.multiply(height));
            bar.maxHeightProperty().bind(bar.prefHeightProperty());
            Label name = Ui.label(s.label(), "column-label");
            name.setMaxWidth(Double.MAX_VALUE);
            name.setAlignment(Pos.CENTER);
            name.setTooltip(new Tooltip(s.label() + ": " + s.value()));
            StackPane slot = new StackPane(bar);
            slot.setAlignment(Pos.BOTTOM_CENTER);
            slot.setMinHeight(height);
            slot.setPrefHeight(height);
            VBox col = new VBox(4, value, slot, name);
            col.setAlignment(Pos.BOTTOM_CENTER);
            col.setMinWidth(0);
            col.setPrefWidth(0);
            HBox.setHgrow(col, Priority.ALWAYS);
            col.setMaxWidth(Double.MAX_VALUE);
            row.getChildren().add(col);
        }
        return row;
    }

    public static Node donut(List<Slice> data, String centerCaption) {
        long total = data.stream().mapToLong(Slice::value).sum();
        double radius = 66;
        double thickness = 18;
        Group arcs = new Group();
        Circle base = new Circle(radius);
        base.getStyleClass().add("donut-track");
        base.setStrokeWidth(thickness);
        arcs.getChildren().add(base);
        Label big = Ui.label(String.valueOf(total), "donut-total");
        Label small = Ui.label(centerCaption, "donut-caption");
        small.setMaxWidth(radius * 1.3);
        small.setWrapText(true);
        small.setAlignment(Pos.CENTER);
        VBox center = new VBox(0, big, small);
        center.setAlignment(Pos.CENTER);
        center.setMouseTransparent(true);
        DoubleProperty progress = Motion.grow(1.0);
        VBox legend = new VBox(7);
        legend.getStyleClass().add("donut-legend");
        double start = 90;
        List<Arc> all = new ArrayList<>();
        for (Slice s : data) {
            double sweep = total == 0 ? 0 : -360.0 * s.value() / total;
            Arc arc = new Arc(0, 0, radius, radius, start, 0);
            arc.setType(ArcType.OPEN);
            arc.getStyleClass().addAll("donut-seg", s.style());
            arc.setStrokeWidth(thickness);
            double from = start;
            arc.startAngleProperty().bind(progress.multiply(from - 90).add(90));
            arc.lengthProperty().bind(progress.multiply(sweep == 0 ? 0 : sweep + (sweep < -2 ? 1.2 : 0)));
            all.add(arc);
            arcs.getChildren().add(arc);
            start += sweep;
            Circle dot = new Circle(4.5);
            dot.getStyleClass().addAll("donut-dot", s.style());
            Label name = Ui.label(s.label(), "donut-legend-label");
            name.setMinWidth(0);
            HBox.setHgrow(name, Priority.SOMETIMES);
            String pct = total == 0 ? "0%" : Math.round(100.0 * s.value() / total) + "%";
            Label val = Ui.label(s.value() + "  ·  " + pct, "donut-legend-value");
            val.setMinWidth(Region.USE_PREF_SIZE);
            HBox row = new HBox(8, dot, name, Ui.spacer(), val);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("donut-legend-row");
            Runnable on = () -> {
                big.setText(String.valueOf(s.value()));
                small.setText(s.label());
                arc.setStrokeWidth(thickness + 6);
                row.getStyleClass().add("hover");
            };
            Runnable off = () -> {
                big.setText(String.valueOf(total));
                small.setText(centerCaption);
                arc.setStrokeWidth(thickness);
                row.getStyleClass().remove("hover");
            };
            arc.setOnMouseEntered(e -> on.run());
            arc.setOnMouseExited(e -> off.run());
            row.setOnMouseEntered(e -> on.run());
            row.setOnMouseExited(e -> off.run());
            legend.getChildren().add(row);
        }
        StackPane ring = new StackPane(arcs, center);
        ring.setMinSize((radius + thickness) * 2 + 8, (radius + thickness) * 2 + 8);
        ring.setPrefSize((radius + thickness) * 2 + 8, (radius + thickness) * 2 + 8);
        legend.setMaxWidth(Double.MAX_VALUE);
        legend.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(14, ring, legend);
        box.setAlignment(Pos.TOP_CENTER);
        box.getStyleClass().add("donut-chart");
        return box;
    }

    public static String seriesStyle(int index) {
        return "seg-" + (index % 8);
    }
}
