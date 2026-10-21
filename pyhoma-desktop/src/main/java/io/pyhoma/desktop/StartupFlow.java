package io.pyhoma.desktop;

import io.pyhoma.desktop.config.AppConfig;
import io.pyhoma.desktop.config.AppPaths;
import io.pyhoma.desktop.shell.MainWindow;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Theme;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.views.admin.PasswordDialog;
import io.pyhoma.desktop.views.setup.DatabaseForm;
import io.pyhoma.domain.memory.InMemoryDataStore;
import io.pyhoma.domain.port.BackendProvider;
import io.pyhoma.domain.port.DataStore;
import io.pyhoma.domain.port.DatabaseSettings;
import io.pyhoma.domain.security.PasswordHasher;
import io.pyhoma.domain.security.UserSession;
import io.pyhoma.domain.service.InstallationService.InstallRequest;
import io.pyhoma.domain.service.Services;
import java.util.ServiceLoader;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class StartupFlow {

    private static StartupFlow instance;
    private final Stage stage;
    private BackendProvider provider;
    private String lastLogin;
    private Scene launcherScene;
    private StackPane launcherSlot;
    private boolean fullWindow;

    public StartupFlow(Stage stage) {
        this.stage = stage;
        instance = this;
    }

    public static StartupFlow instance() {
        return instance;
    }

    public BackendProvider provider() {
        return provider;
    }

    public void start() {
        AppConfig config = AppConfig.load();
        App.setConfig(config);
        I18n.setLanguage(config.get("ui.language", java.util.Locale.getDefault().getLanguage().equals("en") ? "en" : "pt-BR"));
        Theme.setMode(Theme.Mode.valueOf(config.get("ui.theme", "LIGHT")));
        io.pyhoma.desktop.ui.Zoom.parse(config.get("ui.scale", String.valueOf(io.pyhoma.desktop.ui.Zoom.DEFAULT)));
        io.pyhoma.desktop.ui.Zoom.onChange(v -> {
            App.config().set("ui.scale", String.valueOf(v));
            App.config().save();
        });
        provider = ServiceLoader.load(BackendProvider.class).findFirst().orElse(null);
        if (provider == null) {
            showError(I18n.t("startup.noBackend"), false);
            return;
        }
        if (config.hasDatabase()) {
            connect(config.database().orElseThrow(), false);
        } else {
            showWelcome();
        }
    }

    private void setLauncherScene(Parent content) {
        stage.setTitle("Pyhoma.");
        Dialogs.setOwner(stage);
        if (launcherScene != null && stage.getScene() == launcherScene) {
            launcherSlot.getChildren().setAll(content);
            if (fullWindow && !stage.isMaximized()) {
                stage.setMaximized(true);
            }
            io.pyhoma.desktop.ui.Motion.fadeIn(content, 240);
            return;
        }
        launcherSlot = new StackPane(content);
        StackPane root = new StackPane(io.pyhoma.desktop.ui.Zoom.wrap(launcherSlot));
        root.getStyleClass().add("launcher-root");
        javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
        double[] size = fullWindow
                ? Ui.sceneSize(stage, Math.min(1040, screen.getWidth() * 0.94), Math.min(700, screen.getHeight() * 0.92))
                : new double[] {Math.min(1040, screen.getWidth() * 0.94), Math.min(700, screen.getHeight() * 0.92)};
        Scene scene = new Scene(root, size[0], size[1]);
        Theme.install(scene);
        io.pyhoma.desktop.ui.Motion.install(scene);
        io.pyhoma.desktop.ui.Zoom.install(scene, root);
        launcherScene = scene;
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(Math.min(900, screen.getWidth() * 0.9));
        stage.setMinHeight(Math.min(620, screen.getHeight() * 0.9));
        if (fullWindow) {
            Ui.showMaximized(stage);
        } else {
            Ui.showCentered(stage);
        }
        io.pyhoma.desktop.ui.Motion.fadeIn(content, 300);
    }

    private Node brandPanel(String tagline) {
        VBox panel = new VBox(18);
        panel.getStyleClass().add("brand-panel");
        panel.setPrefWidth(380);
        Label tag = Ui.label(tagline, "brand-tagline");
        tag.setWrapText(true);
        VBox points = new VBox(10,
                bullet("ticket", I18n.t("brand.point.helpdesk")),
                bullet("square-kanban", I18n.t("brand.point.itsm")),
                bullet("monitor-smartphone", I18n.t("brand.point.assets")),
                bullet("database", I18n.t("brand.point.database")));
        panel.getChildren().addAll(MainWindow.brand(34), tag, Ui.spacer(), points,
                Ui.label("v" + Services.VERSION, "brand-version"));
        return panel;
    }

    private static Node bullet(String icon, String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        HBox h = new HBox(10, Icons.of(icon, 16), l);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("brand-bullet");
        return h;
    }

    private Parent split(Node right) {
        if (right instanceof ScrollPane sp && sp.getContent() != null) {
            StackPane holder = new StackPane(sp.getContent());
            holder.setAlignment(Pos.TOP_CENTER);
            holder.paddingProperty().bind(javafx.beans.binding.Bindings.createObjectBinding(
                    () -> new javafx.geometry.Insets(Math.max(0, (sp.getViewportBounds().getHeight() - 640) / 2), 0, 0, 0),
                    sp.viewportBoundsProperty()));
            sp.setContent(holder);
            sp.setFitToWidth(true);
        } else if (right instanceof Region r) {
            r.setMaxHeight(Region.USE_PREF_SIZE);
        }
        StackPane rightBox = new StackPane(right);
        rightBox.getStyleClass().add("launcher-content");
        HBox.setHgrow(rightBox, Priority.ALWAYS);
        return new HBox(brandPanel(I18n.t("brand.tagline")), rightBox);
    }

    private void showProgress(String message) {
        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(320);
        bar.getStyleClass().add("thin-progress");
        VBox box = new VBox(14, Ui.label(message, "launcher-title"), bar, Ui.muted(I18n.t("startup.wait")));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxWidth(420);
        setLauncherScene(split(box));
    }

    private void showError(String message, boolean canEditConnection) {
        Label title = Ui.label(I18n.t("startup.errorTitle"), "launcher-title");
        Label text = new Label(message);
        text.setWrapText(true);
        text.getStyleClass().add("error-text");
        HBox actions = Ui.row(10);
        if (canEditConnection) {
            actions.getChildren().addAll(
                    Ui.primary(I18n.t("startup.retry"), "refresh-cw", () -> connect(App.config().database().orElseThrow(), false)),
                    Ui.button(I18n.t("startup.editConnection"), "database", () -> showDatabaseStep(true)));
        }
        actions.getChildren().add(Ui.ghost(I18n.t("startup.logs"), "folder", () -> {
            try {
                java.awt.Desktop.getDesktop().open(AppPaths.logDir().toFile());
            } catch (Exception ignored) {
            }
        }));
        VBox box = new VBox(16, Icons.of("circle-alert", 28), title, text, actions);
        box.setMaxWidth(520);
        setLauncherScene(split(box));
    }

    private void showWelcome() {
        Label title = Ui.label(I18n.t("wizard.welcome.title"), "launcher-title");
        Label text = new Label(I18n.t("wizard.welcome.text"));
        text.setWrapText(true);
        ToggleGroup lang = new ToggleGroup();
        RadioButton pt = new RadioButton("Português (Brasil)");
        RadioButton en = new RadioButton("English");
        pt.setToggleGroup(lang);
        en.setToggleGroup(lang);
        (I18n.isPortuguese() ? pt : en).setSelected(true);
        lang.selectedToggleProperty().addListener((o, a, b) -> {
            I18n.setLanguage(b == en ? "en" : "pt-BR");
            App.config().set("ui.language", I18n.languageTag());
            showWelcome();
        });
        VBox steps = new VBox(8, step("1", I18n.t("wizard.step.database")), step("2", I18n.t("wizard.step.organization")),
                step("3", I18n.t("wizard.step.login")), step("4", I18n.t("wizard.step.setup")));
        Button next = Ui.primary(I18n.t("wizard.start"), "chevron-right", () -> showDatabaseStep(false));
        Button explore = Ui.button(I18n.t("demo.start"), "eye", this::startDemo);
        Label exploreHint = Ui.muted(I18n.t("demo.hint"));
        exploreHint.setWrapText(true);
        VBox box = new VBox(18, title, text, Ui.row(16, pt, en), steps, Ui.row(10, next, explore), exploreHint);
        box.setMaxWidth(520);
        setLauncherScene(split(box));
    }

    private static Node step(String n, String text) {
        Label num = Ui.label(n, "step-num");
        HBox h = new HBox(10, num, new Label(text));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private void showDatabaseStep(boolean fromError) {
        Label title = Ui.label(I18n.t("wizard.db.title"), "launcher-title");
        Label text = new Label(I18n.t("wizard.db.text"));
        text.setWrapText(true);
        DatabaseForm form = new DatabaseForm(provider, App.config().database().orElse(null));
        Button back = Ui.ghost(I18n.t("action.back"), "chevron-left", fromError ? () -> connect(App.config().database().orElseThrow(),
                false) : this::showWelcome);
        Button next = Ui.primary(I18n.t("wizard.connect"), "chevron-right", () -> {
            if (!form.validateFields()) {
                return;
            }
            DatabaseSettings s = form.settings();
            App.config().setDatabase(s);
            connect(s, true);
        });
        VBox box = new VBox(16, title, text, form, Ui.row(10, back, Ui.spacer(), next));
        box.setMaxWidth(640);
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("launcher-scroll");
        setLauncherScene(split(sp));
    }

    private void startDemo() {
        fullWindow = true;
        stage.setMaximized(true);
        showProgress(I18n.t("demo.preparing"));
        String password = demoPassword();
        InstallRequest req = new InstallRequest(I18n.t("demo.org"), "demo", I18n.t("demo.user"), null, password,
                I18n.languageTag(), true);
        Async.run(() -> {
            Services services = new Services(new InMemoryDataStore());
            services.installation().install(req);
            App.connect(new DemoBackend(services.store()), services);
            App.setDemo(true);
            return services.auth().login("demo", password);
        }, this::afterLogin, e -> {
            App.setDemo(false);
            fullWindow = false;
            showWelcome();
            Dialogs.error(e);
        });
    }

    private static String demoPassword() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        String letters = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";
        String digits = "23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            String pool = i % 4 == 3 ? digits : letters;
            sb.append(pool.charAt(random.nextInt(pool.length())));
        }
        return sb.toString();
    }

    public void leaveDemo(boolean configure) {
        MainWindow.closeActive(() -> finishDemo(configure));
    }

    private void finishDemo(boolean configure) {
        App.shutdown();
        App.setDemo(false);
        fullWindow = false;
        if (configure) {
            showDatabaseStep(false);
        } else {
            showWelcome();
        }
    }

    private record DemoBackend(DataStore dataStore) implements BackendProvider.Backend {

        @Override
        public void scheduleJobs(Runnable everyMinute, Runnable hourly) {
        }

        @Override
        public void close() {
        }
    }

    private void showInstallStep() {
        Label title = Ui.label(I18n.t("wizard.org.title"), "launcher-title");
        Label text = new Label(I18n.t("wizard.org.text"));
        text.setWrapText(true);
        TextField org = new TextField();
        TextField fullName = new TextField();
        TextField login = new TextField("admin");
        TextField email = new TextField();
        PasswordField password = new PasswordField();
        PasswordField confirm = new PasswordField();
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        int r = 0;
        grid.add(Ui.label(I18n.t("wizard.org.name"), "field-label"), 0, r++, 2, 1);
        grid.add(org, 0, r++, 2, 1);
        grid.add(Ui.label(I18n.t("field.fullName"), "field-label"), 0, r);
        grid.add(Ui.label(I18n.t("field.login"), "field-label"), 1, r++);
        grid.add(fullName, 0, r);
        grid.add(login, 1, r++);
        grid.add(Ui.label(I18n.t("field.email"), "field-label"), 0, r++, 2, 1);
        grid.add(email, 0, r++, 2, 1);
        grid.add(Ui.label(I18n.t("password.new"), "field-label"), 0, r);
        grid.add(Ui.label(I18n.t("password.confirm"), "field-label"), 1, r++);
        grid.add(password, 0, r);
        grid.add(confirm, 1, r++);
        javafx.scene.layout.ColumnConstraints c = new javafx.scene.layout.ColumnConstraints();
        c.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c, c);
        Label error = Ui.label("", "error-text");
        error.setWrapText(true);
        Button create = Ui.primary(I18n.t("wizard.org.create"), "check", () -> {
            if (org.getText().isBlank() || fullName.getText().isBlank() || login.getText().isBlank()) {
                error.setText(I18n.t("wizard.org.required"));
                return;
            }
            if (!password.getText().equals(confirm.getText())) {
                error.setText(I18n.t("password.mismatch"));
                return;
            }
            String typedLogin = login.getText().trim().toLowerCase(java.util.Locale.ROOT);
            if (!PasswordHasher.isAcceptable(password.getText(), typedLogin, fullName.getText())) {
                error.setText(I18n.t("password.rules", PasswordHasher.MIN_LENGTH));
                return;
            }
            InstallRequest req = new InstallRequest(org.getText().trim(), login.getText().trim(), fullName.getText().trim(),
                    email.getText().isBlank() ? null : email.getText().trim(), password.getText(), I18n.languageTag(), false);
            String adminLogin = login.getText().trim().toLowerCase();
            showProgress(I18n.t("wizard.org.installing"));
            Async.run(() -> {
                App.services().installation().install(req);
                return Boolean.TRUE;
            }, ok -> {
                lastLogin = adminLogin;
                App.config().set("ui.lastLogin", adminLogin);
                App.config().save();
                showLogin(I18n.t("wizard.org.done"));
            }, e -> {
                showInstallStep();
                Dialogs.error(e);
            });
        });
        VBox box = new VBox(16, title, text, grid, Ui.muted(I18n.t("password.rules", PasswordHasher.MIN_LENGTH)), error,
                Ui.row(10, Ui.spacer(), create));
        box.setMaxWidth(560);
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("launcher-scroll");
        setLauncherScene(split(sp));
        Platform.runLater(org::requestFocus);
    }

    private void connect(DatabaseSettings settings, boolean fromWizard) {
        showProgress(I18n.t("startup.connecting", settings.vendor().label()));
        Async.run(() -> {
            BackendProvider.Backend backend = provider.start(settings,
                    new BackendProvider.BackendOptions(AppPaths.dataDir(), AppPaths.logDir(), true));
            Services services = new Services(backend.dataStore());
            backend.scheduleJobs(() -> services.jobs().runFrequent(), () -> services.jobs().runHourly());
            App.connect(backend, services);
            return services.installation().isInstalled();
        }, installed -> {
            if (installed) {
                if (fromWizard) {
                    App.config().save();
                }
                showLogin(fromWizard ? I18n.t("wizard.db.existing") : null);
            } else if (fromWizard) {
                showInstallStep();
            } else {
                restartSetup();
            }
        }, e -> {
            System.getLogger("pyhoma").log(System.Logger.Level.ERROR, "Connection failed", e);
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            showError(I18n.t("startup.connectionFailed", settings.safeDescription(),
                    root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage()), true);
        });
    }

    private void restartSetup() {
        App.shutdown();
        App.config().clearDatabase();
        App.config().save();
        showWelcome();
    }

    public void showLogin(String notice) {
        Label title = Ui.label(I18n.t("login.title"), "launcher-title");
        Label subtitle = Ui.muted(App.services().settings().get(io.pyhoma.domain.service.SettingKeys.ORGANIZATION_NAME, ""));
        TextField login = new TextField(lastLogin != null ? lastLogin : App.config().get("ui.lastLogin", ""));
        login.setPromptText(I18n.t("field.login"));
        PasswordField password = new PasswordField();
        password.setPromptText(I18n.t("login.password"));
        Label error = Ui.label(notice == null ? "" : notice, notice == null ? "error-text" : "notice-text");
        error.setWrapText(true);
        Button enter = Ui.primary(I18n.t("login.enter"), "log-out", null);
        enter.setDefaultButton(true);
        enter.setMaxWidth(Double.MAX_VALUE);
        Runnable doLogin = () -> {
            error.setText("");
            error.getStyleClass().setAll("label", "error-text");
            enter.setDisable(true);
            Async.run(() -> App.services().auth().login(login.getText(), password.getText()), session -> {
                lastLogin = session.login();
                App.config().set("ui.lastLogin", session.login());
                App.config().save();
                afterLogin(session);
            }, e -> {
                enter.setDisable(false);
                password.clear();
                error.setText(I18n.error(e));
                password.requestFocus();
            });
        };
        enter.setOnAction(e -> doLogin.run());
        Hyperlink change = new Hyperlink(I18n.t("login.changeConnection"));
        change.setOnAction(e -> showDatabaseStep(true));
        Label db = Ui.label(App.config().database().map(DatabaseSettings::safeDescription).orElse(""), "muted");
        if (!db.getText().isBlank()) {
            db.setGraphic(Icons.of("database", 13));
        }
        db.setWrapText(true);
        db.setManaged(!db.getText().isBlank());
        db.setVisible(!db.getText().isBlank());
        VBox form = new VBox(12, title, subtitle, Ui.label(I18n.t("field.login"), "field-label"), login,
                Ui.label(I18n.t("login.password"), "field-label"), password, error, enter);
        form.setMaxWidth(380);
        VBox box = new VBox(30, form, new VBox(4, db, change));
        box.setMaxWidth(380);
        box.setAlignment(Pos.CENTER_LEFT);
        StackPane.setMargin(box, new Insets(0));
        setLauncherScene(split(box));
        Platform.runLater(() -> (login.getText().isBlank() ? login : password).requestFocus());
    }

    private void afterLogin(UserSession session) {
        if (session.language() != null && !session.language().equals(I18n.languageTag())) {
            I18n.setLanguage(session.language());
        }
        if (App.services().auth().mustChangePassword() && !PasswordDialog.forceChange()) {
            App.services().auth().logout();
            showLogin(null);
            return;
        }
        if (!App.demo() && App.can(io.pyhoma.domain.model.core.Module.SETTINGS, io.pyhoma.domain.security.Right.UPDATE)
                && App.services().installation().setupPending()) {
            new io.pyhoma.desktop.views.setup.SetupWizard(this::setLauncherScene, this::loadMainWindow).start();
            return;
        }
        loadMainWindow();
    }

    private void loadMainWindow() {
        showProgress(I18n.t("login.loading"));
        Async.run(() -> {
            App.lookups().warmUp();
            return Boolean.TRUE;
        }, ok -> openMainWindow());
    }

    private void openMainWindow() {
        MainWindow window = new MainWindow(stage, notice -> {
            if (App.demo()) {
                finishDemo(false);
                return;
            }
            App.services().auth().logout();
            App.lookups().invalidate();
            showLogin(notice);
        });
        window.show();
        stage.setResizable(true);
    }

    public void reopenMainWindow() {
        App.lookups().invalidate();
        Async.run(() -> {
            App.lookups().warmUp();
            return Boolean.TRUE;
        }, ok -> openMainWindow());
    }

    public void restart() {
        App.shutdown();
        start();
    }
}
