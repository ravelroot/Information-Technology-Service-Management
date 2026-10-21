package io.pyhoma.desktop.views.assets;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.views.common.ListPage;
import io.pyhoma.domain.model.asset.Contract;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.ContractService;
import java.time.LocalDate;
import javafx.scene.Node;

public class ContractsView extends ListPage<Contract> {

    @Override
    protected String titleText() {
        return I18n.t("nav.CONTRACTS");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("contracts.subtitle");
    }

    @Override
    protected void columns(DataTable<Contract> t) {
        t.col(I18n.t("field.name"), 280, Contract::getName)
                .col(I18n.t("field.contractNumber"), 120, "mono", Contract::getContractNumber)
                .col(I18n.t("field.typeId"), 140, c -> App.lookups().dd(DropdownKind.CONTRACT_TYPE, c.getTypeId()))
                .col(I18n.t("field.supplierId"), 180, c -> App.lookups().supplier(c.getSupplierId()))
                .col(I18n.t("field.startDate"), 110, c -> I18n.date(c.getStartDate()))
                .node(I18n.t("field.endDate"), 150, ContractsView::endCell, c -> I18n.date(ContractService.endDate(c)))
                .col(I18n.t("field.renewal"), 120, c -> I18n.e(c.getRenewal()))
                .col(I18n.t("field.cost"), 120, "num", c -> c.getCost() == null ? "" : String.format(I18n.locale(), "%,.2f", c.getCost()));
    }

    static Node endCell(Contract c) {
        LocalDate end = ContractService.endDate(c);
        if (end == null) {
            return Ui.label("—", "muted");
        }
        LocalDate today = LocalDate.now();
        String tone = end.isBefore(today) ? "gray" : end.isBefore(today.plusDays(60)) ? "amber" : "green";
        return Badge.of(I18n.date(end), tone);
    }

    @Override
    protected PageResult<Contract> load(String text, int page, int size) {
        return App.services().contracts().search(text, false, page, size);
    }

    @Override
    protected Node[] actions() {
        if (!App.can(Module.CONTRACT, Right.CREATE)) {
            return new Node[0];
        }
        return new Node[] {Ui.primary(I18n.t("contracts.new"), "plus", () -> App.nav().open(new ContractDetailView(null)))};
    }

    @Override
    protected void open(Contract row) {
        App.nav().open(new ContractDetailView(row.getId()));
    }
}
