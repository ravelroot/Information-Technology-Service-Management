package io.pyhoma.desktop.views.assets;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.shell.View;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.desktop.ui.components.PageHeader;
import io.pyhoma.desktop.ui.components.PickerField;
import io.pyhoma.desktop.views.common.DocumentsPanel;
import io.pyhoma.desktop.views.common.HistoryPanel;
import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.asset.Contract;
import io.pyhoma.domain.model.asset.RenewalType;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.ContractService;
import io.pyhoma.domain.support.ValidationException;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ContractDetailView extends View {

    private Long id;
    private Contract contract;
    private final PageHeader header = new PageHeader("", null);
    private final TabPane tabs = new TabPane();
    private Form form;

    public ContractDetailView(Long id) {
        this.id = id;
    }

    @Override
    public String title() {
        return contract == null || contract.getName() == null ? I18n.t("contracts.new") : contract.getName();
    }

    private boolean canEdit() {
        return App.can(Module.CONTRACT, id == null ? Right.CREATE : Right.UPDATE);
    }

    @Override
    protected Node build() {
        header.back(() -> App.nav().back());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("detail-tabs");
        BorderPane root = new BorderPane(tabs);
        root.setTop(header);
        BorderPane.setMargin(tabs, new javafx.geometry.Insets(16, 0, 0, 0));
        root.getStyleClass().add("page");
        return root;
    }

    @Override
    public void onShow() {
        if (id == null) {
            contract = new Contract();
            render();
        } else {
            Async.run(() -> App.services().contracts().get(id), c -> {
                contract = c;
                render();
            });
        }
    }

    private void render() {
        header.setTitle(contract.isNew() ? I18n.t("contracts.new") : contract.getName());
        header.setSubtitle(contract.isNew() ? null : I18n.t("contracts.period", I18n.date(contract.getStartDate()),
                I18n.date(ContractService.endDate(contract)), I18n.date(ContractService.noticeDate(contract))));
        if (!contract.isNew()) {
            header.tags(ContractsView.endCell(contract));
        }
        List<Node> actions = new ArrayList<>();
        if (!contract.isNew() && App.can(Module.CONTRACT, Right.DELETE)) {
            actions.add(Ui.button(I18n.t("action.trash"), "trash-2", () -> {
                if (Dialogs.confirm(I18n.t("action.trash"), I18n.t("confirm.trash"), I18n.t("action.trash"), true)) {
                    Async.exec(() -> App.services().contracts().trash(id, true), () -> App.nav().back());
                }
            }, "ghost"));
        }
        if (canEdit()) {
            actions.add(Ui.primary(I18n.t(contract.isNew() ? "action.create" : "action.save"), "save", this::save));
        }
        header.actions(actions.toArray(Node[]::new));
        Contract c = contract;
        form = new Form(3).readOnly(!canEdit())
                .text("name", c::getName, c::setName, 2, true)
                .code("contractNumber", c::getContractNumber, c::setContractNumber)
                .choice("typeId", () -> Option.of(App.lookups().dropdown(DropdownKind.CONTRACT_TYPE)), c::getTypeId, c::setTypeId, true, false)
                .choice("supplierId", () -> Option.of(App.lookups().suppliers()), c::getSupplierId, c::setSupplierId, true, false)
                .choice("renewal", () -> Option.ofEnum(RenewalType.values()), c::getRenewal, c::setRenewal, false, true)
                .date("startDate", c::getStartDate, c::setStartDate)
                .integer("durationMonths", c::getDurationMonths, c::setDurationMonths)
                .integer("noticeMonths", c::getNoticeMonths, c::setNoticeMonths)
                .decimal("cost", c::getCost, c::setCost)
                .area("notes", c::getNotes, c::setNotes, 4, false);
        form.load();
        ScrollPane general = new ScrollPane(Ui.style(new VBox(form.node()), "tab-body"));
        general.setFitToWidth(true);
        tabs.getTabs().setAll(tab("assets.tab.general", "info", general));
        if (!contract.isNew()) {
            tabs.getTabs().add(tab("contracts.tab.assets", "monitor", assetsTab()));
            DocumentsPanel docs = new DocumentsPanel("Contract", id);
            docs.load();
            tabs.getTabs().add(tab("tab.documents", "paperclip", docs));
            HistoryPanel h = new HistoryPanel("Contract", id);
            h.load();
            tabs.getTabs().add(tab("tab.history", "history", h));
        }
    }

    private static Tab tab(String key, String icon, Node content) {
        Tab t = new Tab(I18n.t(key), content);
        t.setGraphic(Icons.of(icon, 14));
        return t;
    }

    private void save() {
        if (!form.commit()) {
            return;
        }
        Async.run(() -> App.services().contracts().save(contract), c -> {
            id = c.getId();
            contract = c;
            Toasts.success(I18n.t("toast.saved"));
            render();
        }, e -> {
            if (e instanceof ValidationException v && form.showErrors(v)) {
                return;
            }
            Dialogs.error(e);
        });
    }

    private Node assetsTab() {
        DataTable<Asset> table = new DataTable<>();
        table.node(I18n.t("field.name"), 220, AssetsView::nameCell)
                .col(I18n.t("field.assetTag"), 130, "mono", Asset::getAssetTag)
                .col(I18n.t("field.serialNumber"), 150, "mono", Asset::getSerialNumber)
                .col(I18n.t("field.locationId"), 180, a -> App.lookups().dd(DropdownKind.LOCATION, a.getLocationId()));
        table.placeholder(new EmptyState("monitor", I18n.t("contracts.noAssets"), null, null));
        table.onOpen(a -> App.nav().open(new AssetDetailView(a.getId())));
        Runnable reload = () -> Async.run(() -> App.services().contracts().assets(id), table::setRows);
        reload.run();
        HBox actions = Ui.row(8, Ui.spacer());
        if (canEdit()) {
            actions.getChildren().addAll(
                    Ui.button(I18n.t("links.remove"), "unlink", () -> {
                        Asset a = table.selected();
                        if (a != null) {
                            Async.exec(() -> App.services().contracts().removeAsset(id, a.getId()), reload);
                        }
                    }, "ghost"),
                    Ui.primary(I18n.t("links.addAsset"), "plus", () -> {
                        PickerField<Long> picker = AssetDetailView.assetPicker();
                        Dialogs.form(I18n.t("links.addAsset"), picker, I18n.t("action.add"), () -> {
                            if (picker.getValue() == null) {
                                return false;
                            }
                            App.services().contracts().addAsset(id, picker.getValue());
                            reload.run();
                            return true;
                        });
                    }));
        }
        VBox.setVgrow(table, Priority.ALWAYS);
        return Ui.style(new VBox(10, actions, table), "tab-body");
    }
}
