package io.pyhoma.desktop.views;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.shell.View;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.PageHeader;
import io.pyhoma.domain.service.ReportService.ChartKind;
import io.pyhoma.domain.service.ReportService.Report;
import io.pyhoma.domain.service.ReportService.ReportKind;
import io.pyhoma.domain.support.Csv;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class ReportsView extends View {

    private final ListView<ReportKind> list = new ListView<>();
    private final StackPane chartHolder = new StackPane();
    private final DataTable<List<String>> table = new DataTable<>();
    private final DatePicker from = new DatePicker(LocalDate.now().minusMonths(11).withDayOfMonth(1));
    private final DatePicker to = new DatePicker(LocalDate.now());
    private final PageHeader header = new PageHeader(I18n.t("nav.REPORTS"), I18n.t("reports.subtitle"));
    private Report current;

    @Override
    public String title() {
        return I18n.t("nav.REPORTS");
    }

    @Override
    protected Node build() {
        header.actions(Ui.button(I18n.t("list.export"), "download", this::export));
        list.setItems(FXCollections.observableArrayList(ReportKind.values()));
        list.getStyleClass().add("report-list");
        list.setPrefWidth(270);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ReportKind item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                VBox v = new VBox(2, Ui.label(I18n.t("report." + item.name()), "report-name"),
                        Ui.label(I18n.t("report." + item.name() + ".hint"), "muted"));
                setGraphic(v);
                setText(null);
            }
        });
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> run());
        HBox range = Ui.row(8, Icons.of("calendar", 15), Ui.label(I18n.t("reports.period"), "filter-label"), from, Ui.label("–", "muted"), to,
                Ui.button(I18n.t("reports.apply"), "play", this::run, "ghost"));
        range.getStyleClass().add("toolbar");
        chartHolder.getStyleClass().add("report-chart");
        chartHolder.setMinHeight(300);
        table.setPrefHeight(300);
        VBox content = new VBox(14, range, Ui.panel(null, chartHolder), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox body = new HBox(16, list, content);
        HBox.setHgrow(content, Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox root = new VBox(16, header, body);
        root.getStyleClass().add("page");
        return root;
    }

    @Override
    public void onShow() {
        if (list.getSelectionModel().isEmpty()) {
            list.getSelectionModel().selectFirst();
        } else {
            run();
        }
    }

    private void run() {
        ReportKind kind = list.getSelectionModel().getSelectedItem();
        if (kind == null) {
            return;
        }
        LocalDate f = from.getValue();
        LocalDate t = to.getValue();
        Async.run(() -> App.services().reports().run(kind, f, t), this::render);
    }

    private static String text(String raw) {
        if (raw == null) {
            return "";
        }
        if (raw.startsWith("enum.") || raw.startsWith("report.value.")) {
            return I18n.t(raw);
        }
        return raw;
    }

    private void render(Report r) {
        current = r;
        header.setTitle(I18n.t("report." + r.kind().name()));
        table.getColumns().clear();
        for (int i = 0; i < r.columns().size(); i++) {
            int idx = i;
            table.col(I18n.t("report.col." + r.columns().get(i)), 140, row -> text(row.get(idx)));
        }
        table.placeholder(new EmptyState("chart-column", I18n.t("reports.empty"), null, null));
        table.setRows(r.rows());
        chartHolder.getChildren().setAll(chart(r));
    }

    private Node chart(Report r) {
        if (r.chart() == ChartKind.NONE || r.categories().isEmpty()) {
            return new EmptyState("list", I18n.t("reports.tableOnly"), null, null);
        }
        List<String> cats = r.categories().stream().map(ReportsView::text).toList();
        if (r.chart() == ChartKind.PIE) {
            PieChart pie = new PieChart();
            List<Number> values = r.series().values().iterator().next();
            for (int i = 0; i < cats.size(); i++) {
                pie.getData().add(new PieChart.Data(cats.get(i) + " (" + values.get(i) + ")", values.get(i).doubleValue()));
            }
            pie.setAnimated(false);
            pie.setLegendSide(javafx.geometry.Side.RIGHT);
            pie.setLabelsVisible(false);
            return pie;
        }
        XYChart<String, Number> chart = switch (r.chart()) {
            case LINE -> new LineChart<>(new CategoryAxis(), new NumberAxis());
            case STACKED_BAR -> new StackedBarChart<>(new CategoryAxis(), new NumberAxis());
            default -> new BarChart<>(new CategoryAxis(), new NumberAxis());
        };
        chart.setAnimated(false);
        if (chart instanceof LineChart<String, Number> lc) {
            lc.setCreateSymbols(true);
        }
        if (chart instanceof BarChart<String, Number> bc) {
            bc.setCategoryGap(Math.max(4, 40 - cats.size() * 2));
        }
        for (Map.Entry<String, List<Number>> e : r.series().entrySet()) {
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName(I18n.t(e.getKey()));
            for (int i = 0; i < cats.size(); i++) {
                s.getData().add(new XYChart.Data<>(cats.get(i), e.getValue().get(i)));
            }
            chart.getData().add(s);
        }
        chart.setLegendVisible(r.series().size() > 1);
        HBox.setHgrow(chart, Priority.ALWAYS);
        return chart;
    }

    private void export() {
        if (current == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(current.kind().name().toLowerCase() + ".csv");
        File file = chooser.showSaveDialog(Dialogs.owner());
        if (file == null) {
            return;
        }
        try {
            List<String> header = current.columns().stream().map(c -> I18n.t("report.col." + c)).toList();
            List<List<String>> rows = new ArrayList<>();
            for (List<String> row : current.rows()) {
                rows.add(row.stream().map(ReportsView::text).toList());
            }
            Files.writeString(file.toPath(), "﻿" + Csv.write(header, rows, I18n.isPortuguese() ? ';' : ','), StandardCharsets.UTF_8);
            Toasts.success(I18n.t("list.exported", rows.size(), file.getName()));
        } catch (Exception e) {
            Dialogs.error(e);
        }
    }

}
