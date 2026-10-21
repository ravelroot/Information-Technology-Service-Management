package io.pyhoma.desktop.views;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.shell.Route;
import io.pyhoma.desktop.shell.View;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.Charts;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.PageHeader;
import io.pyhoma.desktop.views.itil.ItilDetailView;
import io.pyhoma.domain.model.asset.AssetType;
import io.pyhoma.domain.model.itil.ItilApproval;
import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.service.DashboardService.DayPoint;
import io.pyhoma.domain.service.DashboardService.Overview;
import io.pyhoma.domain.service.ItilSearch;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

public class DashboardView extends View {

    private final VBox root = new VBox(18);
    private final HBox strip = new HBox();
    private final GridPane grid = new GridPane();

    @Override
    public String title() {
        return I18n.t("nav.DASHBOARD");
    }

    @Override
    protected Node build() {
        PageHeader header = new PageHeader(I18n.t("dashboard.greeting", App.session().fullName().split(" ")[0]),
                I18n.t("dashboard.subtitle", I18n.date(java.time.LocalDate.now())))
                .actions(Ui.iconButton("refresh-cw", I18n.t("action.refresh"), this::onShow),
                        Ui.primary(I18n.t("itil.ticket.new"), "plus", () -> io.pyhoma.desktop.views.itil.ItilQuickForm.open(ItilType.TICKET, io.pyhoma.domain.model.itil.ItilStatus.NEW,
                                io.pyhoma.desktop.views.itil.ItilQuickForm::openCreated)));
        strip.getStyleClass().add("kpi-strip");
        grid.getStyleClass().add("dash-grid");
        grid.setHgap(16);
        grid.setVgap(16);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100 / 3.0);
            grid.getColumnConstraints().add(c);
        }
        root.getChildren().addAll(header, strip, grid);
        root.getStyleClass().add("page");
        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        return sp;
    }

    private record Data(Overview overview, List<DayPoint> trend, Map<ItilStatus, Long> byStatus, Map<Integer, Long> byPriority,
                        Map<String, Long> technicians, Map<String, Long> categories, Map<AssetType, Long> assets,
                        List<Ticket> myQueue, List<ItilApproval> approvals) {
    }

    @Override
    public void onShow() {
        Async.run(() -> {
            var d = App.services().dashboard();
            List<Ticket> queue = App.services().tickets().search(ItilSearch.create().view(ItilSearch.View.MINE_ASSIGNED)
                    .sort("priority", false).page(0, 7)).items();
            return new Data(d.overview(), d.trend(30), d.ticketsByStatus(), d.ticketsByPriority(), d.technicianLoad(8),
                    d.openByCategory(8), d.assetsByType(), queue, App.services().itilActivity().myPendingApprovals());
        }, this::render);
    }

    private void render(Data d) {
        Overview o = d.overview();
        strip.getChildren().clear();
        addKpi(I18n.t("kpi.open"), o.openTickets(), "blue", Route.TICKETS);
        addKpi(I18n.t("kpi.unassigned"), o.unassigned(), o.unassigned() > 0 ? "amber" : "green", Route.TICKETS);
        addKpi(I18n.t("kpi.overdue"), o.overdue(), o.overdue() > 0 ? "red" : "green", Route.TICKETS);
        addKpi(I18n.t("kpi.atRisk"), o.atRisk(), o.atRisk() > 0 ? "amber" : "green", Route.TICKETS);
        addKpi(I18n.t("kpi.solvedToday"), o.solvedToday(), "green", null);
        addKpiText(I18n.t("kpi.avgResolution"), String.format(I18n.locale(), "%.1f h", o.avgResolutionHours()), "slate");
        addKpiText(I18n.t("kpi.satisfaction"), o.satisfaction() == 0 ? "—" : String.format(I18n.locale(), "%.1f ★",
                o.satisfaction()), "teal");

        grid.getChildren().clear();
        List<Charts.Mode> lineModes = List.of(Charts.Mode.LINE, Charts.Mode.COLUMNS);
        List<Charts.Mode> modes = List.of(Charts.Mode.BARS, Charts.Mode.COLUMNS, Charts.Mode.DONUT);
        grid.add(Charts.switchable(I18n.t("dash.trend"), "trend", lineModes, Charts.Mode.LINE, m -> trendChart(d.trend(), m)),
                0, 0, 2, 1);
        grid.add(Charts.switchable(I18n.t("dash.byStatus"), "status", modes, Charts.Mode.BARS,
                m -> chart(statusSlices(d.byStatus()), m, "dash.noData")), 2, 0);
        grid.add(Ui.panel(I18n.t("dash.myQueue"), myQueue(d.myQueue()),
                Ui.ghost(I18n.t("dash.seeAll"), null, () -> App.nav().go(Route.TICKETS))), 0, 1);
        grid.add(Charts.switchable(I18n.t("dash.technicians"), "technicians", modes, Charts.Mode.BARS,
                m -> chart(slices(d.technicians(), "bar-teal", I18n.t("itil.unassigned")), m, "dash.noData")), 1, 1);
        grid.add(Charts.switchable(I18n.t("dash.byPriority"), "priority", modes, Charts.Mode.BARS,
                m -> chart(prioritySlices(d.byPriority()), m, "dash.noData")), 2, 1);
        grid.add(Charts.switchable(I18n.t("dash.categories"), "categories", modes, Charts.Mode.BARS,
                m -> chart(slices(d.categories(), "bar-blue", I18n.t("dash.noCategory")), m, "dash.noData")), 0, 2);
        grid.add(Charts.switchable(I18n.t("dash.assets"), "assets", modes, Charts.Mode.DONUT,
                m -> chart(assetSlices(d.assets()), m, "dash.noData")), 1, 2);
        grid.add(Ui.panel(I18n.t("dash.attention"), attention(o, d.approvals())), 2, 2);
    }

    private void addKpi(String label, long value, String tone, Route route) {
        addKpiText(label, value < 0 ? "—" : String.valueOf(value), tone);
        Node last = strip.getChildren().get(strip.getChildren().size() - 1);
        if (value >= 0 && last.lookup(".kpi-value") instanceof Label v) {
            io.pyhoma.desktop.ui.Motion.countUp(v, value);
        }
        if (route != null) {
            last.getStyleClass().add("clickable");
            last.setOnMouseClicked(e -> App.nav().go(route));
            io.pyhoma.desktop.ui.Motion.hoverLift(last);
        }
    }

    private void addKpiText(String label, String value, String tone) {
        Circle led = new Circle(4);
        led.getStyleClass().addAll("led", "tone-" + tone);
        Label l = Ui.label(label, "kpi-label");
        HBox top = new HBox(7, led, l);
        top.setAlignment(Pos.CENTER_LEFT);
        Label v = Ui.label(value, "kpi-value");
        VBox box = new VBox(6, top, v);
        box.getStyleClass().addAll("kpi", "tone-" + tone);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        strip.getChildren().add(box);
    }

    private Node trendChart(List<DayPoint> points, Charts.Mode mode) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setMinorTickVisible(false);
        y.setForceZeroInRange(true);
        y.setTickLabelFormatter(Ui.integerAxisFormatter());
        XYChart.Series<String, Number> opened = new XYChart.Series<>();
        opened.setName(I18n.t("report.series.opened"));
        XYChart.Series<String, Number> solved = new XYChart.Series<>();
        solved.setName(I18n.t("report.series.solved"));
        DateTimeFormatter f = DateTimeFormatter.ofPattern(I18n.isPortuguese() ? "dd/MM" : "MM/dd");
        for (DayPoint p : points) {
            String day = f.format(p.day());
            opened.getData().add(new XYChart.Data<>(day, p.opened()));
            solved.getData().add(new XYChart.Data<>(day, p.solved()));
        }
        XYChart<String, Number> chart;
        if (mode == Charts.Mode.COLUMNS) {
            BarChart<String, Number> bars = new BarChart<>(x, y);
            bars.setCategoryGap(5);
            bars.setBarGap(1);
            chart = bars;
        } else {
            LineChart<String, Number> line = new LineChart<>(x, y);
            line.setCreateSymbols(false);
            chart = line;
        }
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.setPrefHeight(260);
        chart.getData().add(opened);
        chart.getData().add(solved);
        chart.getStyleClass().add("trend-chart");
        return chart;
    }

    private static Node chart(List<Charts.Slice> data, Charts.Mode mode, String emptyKey) {
        if (data.isEmpty() || data.stream().allMatch(sl -> sl.value() == 0) && mode == Charts.Mode.DONUT) {
            return new EmptyState("chart-pie", I18n.t(emptyKey), null, null);
        }
        return switch (mode) {
            case COLUMNS -> Charts.columns(data, 150);
            case DONUT -> {
                List<Charts.Slice> distinct = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    Charts.Slice sl = data.get(i);
                    if (sl.value() > 0) {
                        distinct.add(new Charts.Slice(sl.label(), sl.value(), Charts.seriesStyle(distinct.size())));
                    }
                }
                yield Charts.donut(distinct, I18n.t("chart.total"));
            }
            default -> Charts.bars(data);
        };
    }

    private static List<Charts.Slice> statusSlices(Map<ItilStatus, Long> byStatus) {
        List<Charts.Slice> list = new ArrayList<>();
        byStatus.forEach((k, v) -> list.add(new Charts.Slice(I18n.e(k), v, "tone-" + Badge.tone(k))));
        return list;
    }

    private static List<Charts.Slice> prioritySlices(Map<Integer, Long> byPriority) {
        List<Integer> keys = new ArrayList<>(byPriority.keySet());
        java.util.Collections.reverse(keys);
        List<Charts.Slice> list = new ArrayList<>();
        for (Integer p : keys) {
            list.add(new Charts.Slice(I18n.priority(p), byPriority.get(p), "tone-" + Badge.priorityTone(p)));
        }
        return list;
    }

    private static List<Charts.Slice> slices(Map<String, Long> data, String style, String emptyLabel) {
        List<Charts.Slice> list = new ArrayList<>();
        data.forEach((k, v) -> list.add(new Charts.Slice("—".equals(k) ? emptyLabel : k, v, style)));
        return list;
    }

    private static List<Charts.Slice> assetSlices(Map<AssetType, Long> byType) {
        List<Charts.Slice> list = new ArrayList<>();
        byType.entrySet().stream().sorted(Map.Entry.<AssetType, Long>comparingByValue().reversed()).limit(8)
                .forEach(e -> list.add(new Charts.Slice(I18n.e(e.getKey()), e.getValue(), "bar-blue")));
        return list;
    }

    private Node myQueue(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return new EmptyState("circle-check", I18n.t("dash.queueEmpty"), I18n.t("dash.queueEmptyHint"), null);
        }
        VBox list = new VBox(2);
        for (Ticket t : tickets) {
            Label title = Ui.label(t.getTitle(), "queue-title");
            title.setMaxWidth(260);
            Label meta = Ui.label("#" + t.getId() + "  •  " + I18n.relative(t.getOpenedAt()), "muted");
            VBox texts = new VBox(1, title, meta);
            HBox row = new HBox(10, texts, Ui.spacer(), Badge.priority(t.getPriority()));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("queue-row");
            row.setOnMouseClicked(e -> App.nav().open(new ItilDetailView(ItilType.TICKET, t.getId())));
            list.getChildren().add(row);
        }
        return list;
    }

    private Node attention(Overview o, List<ItilApproval> approvals) {
        VBox list = new VBox(8);
        if (!approvals.isEmpty()) {
            list.getChildren().add(alert("shield-check", I18n.t("dash.alert.approvals", approvals.size()), "amber",
                    () -> App.nav().openItem(approvals.get(0).getItemType().entityClass().getSimpleName(), approvals.get(0).getItemId())));
        }
        if (o.warrantyExpiring() > 0) {
            list.getChildren().add(alert("monitor", I18n.t("dash.alert.warranty", o.warrantyExpiring()), "amber",
                    () -> App.nav().go(Route.ASSETS)));
        }
        if (o.contractsExpiring() > 0) {
            list.getChildren().add(alert("file-signature", I18n.t("dash.alert.contracts", o.contractsExpiring()), "amber",
                    () -> App.nav().go(Route.CONTRACTS)));
        }
        if (o.licensesOverUsed() > 0) {
            list.getChildren().add(alert("key-round", I18n.t("dash.alert.licenses", o.licensesOverUsed()), "red",
                    () -> App.nav().go(Route.SOFTWARE)));
        }
        if (o.lateTasks() > 0) {
            list.getChildren().add(alert("chart-gantt", I18n.t("dash.alert.lateTasks", o.lateTasks()), "red",
                    () -> App.nav().go(Route.PORTFOLIO)));
        }
        if (o.openProblems() > 0) {
            list.getChildren().add(alert("triangle-alert", I18n.t("dash.alert.problems", o.openProblems()), "violet",
                    () -> App.nav().go(Route.PROBLEMS)));
        }
        if (o.openChanges() > 0) {
            list.getChildren().add(alert("git-pull-request-arrow", I18n.t("dash.alert.changes", o.openChanges()), "blue",
                    () -> App.nav().go(Route.CHANGES)));
        }
        if (list.getChildren().isEmpty()) {
            return new EmptyState("circle-check", I18n.t("dash.allClear"), null, null);
        }
        return list;
    }

    private Node alert(String icon, String text, String tone, Runnable action) {
        Label l = new Label(text);
        l.setWrapText(true);
        HBox row = new HBox(10, Icons.of(icon, 16), l, Ui.spacer(), Icons.of("chevron-right", 14));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("alert-row", "tone-" + tone);
        row.setOnMouseClicked(e -> action.run());
        return row;
    }

    static BarChart<String, Number> barChart() {
        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        return chart;
    }
}
