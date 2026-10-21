package io.pyhoma.desktop.views.projects;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.views.common.ListPage;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.project.Project;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.security.Right;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;

public class ProjectsView extends ListPage<Project> {

    @Override
    protected String titleText() {
        return I18n.t("nav.PROJECTS");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("projects.subtitle");
    }

    @Override
    protected void columns(DataTable<Project> t) {
        t.col(I18n.t("field.code"), 90, "mono", Project::getCode)
                .col(I18n.t("field.name"), 280, Project::getName)
                .node(I18n.t("field.stateId"), 140, p -> Badge.colored(App.lookups().dd(DropdownKind.PROJECT_STATE, p.getStateId()),
                        App.lookups().colors(DropdownKind.PROJECT_STATE).get(p.getStateId())),
                        p -> App.lookups().dd(DropdownKind.PROJECT_STATE, p.getStateId()))
                .col(I18n.t("field.managerId"), 160, p -> App.lookups().user(p.getManagerId()))
                .col(I18n.t("field.planStart"), 110, p -> I18n.date(p.getPlanStart()))
                .col(I18n.t("field.planEnd"), 110, p -> I18n.date(p.getPlanEnd()))
                .node(I18n.t("field.progress"), 160, ProjectsView::progress, p -> p.getProgress() + "%");
    }

    static Node progress(Project p) {
        ProgressBar bar = new ProgressBar(p.getProgress() / 100.0);
        bar.getStyleClass().add("thin-progress");
        bar.setPrefWidth(100);
        HBox h = new HBox(8, bar, Ui.label(p.getProgress() + "%", "muted"));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    @Override
    protected PageResult<Project> load(String text, int page, int size) {
        return App.services().projects().search(text, null, false, page, size);
    }

    @Override
    protected Node[] actions() {
        if (!App.can(Module.PROJECT, Right.CREATE)) {
            return new Node[0];
        }
        return new Node[] {Ui.primary(I18n.t("projects.new"), "plus", () -> App.nav().open(new ProjectDetailView(null)))};
    }

    @Override
    protected void open(Project row) {
        App.nav().open(new ProjectDetailView(row.getId()));
    }
}
