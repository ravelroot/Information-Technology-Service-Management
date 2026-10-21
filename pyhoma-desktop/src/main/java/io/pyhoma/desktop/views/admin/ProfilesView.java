package io.pyhoma.desktop.views.admin;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.domain.model.core.InterfaceType;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.Profile;
import io.pyhoma.domain.security.Right;
import java.util.EnumMap;
import java.util.Map;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ProfilesView extends MasterDetail {

    private final DataTable<Profile> profiles = new DataTable<>();

    @Override
    protected String titleText() {
        return I18n.t("nav.PROFILES");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("profiles.subtitle");
    }

    @Override
    protected Node[] actions() {
        if (!App.can(Module.PROFILE, Right.CREATE)) {
            return new Node[0];
        }
        return new Node[] {Ui.primary(I18n.t("profiles.new"), "plus", () -> edit(new Profile(), new EnumMap<>(Module.class)))};
    }

    @Override
    protected Node master() {
        profiles.node(I18n.t("field.name"), 220, p -> {
            HBox h = new HBox(8, new Label(p.getName()));
            h.setAlignment(Pos.CENTER_LEFT);
            if (p.isProtectedProfile()) {
                h.getChildren().add(Badge.of(I18n.t("profiles.protected"), "slate"));
            }
            if (p.isDefaultProfile()) {
                h.getChildren().add(Badge.of(I18n.t("profiles.default"), "teal"));
            }
            return h;
        });
        profiles.autoSelectFirst();
        profiles.getSelectionModel().selectedItemProperty().addListener((o, a, p) -> {
            if (p != null) {
                Async.run(() -> App.services().profiles().rights(p.getId()), r -> edit(p, r));
            }
        });
        return profiles;
    }

    @Override
    public void onShow() {
        Async.run(() -> App.services().profiles().all(), profiles::setRows);
    }

    private void edit(Profile p, Map<Module, Integer> rights) {
        boolean canEdit = App.can(Module.PROFILE, p.isNew() ? Right.CREATE : Right.UPDATE);
        Form f = new Form(3).readOnly(!canEdit)
                .text("name", p::getName, p::setName, 1, true)
                .choice("interfaceType", () -> Option.ofEnum(InterfaceType.values()), p::getInterfaceType, p::setInterfaceType, false, true)
                .check("defaultProfile", p::isDefaultProfile, p::setDefaultProfile)
                .area("notes", p::getNotes, p::setNotes, 2, false);
        f.load();
        GridPane matrix = new GridPane();
        matrix.getStyleClass().add("rights-matrix");
        matrix.setHgap(4);
        matrix.setVgap(2);
        Map<Module, Map<Integer, CheckBox>> boxes = new EnumMap<>(Module.class);
        int col = 1;
        for (int bit : Right.BITS) {
            Label h = Ui.label(I18n.t("right." + Right.name(bit)), "matrix-head");
            GridPane.setHalignment(h, HPos.CENTER);
            matrix.add(h, col++, 0);
        }
        int row = 1;
        Module.Section section = null;
        for (Module m : Module.values()) {
            if (m.section() != section) {
                section = m.section();
                Label s = Ui.label(I18n.t("module.section." + section.name()), "matrix-section");
                matrix.add(s, 0, row++, Right.BITS.size() + 1, 1);
            }
            matrix.add(Ui.label(I18n.t("module." + m.name()), "matrix-module"), 0, row);
            Map<Integer, CheckBox> line = new java.util.HashMap<>();
            col = 1;
            for (int bit : Right.BITS) {
                if (m.supports(bit)) {
                    CheckBox cb = new CheckBox();
                    cb.setSelected(Right.has(rights.getOrDefault(m, 0), bit));
                    cb.setDisable(!canEdit || p.isProtectedProfile());
                    GridPane.setHalignment(cb, HPos.CENTER);
                    matrix.add(cb, col, row);
                    line.put(bit, cb);
                }
                col++;
            }
            boxes.put(m, line);
            row++;
        }
        HBox actions = Ui.row(8, Ui.spacer());
        if (!p.isNew() && App.can(Module.PROFILE, Right.CREATE)) {
            actions.getChildren().add(Ui.button(I18n.t("action.duplicate"), "copy", () -> Dialogs.prompt(I18n.t("action.duplicate"),
                    I18n.t("field.name"), p.getName() + " (2)", false).ifPresent(name -> Async.run(() -> App.services().profiles()
                    .duplicate(p.getId(), name), d -> {
                        Toasts.success(I18n.t("toast.duplicated"));
                        App.lookups().refreshAsync(null);
                        onShow();
                    }))));
        }
        if (!p.isNew() && !p.isProtectedProfile() && App.can(Module.PROFILE, Right.DELETE)) {
            actions.getChildren().add(Ui.button(I18n.t("action.delete"), "trash-2", () -> {
                if (Dialogs.confirm(I18n.t("action.delete"), I18n.t("confirm.delete"), I18n.t("action.delete"), true)) {
                    Async.exec(() -> App.services().profiles().delete(p.getId()), () -> {
                        detail.getChildren().clear();
                        onShow();
                    });
                }
            }, "ghost"));
        }
        if (canEdit) {
            actions.getChildren().add(Ui.primary(I18n.t("action.save"), "save", () -> {
                if (!f.commit()) {
                    return;
                }
                Map<Module, Integer> result = new EnumMap<>(Module.class);
                boxes.forEach((m, line) -> {
                    int mask = 0;
                    for (Map.Entry<Integer, CheckBox> e : line.entrySet()) {
                        if (e.getValue().isSelected()) {
                            mask |= e.getKey();
                        }
                    }
                    result.put(m, mask);
                });
                Async.run(() -> App.services().profiles().save(p, p.isProtectedProfile() ? null : result), saved -> {
                    Toasts.success(I18n.t("toast.saved"));
                    App.lookups().refreshAsync(null);
                    onShow();
                });
            }));
        }
        ScrollPane sp = new ScrollPane(new VBox(14, Ui.panel(p.isNew() ? I18n.t("profiles.new") : p.getName(), f.node()),
                Ui.panel(I18n.t("profiles.rights"), matrix), actions));
        sp.setFitToWidth(true);
        detail.getChildren().setAll(sp);
    }
}
