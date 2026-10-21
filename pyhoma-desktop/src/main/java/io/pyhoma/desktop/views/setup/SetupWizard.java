package io.pyhoma.desktop.views.setup;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.shell.MainWindow;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Motion;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.domain.model.core.DropdownItem;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.InterfaceType;
import io.pyhoma.domain.model.core.OrgUnit;
import io.pyhoma.domain.model.core.Profile;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.core.UserGroup;
import io.pyhoma.domain.security.PasswordHasher;
import io.pyhoma.domain.service.InstallationService.SetupStatus;
import io.pyhoma.domain.service.SettingKeys;
import io.pyhoma.domain.support.ValidationException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class SetupWizard {

    public enum Step {
        ORGANIZATION(true), UNITS(false), LOCATIONS(true), TEAM(true), HOURS(true), CATEGORIES(false), DONE(false);

        private final boolean required;

        Step(boolean required) {
            this.required = required;
        }

        public boolean required() {
            return required;
        }
    }

    private final Consumer<Parent> show;
    private final Runnable onFinish;
    private Step step = Step.ORGANIZATION;
    private SetupStatus status;
    private final Set<Step> visited = EnumSet.noneOf(Step.class);

    public SetupWizard(Consumer<Parent> show, Runnable onFinish) {
        this.show = show;
        this.onFinish = onFinish;
    }

    public void start() {
        go(Step.ORGANIZATION);
    }

    private void go(Step target) {
        Async.run(() -> App.services().installation().setupStatus(), s -> {
            status = s;
            step = target;
            visited.add(target);
            render();
        });
    }

    private void render() {
        Node body = switch (step) {
            case ORGANIZATION -> organization();
            case UNITS -> units();
            case LOCATIONS -> locations();
            case TEAM -> team();
            case HOURS -> hours();
            case CATEGORIES -> categories();
            case DONE -> done();
        };
        Label counter = Ui.label(I18n.t("setup.counter", step.ordinal() + 1, Step.values().length), "setup-counter");
        Label title = Ui.label(I18n.t("setup." + key(step) + ".title"), "launcher-title");
        Label text = new Label(I18n.t("setup." + key(step) + ".text"));
        text.setWrapText(true);
        text.getStyleClass().add("setup-text");
        HBox head = new HBox(10, counter);
        if (step.required()) {
            head.getChildren().add(Ui.label(I18n.t("setup.required"), "setup-required"));
        }
        head.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(16, head, title, text, body);
        box.setMaxWidth(680);
        box.getStyleClass().add("setup-body");
        StackPane holder = new StackPane(box);
        holder.setAlignment(Pos.TOP_CENTER);
        ScrollPane sp = new ScrollPane(holder);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("launcher-scroll");
        holder.paddingProperty().bind(javafx.beans.binding.Bindings.createObjectBinding(
                () -> new javafx.geometry.Insets(Math.max(0, (sp.getViewportBounds().getHeight() - 680) / 2), 0, 0, 0),
                sp.viewportBoundsProperty()));
        StackPane right = new StackPane(sp);
        right.getStyleClass().add("launcher-content");
        HBox.setHgrow(right, Priority.ALWAYS);
        show.accept(new HBox(sidePanel(), right));
    }

    private Node sidePanel() {
        VBox list = new VBox(4);
        for (Step s : Step.values()) {
            boolean done = isDone(s);
            Label num = Ui.label(done && s != step ? "" : String.valueOf(s.ordinal() + 1), "setup-step-num");
            if (done && s != step) {
                num.setGraphic(Icons.of("check", 12));
            }
            Label name = Ui.label(I18n.t("setup." + key(s) + ".name"), "setup-step-name");
            HBox row = new HBox(10, num, name);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("setup-step");
            if (s == step) {
                row.getStyleClass().add("current");
            } else if (done) {
                row.getStyleClass().add("done");
            }
            if (s != step && canJump(s)) {
                row.getStyleClass().add("clickable");
                row.setOnMouseClicked(e -> go(s));
            }
            list.getChildren().add(row);
        }
        Label hint = new Label(I18n.t("setup.sideHint"));
        hint.setWrapText(true);
        hint.getStyleClass().add("brand-tagline");
        VBox panel = new VBox(22, MainWindow.brand(30), Ui.label(I18n.t("setup.sideTitle"), "setup-side-title"), list,
                Ui.spacer(), hint);
        VBox.setVgrow(panel.getChildren().get(3), Priority.ALWAYS);
        panel.getStyleClass().addAll("brand-panel", "setup-side");
        panel.setPrefWidth(340);
        panel.setMinWidth(300);
        return panel;
    }

    private boolean isDone(Step s) {
        if (status == null) {
            return false;
        }
        return switch (s) {
            case ORGANIZATION -> status.organization();
            case LOCATIONS -> status.locations() > 0;
            case TEAM -> status.groups() > 0;
            case HOURS -> visited.contains(Step.HOURS) && step.ordinal() > Step.HOURS.ordinal();
            case UNITS, CATEGORIES -> visited.contains(s) && step.ordinal() > s.ordinal();
            case DONE -> false;
        };
    }

    private boolean canJump(Step target) {
        for (Step s : Step.values()) {
            if (s == target) {
                return true;
            }
            if (s.required() && !isDone(s) && s != Step.HOURS) {
                return false;
            }
        }
        return true;
    }

    private static String key(Step s) {
        return s.name().toLowerCase(Locale.ROOT);
    }

    private Node footer(Runnable onNext, boolean skippable) {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("setup-footer");
        if (step.ordinal() > 0) {
            bar.getChildren().add(Ui.ghost(I18n.t("action.back"), "chevron-left", () -> go(Step.values()[step.ordinal() - 1])));
        }
        bar.getChildren().add(Ui.spacer());
        if (skippable) {
            bar.getChildren().add(Ui.button(I18n.t("setup.skip"), "chevron-right", this::next));
        }
        Button next = Ui.primary(I18n.t("setup.saveNext"), "chevron-right", onNext);
        next.setDefaultButton(true);
        bar.getChildren().add(next);
        return bar;
    }

    private void next() {
        go(Step.values()[Math.min(step.ordinal() + 1, Step.values().length - 1)]);
    }

    private void save(Runnable work, Runnable after, Form form) {
        Async.run(() -> {
            work.run();
            return Boolean.TRUE;
        }, ok -> after.run(), e -> {
            if (form != null && e instanceof ValidationException v && form.showErrors(v)) {
                return;
            }
            Dialogs.error(e);
        });
    }

    private Node organization() {
        OrgUnit root = App.services().orgUnits().root();
        String[] name = {App.services().settings().get(SettingKeys.ORGANIZATION_NAME, root.getName())};
        Form f = new Form(2)
                .text("organizationName", () -> name[0], v -> name[0] = v, 2, true)
                .text("email", root::getEmail, root::setEmail, 1, true)
                .text("phone", root::getPhone, root::setPhone, 1, false)
                .text("address", root::getAddress, root::setAddress, 2, false);
        f.load();
        return new VBox(18, f.node(), footer(() -> {
            if (!f.commit()) {
                return;
            }
            save(() -> {
                root.setName(name[0]);
                App.services().orgUnits().save(root);
                App.services().settings().set(SettingKeys.ORGANIZATION_NAME, name[0].trim());
                App.lookups().invalidate();
            }, () -> {
                Toasts.success(I18n.t("toast.saved"));
                next();
            }, f);
        }, false));
    }

    private Node units() {
        OrgUnit root = App.services().orgUnits().root();
        List<OrgUnit> children = App.services().orgUnits().all().stream()
                .filter(u -> u.getParentId() != null).toList();
        OrgUnit draft = new OrgUnit();
        draft.setParentId(root.getId());
        Form f = new Form(2)
                .text("name", draft::getName, draft::setName, 1, true)
                .text("email", draft::getEmail, draft::setEmail, 1, false);
        f.load();
        Button add = Ui.button(I18n.t("setup.add"), "plus", () -> {
            if (!f.commit()) {
                return;
            }
            save(() -> App.services().orgUnits().save(draft), () -> {
                App.lookups().invalidate();
                go(Step.UNITS);
            }, f);
        });
        return new VBox(18, chips(children.stream().map(OrgUnit::getCompleteName).toList(), "building-2", "setup.units.empty"),
                adder(f, add), footer(this::next, false));
    }

    private Node locations() {
        List<DropdownItem> items = App.services().dropdowns().list(DropdownKind.LOCATION, true);
        DropdownItem draft = new DropdownItem();
        draft.setKind(DropdownKind.LOCATION);
        Form f = new Form(2)
                .text("name", draft::getName, draft::setName, 1, true)
                .choice("parentId", () -> Option.of(App.services().dropdowns().names(DropdownKind.LOCATION)), draft::getParentId,
                        draft::setParentId, true, false);
        f.load();
        Button add = Ui.button(I18n.t("setup.add"), "plus", () -> {
            if (!f.commit()) {
                return;
            }
            save(() -> App.services().dropdowns().save(draft), () -> {
                App.lookups().invalidate();
                go(Step.LOCATIONS);
            }, f);
        });
        return new VBox(18, chips(items.stream().map(i -> i.getCompleteName() == null ? i.getName() : i.getCompleteName()).toList(),
                "map-pin", "setup.locations.empty"), adder(f, add), footer(() -> requireThen(status.locations() > 0,
                "setup.missingLocation"), false));
    }

    private void requireThen(boolean ok, String messageKey) {
        if (!ok) {
            Toasts.info(I18n.t(messageKey));
            return;
        }
        next();
    }

    private Node team() {
        Long rootId = App.services().orgUnits().root().getId();
        List<UserGroup> groups = App.services().groups().assignable();
        UserGroup draft = new UserGroup();
        draft.setOrgUnitId(rootId);
        Form gf = new Form(2)
                .text("name", draft::getName, draft::setName, 1, true)
                .text("email", draft::getEmail, draft::setEmail, 1, false);
        gf.load();
        Button addGroup = Ui.button(I18n.t("setup.addGroup"), "plus", () -> {
            if (!gf.commit()) {
                return;
            }
            save(() -> App.services().groups().save(draft), () -> {
                App.lookups().invalidate();
                go(Step.TEAM);
            }, gf);
        });
        VBox content = new VBox(18, Ui.label(I18n.t("setup.team.groups"), "setup-section"),
                chips(groups.stream().map(UserGroup::getName).toList(), "users", "setup.team.empty"), adder(gf, addGroup));
        if (!groups.isEmpty()) {
            User tech = new User();
            tech.setLanguage(I18n.languageTag());
            tech.setDefaultOrgUnitId(rootId);
            javafx.scene.control.PasswordField password = new javafx.scene.control.PasswordField();
            Long[] groupId = {groups.get(0).getId()};
            Form uf = new Form(2)
                    .text("fullName", tech::getFullName, tech::setFullName, 1, true)
                    .text("login", tech::getLogin, tech::setLogin, 1, true)
                    .text("email", tech::getEmail, tech::setEmail, 1, false)
                    .custom("password", password, 1)
                    .choice("groupId", () -> Option.of(groupNames(groups)), () -> groupId[0], v -> groupId[0] = v, false, true);
            uf.load();
            Label rules = Ui.muted(I18n.t("password.rules", PasswordHasher.MIN_LENGTH));
            rules.setWrapText(true);
            Button addTech = Ui.button(I18n.t("setup.addTechnician"), "user-plus", () -> {
                if (!uf.commit()) {
                    return;
                }
                if (!PasswordHasher.isAcceptable(password.getText(), tech.getLogin(), tech.getFullName())) {
                    Toasts.info(I18n.t("password.rules", PasswordHasher.MIN_LENGTH));
                    password.requestFocus();
                    return;
                }
                String secret = password.getText();
                save(() -> {
                    User created = App.services().users().create(tech, secret);
                    Profile profile = technicianProfile();
                    if (profile != null) {
                        App.services().users().addAssignment(created.getId(), profile.getId(), rootId, true);
                    }
                    if (groupId[0] != null) {
                        App.services().groups().addMember(groupId[0], created.getId(), false);
                    }
                }, () -> {
                    Toasts.success(I18n.t("users.created"));
                    App.lookups().invalidate();
                    go(Step.TEAM);
                }, uf);
            });
            List<String> techs = App.services().users().activeUsers().stream()
                    .filter(u -> !u.getId().equals(App.session().userId())).map(User::getFullName).toList();
            content.getChildren().addAll(Ui.label(I18n.t("setup.team.people"), "setup-section"),
                    chips(techs, "user", "setup.team.noPeople"), adder(uf, addTech), rules);
        }
        content.getChildren().add(footer(() -> requireThen(status.groups() > 0, "setup.missingGroup"), false));
        return content;
    }

    private static Map<Long, String> groupNames(List<UserGroup> groups) {
        Map<Long, String> m = new java.util.LinkedHashMap<>();
        groups.forEach(g -> m.put(g.getId(), g.getName()));
        return m;
    }

    private static Profile technicianProfile() {
        List<Profile> all = App.services().profiles().all();
        for (Profile p : all) {
            if (p.getName().equals("Técnico") || p.getName().equals("Technician")) {
                return p;
            }
        }
        return all.stream().filter(p -> !p.isProtectedProfile() && p.getInterfaceType() == InterfaceType.STANDARD)
                .findFirst().orElse(null);
    }

    private Node hours() {
        var cal = App.services().slas().calendar();
        Map<DayOfWeek, CheckBox> days = new EnumMap<>(DayOfWeek.class);
        FlowPane dayBoxes = new FlowPane(12, 8);
        for (DayOfWeek d : DayOfWeek.values()) {
            CheckBox cb = new CheckBox(d.getDisplayName(TextStyle.SHORT, I18n.locale()));
            cb.setSelected(cal.workDays().contains(d));
            days.put(d, cb);
            dayBoxes.getChildren().add(cb);
        }
        TextField start = new TextField(cal.dayStart().toString());
        TextField end = new TextField(cal.dayEnd().toString());
        start.setPrefColumnCount(5);
        end.setPrefColumnCount(5);
        Label error = Ui.label("", "error-text");
        HBox times = Ui.row(10, Ui.label(I18n.t("sla.opensAt"), "field-label"), start, Ui.label(I18n.t("sla.closesAt"), "field-label"),
                end);
        return new VBox(16, Ui.label(I18n.t("sla.workDays"), "field-label"), dayBoxes, times, error,
                Ui.muted(I18n.t("setup.hours.sla")), footer(() -> {
                    Set<DayOfWeek> selected = EnumSet.noneOf(DayOfWeek.class);
                    days.forEach((d, cb) -> {
                        if (cb.isSelected()) {
                            selected.add(d);
                        }
                    });
                    LocalTime s;
                    LocalTime e;
                    try {
                        s = LocalTime.parse(start.getText().trim());
                        e = LocalTime.parse(end.getText().trim());
                    } catch (RuntimeException ex) {
                        error.setText(I18n.t("setup.hours.invalid"));
                        return;
                    }
                    if (selected.isEmpty() || !e.isAfter(s)) {
                        error.setText(I18n.t("setup.hours.invalid"));
                        return;
                    }
                    save(() -> App.services().slas().saveCalendar(selected, s, e), this::next, null);
                }, false));
    }

    private Node categories() {
        List<DropdownItem> items = App.services().dropdowns().list(DropdownKind.ITIL_CATEGORY, true);
        DropdownItem draft = new DropdownItem();
        draft.setKind(DropdownKind.ITIL_CATEGORY);
        Form f = new Form(2)
                .text("name", draft::getName, draft::setName, 1, true)
                .choice("parentId", () -> Option.of(App.services().dropdowns().names(DropdownKind.ITIL_CATEGORY)), draft::getParentId,
                        draft::setParentId, true, false);
        f.load();
        Button add = Ui.button(I18n.t("setup.add"), "plus", () -> {
            if (!f.commit()) {
                return;
            }
            save(() -> App.services().dropdowns().save(draft), () -> {
                App.lookups().invalidate();
                go(Step.CATEGORIES);
            }, f);
        });
        return new VBox(18, chips(items.stream().map(i -> i.getCompleteName() == null ? i.getName() : i.getCompleteName()).toList(),
                "tag", "setup.categories.empty"), adder(f, add), footer(this::next, false));
    }

    private Node done() {
        VBox facts = new VBox(10,
                fact("building-2", I18n.t("setup.done.organization"), App.services().settings().get(SettingKeys.ORGANIZATION_NAME, "")),
                fact("map-pin", I18n.t("setup.done.locations"), String.valueOf(status.locations())),
                fact("users", I18n.t("setup.done.groups"), String.valueOf(status.groups())),
                fact("user", I18n.t("setup.done.users"), String.valueOf(status.users())),
                fact("tag", I18n.t("setup.done.categories"), String.valueOf(status.categories())));
        Button finish = Ui.primary(I18n.t("setup.finish"), "check", () -> save(
                () -> App.services().installation().completeSetup(), onFinish, null));
        finish.setDefaultButton(true);
        HBox bar = new HBox(10, Ui.ghost(I18n.t("action.back"), "chevron-left", () -> go(Step.CATEGORIES)), Ui.spacer(), finish);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("setup-footer");
        return new VBox(18, facts, Ui.muted(I18n.t("setup.done.hint")), bar);
    }

    private static Node fact(String icon, String label, String value) {
        Label l = Ui.label(label, "setup-fact-label");
        l.setGraphic(Icons.of(icon, 15));
        HBox row = new HBox(10, l, Ui.spacer(), Ui.label(value, "setup-fact-value"));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("setup-fact");
        return row;
    }

    private static Node chips(List<String> names, String icon, String emptyKey) {
        if (names.isEmpty()) {
            Label empty = Ui.label(I18n.t(emptyKey), "setup-empty");
            empty.setWrapText(true);
            return empty;
        }
        FlowPane flow = new FlowPane(8, 8);
        for (String n : names) {
            Label chip = Ui.label(n, "setup-chip");
            chip.setGraphic(Icons.of(icon, 13));
            flow.getChildren().add(chip);
            Motion.fadeIn(chip, 260);
        }
        return flow;
    }

    private static Node adder(Form form, Button add) {
        HBox actions = new HBox(add);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox box = new VBox(12, form.node(), actions);
        box.getStyleClass().add("setup-adder");
        ((Region) form.node()).setMaxWidth(Double.MAX_VALUE);
        return box;
    }
}
