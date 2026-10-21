package io.pyhoma.desktop.views.assets;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.views.common.ListPage;
import io.pyhoma.domain.model.asset.Software;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.security.Right;
import javafx.scene.Node;

public class SoftwareView extends ListPage<Software> {

    @Override
    protected String titleText() {
        return I18n.t("nav.SOFTWARE");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("software.subtitle");
    }

    @Override
    protected void columns(DataTable<Software> t) {
        t.col(I18n.t("field.name"), 280, Software::getName)
                .col(I18n.t("field.publisherId"), 180, s -> App.lookups().dd(DropdownKind.MANUFACTURER, s.getPublisherId()))
                .col(I18n.t("field.categoryId"), 180, s -> App.lookups().dd(DropdownKind.SOFTWARE_CATEGORY, s.getCategoryId()))
                .col(I18n.t("field.orgUnitId"), 200, s -> App.lookups().orgUnit(s.getOrgUnitId()));
    }

    @Override
    protected PageResult<Software> load(String text, int page, int size) {
        return App.services().software().search(text, false, page, size);
    }

    @Override
    protected Node[] actions() {
        if (!App.can(Module.SOFTWARE, Right.CREATE)) {
            return new Node[0];
        }
        return new Node[] {Ui.primary(I18n.t("software.new"), "plus", () -> App.nav().open(new SoftwareDetailView(null)))};
    }

    @Override
    protected void open(Software row) {
        App.nav().open(new SoftwareDetailView(row.getId()));
    }
}
