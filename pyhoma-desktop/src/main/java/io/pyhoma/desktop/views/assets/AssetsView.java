package io.pyhoma.desktop.views.assets;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.PickerField;
import io.pyhoma.desktop.ui.components.Segmented;
import io.pyhoma.desktop.views.common.ListPage;
import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.asset.AssetType;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.AssetService;
import io.pyhoma.domain.service.AssetService.AssetSearch;
import io.pyhoma.domain.support.Csv;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

public class AssetsView extends ListPage<Asset> {

    private AssetType type;
    private boolean trash;
    private final PickerField<Long> state = PickerField.compact(() -> io.pyhoma.desktop.views.projects.ProjectDetailViewHelper
            .colored(DropdownKind.ASSET_STATE), null);
    private final PickerField<Long> location = PickerField.compact(() -> Option.of(App.lookups().dropdown(DropdownKind.LOCATION)), null);

    @Override
    protected String titleText() {
        return I18n.t("nav.ASSETS");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("assets.subtitle");
    }

    @Override
    protected Node top() {
        List<String> keys = new ArrayList<>();
        keys.add("ALL");
        for (AssetType t : AssetType.values()) {
            keys.add(t.name());
        }
        if (App.can(Module.ASSET, Right.DELETE)) {
            keys.add("TRASH");
        }
        return new Segmented<>(keys, k -> "ALL".equals(k) ? I18n.t("assets.all") : "TRASH".equals(k) ? I18n.t("itil.view.TRASH")
                : I18n.t("enum.AssetType." + k + ".plural"), "ALL", k -> {
                    trash = "TRASH".equals(k);
                    type = "ALL".equals(k) || trash ? null : AssetType.valueOf(k);
                    loadPage(0);
                });
    }

    @Override
    protected Node[] filters() {
        state.onChange(v -> loadPage(0));
        location.onChange(v -> loadPage(0));
        return new Node[] {io.pyhoma.desktop.views.itil.ItilListViewHelper.labeled("field.stateId", state),
            io.pyhoma.desktop.views.itil.ItilListViewHelper.labeled("field.locationId", location)};
    }

    @Override
    protected Node[] actions() {
        List<Node> nodes = new ArrayList<>();
        if (App.can(Module.ASSET, Right.CREATE)) {
            nodes.add(Ui.button(I18n.t("assets.import"), "upload", this::importCsv));
            nodes.add(Ui.primary(I18n.t("assets.new"), "plus", () -> App.nav().open(new AssetDetailView(null))));
        }
        return nodes.toArray(Node[]::new);
    }

    @Override
    protected Node emptyState() {
        return new EmptyState("monitor-smartphone", I18n.t("assets.empty"), I18n.t("assets.emptyHint"), null);
    }

    @Override
    protected void columns(DataTable<Asset> t) {
        t.node(I18n.t("field.name"), 200, AssetsView::nameCell, Asset::getName)
                .col(I18n.t("field.assetTag"), 120, "mono", Asset::getAssetTag)
                .col(I18n.t("field.assetType"), 120, a -> I18n.e(a.getAssetType()))
                .node(I18n.t("field.stateId"), 130, a -> state(a), a -> App.lookups().dd(DropdownKind.ASSET_STATE, a.getStateId()))
                .col(I18n.t("field.userId"), 160, a -> App.lookups().user(a.getUserId()))
                .col(I18n.t("field.locationId"), 170, a -> App.lookups().dd(DropdownKind.LOCATION, a.getLocationId()))
                .col(I18n.t("field.manufacturerId"), 110, a -> App.lookups().dd(DropdownKind.MANUFACTURER, a.getManufacturerId()))
                .col(I18n.t("field.serialNumber"), 130, "mono", Asset::getSerialNumber)
                .col(I18n.t("field.ipAddress"), 120, "mono", Asset::getIpAddress)
                .col(I18n.t("field.warrantyExpiresOn"), 110, a -> I18n.date(a.getWarrantyExpiresOn()));
    }

    static Node nameCell(Asset a) {
        Label l = new Label(a.getName());
        HBox h = new HBox(8, Icons.of(a.getAssetType().icon(), 15), l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    static Node state(Asset a) {
        if (a.getStateId() == null) {
            return Ui.label("—", "muted");
        }
        return Badge.colored(App.lookups().dd(DropdownKind.ASSET_STATE, a.getStateId()),
                App.lookups().colors(DropdownKind.ASSET_STATE).get(a.getStateId()));
    }

    @Override
    protected PageResult<Asset> load(String text, int page, int size) {
        AssetSearch s = new AssetSearch(text, type, state.getValue(), location.getValue(), null, trash, null, "name", true, page, size);
        return App.services().assets().search(s);
    }

    @Override
    protected void open(Asset row) {
        App.nav().open(new AssetDetailView(row.getId()));
    }

    private void importCsv() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = chooser.showOpenDialog(Dialogs.owner());
        if (file == null) {
            boolean template = Dialogs.confirm(I18n.t("assets.import"), I18n.t("assets.importHelp",
                    String.join(";", AssetService.CSV_COLUMNS)), I18n.t("assets.saveTemplate"), false);
            if (template) {
                saveTemplate();
            }
            return;
        }
        Async.run(() -> App.services().assets().importCsv(Files.readString(file.toPath(), StandardCharsets.UTF_8)), r -> {
            String msg = I18n.t("assets.imported", r.created(), r.updated());
            if (!r.errors().isEmpty()) {
                msg += "\n\n" + String.join("\n", r.errors().subList(0, Math.min(15, r.errors().size())));
            }
            Dialogs.info(I18n.t("assets.import"), msg);
            App.lookups().refreshAsync(this::reload);
        });
    }

    private void saveTemplate() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("pyhoma-assets-template.csv");
        File f = chooser.showSaveDialog(Dialogs.owner());
        if (f != null) {
            try {
                List<List<String>> sample = List.of(List.of("NB-001", "LAPTOP", "SN123456", "", "In use", "Main campus", "Lenovo",
                        "ThinkPad T14", "ana.souza", "nb-001", "10.0.0.21", "00:1A:2B:3C:4D:5E", "Windows 11", "24H2",
                        "Intel Core Ultra 5", "16384", "512", "2025-01-15", "2028-01-15", "7200.00"));
                Files.writeString(f.toPath(), "﻿" + Csv.write(AssetService.CSV_COLUMNS, sample, ';'), StandardCharsets.UTF_8);
            } catch (Exception e) {
                Dialogs.error(e);
            }
        }
    }
}
