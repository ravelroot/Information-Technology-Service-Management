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
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.desktop.ui.components.PageHeader;
import io.pyhoma.desktop.ui.components.PickerField;
import io.pyhoma.desktop.views.common.DocumentsPanel;
import io.pyhoma.desktop.views.common.HistoryPanel;
import io.pyhoma.desktop.views.itil.ItilDetailView;
import io.pyhoma.desktop.views.itil.ItilListViewHelper;
import io.pyhoma.desktop.views.projects.ProjectDetailViewHelper;
import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.asset.AssetType;
import io.pyhoma.domain.model.asset.Contract;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.AssetService.AssetSearch;
import io.pyhoma.domain.service.ContractService;
import io.pyhoma.domain.service.RelatedItem;
import io.pyhoma.domain.service.SoftwareService.InstallationRow;
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

public class AssetDetailView extends View {

    private Long id;
    private Asset asset;
    private final PageHeader header = new PageHeader("", null);
    private final TabPane tabs = new TabPane();
    private Form form;

    public AssetDetailView(Long id) {
        this.id = id;
    }

    private boolean canEdit() {
        return App.can(Module.ASSET, id == null ? Right.CREATE : Right.UPDATE);
    }

    @Override
    public String title() {
        return asset == null || asset.getName() == null ? I18n.t("assets.new") : asset.getName();
    }

    @Override
    protected Node build() {
        header.back(() -> App.nav().back());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("detail-tabs");
        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(tabs);
        BorderPane.setMargin(tabs, new javafx.geometry.Insets(16, 0, 0, 0));
        root.getStyleClass().add("page");
        return root;
    }

    @Override
    public void onShow() {
        if (id == null) {
            asset = new Asset();
            asset.setAssetType(AssetType.COMPUTER);
            render();
        } else {
            Async.run(() -> App.services().assets().get(id), a -> {
                asset = a;
                render();
            });
        }
    }

    private void render() {
        boolean creating = asset.isNew();
        header.setTitle(creating ? I18n.t("assets.new") : asset.getName());
        header.setSubtitle(creating ? I18n.t("assets.newHint") : I18n.e(asset.getAssetType()) + "  •  "
                + (asset.getAssetTag() == null ? "" : asset.getAssetTag()));
        if (!creating) {
            List<Node> tags = new ArrayList<>();
            tags.add(AssetsView.state(asset));
            if (asset.getWarrantyExpiresOn() != null && asset.getWarrantyExpiresOn().isBefore(java.time.LocalDate.now().plusDays(30))) {
                tags.add(Badge.of(asset.getWarrantyExpiresOn().isBefore(java.time.LocalDate.now()) ? I18n.t("assets.warrantyExpired")
                        : I18n.t("assets.warrantyEnding"), "amber"));
            }
            if (asset.isDeleted()) {
                tags.add(Badge.of(I18n.t("itil.inTrash"), "red"));
            }
            header.tags(tags.toArray(Node[]::new));
        }
        List<Node> actions = new ArrayList<>();
        if (!creating && App.can(Module.TICKET, Right.CREATE)) {
            actions.add(Ui.button(I18n.t("assets.openTicket"), "ticket", this::openTicket));
        }
        if (!creating && App.can(Module.ASSET, Right.CREATE)) {
            actions.add(Ui.button(I18n.t("action.duplicate"), "copy", () -> Async.run(() -> App.services().assets().duplicate(id), a -> {
                Toasts.success(I18n.t("toast.duplicated"));
                App.nav().open(new AssetDetailView(a.getId()));
            })));
        }
        if (!creating && App.can(Module.ASSET, Right.DELETE)) {
            actions.add(Ui.button(I18n.t(asset.isDeleted() ? "action.restore" : "action.trash"), asset.isDeleted() ? "archive-restore"
                    : "trash-2", () -> {
                if (asset.isDeleted()) {
                    Async.exec(() -> App.services().assets().restore(id), this::onShow);
                } else if (Dialogs.confirm(I18n.t("action.trash"), I18n.t("confirm.trash"), I18n.t("action.trash"), true)) {
                    Async.exec(() -> App.services().assets().trash(id), () -> App.nav().back());
                }
            }, "ghost"));
        }
        if (asset.isDeleted() && App.can(Module.ASSET, Right.PURGE)) {
            actions.add(Ui.danger(I18n.t("action.purge"), "trash-2", () -> {
                if (Dialogs.confirm(I18n.t("action.purge"), I18n.t("confirm.purge"), I18n.t("action.purge"), true)) {
                    Async.exec(() -> App.services().assets().purgeAsset(id), () -> App.nav().back());
                }
            }));
        }
        if (canEdit()) {
            actions.add(Ui.primary(I18n.t(creating ? "action.create" : "action.save"), "save", this::save));
        }
        header.actions(actions.toArray(Node[]::new));
        int sel = Math.max(0, tabs.getSelectionModel().getSelectedIndex());
        tabs.getTabs().clear();
        tabs.getTabs().add(tab("assets.tab.general", "info", general()));
        if (!creating) {
            if (App.can(Module.SOFTWARE, Right.READ) && asset.getAssetType().hasHardwareDetails()) {
                tabs.getTabs().add(tab("assets.tab.software", "app-window", softwareTab()));
            }
            tabs.getTabs().add(tab("assets.tab.connections", "network", connectionsTab()));
            if (App.can(Module.CONTRACT, Right.READ)) {
                tabs.getTabs().add(tab("assets.tab.contracts", "file-signature", contractsTab()));
            }
            tabs.getTabs().add(tab("assets.tab.assistance", "ticket", assistanceTab()));
            DocumentsPanel docs = new DocumentsPanel("Asset", id);
            docs.load();
            tabs.getTabs().add(tab("tab.documents", "paperclip", docs));
            HistoryPanel h = new HistoryPanel("Asset", id);
            h.load();
            tabs.getTabs().add(tab("tab.history", "history", h));
        }
        tabs.getSelectionModel().select(Math.min(sel, tabs.getTabs().size() - 1));
    }

    private static Tab tab(String key, String icon, Node content) {
        Tab t = new Tab(I18n.t(key), content);
        t.setGraphic(Icons.of(icon, 14));
        return t;
    }

    private Node general() {
        Asset a = asset;
        form = new Form(3).readOnly(!canEdit());
        form.section(I18n.t("assets.section.identification"))
                .text("name", a::getName, a::setName, 1, true)
                .choice("assetType", () -> Option.ofEnum(AssetType.values()), a::getAssetType, a::setAssetType, false, true)
                .choice("stateId", () -> ProjectDetailViewHelper.colored(DropdownKind.ASSET_STATE), a::getStateId, a::setStateId, true, false)
                .code("assetTag", a::getAssetTag, a::setAssetTag)
                .code("serialNumber", a::getSerialNumber, a::setSerialNumber)
                .choice("orgUnitId", () -> Option.of(App.lookups().orgUnits()), a::getOrgUnitId, a::setOrgUnitId, true, false)
                .choice("manufacturerId", () -> Option.of(App.lookups().dropdown(DropdownKind.MANUFACTURER)), a::getManufacturerId,
                        a::setManufacturerId, true, false)
                .choice("modelId", () -> Option.of(App.lookups().dropdown(DropdownKind.ASSET_MODEL)), a::getModelId, a::setModelId, true, false);
        form.section(I18n.t("assets.section.assignment"))
                .choice("userId", ProjectDetailViewHelper::users, a::getUserId, a::setUserId, true, false)
                .choice("locationId", () -> Option.of(App.lookups().dropdown(DropdownKind.LOCATION)), a::getLocationId, a::setLocationId, true, false)
                .choice("groupId", () -> Option.of(App.lookups().groups()), a::getGroupId, a::setGroupId, true, false)
                .choice("techUserId", ItilListViewHelper::technicians, a::getTechUserId, a::setTechUserId, true, false)
                .choice("techGroupId", () -> Option.of(App.lookups().groups()), a::getTechGroupId, a::setTechGroupId, true, false);
        if (a.getAssetType() == null || a.getAssetType().hasHardwareDetails() || a.getAssetType().isNetworked()) {
            form.section(I18n.t("assets.section.technical"))
                    .choice("operatingSystemId", () -> Option.of(App.lookups().dropdown(DropdownKind.OPERATING_SYSTEM)),
                            a::getOperatingSystemId, a::setOperatingSystemId, true, false)
                    .text("osVersion", a::getOsVersion, a::setOsVersion)
                    .text("cpu", a::getCpu, a::setCpu)
                    .integer("ramMb", a::getRamMb, a::setRamMb)
                    .integer("storageGb", a::getStorageGb, a::setStorageGb)
                    .code("hostname", a::getHostname, a::setHostname)
                    .code("ipAddress", a::getIpAddress, a::setIpAddress)
                    .code("macAddress", a::getMacAddress, a::setMacAddress);
        }
        form.section(I18n.t("assets.section.purchase"))
                .choice("supplierId", () -> Option.of(App.lookups().suppliers()), a::getSupplierId, a::setSupplierId, true, false)
                .date("purchaseDate", a::getPurchaseDate, a::setPurchaseDate)
                .date("warrantyExpiresOn", a::getWarrantyExpiresOn, a::setWarrantyExpiresOn)
                .decimal("purchaseCost", a::getPurchaseCost, a::setPurchaseCost)
                .area("notes", a::getNotes, a::setNotes, 4, false);
        form.load();
        ScrollPane sp = new ScrollPane(Ui.style(new VBox(form.node()), "tab-body"));
        sp.setFitToWidth(true);
        return sp;
    }

    private void save() {
        if (!form.commit()) {
            return;
        }
        Async.run(() -> App.services().assets().save(asset), saved -> {
            id = saved.getId();
            asset = saved;
            Toasts.success(I18n.t("toast.saved"));
            render();
        }, e -> {
            if (e instanceof ValidationException v && form.showErrors(v)) {
                return;
            }
            Dialogs.error(e);
        });
    }

    private void openTicket() {
        ItilDetailView view = ItilDetailView.create(ItilType.TICKET);
        App.nav().open(view);
        Toasts.info(I18n.t("assets.ticketHint"));
        pendingAsset = id;
    }

    static Long pendingAsset;

    private Node softwareTab() {
        DataTable<InstallationRow> table = new DataTable<>();
        table.col(I18n.t("field.softwareId"), 240, InstallationRow::softwareName)
                .col(I18n.t("field.softwareVersion"), 120, r -> r.installation().getSoftwareVersion())
                .col(I18n.t("field.licenseId"), 240, InstallationRow::licenseName)
                .col(I18n.t("field.installedOn"), 120, r -> I18n.date(r.installation().getInstalledOn()));
        table.placeholder(new EmptyState("app-window", I18n.t("assets.noSoftware"), null, null));
        Runnable reload = () -> Async.run(() -> App.services().software().installationsOfAsset(id), table::setRows);
        reload.run();
        HBox actions = Ui.row(8, Ui.spacer());
        if (App.can(Module.SOFTWARE, Right.UPDATE)) {
            actions.getChildren().addAll(
                    Ui.button(I18n.t("software.uninstall"), "x", () -> {
                        InstallationRow r = table.selected();
                        if (r != null) {
                            Async.exec(() -> App.services().software().uninstall(r.installation().getId()), reload);
                        }
                    }, "ghost"),
                    Ui.primary(I18n.t("software.install"), "plus", () -> SoftwareDetailView.installDialog(id, null, reload)));
        }
        VBox.setVgrow(table, Priority.ALWAYS);
        return Ui.style(new VBox(10, actions, table), "tab-body");
    }

    private Node connectionsTab() {
        DataTable<Asset> table = new DataTable<>();
        table.node(I18n.t("field.name"), 220, AssetsView::nameCell)
                .col(I18n.t("field.assetType"), 140, x -> I18n.e(x.getAssetType()))
                .col(I18n.t("field.assetTag"), 130, "mono", Asset::getAssetTag)
                .col(I18n.t("field.serialNumber"), 150, "mono", Asset::getSerialNumber);
        table.placeholder(new EmptyState("network", I18n.t("assets.noConnections"), I18n.t("assets.noConnectionsHint"), null));
        table.onOpen(x -> App.nav().open(new AssetDetailView(x.getId())));
        Runnable reload = () -> Async.run(() -> App.services().assets().connections(id), table::setRows);
        reload.run();
        HBox actions = Ui.row(8, Ui.spacer());
        if (canEdit()) {
            actions.getChildren().addAll(
                    Ui.button(I18n.t("assets.disconnect"), "unlink", () -> {
                        Asset x = table.selected();
                        if (x != null) {
                            Async.exec(() -> App.services().assets().disconnect(id, x.getId()), reload);
                        }
                    }, "ghost"),
                    Ui.primary(I18n.t("assets.connect"), "link", () -> {
                        PickerField<Long> picker = assetPicker();
                        Dialogs.form(I18n.t("assets.connect"), picker, I18n.t("assets.connect"), () -> {
                            if (picker.getValue() == null) {
                                return false;
                            }
                            App.services().assets().connect(id, picker.getValue());
                            reload.run();
                            return true;
                        });
                    }));
        }
        VBox.setVgrow(table, Priority.ALWAYS);
        return Ui.style(new VBox(10, actions, table), "tab-body");
    }

    static PickerField<Long> assetPicker() {
        PickerField<Long> picker = new PickerField<>(() -> {
            List<Option<Long>> list = new ArrayList<>();
            App.services().assets().search(AssetSearch.of(null, null).withSize(5000)).items()
                    .forEach(a -> list.add(new Option<>(a.getId(), a.getName(), a.getAssetTag(), null)));
            return list;
        }, false);
        picker.setPrefWidth(420);
        return picker;
    }

    private Node contractsTab() {
        DataTable<Contract> table = new DataTable<>();
        table.col(I18n.t("field.name"), 260, Contract::getName)
                .col(I18n.t("field.contractNumber"), 120, "mono", Contract::getContractNumber)
                .col(I18n.t("field.supplierId"), 180, c -> App.lookups().supplier(c.getSupplierId()))
                .col(I18n.t("field.endDate"), 120, c -> I18n.date(ContractService.endDate(c)));
        table.placeholder(new EmptyState("file-signature", I18n.t("assets.noContracts"), null, null));
        table.onOpen(c -> App.nav().open(new ContractDetailView(c.getId())));
        Async.run(() -> App.services().contracts().contractsOfAsset(id), table::setRows);
        VBox.setVgrow(table, Priority.ALWAYS);
        return Ui.style(new VBox(10, table), "tab-body");
    }

    private Node assistanceTab() {
        DataTable<RelatedItem> table = new DataTable<>();
        table.col(I18n.t("links.type"), 110, r -> I18n.t("type." + r.type().entityClass().getSimpleName()))
                .col("#", 70, "num", RelatedItem::id)
                .col(I18n.t("field.title"), 340, RelatedItem::title)
                .node(I18n.t("field.status"), 130, r -> Badge.status(r.status()))
                .node(I18n.t("field.priority"), 110, r -> Badge.priority(r.priority()));
        table.placeholder(new EmptyState("ticket", I18n.t("assets.noTickets"), null, null));
        table.onOpen(r -> App.nav().open(new ItilDetailView(r.type(), r.id())));
        Async.run(() -> App.services().itilActivity().itemsForAsset(id), table::setRows);
        VBox.setVgrow(table, Priority.ALWAYS);
        return Ui.style(new VBox(10, table), "tab-body");
    }

    public static void linkPendingAsset(Ticket ticket) {
        Long assetId = pendingAsset;
        pendingAsset = null;
        if (assetId != null && ticket != null && ticket.getId() != null) {
            Async.exec(() -> App.services().itilActivity().linkAsset(ItilType.TICKET, ticket.getId(), assetId), null);
        }
    }
}
