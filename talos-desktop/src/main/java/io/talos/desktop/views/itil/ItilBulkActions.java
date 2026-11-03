package io.talos.desktop.views.itil;

import io.talos.desktop.App;
import io.talos.desktop.ui.Async;
import io.talos.desktop.ui.Dialogs;
import io.talos.desktop.ui.I18n;
import io.talos.desktop.ui.Icons;
import io.talos.desktop.ui.Option;
import io.talos.desktop.ui.Toasts;
import io.talos.desktop.ui.Ui;
import io.talos.desktop.ui.components.PickerField;
import io.talos.domain.model.core.DropdownKind;
import io.talos.domain.model.itil.ItilItem;
import io.talos.domain.model.itil.ItilStatus;
import io.talos.domain.model.itil.ItilType;
import io.talos.domain.security.Right;
import io.talos.domain.service.ItilBulkService.BulkResult;
import io.talos.domain.service.ItilBulkService.Failure;
import io.talos.domain.service.ItilService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

final class ItilBulkActions {

    private final ItilType type;
    private final Supplier<List<ItilItem>> selection;
    private final Runnable done;

    ItilBulkActions(ItilType type, Supplier<List<ItilItem>> selection, Runnable done) {
        this.type = type;
        this.selection = selection;
        this.done = done;
    }

    static boolean available(ItilType type) {
        return !App.session().isSelfService() && App.can(App.services().itil(type).module(), Right.UPDATE);
    }

    private ItilService<? extends ItilItem> service() {
        return App.services().itil(type);
    }

    private List<Long> ids() {
        return selection.get().stream().map(ItilItem::getId).toList();
    }

    MenuButton button() {
        MenuButton b = new MenuButton(I18n.t("bulk.actions"));
        b.setGraphic(Icons.of("list-checks", 15));
        b.setOnShowing(e -> {
            b.getItems().setAll(items(false));
        });
        b.getItems().setAll(items(false));
        return b;
    }

    javafx.scene.control.ContextMenu contextMenu() {
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
        menu.setOnShowing(e -> menu.getItems().setAll(items(true)));
        return menu;
    }

    private List<MenuItem> items(boolean context) {
        List<ItilItem> selected = selection.get();
        int n = selected.size();
        List<MenuItem> list = new ArrayList<>();
        MenuItem header = new MenuItem(n == 0 ? I18n.t("bulk.none") : I18n.t("bulk.selected", n));
        header.setDisable(true);
        list.add(header);
        list.add(new SeparatorMenuItem());
        boolean empty = n == 0;
        boolean trash = selected.stream().anyMatch(ItilItem::isDeleted);
        if (context && n == 1) {
            list.add(item(I18n.t("action.open"), "external-link", false,
                    () -> App.nav().open(new ItilDetailView(type, selected.get(0).getId()))));
        }
        if (App.can(service().module(), Right.ASSIGN)) {
            list.add(item(I18n.t("bulk.assignMe"), "user", empty || trash,
                    () -> run(ids -> App.services().itilBulk().assign(type, ids, App.session().userId(), null))));
            list.add(item(I18n.t("bulk.assign"), "user-plus", empty || trash, this::assign));
        }
        Menu status = new Menu(I18n.t("itil.changeStatus"));
        status.setGraphic(Icons.of("circle-dot", 14));
        for (ItilStatus s : ItilStatus.workflow(type)) {
            if (type != ItilType.CHANGE && (s == ItilStatus.SOLVED || s == ItilStatus.CLOSED)) {
                continue;
            }
            status.getItems().add(item(I18n.e(s), null, false,
                    () -> run(ids -> App.services().itilBulk().changeStatus(type, ids, s))));
        }
        status.setDisable(empty || trash);
        list.add(status);
        Menu urgency = new Menu(I18n.t("field.urgency"));
        urgency.setGraphic(Icons.of("gauge", 14));
        for (Option<Integer> o : Option.levels("level.", 5)) {
            urgency.getItems().add(item(o.label(), null, false,
                    () -> run(ids -> App.services().itilBulk().changeUrgencyImpact(type, ids, o.value(), null))));
        }
        urgency.setDisable(empty || trash);
        list.add(urgency);
        list.add(item(I18n.t("bulk.category"), "tag", empty || trash, this::category));
        list.add(item(I18n.t("bulk.followup"), "message-square", empty || trash, this::followup));
        list.add(new SeparatorMenuItem());
        list.add(item(I18n.t("bulk.merge"), "git-pull-request-arrow", n < 2 || trash, this::merge));
        if (App.can(service().module(), Right.CREATE)) {
            list.add(item(I18n.t("action.duplicate"), "copy", n != 1 || trash, () -> duplicate(selected.get(0))));
        }
        if (App.can(service().module(), Right.DELETE)) {
            list.add(new SeparatorMenuItem());
            if (trash) {
                list.add(item(I18n.t("action.restore"), "archive-restore", empty,
                        () -> run(ids -> App.services().itilBulk().restore(type, ids))));
            } else {
                list.add(item(I18n.t("action.trash"), "trash-2", empty, () -> {
                    List<Long> chosen = ids();
                    if (Dialogs.confirm(I18n.t("action.trash"), I18n.t("bulk.confirmTrash", chosen.size()), I18n.t("action.trash"), true)) {
                        run(chosen, ids -> App.services().itilBulk().trash(type, ids));
                    }
                }));
            }
        }
        return list;
    }

    private static MenuItem item(String text, String icon, boolean disabled, Runnable action) {
        MenuItem mi = new MenuItem(text);
        if (icon != null) {
            mi.setGraphic(Icons.of(icon, 14));
        }
        mi.setDisable(disabled);
        mi.setOnAction(e -> action.run());
        return mi;
    }

    private void assign() {
        PickerField<Long> user = new PickerField<>(() -> {
            List<Option<Long>> list = new ArrayList<>();
            App.lookups().technicians().forEach(u -> list.add(new Option<>(u.getId(), u.getFullName())));
            return list;
        }, true);
        PickerField<Long> group = new PickerField<>(() -> Option.of(App.lookups().groups()), true);
        VBox body = new VBox(10, Ui.label(I18n.t("bulk.selected", ids().size()), "muted"),
                Ui.label(I18n.t("field.assigneeId"), "field-label"), user,
                Ui.label(I18n.t("field.assigneeGroupId"), "field-label"), group);
        body.setPrefWidth(420);
        List<Long> chosen = ids();
        Dialogs.form(I18n.t("bulk.assign"), body, I18n.t("action.confirm"), () -> {
            Long u = user.getValue();
            Long g = group.getValue();
            if (u == null && g == null) {
                return false;
            }
            run(chosen, ids -> App.services().itilBulk().assign(type, ids, u, g));
            return true;
        });
    }

    private void category() {
        PickerField<Long> category = new PickerField<>(() -> Option.of(App.lookups().dropdown(DropdownKind.ITIL_CATEGORY)), true);
        VBox body = new VBox(10, Ui.label(I18n.t("bulk.selected", ids().size()), "muted"),
                Ui.label(I18n.t("field.categoryId"), "field-label"), category);
        body.setPrefWidth(420);
        List<Long> chosen = ids();
        Dialogs.form(I18n.t("bulk.category"), body, I18n.t("action.confirm"), () -> {
            Long c = category.getValue();
            run(chosen, ids -> App.services().itilBulk().changeCategory(type, ids, c));
            return true;
        });
    }

    private void followup() {
        TextArea text = new TextArea();
        text.setWrapText(true);
        text.setPrefRowCount(5);
        CheckBox internal = new CheckBox(I18n.t("bulk.private"));
        VBox body = new VBox(10, Ui.label(I18n.t("bulk.selected", ids().size()), "muted"), text, internal);
        body.setPrefWidth(460);
        List<Long> chosen = ids();
        Dialogs.form(I18n.t("bulk.followup"), body, I18n.t("composer.send"), () -> {
            if (text.getText() == null || text.getText().isBlank()) {
                return false;
            }
            String content = text.getText();
            boolean priv = internal.isSelected();
            run(chosen, ids -> App.services().itilBulk().addFollowup(type, ids, content, priv));
            return true;
        });
    }

    private void merge() {
        List<ItilItem> selected = selection.get();
        PickerField<Long> target = new PickerField<>(() -> selected.stream()
                .map(i -> new Option<>(i.getId(), i.reference() + "  " + i.getTitle())).toList(), false);
        target.setValue(selected.stream().map(ItilItem::getId).min(Long::compare).orElse(null));
        VBox body = new VBox(10, Ui.label(I18n.t("bulk.mergeHint", selected.size() - 1), "muted"),
                Ui.label(I18n.t("bulk.mergeTarget"), "field-label"), target);
        body.setPrefWidth(480);
        Dialogs.form(I18n.t("bulk.merge"), body, I18n.t("bulk.mergeConfirm"), () -> {
            Long main = target.getValue();
            if (main == null) {
                return false;
            }
            List<Long> others = selected.stream().map(ItilItem::getId).filter(id -> !id.equals(main)).toList();
            Async.run(() -> App.services().itilBulk().merge(type, main, others, App::label), count -> {
                Toasts.success(I18n.t("bulk.merged", count));
                done.run();
            });
            return true;
        });
    }

    private void duplicate(ItilItem item) {
        Async.run(() -> service().duplicate(item.getId()), copy -> {
            Toasts.success(I18n.t("itil.created", copy.reference()));
            App.nav().open(new ItilDetailView(type, copy.getId()));
        });
    }

    private void run(Function<List<Long>, BulkResult> work) {
        run(ids(), work);
    }

    private void run(List<Long> ids, Function<List<Long>, BulkResult> work) {
        List<Long> chosen = List.copyOf(ids);
        Async.run(() -> work.apply(chosen), r -> {
            report(r);
            done.run();
        });
    }

    static void report(BulkResult r) {
        if (r.allSucceeded()) {
            Toasts.success(I18n.t("bulk.done", r.succeeded()));
            return;
        }
        Toasts.warning(I18n.t("bulk.partial", r.succeeded(), r.failures().size()));
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (Failure f : r.failures()) {
            if (shown++ == 8) {
                sb.append("…");
                break;
            }
            sb.append('#').append(f.id()).append(": ").append(I18n.error(f.error())).append('\n');
        }
        Dialogs.info(I18n.t("bulk.partialTitle"), sb.toString().trim());
    }
}
