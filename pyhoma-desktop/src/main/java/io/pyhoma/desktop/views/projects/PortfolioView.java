package io.pyhoma.desktop.views.projects;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.shell.View;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.GanttChart;
import io.pyhoma.desktop.ui.components.PageHeader;
import io.pyhoma.desktop.ui.components.Segmented;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.project.Project;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PortfolioView extends View {

    private final GanttChart gantt = new GanttChart();

    @Override
    public String title() {
        return I18n.t("nav.PORTFOLIO");
    }

    @Override
    protected Node build() {
        PageHeader header = new PageHeader(I18n.t("nav.PORTFOLIO"), I18n.t("portfolio.subtitle"))
                .actions(Ui.button(I18n.t("gantt.today"), "crosshair", gantt::scrollToToday, "ghost"));
        gantt.setScale(GanttChart.Scale.WEEK);
        gantt.onOpen(id -> App.nav().open(new ProjectDetailView(id)));
        Segmented<GanttChart.Scale> zoom = new Segmented<>(List.of(GanttChart.Scale.values()),
                s -> I18n.t("gantt.scale." + s.name()), GanttChart.Scale.WEEK, gantt::setScale);
        HBox toolbar = Ui.row(8, zoom, Ui.spacer());
        toolbar.getStyleClass().add("toolbar");
        VBox panel = new VBox(gantt);
        panel.getStyleClass().add("table-panel");
        VBox.setVgrow(gantt, Priority.ALWAYS);
        VBox.setVgrow(panel, Priority.ALWAYS);
        VBox root = new VBox(16, header, toolbar, panel);
        root.getStyleClass().add("page");
        return root;
    }

    @Override
    public void onShow() {
        Async.run(() -> App.services().projects().portfolio(), projects -> {
            List<GanttChart.Bar> bars = new ArrayList<>();
            for (Project p : projects) {
                boolean late = p.getProgress() < 100 && p.getPlanEnd() != null && p.getPlanEnd().isBefore(LocalDate.now());
                bars.add(new GanttChart.Bar(p.getId(), p.getName(), App.lookups().dd(DropdownKind.PROJECT_STATE, p.getStateId()),
                        p.getPlanStart(), p.getPlanEnd(), p.getProgress(), false, p.getColor(), 0, false, late, false));
            }
            gantt.setData(bars, List.of());
            gantt.scrollToToday();
        });
    }
}
