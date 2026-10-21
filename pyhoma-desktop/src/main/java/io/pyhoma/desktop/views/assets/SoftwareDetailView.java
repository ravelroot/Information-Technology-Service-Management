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
import io.pyhoma.desktop.views.common.HistoryPanel;
import io.pyhoma.domain.model.asset.Software;
import io.pyhoma.domain.model.asset.SoftwareLicense;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.SoftwareService.InstallationRow;
import io.pyhoma.domain.service.SoftwareService.LicenseUsage;
import io.pyhoma.domain.support.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SoftwareDetailView extends View {

    private Long id;
    private Software software;
    private final PageHeader header = new PageHeader("", null);
    private final TabPane tabs = new TabPane();
    private Form form;

    public SoftwareDetailView(Long id) {
        this.id = id;
    }

    @Override
    public String title() {
        return software == null || software.getName() == null ? I18n.t("software.new") : software.getName();
    }

    private boolean canEdit() {
        return App.can(Module.SOFTWARE, id == null ? Right.CREATE : Right.UPDATE);
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
            software = new Software();
            render();
        } else {
            Async.run(() -> App.services().software().get(id), s -> {
                software = s;
                render();
            });
        }
    }

    private void render() {
        header.setTitle(software.isNew() ? I18n.t("software.new") : software.getName());
        header.setSubtitle(software.isNew() ? null : App.lookups().dd(DropdownKind.MANUFACTURER, software.getPublisherId()));
        List<Node> actions = new ArrayList<>();
        if (!software.isNew() && App.can(Module.SOFTWARE, Right.DELETE)) {
            actions.add(Ui.button(I18n.t("action.trash"), "trash-2", () -> {
                if (Dialogs.confirm(I18n.t("action.trash"), I18n.t("confirm.trash"), I18n.t("action.trash"), true)) {
                    Async.exec(() -> App.services().software().trash(id, true), () -> App.nav().back());
                }
            }, "ghost"));
        }
        if (canEdit()) {
            actions.add(Ui.primary(I18n.t(software.isNew() ? "action.create" : "action.save"), "save", this::save));
        }
        header.actions(actions.toArray(Node[]::new));
        form = new Form(2).readOnly(!canEdit())
                .text("name", software::getName, software::setName, 2, true)
                .choice("publisherId", () -> Option.of(App.lookups().dropdown(DropdownKind.MANUFACTURER)), software::getPublisherId,
                        software::setPublisherId, true, false)
                .choice("categoryId", () -> Option.of(App.lookups().dropdown(DropdownKind.SOFTWARE_CATEGORY)), software::getCategoryId,
                        software::setCategoryId, true, false)
                .area("notes", software::getNotes, software::setNotes, 4, false);
        form.load();
        ScrollPane general = new ScrollPane(Ui.style(new VBox(form.node()), "tab-body"));
        general.setFitToWidth(true);
        tabs.getTabs().setAll(tab("assets.tab.general", "info", general));
        if (!software.isNew()) {
            tabs.getTabs().add(tab("software.tab.licenses", "key-round", licensesTab()));
            tabs.getTabs().add(tab("software.tab.installations", "monitor", installationsTab()));
            HistoryPanel h = new HistoryPanel("Software", id);
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
        Async.run(() -> App.services().software().save(software), s -> {
            id = s.getId();
            software = s;
            Toasts.success(I18n.t("toast.saved"));
            render();
        }, e -> {
            if (e instanceof ValidationException v && form.showErrors(v)) {
                return;
            }
            Dialogs.error(e);
        });
    }

    private Node licensesTab() {
        DataTable<LicenseUsage> table = new DataTable<>();
        table.col(I18n.t("field.name"), 260, u -> u.license().getName())
                .col(I18n.t("field.seats"), 90, "num", u -> u.unlimited() ? "∞" : u.license().getSeats())
                .col(I18n.t("software.used"), 90, "num", LicenseUsage::used)
                .node(I18n.t("software.compliance"), 140, u -> u.overUsed() ? Badge.of(I18n.t("report.value.overUsed"), "red")
                        : Badge.of(I18n.t("report.value.compliant"), "green"))
                .col(I18n.t("field.expiresOn"), 120, u -> I18n.date(u.license().getExpiresOn()))
                .col(I18n.t("field.supplierId"), 160, u -> App.lookups().supplier(u.license().getSupplierId()));
        table.placeholder(new EmptyState("key-round", I18n.t("software.noLicenses"), null, null));
        Runnable reload = () -> Async.run(() -> App.services().software().compliance().stream()
                .filter(u -> Objects.equals(u.license().getSoftwareId(), id)).toList(), table::setRows);
        reload.run();
        table.onOpen(u -> editLicense(u.license(), reload));
        HBox actions = Ui.row(8, Ui.spacer());
        if (App.can(Module.SOFTWARE, Right.UPDATE)) {
            actions.getChildren().addAll(
                    Ui.button(I18n.t("action.delete"), "trash-2", () -> {
                        LicenseUsage u = table.selected();
                        if (u != null && Dialogs.confirm(I18n.t("action.delete"), I18n.t("confirm.delete"), I18n.t("action.delete"), true)) {
                            Async.exec(() -> App.services().software().deleteLicense(u.license().getId()), reload);
                        }
                    }, "ghost"),
                    Ui.primary(I18n.t("software.newLicense"), "plus", () -> {
                        SoftwareLicense l = new SoftwareLicense();
                        l.setSoftwareId(id);
                        editLicense(l, reload);
                    }));
        }
        VBox.setVgrow(table, Priority.ALWAYS);
        return Ui.style(new VBox(10, actions, table), "tab-body");
    }

    private void editLicense(SoftwareLicense l, Runnable reload) {
        Form f = new Form(2)
                .text("name", l::getName, l::setName, 2, true)
                .code("licenseKey", l::getLicenseKey, l::setLicenseKey)
                .integer("seats", l::getSeats, l::setSeats)
                .date("expiresOn", l::getExpiresOn, l::setExpiresOn)
                .choice("supplierId", () -> Option.of(App.lookups().suppliers()), l::getSupplierId, l::setSupplierId, true, false)
                .decimal("purchaseCost", l::getPurchaseCost, l::setPurchaseCost)
                .area("notes", l::getNotes, l::setNotes, 3, false);
        f.load();
        ((javafx.scene.layout.Region) f.node()).setPrefWidth(560);
        Dialogs.form(I18n.t(l.isNew() ? "software.newLicense" : "software.editLicense"), f.node(), I18n.t("action.save"), () -> {
            if (!f.commit()) {
                return false;
            }
            try {
                App.services().software().saveLicense(l);
                reload.run();
                return true;
            } catch (ValidationException e) {
                f.showErrors(e);
                return false;
            }
        });
    }

    private Node installationsTab() {
        DataTable<InstallationRow> table = new DataTable<>();
        table.col(I18n.t("field.assetId"), 220, InstallationRow::assetName)
                .col(I18n.t("field.softwareVersion"), 120, r -> r.installation().getSoftwareVersion())
                .col(I18n.t("field.licenseId"), 260, InstallationRow::licenseName)
                .col(I18n.t("field.installedOn"), 120, r -> I18n.date(r.installation().getInstalledOn()));
        table.placeholder(new EmptyState("monitor", I18n.t("software.noInstallations"), null, null));
        table.onOpen(r -> App.nav().open(new AssetDetailView(r.installation().getAssetId())));
        Runnable reload = () -> Async.run(() -> App.services().software().installationsOfSoftware(id), table::setRows);
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
                    Ui.primary(I18n.t("software.install"), "plus", () -> installDialog(null, id, reload)));
        }
        VBox.setVgrow(table, Priority.ALWAYS);
        return Ui.style(new VBox(10, actions, table), "tab-body");
    }

    static void installDialog(Long assetId, Long softwareId, Runnable reload) {
        PickerField<Long> asset = AssetDetailView.assetPicker();
        asset.setValue(assetId);
        PickerField<Long> sw = new PickerField<>(() -> {
            List<Option<Long>> list = new ArrayList<>();
            App.services().software().search(null, false, 0, 2000).items().forEach(s -> list.add(new Option<>(s.getId(), s.getName())));
            return list;
        }, false);
        sw.setValue(softwareId);
        PickerField<Long> license = new PickerField<>(() -> {
            List<Option<Long>> list = new ArrayList<>();
            if (sw.getValue() != null) {
                App.services().software().licenses(sw.getValue()).forEach(l -> list.add(new Option<>(l.getId(), l.getName())));
            }
            return list;
        }, true);
        sw.onChange(v -> license.reload());
        javafx.scene.control.TextField version = new javafx.scene.control.TextField();
        VBox content = new VBox(10, Ui.fact(I18n.t("field.assetId"), asset), Ui.fact(I18n.t("field.softwareId"), sw),
                Ui.fact(I18n.t("field.softwareVersion"), version), Ui.fact(I18n.t("field.licenseId"), license));
        content.setPrefWidth(460);
        Dialogs.form(I18n.t("software.install"), content, I18n.t("software.install"), () -> {
            if (asset.getValue() == null || sw.getValue() == null) {
                return false;
            }
            App.services().software().install(asset.getValue(), sw.getValue(), version.getText(), license.getValue());
            Toasts.success(I18n.t("software.installed"));
            reload.run();
            return true;
        });
    }
}
