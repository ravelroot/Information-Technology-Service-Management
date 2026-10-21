package io.pyhoma.desktop.ui.components;

import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Ui;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

public final class GanttChart extends BorderPane {

    public record Bar(Long id, String label, String detail, LocalDate start, LocalDate end, int progress, boolean milestone,
                      String color, int depth, boolean critical, boolean late, boolean summary) {
    }

    public record Link(Long from, Long to) {
    }

    public enum Scale {
        DAY(34), WEEK(15), MONTH(5);

        final double px;

        Scale(double px) {
            this.px = px;
        }
    }

    public interface MoveHandler {
        void moved(Long id, LocalDate start, LocalDate end);
    }

    private static final double ROW = 36;
    private static final double HEADER = 48;
    private static final double LIST_WIDTH = 300;

    private List<Bar> bars = List.of();
    private List<Link> links = List.of();
    private Scale scale = Scale.DAY;
    private boolean showCritical = true;
    private MoveHandler onMove;
    private Consumer<Long> onOpen;
    private LocalDate origin;
    private final ScrollPane outer = new ScrollPane();
    private final ScrollPane timelineScroll = new ScrollPane();

    public GanttChart() {
        getStyleClass().add("gantt");
        outer.setFitToWidth(true);
        outer.getStyleClass().add("gantt-outer");
        timelineScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        timelineScroll.getStyleClass().add("gantt-timeline-scroll");
        setCenter(outer);
    }

    public void setScale(Scale s) {
        this.scale = s;
        redraw();
    }

    public void setShowCritical(boolean show) {
        this.showCritical = show;
        redraw();
    }

    public void onMove(MoveHandler handler) {
        this.onMove = handler;
    }

    public void onOpen(Consumer<Long> handler) {
        this.onOpen = handler;
    }

    public void setData(List<Bar> newBars, List<Link> newLinks) {
        this.bars = List.copyOf(newBars);
        this.links = List.copyOf(newLinks);
        redraw();
    }

    public void scrollToToday() {
        if (origin == null) {
            return;
        }
        javafx.application.Platform.runLater(() -> {
            double width = timelineScroll.getContent() == null ? 0 : timelineScroll.getContent().getBoundsInLocal().getWidth();
            double viewport = timelineScroll.getViewportBounds().getWidth();
            double x = x(LocalDate.now()) - viewport / 3;
            if (width > viewport) {
                timelineScroll.setHvalue(Math.max(0, Math.min(1, x / (width - viewport))));
            }
        });
    }

    private double x(LocalDate d) {
        return ChronoUnit.DAYS.between(origin, d) * scale.px;
    }

    private void redraw() {
        if (bars.isEmpty()) {
            outer.setContent(new EmptyState("chart-gantt", I18n.t("gantt.empty"), I18n.t("gantt.emptyHint"), null));
            return;
        }
        LocalDate min = bars.stream().map(Bar::start).filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate max = bars.stream().map(b -> b.end() == null ? b.start() : b.end()).filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate today = LocalDate.now();
        if (today.isBefore(min)) {
            min = today;
        }
        if (today.isAfter(max)) {
            max = today;
        }
        origin = scale == Scale.MONTH ? min.withDayOfMonth(1).minusMonths(0) : min.minusDays(min.getDayOfWeek().getValue() - 1L + 7);
        LocalDate last = max.plusDays(scale == Scale.MONTH ? 45 : 21);
        long days = ChronoUnit.DAYS.between(origin, last);
        double width = Math.max(days * scale.px, 600);
        double height = HEADER + bars.size() * ROW;

        Pane timeline = new Pane();
        timeline.getStyleClass().add("gantt-timeline");
        timeline.setPrefSize(width, height);
        timeline.setMinSize(width, height);
        drawGrid(timeline, days, height);
        Map<Long, Integer> rowOf = new HashMap<>();
        for (int i = 0; i < bars.size(); i++) {
            rowOf.put(bars.get(i).id(), i);
        }
        Group arrows = new Group();
        timeline.getChildren().add(arrows);
        for (int i = 0; i < bars.size(); i++) {
            Bar b = bars.get(i);
            if (b.start() != null) {
                timeline.getChildren().add(bar(b, i));
            }
        }
        for (Link l : links) {
            Integer from = rowOf.get(l.from());
            Integer to = rowOf.get(l.to());
            if (from != null && to != null) {
                Node arrow = arrow(bars.get(from), from, bars.get(to), to);
                if (arrow != null) {
                    arrows.getChildren().add(arrow);
                }
            }
        }
        double todayX = x(today) + scale.px / 2;
        Line todayLine = new Line(todayX, HEADER - 6, todayX, height);
        todayLine.getStyleClass().add("gantt-today");
        timeline.getChildren().add(todayLine);

        timelineScroll.setContent(timeline);
        timelineScroll.setPrefViewportHeight(height);
        timelineScroll.setMinHeight(height + 16);
        HBox content = new HBox(list(), timelineScroll);
        javafx.scene.layout.HBox.setHgrow(timelineScroll, javafx.scene.layout.Priority.ALWAYS);
        outer.setContent(content);
    }

    private Node list() {
        VBox list = new VBox();
        list.getStyleClass().add("gantt-list");
        list.setPrefWidth(LIST_WIDTH);
        list.setMinWidth(LIST_WIDTH);
        Label head = Ui.label(I18n.t("gantt.task"), "gantt-list-head");
        head.setPrefHeight(HEADER);
        head.setMinHeight(HEADER);
        head.setMaxWidth(Double.MAX_VALUE);
        list.getChildren().add(head);
        for (Bar b : bars) {
            Label name = Ui.label(b.label(), b.summary() ? "gantt-name-summary" : "gantt-name");
            name.setMaxWidth(LIST_WIDTH - 90 - b.depth() * 16);
            Label detail = Ui.label(b.detail() == null ? "" : b.detail(), "gantt-detail");
            Label pct = Ui.label(b.milestone() ? "◆" : b.progress() + "%", "gantt-pct");
            Region indent = new Region();
            indent.setMinWidth(b.depth() * 16);
            HBox row = new HBox(8, indent, new VBox(0, name, detail), Ui.spacer(), pct);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("gantt-row");
            row.setPrefHeight(ROW);
            row.setMinHeight(ROW);
            row.setMaxHeight(ROW);
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && onOpen != null) {
                    onOpen.accept(b.id());
                }
            });
            list.getChildren().add(row);
        }
        return list;
    }

    private void drawGrid(Pane pane, long days, double height) {
        Rectangle headBg = new Rectangle(0, 0, days * scale.px, HEADER);
        headBg.getStyleClass().add("gantt-head-bg");
        pane.getChildren().add(headBg);
        for (int r = 0; r < bars.size(); r++) {
            if (r % 2 == 1) {
                Rectangle stripe = new Rectangle(0, HEADER + r * ROW, days * scale.px, ROW);
                stripe.getStyleClass().add("gantt-stripe");
                pane.getChildren().add(stripe);
            }
        }
        LocalDate d = origin;
        int lastMonth = -1;
        for (long i = 0; i < days; i++, d = d.plusDays(1)) {
            double x = i * scale.px;
            boolean weekend = d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
            if (weekend && scale != Scale.MONTH) {
                Rectangle w = new Rectangle(x, HEADER, scale.px, height - HEADER);
                w.getStyleClass().add("gantt-weekend");
                pane.getChildren().add(w);
            }
            if (d.getMonthValue() != lastMonth) {
                lastMonth = d.getMonthValue();
                Label m = Ui.label(d.getMonth().getDisplayName(TextStyle.SHORT, I18n.locale()) + " " + d.getYear(), "gantt-month");
                m.setLayoutX(x + 4);
                m.setLayoutY(4);
                pane.getChildren().add(m);
                Line ml = new Line(x, 0, x, height);
                ml.getStyleClass().add("gantt-month-line");
                pane.getChildren().add(ml);
            }
            boolean tick = switch (scale) {
                case DAY -> true;
                case WEEK -> d.getDayOfWeek() == DayOfWeek.MONDAY;
                case MONTH -> false;
            };
            if (tick) {
                String text = scale == Scale.DAY ? String.valueOf(d.getDayOfMonth())
                        : I18n.t("gantt.week", d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
                Label l = Ui.label(text, "gantt-day");
                l.setLayoutX(x + (scale == Scale.DAY ? 0 : 3));
                l.setLayoutY(26);
                if (scale == Scale.DAY) {
                    l.setPrefWidth(scale.px);
                    l.setAlignment(Pos.CENTER);
                }
                pane.getChildren().add(l);
                if (scale == Scale.WEEK) {
                    Line wl = new Line(x, HEADER, x, height);
                    wl.getStyleClass().add("gantt-week-line");
                    pane.getChildren().add(wl);
                }
            }
        }
    }

    private Node bar(Bar b, int row) {
        double y = HEADER + row * ROW;
        LocalDate end = b.end() == null ? b.start() : b.end();
        if (b.milestone()) {
            double cx = x(b.start()) + scale.px / 2;
            double cy = y + ROW / 2;
            Polygon diamond = new Polygon(cx, cy - 8, cx + 8, cy, cx, cy + 8, cx - 8, cy);
            diamond.getStyleClass().add("gantt-milestone");
            if (showCritical && b.critical()) {
                diamond.getStyleClass().add("critical");
            }
            Tooltip.install(diamond, new Tooltip(b.label() + "\n" + I18n.date(b.start())));
            Label label = Ui.label(b.label(), "gantt-bar-label");
            label.setLayoutX(cx + 12);
            label.setLayoutY(cy - 9);
            return new Group(diamond, label);
        }
        double x0 = x(b.start());
        double w = Math.max(scale.px, (ChronoUnit.DAYS.between(b.start(), end) + 1) * scale.px);
        double barH = b.summary() ? 10 : 18;
        double top = y + (ROW - barH) / 2;
        Rectangle bg = new Rectangle(0, 0, w, barH);
        bg.setArcWidth(b.summary() ? 3 : 7);
        bg.setArcHeight(b.summary() ? 3 : 7);
        bg.getStyleClass().add(b.summary() ? "gantt-summary" : "gantt-bar");
        Rectangle done = new Rectangle(0, 0, w * Math.max(0, Math.min(100, b.progress())) / 100.0, barH);
        done.setArcWidth(bg.getArcWidth());
        done.setArcHeight(bg.getArcHeight());
        done.getStyleClass().add("gantt-progress");
        Pane barPane = new Pane(bg, done);
        if (showCritical && b.critical()) {
            bg.getStyleClass().add("critical");
        }
        if (b.late()) {
            bg.getStyleClass().add("late");
        }
        barPane.setLayoutX(x0);
        barPane.setLayoutY(top);
        barPane.setPrefSize(w, barH);
        Label label = Ui.label(b.label(), "gantt-bar-label");
        label.setLayoutX(x0 + w + 8);
        label.setLayoutY(y + ROW / 2 - 9);
        Tooltip.install(barPane, new Tooltip(b.label() + "\n" + I18n.date(b.start()) + " – " + I18n.date(end) + "\n"
                + b.progress() + "%"));
        if (onMove != null && !b.summary()) {
            enableDrag(barPane, label, b, w);
        }
        barPane.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && onOpen != null) {
                onOpen.accept(b.id());
            }
        });
        return new Group(barPane, label);
    }

    private void enableDrag(Pane barPane, Label label, Bar b, double width) {
        double[] press = new double[2];
        boolean[] resizing = new boolean[1];
        barPane.setCursor(Cursor.OPEN_HAND);
        barPane.setOnMouseMoved(e -> barPane.setCursor(e.getX() > width - 7 ? Cursor.H_RESIZE : Cursor.OPEN_HAND));
        barPane.setOnMousePressed(e -> {
            press[0] = e.getSceneX();
            press[1] = barPane.getLayoutX();
            resizing[0] = e.getX() > width - 7;
            e.consume();
        });
        barPane.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - press[0];
            if (resizing[0]) {
                double w = Math.max(scale.px, width + dx);
                ((Rectangle) barPane.getChildren().get(0)).setWidth(w);
                label.setLayoutX(barPane.getLayoutX() + w + 8);
            } else {
                barPane.setLayoutX(press[1] + dx);
                label.setLayoutX(press[1] + dx + width + 8);
                barPane.setCursor(Cursor.CLOSED_HAND);
            }
            e.consume();
        });
        barPane.setOnMouseReleased(e -> {
            long days = Math.round((e.getSceneX() - press[0]) / scale.px);
            if (days == 0) {
                redraw();
                return;
            }
            LocalDate end = b.end() == null ? b.start() : b.end();
            if (resizing[0]) {
                LocalDate newEnd = end.plusDays(days);
                if (newEnd.isBefore(b.start())) {
                    newEnd = b.start();
                }
                onMove.moved(b.id(), b.start(), newEnd);
            } else {
                onMove.moved(b.id(), b.start().plusDays(days), end.plusDays(days));
            }
            e.consume();
        });
    }

    private Node arrow(Bar from, int fromRow, Bar to, int toRow) {
        if (from.start() == null || to.start() == null) {
            return null;
        }
        LocalDate fromEnd = from.end() == null ? from.start() : from.end();
        double x1 = from.milestone() ? x(from.start()) + scale.px / 2 + 8 : x(fromEnd) + scale.px;
        double y1 = HEADER + fromRow * ROW + ROW / 2;
        double x2 = to.milestone() ? x(to.start()) + scale.px / 2 - 8 : x(to.start());
        double y2 = HEADER + toRow * ROW + ROW / 2;
        Path p = new Path();
        p.getStyleClass().add("gantt-link");
        double elbow = Math.max(x1 + 8, Math.min(x2 - 8, x1 + 8));
        p.getElements().add(new MoveTo(x1, y1));
        p.getElements().add(new LineTo(elbow, y1));
        if (x2 - 8 >= elbow) {
            p.getElements().add(new LineTo(elbow, y2));
            p.getElements().add(new LineTo(x2, y2));
        } else {
            double mid = y1 + (y2 > y1 ? ROW / 2 : -ROW / 2);
            p.getElements().add(new LineTo(elbow, mid));
            p.getElements().add(new LineTo(x2 - 10, mid));
            p.getElements().add(new LineTo(x2 - 10, y2));
            p.getElements().add(new LineTo(x2, y2));
        }
        Polygon head = new Polygon(x2, y2, x2 - 6, y2 - 4, x2 - 6, y2 + 4);
        head.getStyleClass().add("gantt-link-head");
        if (showCritical && from.critical() && to.critical()) {
            p.getStyleClass().add("critical");
            head.getStyleClass().add("critical");
        }
        return new Group(p, head);
    }
}
