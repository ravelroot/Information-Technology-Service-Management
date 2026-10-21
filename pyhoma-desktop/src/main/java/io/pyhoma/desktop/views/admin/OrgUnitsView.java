package io.pyhoma.desktop.views.admin;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.OrgUnit;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.ValidationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class OrgUnitsView extends MasterDetail {

    private final TreeView<OrgUnit> tree = new TreeView<>();

    @Override
    protected String titleText() {
        return I18n.t("nav.ORG_UNITS");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("orgUnits.subtitle");
    }

    @Override
    protected Node[] actions() {
        if (!App.can(Module.ORG_UNIT, Right.CREATE)) {
            return new Node[0];
        }
        return new Node[] {Ui.primary(I18n.t("orgUnits.newChild"), "plus", () -> {
            OrgUnit parent = tree.getSelectionModel().getSelectedItem() == null ? null : tree.getSelectionModel().getSelectedItem().getValue();
            OrgUnit u = new OrgUnit();
            u.setParentId(parent == null ? null : parent.getId());
            edit(u);
        })};
    }

    @Override
    protected Node master() {
        tree.setShowRoot(true);
        tree.getStyleClass().add("org-tree");
        tree.setCellFactory(tv -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(OrgUnit item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
                setGraphic(empty || item == null ? null : Icons.of(item.getParentId() == null ? "building" : "building-2", 14));
            }
        });
        tree.getSelectionModel().selectedItemProperty().addListener((o, a, item) -> {
            if (item != null) {
                edit(item.getValue());
            }
        });
        return tree;
    }

    @Override
    public void onShow() {
        Async.run(() -> App.services().orgUnits().all(), this::fill);
    }

    private void fill(List<OrgUnit> units) {
        TreeItem<OrgUnit> previous = tree.getSelectionModel().getSelectedItem();
        Long selectedId = previous == null ? null : previous.getValue().getId();
        Map<Long, TreeItem<OrgUnit>> items = new HashMap<>();
        for (OrgUnit u : units) {
            TreeItem<OrgUnit> i = new TreeItem<>(u);
            i.setExpanded(true);
            items.put(u.getId(), i);
        }
        TreeItem<OrgUnit> root = null;
        for (OrgUnit u : units) {
            if (u.getParentId() == null) {
                root = items.get(u.getId());
            } else if (items.containsKey(u.getParentId())) {
                items.get(u.getParentId()).getChildren().add(items.get(u.getId()));
            }
        }
        tree.setRoot(root);
        TreeItem<OrgUnit> select = selectedId != null && items.containsKey(selectedId) ? items.get(selectedId) : root;
        if (select != null) {
            tree.getSelectionModel().select(select);
        }
    }

    private void edit(OrgUnit u) {
        boolean canEdit = App.can(Module.ORG_UNIT, u.isNew() ? Right.CREATE : Right.UPDATE);
        Form f = new Form(2).readOnly(!canEdit)
                .text("name", u::getName, u::setName, 2, true)
                .choice("parentId", () -> Option.of(App.lookups().orgUnits()), u::getParentId, u::setParentId, true, false)
                .text("email", u::getEmail, u::setEmail)
                .text("phone", u::getPhone, u::setPhone)
                .text("address", u::getAddress, u::setAddress)
                .area("notes", u::getNotes, u::setNotes, 3, false);
        f.load();
        HBox actions = Ui.row(8, Ui.spacer());
        if (!u.isNew() && u.getParentId() != null && App.can(Module.ORG_UNIT, Right.DELETE)) {
            actions.getChildren().add(Ui.button(I18n.t("action.delete"), "trash-2", () -> {
                if (Dialogs.confirm(I18n.t("action.delete"), I18n.t("confirm.delete"), I18n.t("action.delete"), true)) {
                    Async.exec(() -> App.services().orgUnits().delete(u.getId()), () -> {
                        detail.getChildren().clear();
                        App.lookups().refreshAsync(this::onShow);
                    });
                }
            }, "ghost"));
        }
        if (canEdit) {
            actions.getChildren().add(Ui.primary(I18n.t("action.save"), "save", () -> {
                if (!f.commit()) {
                    return;
                }
                Async.run(() -> App.services().orgUnits().save(u), saved -> {
                    Toasts.success(I18n.t("toast.saved"));
                    App.lookups().refreshAsync(this::onShow);
                }, e -> {
                    if (!(e instanceof ValidationException v && f.showErrors(v))) {
                        Dialogs.error(e);
                    }
                });
            }));
        }
        String title = u.isNew() ? I18n.t("orgUnits.newChild") : u.getCompleteName();
        detail.getChildren().setAll(new VBox(14, Ui.panel(title, f.node()), actions));
    }
}
