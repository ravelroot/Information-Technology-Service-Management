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
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.domain.model.core.DropdownItem;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.ValidationException;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DropdownsView extends MasterDetail {

    private final ListView<DropdownKind> kinds = new ListView<>();
    private final DataTable<DropdownItem> items = new DataTable<>();
    private DropdownKind kind = DropdownKind.LOCATION;

    @Override
    protected String titleText() {
        return I18n.t("nav.DROPDOWNS");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("dropdowns.subtitle");
    }

    @Override
    protected Node master() {
        kinds.setItems(FXCollections.observableArrayList(DropdownKind.values()));
        kinds.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DropdownKind item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : I18n.e(item));
            }
        });
        kinds.getSelectionModel().selectedItemProperty().addListener((o, a, k) -> {
            if (k != null) {
                kind = k;
                load();
            }
        });
        items.col(I18n.t("field.name"), 320, d -> d.getCompleteName() == null ? d.getName() : d.getCompleteName())
                .node(I18n.t("field.color"), 120, d -> d.getColor() == null ? Ui.label("", "muted") : Badge.colored(d.getColor(), d.getColor()))
                .col(I18n.t("field.sortOrder"), 80, "num", DropdownItem::getSortOrder)
                .col(I18n.t("field.active"), 90, d -> I18n.t(d.isActive() ? "common.yes" : "common.no"));
        items.placeholder(new EmptyState("list", I18n.t("dropdowns.empty"), null, null));
        items.onOpen(this::edit);
        HBox actions = Ui.row(8, Ui.spacer());
        if (App.can(Module.DROPDOWN, Right.DELETE)) {
            actions.getChildren().add(Ui.button(I18n.t("action.delete"), "trash-2", () -> {
                DropdownItem d = items.selected();
                if (d != null && Dialogs.confirm(I18n.t("action.delete"), I18n.t("confirm.delete"), I18n.t("action.delete"), true)) {
                    Async.exec(() -> App.services().dropdowns().delete(d.getId()), this::load);
                }
            }, "ghost"));
        }
        if (App.can(Module.DROPDOWN, Right.CREATE)) {
            actions.getChildren().add(Ui.primary(I18n.t("dropdowns.new"), "plus", () -> {
                DropdownItem d = new DropdownItem();
                d.setKind(kind);
                DropdownItem sel = items.selected();
                if (kind.isHierarchical() && sel != null) {
                    d.setParentId(sel.getId());
                }
                edit(d);
            }));
        }
        VBox.setVgrow(items, Priority.ALWAYS);
        VBox right = new VBox(10, actions, items);
        VBox.setVgrow(right, Priority.ALWAYS);
        detail.getChildren().setAll(right);
        return kinds;
    }

    @Override
    public void onShow() {
        if (kinds.getSelectionModel().isEmpty()) {
            kinds.getSelectionModel().selectFirst();
        } else {
            load();
        }
    }

    private void load() {
        DropdownKind k = kind;
        Async.run(() -> App.services().dropdowns().list(k, false), items::setRows);
    }

    private void edit(DropdownItem d) {
        boolean canEdit = App.can(Module.DROPDOWN, d.isNew() ? Right.CREATE : Right.UPDATE);
        Form f = new Form(2).readOnly(!canEdit).text("name", d::getName, d::setName, 2, true);
        if (d.getKind().isHierarchical()) {
            f.choice("parentId", () -> {
                List<Option<Long>> list = new ArrayList<>();
                App.services().dropdowns().list(d.getKind(), false).forEach(x -> list.add(new Option<>(x.getId(), x.getCompleteName())));
                return list;
            }, d::getParentId, d::setParentId, true, false);
        }
        if (d.getKind().isColored()) {
            f.text("color", d::getColor, d::setColor).check("flag", d::isFlag, d::setFlag);
        }
        f.integer("sortOrder", d::getSortOrder, d::setSortOrder)
                .check("active", d::isActive, d::setActive)
                .area("notes", d::getNotes, d::setNotes, 2, false);
        f.load();
        ((javafx.scene.layout.Region) f.node()).setPrefWidth(520);
        Dialogs.form(I18n.t(d.isNew() ? "dropdowns.new" : "dropdowns.edit") + " — " + I18n.e(d.getKind()), f.node(),
                I18n.t("action.save"), () -> {
                    if (!canEdit) {
                        return true;
                    }
                    if (!f.commit()) {
                        return false;
                    }
                    try {
                        App.services().dropdowns().save(d);
                        Toasts.success(I18n.t("toast.saved"));
                        App.lookups().refreshAsync(this::load);
                        return true;
                    } catch (ValidationException e) {
                        f.showErrors(e);
                        return false;
                    }
                });
    }
}
