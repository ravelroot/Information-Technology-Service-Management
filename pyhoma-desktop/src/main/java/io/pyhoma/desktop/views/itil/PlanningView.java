package io.pyhoma.desktop.views.itil;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.shell.View;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.PageHeader;
import io.pyhoma.desktop.ui.components.PickerField;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.itil.ItilTask;
import io.pyhoma.domain.model.itil.TaskStatus;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;

public class PlanningView extends View {

    private static final int FIRST_HOUR = 7;
    private static final int LAST_HOUR = 20;
    private static final double HOUR = 46;
    private LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
    private final GridPane grid = new GridPane();
    private final Label range = Ui.label("", "week-range");
    private final PickerField<Long> technician = PickerField.compact(() -> {
        List<Option<Long>> list = new ArrayList<>();
        for (User u : App.lookups().technicians()) {
            list.add(new Option<>(u.getId(), u.getFullName()));
        }
        return list;
    }, null);

    @Override
    public String title() {
        return I18n.t("nav.PLANNING");
    }

    @Override
    protected Node build() {
        PageHeader header = new PageHeader(I18n.t("nav.PLANNING"), I18n.t("planning.subtitle"));
        technician.setValue(App.session().userId());
        technician.onChange(v -> onShow());
        HBox toolbar = Ui.row(8,
                Ui.iconButton("chevron-left", I18n.t("planning.previous"), () -> move(-1)),
                Ui.button(I18n.t("planning.today"), null, () -> {
                    monday = LocalDate.now().with(DayOfWeek.MONDAY);
                    onShow();
                }, "ghost"),
                Ui.iconButton("chevron-right", I18n.t("planning.next"), () -> move(1)),
                range, Ui.spacer(), ItilListView.labeled("filter.assignee", technician));
        toolbar.getStyleClass().add("toolbar");
        grid.getStyleClass().add("planning-grid");
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("table-panel");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox root = new VBox(16, header, toolbar, scroll);
        root.getStyleClass().add("page");
        return root;
    }

    private void move(int weeks) {
        monday = monday.plusWeeks(weeks);
        onShow();
    }

    @Override
    public void onShow() {
        LocalDateTime from = monday.atStartOfDay();
        LocalDateTime to = monday.plusDays(7).atStartOfDay();
        DateTimeFormatter f = I18n.dateFormatter();
        range.setText(f.format(monday) + " – " + f.format(monday.plusDays(6)));
        Long tech = technician.getValue();
        Async.run(() -> App.services().itilActivity().plannedTasks(tech, from, to), this::draw);
    }

    private void draw(List<ItilTask> tasks) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        javafx.scene.layout.ColumnConstraints hours = new javafx.scene.layout.ColumnConstraints(56);
        grid.getColumnConstraints().add(hours);
        for (int d = 0; d < 7; d++) {
            javafx.scene.layout.ColumnConstraints c = new javafx.scene.layout.ColumnConstraints();
            c.setHgrow(Priority.ALWAYS);
            c.setPercentWidth(-1);
            c.setMinWidth(110);
            grid.getColumnConstraints().add(c);
        }
        VBox hourLabels = new VBox();
        for (int h = FIRST_HOUR; h < LAST_HOUR; h++) {
            Label l = Ui.label(String.format("%02d:00", h), "hour-label");
            l.setMinHeight(HOUR);
            l.setPrefHeight(HOUR);
            l.setAlignment(Pos.TOP_RIGHT);
            hourLabels.getChildren().add(l);
        }
        grid.add(new Pane(), 0, 0);
        grid.add(hourLabels, 0, 1);
        LocalDate today = LocalDate.now();
        for (int d = 0; d < 7; d++) {
            LocalDate day = monday.plusDays(d);
            String name = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, I18n.locale());
            Label head = Ui.label(name + " " + day.getDayOfMonth(), "day-head");
            if (day.equals(today)) {
                head.getStyleClass().add("today");
            }
            head.setMaxWidth(Double.MAX_VALUE);
            grid.add(head, d + 1, 0);
            Pane column = new Pane();
            column.getStyleClass().add(day.getDayOfWeek().getValue() >= 6 ? "day-col-weekend" : "day-col");
            column.setPrefHeight(HOUR * (LAST_HOUR - FIRST_HOUR));
            for (int h = 0; h < LAST_HOUR - FIRST_HOUR; h++) {
                Line line = new Line();
                line.getStyleClass().add("hour-line");
                line.setStartY(h * HOUR);
                line.setEndY(h * HOUR);
                line.endXProperty().bind(column.widthProperty());
                column.getChildren().add(line);
            }
            for (ItilTask t : tasks) {
                if (t.getPlannedStartAt() != null && t.getPlannedStartAt().toLocalDate().equals(day)) {
                    column.getChildren().add(block(t, column));
                }
            }
            grid.add(column, d + 1, 1);
        }
    }

    private Node block(ItilTask t, Pane column) {
        LocalTime start = t.getPlannedStartAt().toLocalTime();
        LocalDateTime endAt = t.getPlannedEndAt() != null ? t.getPlannedEndAt() : t.getPlannedStartAt().plusMinutes(
                Math.max(30, t.getDurationMinutes()));
        double y = Math.max(0, (start.getHour() - FIRST_HOUR + start.getMinute() / 60.0) * HOUR);
        double h = Math.max(24, Duration.between(t.getPlannedStartAt(), endAt).toMinutes() / 60.0 * HOUR);
        Label title = Ui.label(I18n.t("type." + t.getItemType().entityClass().getSimpleName()) + " #" + t.getItemId(), "block-ref");
        Label text = Ui.label(io.pyhoma.domain.support.Texts.abbreviate(t.getContent(), 80), "block-text");
        text.setWrapText(true);
        VBox b = new VBox(2, title, text);
        b.getStyleClass().addAll("plan-block", t.getStatus() == TaskStatus.DONE ? "done" : "todo");
        b.setLayoutY(y);
        b.setLayoutX(4);
        b.prefWidthProperty().bind(column.widthProperty().subtract(8));
        b.setPrefHeight(h);
        b.setMaxHeight(h);
        Tooltip.install(b, new Tooltip(I18n.dateTime(t.getPlannedStartAt()) + "\n" + App.lookups().user(t.getTechnicianId())
                + "\n" + t.getContent()));
        b.setOnMouseClicked(e -> App.nav().openItem(t.getItemType().entityClass().getSimpleName(), t.getItemId()));
        return b;
    }
}
