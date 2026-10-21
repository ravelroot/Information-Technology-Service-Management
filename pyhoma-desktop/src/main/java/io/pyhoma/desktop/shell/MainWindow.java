package io.pyhoma.desktop.shell;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Theme;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Avatar;
import io.pyhoma.desktop.ui.components.PickerField;
import io.pyhoma.domain.model.core.Notification;
import io.pyhoma.domain.model.core.OrgUnit;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.security.UserSession;
import io.pyhoma.domain.service.NotificationService;
import io.pyhoma.domain.service.SettingKeys;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class MainWindow {

    private final Stage stage;
    private final java.util.function.Consumer<String> onLogout;
    private volatile long lastActivity = System.nanoTime();
    private int idleMinutes = 15;
    private Timeline watchdog;
    private boolean ended;
    private static MainWindow active;
    private final Map<Route, HBox> navItems = new EnumMap<>(Route.class);
    private final Label bellCount = Ui.label("", "bell-count");
    private Timeline poller;
    private Navigator navigator;

    public MainWindow(Stage stage, java.util.function.Consumer<String> onLogout) {
        this.stage = stage;
        this.onLogout = onLogout;
    }

    public void show() {
        if (active != null && active != this) {
            active.dispose();
        }
        active = this;
        StackPane content = new StackPane();
        content.getStyleClass().add("content");
        navigator = new Navigator(content, this::select, t -> stage.setTitle(t + " — Pyhoma."));
        App.setNavigator(navigator);

        BorderPane frame = new BorderPane();
        frame.setLeft(sidebar());
        BorderPane main = new BorderPane();
        main.setTop(App.demo() ? new VBox(demoBanner(), topbar()) : topbar());
        main.setCenter(content);
        frame.setCenter(main);

        StackPane root = new StackPane(io.pyhoma.desktop.ui.Zoom.wrap(frame));
        root.getStyleClass().add("app-root");
        Toasts.attach(root);
        javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
        double width = Math.min(parse(App.config().get("ui.width", "1366"), 1366), screen.getWidth() * 0.94);
        double height = Math.min(parse(App.config().get("ui.height", "820"), 820), screen.getHeight() * 0.92);
        double[] size = Ui.sceneSize(stage, width, height);
        Scene scene = new Scene(root, size[0], size[1]);
        Theme.install(scene);
        io.pyhoma.desktop.ui.Motion.install(scene);
        io.pyhoma.desktop.ui.Zoom.install(scene, root);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN), this::openSearch);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                () -> navigator.go(Route.TICKETS));
        scene.addEventFilter(javafx.scene.input.InputEvent.ANY, e -> lastActivity = System.nanoTime());
        stage.setScene(scene);
        stage.setMinWidth(Math.min(1100, screen.getWidth() * 0.9));
        stage.setMinHeight(Math.min(680, screen.getHeight() * 0.9));
        stage.setOnCloseRequest(e -> saveGeometry());
        Dialogs.setOwner(stage);
        Ui.showMaximized(stage);
        io.pyhoma.desktop.ui.Motion.fadeIn(frame, 260);
        navigator.go(App.session().isSelfService() ? Route.HOME : (App.can(io.pyhoma.domain.model.core.Module.TICKET, Right.READ)
                ? Route.DASHBOARD : Route.HOME));
        startPolling();
    }

    private static double parse(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void saveGeometry() {
        if (stage.getScene() != null && !stage.isMaximized()) {
            App.config().set("ui.width", String.valueOf((int) stage.getWidth()));
            App.config().set("ui.height", String.valueOf((int) stage.getHeight()));
            App.config().save();
        }
        if (poller != null) {
            poller.stop();
        }
    }

    private Node sidebar() {
        VBox side = new VBox();
        side.getStyleClass().add("sidebar");
        side.setPrefWidth(232);

        Node brand = brand(20);
        VBox brandBox = new VBox(2, brand, Ui.label(App.services().settings().get(SettingKeys.ORGANIZATION_NAME, ""), "brand-org"));
        brandBox.getStyleClass().add("brand-box");

        VBox items = new VBox(1);
        items.getStyleClass().add("nav");
        UserSession s = App.session();
        Route.Section currentSection = null;
        for (Route r : Route.values()) {
            if (!visible(r, s)) {
                continue;
            }
            if (r.section() != currentSection) {
                currentSection = r.section();
                if (r.section() != Route.Section.MAIN) {
                    Label header = Ui.label(I18n.t("nav.section." + r.section().name()), "nav-section");
                    items.getChildren().add(header);
                }
            }
            HBox item = navItem(r, s.isSelfService());
            navItems.put(r, item);
            items.getChildren().add(item);
        }
        ScrollPane scroll = new ScrollPane(items);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("nav-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Label db = Ui.label((App.demo() ? I18n.t("demo.footer") : App.services().store().describe()), "nav-footer");
        db.setGraphic(Icons.of("database", 13));
        db.setWrapText(true);
        Label version = Ui.label("v" + io.pyhoma.domain.service.Services.VERSION, "nav-footer");
        VBox footer = new VBox(2, db, version);
        footer.getStyleClass().add("nav-footer-box");
        side.getChildren().addAll(brandBox, scroll, footer);
        return side;
    }

    public static Node brand(double size) {
        Text name = new Text("Pyhoma");
        name.getStyleClass().add("brand-name");
        name.setStyle("-fx-font-size: " + size + "px;");
        Text dot = new Text(".");
        dot.getStyleClass().add("brand-dot");
        dot.setStyle("-fx-font-size: " + size + "px;");
        HBox box = new HBox(Math.round(size * 0.4), io.pyhoma.desktop.ui.components.Logo.mark(Math.round(size * 1.25)),
                new TextFlow(name, dot));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static boolean visible(Route r, UserSession s) {
        if (s.isSelfService()) {
            return r == Route.HOME || r == Route.TICKETS || r == Route.KNOWLEDGE;
        }
        if (r == Route.HOME) {
            return false;
        }
        return r.module() == null || s.can(r.module(), Right.READ);
    }

    private HBox navItem(Route r, boolean selfService) {
        String key = selfService && r == Route.TICKETS ? "nav.MY_TICKETS" : "nav." + r.name();
        Label l = new Label(I18n.t(key));
        HBox item = new HBox(10, Icons.of(r.icon(), 16), l);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("nav-item");
        item.setFocusTraversable(true);
        io.pyhoma.desktop.ui.Motion.hoverShift(item, 3, 0);
        item.setOnMouseClicked(e -> navigator.go(r));
        item.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                navigator.go(r);
            }
        });
        item.setAccessibleText(l.getText());
        return item;
    }

    private void select(Route r) {
        navItems.forEach((route, node) -> node.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"),
                route == r));
    }

    private Node topbar() {
        Button search = Ui.button(I18n.t("topbar.search"), "search", this::openSearch, "search-launcher");
        search.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        Label shortcut = Ui.label("Ctrl K", "kbd");
        HBox searchBox = new HBox(search, shortcut);
        searchBox.getStyleClass().add("search-box");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        search.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(search, Priority.ALWAYS);
        searchBox.setPrefWidth(380);

        ProgressIndicator busy = new ProgressIndicator();
        busy.getStyleClass().add("busy");
        busy.setPrefSize(16, 16);
        busy.visibleProperty().bind(Async.running().greaterThan(0));

        Node unit = orgUnitSelector();

        Button theme = Ui.iconButton(Theme.mode() == Theme.Mode.DARK ? "sun" : "moon", I18n.t("topbar.theme"), null);
        theme.setOnAction(e -> {
            Theme.Mode next = Theme.mode() == Theme.Mode.DARK ? Theme.Mode.LIGHT : Theme.Mode.DARK;
            Theme.setMode(next);
            theme.setGraphic(Icons.of(next == Theme.Mode.DARK ? "sun" : "moon", 16));
            App.config().set("ui.theme", next.name());
            App.config().save();
        });

        Button bell = Ui.iconButton("bell", I18n.t("topbar.notifications"), null);
        StackPane bellStack = new StackPane(bell, bellCount);
        StackPane.setAlignment(bellCount, Pos.TOP_RIGHT);
        bellCount.setVisible(false);
        bellCount.setMouseTransparent(true);
        bell.setOnAction(e -> openNotifications(bell));

        MenuButton user = new MenuButton();
        user.getStyleClass().add("user-menu");
        user.setGraphic(new Avatar(App.session().fullName(), 28));
        user.setText(App.session().fullName());
        MenuItem profile = new MenuItem(I18n.t("menu.myAccount"));
        profile.setOnAction(e -> navigator.open(new io.pyhoma.desktop.views.admin.UserDetailView(App.session().userId())));
        MenuItem password = new MenuItem(I18n.t("menu.changePassword"));
        password.setOnAction(e -> io.pyhoma.desktop.views.admin.PasswordDialog.changeOwn());
        MenuItem about = new MenuItem(I18n.t("menu.about"));
        about.setOnAction(e -> io.pyhoma.desktop.views.AboutDialog.show());
        MenuItem logout = new MenuItem(I18n.t("menu.logout"));
        logout.setOnAction(e -> {
            endSession(null);
        });
        user.getItems().addAll(profile, password, new SeparatorMenuItem(), about, new SeparatorMenuItem(), logout);

        Region gap = Ui.spacer();
        HBox bar = new HBox(10, searchBox, gap, busy, unit, theme, bellStack, user);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("topbar");
        return bar;
    }

    private Node demoBanner() {
        Label text = Ui.label(I18n.t("demo.banner"), "demo-banner-text");
        text.setGraphic(Icons.of("eye", 14));
        Button setup = Ui.button(I18n.t("demo.setup"), "database",
                () -> io.pyhoma.desktop.StartupFlow.instance().leaveDemo(true));
        Button exit = Ui.button(I18n.t("demo.exit"), "log-out", () -> endSession(null));
        HBox banner = new HBox(12, text, Ui.spacer(), setup, exit);
        banner.getStyleClass().add("demo-banner");
        return banner;
    }

    private Node orgUnitSelector() {
        List<OrgUnit> units = App.services().auth().accessibleOrgUnits();
        if (units.size() <= 1) {
            Label only = Ui.label(units.isEmpty() ? "" : units.get(0).getName(), "unit-label");
            only.setGraphic(Icons.of("building-2", 14));
            return only;
        }
        PickerField<Long> picker = new PickerField<>(() -> {
            List<Option<Long>> list = new ArrayList<>();
            for (OrgUnit u : units) {
                list.add(new Option<>(u.getId(), u.getCompleteName()));
            }
            return list;
        }, false);
        picker.getStyleClass().add("compact");
        picker.setValue(App.session().activeOrgUnitId());
        picker.setPrefWidth(230);
        CheckBox children = new CheckBox(I18n.t("topbar.subUnits"));
        children.setSelected(App.session().includeChildren());
        Runnable apply = () -> Async.run(() -> App.services().auth().switchOrgUnit(picker.getValue(), children.isSelected()),
                s -> {
                    Toasts.info(I18n.t("topbar.unitChanged", App.lookups().orgUnit(s.activeOrgUnitId())));
                    navigator.refresh();
                });
        picker.onChange(v -> apply.run());
        children.setOnAction(e -> apply.run());
        HBox box = new HBox(8, Icons.of("building-2", 14), picker, children);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void openSearch() {
        GlobalSearch.show(stage);
    }

    private void startPolling() {
        Runnable check = () -> Async.run(() -> {
            if (!App.services().auth().validateSession()) {
                return -1L;
            }
            idleMinutes = App.services().settings().getInt(SettingKeys.SESSION_IDLE_MINUTES, 15);
            return App.services().notifications().unreadCount();
        }, n -> {
            if (n < 0) {
                endSession("login.sessionEnded");
                return;
            }
            bellCount.setText(n > 99 ? "99+" : String.valueOf(n));
            bellCount.setVisible(n > 0);
        }, e -> { });
        check.run();
        poller = new Timeline(new KeyFrame(Duration.seconds(30), e -> check.run()));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();
        if (App.demo()) {
            return;
        }
        watchdog = new Timeline(new KeyFrame(Duration.seconds(10), e -> {
            int minutes = Math.max(1, Math.min(240, idleMinutes));
            if (System.nanoTime() - lastActivity > java.util.concurrent.TimeUnit.MINUTES.toNanos(minutes)) {
                endSession("login.idleLocked");
            }
        }));
        watchdog.setCycleCount(Timeline.INDEFINITE);
        watchdog.play();
    }

    private void dispose() {
        ended = true;
        if (poller != null) {
            poller.stop();
        }
        if (watchdog != null) {
            watchdog.stop();
        }
    }

    public static void closeActive(Runnable after) {
        if (active != null && !active.ended) {
            active.close(after);
        } else {
            after.run();
        }
    }

    private void endSession(String noticeKey) {
        close(() -> onLogout.accept(noticeKey == null ? null : I18n.t(noticeKey)));
    }

    private void close(Runnable after) {
        if (ended) {
            return;
        }
        ended = true;
        saveGeometry();
        if (watchdog != null) {
            watchdog.stop();
        }
        for (javafx.stage.Window w : new java.util.ArrayList<>(javafx.stage.Window.getWindows())) {
            if (w != stage) {
                w.hide();
            }
        }
        after.run();
    }

    public static String notificationText(Notification n) {
        String[] args = n.getMessage() == null ? new String[0] : n.getMessage().split(NotificationService.ARG_SEPARATOR, -1);
        Object[] values = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            values[i] = I18n.has("enum.ItilStatus." + a) ? I18n.t("enum.ItilStatus." + a) : a;
        }
        return I18n.t(n.getTitle(), values);
    }

    private void openNotifications(Node anchor) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        VBox list = new VBox();
        list.getStyleClass().add("notif-list");
        Label title = Ui.label(I18n.t("notifications.title"), "panel-title");
        Button markAll = Ui.ghost(I18n.t("notifications.markAll"), "check", null);
        HBox head = Ui.row(8, title, Ui.spacer(), markAll);
        head.getStyleClass().add("notif-head");
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setPrefSize(400, 420);
        VBox box = new VBox(head, scroll);
        box.getStyleClass().add("notif-popup");
        box.getStylesheets().setAll(stage.getScene().getStylesheets());
        markAll.setOnAction(e -> Async.exec(() -> App.services().notifications().markAllSeen(), () -> {
            popup.hide();
            bellCount.setVisible(false);
        }));
        Async.run(() -> App.services().notifications().mine(false, 0, 40).items(), items -> {
            if (items.isEmpty()) {
                list.getChildren().add(new io.pyhoma.desktop.ui.components.EmptyState("bell", I18n.t("notifications.empty"), null, null));
            }
            for (Notification n : items) {
                list.getChildren().add(notificationRow(n, popup));
            }
        });
        popup.getContent().add(box);
        Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
        popup.show(anchor, b.getMaxX() - 400, b.getMaxY() + 6);
    }

    private Node notificationRow(Notification n, Popup popup) {
        String tone = n.getSeverity() == null ? "info" : n.getSeverity().toLowerCase();
        Region led = new Region();
        led.getStyleClass().addAll("notif-led", "sev-" + tone);
        Label text = new Label(notificationText(n));
        text.setWrapText(true);
        text.getStyleClass().add(n.isSeen() ? "notif-seen" : "notif-unseen");
        Label when = Ui.label(I18n.relative(n.getCreatedAt()), "muted");
        VBox texts = new VBox(2, text, when);
        HBox.setHgrow(texts, Priority.ALWAYS);
        HBox row = new HBox(10, led, texts);
        row.getStyleClass().add("notif-row");
        row.setOnMouseClicked(e -> {
            popup.hide();
            Async.exec(() -> App.services().notifications().markSeen(n.getId()), () -> {
                navigator.openItem(n.getItemType(), n.getItemId());
                startPollingOnce();
            });
        });
        return row;
    }

    private void startPollingOnce() {
        Async.run(() -> App.services().notifications().unreadCount(), c -> {
            bellCount.setText(String.valueOf(c));
            bellCount.setVisible(c > 0);
        }, e -> { });
    }
}
