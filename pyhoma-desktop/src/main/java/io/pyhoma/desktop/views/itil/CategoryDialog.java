package io.pyhoma.desktop.views.itil;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.PickerField;
import io.pyhoma.domain.model.core.DropdownItem;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.security.Right;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class CategoryDialog {

    static final List<String> COLORS = List.of("#000000", "#2B2727", "#6B6262", "#AB9D9D", "#BAB7B5", "#D9D1CE", "#2F5A3F", "#8A2F2F");

    private final VBox rows = new VBox(4);
    private final Runnable onChange;
    private boolean changed;

    private CategoryDialog(Runnable onChange) {
        this.onChange = onChange;
    }

    public static boolean allowed() {
        return App.can(Module.DROPDOWN, Right.CREATE) || App.can(Module.DROPDOWN, Right.DELETE);
    }

    public static Button button(Runnable onChange) {
        Button b = Ui.iconButton("tags", I18n.t("category.manage"), () -> open(onChange));
        b.getStyleClass().add("category-manage");
        return b;
    }

    public static void open(Runnable onChange) {
        new CategoryDialog(onChange).show();
    }

    private void show() {
        TextField name = new TextField();
        name.setPromptText(I18n.t("category.newName"));
        HBox.setHgrow(name, Priority.ALWAYS);
        PickerField<Long> parent = new PickerField<>(() -> Option.of(App.lookups().dropdown(DropdownKind.ITIL_CATEGORY)), true);
        parent.setPrefWidth(200);
        ToggleGroup colors = new ToggleGroup();
        HBox swatches = new HBox(4);
        swatches.setAlignment(Pos.CENTER_LEFT);
        for (String c : COLORS) {
            ToggleButton t = new ToggleButton();
            t.setUserData(c);
            t.setToggleGroup(colors);
            t.getStyleClass().add("swatch");
            t.setStyle("-fx-background-color: " + c + ";");
            t.setTooltip(new Tooltip(c));
            swatches.getChildren().add(t);
        }
        colors.getToggles().get(0).setSelected(true);
        Button add = Ui.primary(I18n.t("category.add"), "plus", () -> {
            DropdownItem d = new DropdownItem();
            d.setKind(DropdownKind.ITIL_CATEGORY);
            d.setName(name.getText());
            d.setParentId(parent.getValue());
            d.setColor(colors.getSelectedToggle() == null ? null : (String) colors.getSelectedToggle().getUserData());
            d.setActive(true);
            Async.run(() -> App.services().dropdowns().save(d), saved -> {
                name.clear();
                parent.setValue(null);
                touched();
                parent.reload();
                Toasts.success(I18n.t("category.added", saved.getCompleteName()));
                reload();
            }, Dialogs::error);
        });
        add.disableProperty().bind(name.textProperty().isEmpty());
        name.setOnAction(e -> add.fire());
        boolean canCreate = App.can(Module.DROPDOWN, Right.CREATE);
        HBox top = new HBox(8, name, parent, add);
        top.setAlignment(Pos.CENTER_LEFT);
        Label parentHint = Ui.muted(I18n.t("category.parentHint"));
        VBox create = new VBox(8, top, Ui.row(10, Ui.muted(I18n.t("category.color")), swatches), parentHint);
        create.getStyleClass().add("category-create");
        create.setVisible(canCreate);
        create.setManaged(canCreate);
        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(340);
        scroll.getStyleClass().add("category-list");
        VBox content = new VBox(14, create, scroll);
        content.setPrefWidth(620);
        reload();
        Dialogs.form(I18n.t("category.title"), content, I18n.t("action.done"), () -> true);
        if (changed) {
            onChange.run();
        }
    }

    private void touched() {
        changed = true;
        App.lookups().invalidate();
    }

    private void reload() {
        Async.run(() -> App.services().dropdowns().list(DropdownKind.ITIL_CATEGORY, false), list -> {
            rows.getChildren().clear();
            if (list.isEmpty()) {
                rows.getChildren().add(Ui.muted(I18n.t("category.empty")));
            }
            boolean canDelete = App.can(Module.DROPDOWN, Right.DELETE);
            for (DropdownItem d : list.stream().sorted(java.util.Comparator.comparing(
                    (DropdownItem x) -> x.getCompleteName() == null ? x.getName() : x.getCompleteName(), String.CASE_INSENSITIVE_ORDER)).toList()) {
                Region dot = new Region();
                dot.getStyleClass().add("category-dot");
                if (d.getColor() != null) {
                    dot.setStyle("-fx-background-color: " + d.getColor() + ";");
                }
                int depth = d.getCompleteName() == null ? 0 : d.getCompleteName().split(" > ").length - 1;
                Label l = new Label(d.getName());
                l.getStyleClass().add(depth == 0 ? "category-root" : "category-child");
                HBox row = new HBox(10, dot, l, Ui.spacer());
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("category-row");
                row.setStyle("-fx-padding: 7 8 7 " + (10 + depth * 22) + ";");
                if (canDelete) {
                    Button del = Ui.iconButton("trash-2", I18n.t("category.delete"), () -> delete(d));
                    del.getStyleClass().add("category-delete");
                    row.getChildren().add(del);
                }
                rows.getChildren().add(row);
            }
        }, Dialogs::error);
    }

    private void delete(DropdownItem d) {
        Async.run(() -> new long[] {App.services().dropdowns().subtree(d.getId()).size() - 1L,
                App.services().dropdowns().categoryUsage(d.getId())}, n -> {
            String msg = I18n.t("category.deleteConfirm", d.getName());
            if (n[0] > 0) {
                msg += "\n" + I18n.t("category.deleteChildren", n[0]);
            }
            if (n[1] > 0) {
                msg += "\n" + I18n.t("category.deleteUsage", n[1]);
            }
            if (!Dialogs.confirm(I18n.t("category.delete"), msg, I18n.t("action.delete"), true)) {
                return;
            }
            Async.run(() -> {
                App.services().dropdowns().delete(d.getId());
                return d;
            }, r -> {
                touched();
                Toasts.success(I18n.t("category.deleted", d.getName()));
                reload();
            }, Dialogs::error);
        }, Dialogs::error);
    }

    static String colorOf(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return App.lookups().colors(DropdownKind.ITIL_CATEGORY).get(categoryId);
    }
}
