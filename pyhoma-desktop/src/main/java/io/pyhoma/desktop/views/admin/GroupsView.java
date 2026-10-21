package io.pyhoma.desktop.views.admin;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.desktop.views.itil.ItilListViewHelper;
import io.pyhoma.domain.model.core.GroupMember;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.UserGroup;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.ValidationException;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GroupsView extends MasterDetail {

    private final DataTable<UserGroup> groups = new DataTable<>();

    @Override
    protected String titleText() {
        return I18n.t("nav.GROUPS");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("groups.subtitle");
    }

    @Override
    protected Node[] actions() {
        if (!App.can(Module.GROUP, Right.CREATE)) {
            return new Node[0];
        }
        return new Node[] {Ui.primary(I18n.t("groups.new"), "plus", () -> {
            UserGroup g = new UserGroup();
            g.setOrgUnitId(App.session().activeOrgUnitId());
            edit(g);
        })};
    }

    @Override
    protected Node master() {
        groups.col(I18n.t("field.name"), 200, UserGroup::getName);
        groups.autoSelectFirst();
        groups.getSelectionModel().selectedItemProperty().addListener((o, a, g) -> {
            if (g != null) {
                edit(g);
            }
        });
        groups.placeholder(new EmptyState("users-round", I18n.t("groups.empty"), null, null));
        return groups;
    }

    @Override
    public void onShow() {
        Async.run(() -> App.services().groups().search(null, 0, 1000).items(), groups::setRows);
    }

    private void edit(UserGroup g) {
        boolean canEdit = App.can(Module.GROUP, g.isNew() ? Right.CREATE : Right.UPDATE);
        Form f = new Form(2).readOnly(!canEdit)
                .text("name", g::getName, g::setName, 2, true)
                .choice("orgUnitId", () -> Option.of(App.lookups().orgUnits()), g::getOrgUnitId, g::setOrgUnitId, false, true)
                .text("email", g::getEmail, g::setEmail)
                .check("assignable", g::isAssignable, g::setAssignable)
                .check("requester", g::isRequester, g::setRequester)
                .area("notes", g::getNotes, g::setNotes, 2, false);
        f.load();
        HBox actions = Ui.row(8, Ui.spacer());
        if (!g.isNew() && App.can(Module.GROUP, Right.DELETE)) {
            actions.getChildren().add(Ui.button(I18n.t("action.delete"), "trash-2", () -> {
                if (Dialogs.confirm(I18n.t("action.delete"), I18n.t("confirm.delete"), I18n.t("action.delete"), true)) {
                    Async.exec(() -> App.services().groups().delete(g.getId()), () -> {
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
                Async.run(() -> App.services().groups().save(g), saved -> {
                    Toasts.success(I18n.t("toast.saved"));
                    App.lookups().refreshAsync(null);
                    onShow();
                    edit(saved);
                }, e -> {
                    if (!(e instanceof ValidationException v && f.showErrors(v))) {
                        Dialogs.error(e);
                    }
                });
            }));
        }
        VBox box = new VBox(14, Ui.panel(g.isNew() ? I18n.t("groups.new") : g.getName(), f.node()), actions);
        if (!g.isNew()) {
            box.getChildren().add(members(g, canEdit));
        }
        detail.getChildren().setAll(box);
    }

    private Node members(UserGroup g, boolean canEdit) {
        DataTable<GroupMember> table = new DataTable<>();
        table.col(I18n.t("field.userId"), 260, m -> App.lookups().user(m.getUserId()))
                .col(I18n.t("field.manager"), 120, m -> I18n.t(m.isManager() ? "common.yes" : "common.no"));
        table.placeholder(new EmptyState("users", I18n.t("groups.noMembers"), null, null));
        Runnable reload = () -> Async.run(() -> App.services().groups().members(g.getId()), table::setRows);
        reload.run();
        HBox actions = Ui.row(8, Ui.label(I18n.t("groups.members"), "panel-title"), Ui.spacer());
        if (canEdit) {
            actions.getChildren().addAll(
                    Ui.button(I18n.t("groups.toggleManager"), "star", () -> {
                        GroupMember m = table.selected();
                        if (m != null) {
                            Async.exec(() -> App.services().groups().setManager(m.getId(), !m.isManager()), reload);
                        }
                    }, "ghost"),
                    Ui.button(I18n.t("groups.removeMember"), "x", () -> {
                        GroupMember m = table.selected();
                        if (m != null) {
                            Async.exec(() -> App.services().groups().removeMember(m.getId()), reload);
                        }
                    }, "ghost"),
                    Ui.button(I18n.t("groups.addMember"), "user-plus", () -> {
                        Long[] user = new Long[1];
                        Form f = new Form(1).choice("userId", ItilListViewHelper::users, () -> user[0], v -> user[0] = v, false, true);
                        f.load();
                        ((javafx.scene.layout.Region) f.node()).setPrefWidth(380);
                        Dialogs.form(I18n.t("groups.addMember"), f.node(), I18n.t("action.add"), () -> {
                            f.commit();
                            if (user[0] == null) {
                                return false;
                            }
                            App.services().groups().addMember(g.getId(), user[0], false);
                            reload.run();
                            return true;
                        });
                    }));
        }
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox box = new VBox(10, actions, table);
        box.getStyleClass().add("panel");
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }
}
