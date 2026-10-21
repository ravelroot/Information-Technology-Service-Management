package io.pyhoma.desktop.views.admin;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.shell.View;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Avatar;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.desktop.ui.components.PageHeader;
import io.pyhoma.desktop.views.assets.AssetDetailView;
import io.pyhoma.desktop.views.common.HistoryPanel;
import io.pyhoma.desktop.views.itil.ItilDetailView;
import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.ProfileAssignment;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.itil.ItilItem;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.security.PasswordHasher;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.ItilSearch;
import io.pyhoma.domain.support.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class UserDetailView extends View {

    private Long id;
    private User user;
    private final PageHeader header = new PageHeader("", null);
    private final TabPane tabs = new TabPane();
    private Form form;
    private final PasswordField password = new PasswordField();

    public UserDetailView(Long id) {
        this.id = id;
    }

    private boolean self() {
        return Objects.equals(id, App.session().userId());
    }

    private boolean canEdit() {
        return App.can(Module.USER, id == null ? Right.CREATE : Right.UPDATE) || self();
    }

    @Override
    public String title() {
        return user == null || user.getFullName() == null ? I18n.t("users.new") : user.getFullName();
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
            user = new User();
            user.setLanguage(I18n.languageTag());
            user.setDefaultOrgUnitId(App.session().activeOrgUnitId());
            render();
        } else {
            Async.run(() -> App.services().users().get(id), u -> {
                user = u;
                render();
            });
        }
    }

    private void render() {
        boolean creating = user.isNew();
        header.setTitle(creating ? I18n.t("users.new") : user.getFullName());
        header.setSubtitle(creating ? null : user.getLogin() + (user.getJobTitle() == null ? "" : "  •  " + user.getJobTitle()));
        if (!creating) {
            List<Node> tags = new ArrayList<>();
            tags.add(new Avatar(user.getFullName(), 28));
            tags.add(Badge.of(I18n.t(user.isActive() ? "common.active" : "common.inactive"), user.isActive() ? "green" : "gray"));
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.LocalDateTime.now())) {
                tags.add(Badge.of(I18n.t("users.locked"), "red"));
            }
            header.tags(tags.toArray(Node[]::new));
        }
        List<Node> actions = new ArrayList<>();
        if (!creating && App.can(Module.USER, Right.UPDATE)) {
            actions.add(Ui.button(I18n.t("users.unlock"), "lock", () -> Async.exec(() -> App.services().users().unlock(id),
                    () -> Toasts.success(I18n.t("users.unlocked")))));
            actions.add(Ui.button(I18n.t("users.resetPassword"), "key-round", () -> PasswordDialog.reset(id)));
        }
        if (canEdit()) {
            actions.add(Ui.primary(I18n.t(creating ? "action.create" : "action.save"), "save", this::save));
        }
        header.actions(actions.toArray(Node[]::new));
        tabs.getTabs().setAll(tab("users.tab.account", "user", account()));
        if (!creating) {
            tabs.getTabs().add(tab("users.tab.profiles", "shield-check", profilesTab()));
            tabs.getTabs().add(tab("users.tab.groups", "users-round", groupsTab()));
            if (App.can(Module.ASSET, Right.READ)) {
                tabs.getTabs().add(tab("users.tab.assets", "monitor", assetsTab()));
            }
            if (App.can(Module.TICKET, Right.READ)) {
                tabs.getTabs().add(tab("users.tab.tickets", "ticket", ticketsTab()));
            }
            HistoryPanel h = new HistoryPanel("User", id);
            h.load();
            tabs.getTabs().add(tab("tab.history", "history", h));
        }
    }

    private static Tab tab(String key, String icon, Node content) {
        Tab t = new Tab(I18n.t(key), content);
        t.setGraphic(Icons.of(icon, 14));
        return t;
    }

    private Node account() {
        User u = user;
        boolean admin = App.can(Module.USER, u.isNew() ? Right.CREATE : Right.UPDATE);
        form = new Form(3).readOnly(!canEdit())
                .text("fullName", u::getFullName, u::setFullName, 2, true)
                .code("login", u::getLogin, u::setLogin)
                .text("email", u::getEmail, u::setEmail)
                .text("phone", u::getPhone, u::setPhone)
                .text("jobTitle", u::getJobTitle, u::setJobTitle)
                .choice("language", () -> List.of(new Option<>("pt-BR", "Português (Brasil)"), new Option<>("en", "English")),
                        u::getLanguage, u::setLanguage, false, true)
                .choice("defaultOrgUnitId", () -> Option.of(App.lookups().orgUnits()), u::getDefaultOrgUnitId, u::setDefaultOrgUnitId, true, false)
                .choice("locationId", () -> Option.of(App.lookups().dropdown(DropdownKind.LOCATION)), u::getLocationId, u::setLocationId, true, false);
        if (admin) {
            form.check("active", u::isActive, u::setActive);
        }
        form.area("notes", u::getNotes, u::setNotes, 3, false);
        form.load();
        VBox box = new VBox(16, form.node());
        if (u.isNew()) {
            box.getChildren().add(Ui.fact(I18n.t("password.initial"), password));
            box.getChildren().add(Ui.muted(I18n.t("password.rules", PasswordHasher.MIN_LENGTH)));
        }
        ScrollPane sp = new ScrollPane(Ui.style(box, "tab-body"));
        sp.setFitToWidth(true);
        return sp;
    }

    private void save() {
        if (!form.commit()) {
            return;
        }
        Async.run(() -> user.isNew() ? App.services().users().create(user, password.getText()) : App.services().users().save(user), u -> {
            boolean created = id == null;
            id = u.getId();
            user = u;
            Toasts.success(I18n.t(created ? "users.created" : "toast.saved"));
            App.lookups().refreshAsync(null);
            render();
            if (created) {
                tabs.getSelectionModel().select(1);
            }
        }, e -> {
            if (e instanceof ValidationException v && form.showErrors(v)) {
                return;
            }
            Dialogs.error(e);
        });
    }

    private Node profilesTab() {
        DataTable<ProfileAssignment> table = new DataTable<>();
        table.col(I18n.t("field.profileId"), 220, a -> io.pyhoma.desktop.ui.LookupCache.name(App.lookups().profiles(), a.getProfileId()))
                .col(I18n.t("field.orgUnitId"), 300, a -> App.lookups().orgUnit(a.getOrgUnitId()))
                .col(I18n.t("field.includeChildren"), 140, a -> I18n.t(a.isIncludeChildren() ? "common.yes" : "common.no"));
        table.placeholder(new EmptyState("shield-check", I18n.t("users.noProfiles"), I18n.t("users.noProfilesHint"), null));
        Runnable reload = () -> Async.run(() -> App.services().users().assignments(id), table::setRows);
        reload.run();
        HBox actions = Ui.row(8, Ui.spacer());
        if (App.can(Module.USER, Right.UPDATE)) {
            actions.getChildren().addAll(
                    Ui.button(I18n.t("users.removeProfile"), "x", () -> {
                        ProfileAssignment a = table.selected();
                        if (a != null) {
                            Async.exec(() -> App.services().users().removeAssignment(a.getId()), reload);
                        }
                    }, "ghost"),
                    Ui.primary(I18n.t("users.addProfile"), "plus", () -> {
                        Long[] v = new Long[2];
                        CheckBox children = new CheckBox(I18n.t("field.includeChildren"));
                        children.setSelected(true);
                        Form f = new Form(1)
                                .choice("profileId", () -> Option.of(App.lookups().profiles()), () -> v[0], x -> v[0] = x, false, true)
                                .choice("orgUnitId", () -> Option.of(App.lookups().orgUnits()), () -> v[1], x -> v[1] = x, false, true);
                        f.load();
                        VBox content = new VBox(10, f.node(), children);
                        content.setPrefWidth(420);
                        Dialogs.form(I18n.t("users.addProfile"), content, I18n.t("action.add"), () -> {
                            f.commit();
                            if (v[0] == null || v[1] == null) {
                                return false;
                            }
                            App.services().users().addAssignment(id, v[0], v[1], children.isSelected());
                            reload.run();
                            return true;
                        });
                    }));
        }
        VBox.setVgrow(table, Priority.ALWAYS);
        return Ui.style(new VBox(10, actions, table), "tab-body");
    }

    private Node groupsTab() {
        DataTable<Long> table = new DataTable<>();
        table.col(I18n.t("field.groupId"), 300, g -> App.lookups().group(g));
        table.placeholder(new EmptyState("users-round", I18n.t("users.noGroups"), null, null));
        Async.run(() -> App.services().users().groupIds(id), table::setRows);
        VBox.setVgrow(table, Priority.ALWAYS);
        return Ui.style(new VBox(10, table), "tab-body");
    }

    private Node assetsTab() {
        DataTable<Asset> table = new DataTable<>();
        table.col(I18n.t("field.name"), 220, Asset::getName)
                .col(I18n.t("field.assetType"), 140, a -> I18n.e(a.getAssetType()))
                .col(I18n.t("field.assetTag"), 130, "mono", Asset::getAssetTag)
                .col(I18n.t("field.serialNumber"), 150, "mono", Asset::getSerialNumber);
        table.placeholder(new EmptyState("monitor", I18n.t("users.noAssets"), null, null));
        table.onOpen(a -> App.nav().open(new AssetDetailView(a.getId())));
        Async.run(() -> App.services().assets().ofUser(id), table::setRows);
        VBox.setVgrow(table, Priority.ALWAYS);
        return Ui.style(new VBox(10, table), "tab-body");
    }

    private Node ticketsTab() {
        DataTable<ItilItem> table = new DataTable<>();
        table.col("#", 70, "num", ItilItem::getId)
                .col(I18n.t("field.title"), 320, ItilItem::getTitle)
                .node(I18n.t("field.status"), 130, i -> Badge.status(i.getStatus()))
                .col(I18n.t("field.openedAt"), 140, i -> I18n.dateTime(i.getOpenedAt()));
        table.placeholder(new EmptyState("ticket", I18n.t("users.noTickets"), null, null));
        table.onOpen(i -> App.nav().open(new ItilDetailView(ItilType.TICKET, i.getId())));
        Async.run(() -> new ArrayList<ItilItem>(App.services().tickets().search(ItilSearch.create().view(ItilSearch.View.ALL)
                .requesterId(id).page(0, 200)).items()), table::setRows);
        VBox.setVgrow(table, Priority.ALWAYS);
        return Ui.style(new VBox(10, table), "tab-body");
    }
}
