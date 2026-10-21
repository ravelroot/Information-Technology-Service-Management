package io.pyhoma.desktop.tour;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.StartupFlow;
import io.pyhoma.desktop.config.AppConfig;
import io.pyhoma.desktop.shell.MainWindow;
import io.pyhoma.desktop.shell.Route;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Theme;
import io.pyhoma.domain.memory.InMemoryDataStore;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.model.project.Project;
import io.pyhoma.domain.port.BackendProvider;
import io.pyhoma.domain.port.DataStore;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.service.InstallationService;
import io.pyhoma.domain.service.Services;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

public final class ScreenshotTour {

    private static final String ADMIN_PASSWORD = "Tour-Only-1";

    private final Path out;
    private final String language;
    private final Theme.Mode mode;
    private Stage stage;
    private int failures;

    private ScreenshotTour(Path out, String language, Theme.Mode mode) {
        this.out = out;
        this.language = language;
        this.mode = mode;
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "target/screens");
        String language = args.length > 1 ? args[1] : "pt-BR";
        Theme.Mode mode = Theme.Mode.valueOf(args.length > 2 ? args[2] : "LIGHT");
        Files.createDirectories(out);
        Path home = Files.createTempDirectory("pyhoma-tour");
        System.setProperty("pyhoma.home", home.toString());
        Platform.startup(() -> { });
        Platform.setImplicitExit(false);
        ScreenshotTour tour = new ScreenshotTour(out, language, mode);
        int failures = tour.run(home);
        Platform.exit();
        System.exit(failures == 0 ? 0 : 1);
    }

    private int run(Path home) throws Exception {
        fx(() -> {
            Theme.loadFonts();
            App.setConfig(new AppConfig(home.resolve("config")));
            App.config().set("ui.width", "1440");
            App.config().set("ui.height", "900");
            App.config().set("ui.scale", "1.0");
            I18n.setLanguage(language);
            Theme.setMode(mode);
            stage = new Stage();
            return null;
        });

        StartupFlow flow = fx(() -> new StartupFlow(stage));
        fx(() -> {
            invoke(flow, "showWelcome");
            stage.setWidth(1180);
            stage.setHeight(760);
            return null;
        });
        shot("01-welcome");
        fx(() -> {
            Method m = StartupFlow.class.getDeclaredMethod("showDatabaseStep", boolean.class);
            m.setAccessible(true);
            m.invoke(flow, false);
            stage.getScene().getRoot().lookupAll(".text-field").stream()
                    .filter(javafx.scene.control.TextField.class::isInstance)
                    .map(javafx.scene.control.TextField.class::cast)
                    .filter(t -> t.getText() != null && t.getText().contains(home.getFileName().toString()))
                    .forEach(t -> t.setText("C:\\Users\\usuario\\AppData\\Local\\Pyhoma\\database"));
            return null;
        });
        shot("02-database");

        Services services = new Services(new InMemoryDataStore());
        services.installation().install(new InstallationService.InstallRequest(
                I18n.isPortuguese() ? "Pyhoma Demonstração" : "Pyhoma Demo", "admin",
                I18n.isPortuguese() ? "Administrador" : "Administrator", "admin@example.com", ADMIN_PASSWORD,
                language, true));
        App.connect(new MemoryBackend(services.store()), services);
        fx(() -> {
            flow.showLogin(null);
            return null;
        });
        shot("03-login");

        services.auth().login("admin", ADMIN_PASSWORD);
        App.lookups().warmUp();
        fx(() -> {
            new MainWindow(stage, notice -> { }).show();
            stage.setWidth(1440);
            stage.setHeight(900);
            return null;
        });
        settle(2500);
        shot("10-dashboard");

        Route[] routes = {Route.TICKETS, Route.KANBAN, Route.PROBLEMS, Route.CHANGES, Route.PLANNING, Route.PROJECTS,
            Route.PORTFOLIO, Route.ASSETS, Route.SOFTWARE, Route.CONTRACTS, Route.SUPPLIERS, Route.DOCUMENTS,
            Route.KNOWLEDGE, Route.REPORTS, Route.RECURRING, Route.USERS, Route.GROUPS, Route.PROFILES, Route.ORG_UNITS,
            Route.DROPDOWNS, Route.RULES, Route.SLAS, Route.AUDIT, Route.SETTINGS, Route.HOME};
        int n = 11;
        for (Route r : routes) {
            go(r);
            shot(String.format("%02d-%s", n++, r.name().toLowerCase()));
        }

        Ticket ticket = services.store().repository(Ticket.class)
                .find(Query.all().orderBy(Sort.desc("priority")).limit(1)).get(0);
        open("Ticket", ticket.getId());
        shot("40-ticket");
        selectTab(1);
        shot("41-ticket-timeline");

        Project project = services.store().repository(Project.class).find(Query.all().limit(1)).get(0);
        open("Project", project.getId());
        shot("42-project");
        selectTab(3);
        shot("43-project-kanban");
        selectTab(1);
        shot("44-project-gantt");

        Long assetId = services.assets().search(io.pyhoma.domain.service.AssetService.AssetSearch.of(null, null))
                .items().get(0).getId();
        open("Asset", assetId);
        shot("45-asset");
        return failures;
    }

    private void go(Route r) throws Exception {
        fx(() -> {
            App.nav().go(r);
            return null;
        });
        settle(1800);
    }

    private void open(String type, Long id) throws Exception {
        fx(() -> {
            App.nav().openItem(type, id);
            return null;
        });
        settle(1800);
    }

    private void selectTab(int index) throws Exception {
        fx(() -> {
            stage.getScene().getRoot().lookupAll(".tab-pane").stream().filter(TabPane.class::isInstance)
                    .map(TabPane.class::cast).filter(tp -> tp.getTabs().size() > index).findFirst()
                    .ifPresent(tp -> tp.getSelectionModel().select(index));
            return null;
        });
        settle(1500);
    }

    private static void invoke(Object target, String method) throws Exception {
        Method m = target.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(target);
    }

    private static void settle(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    private void shot(String name) {
        try {
            settle(600);
            WritableImage img = fx(() -> {
                Scene scene = stage.getScene();
                return scene.snapshot(null);
            });
            int w = (int) img.getWidth();
            int h = (int) img.getHeight();
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            PixelReader pr = img.getPixelReader();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    bi.setRGB(x, y, pr.getArgb(x, y));
                }
            }
            ImageIO.write(bi, "png", out.resolve(name + ".png").toFile());
            System.out.println("saved " + name);
        } catch (Exception e) {
            failures++;
            System.err.println("FAILED " + name + ": " + e);
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    private interface FxCall<T> {
        T call() throws Exception;
    }

    private static <T> T fx(FxCall<T> call) throws Exception {
        CompletableFuture<T> f = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                f.complete(call.call());
            } catch (Throwable t) {
                f.completeExceptionally(t);
            }
        });
        return f.get(60, TimeUnit.SECONDS);
    }

    private record MemoryBackend(DataStore dataStore) implements BackendProvider.Backend {
        @Override
        public void scheduleJobs(Runnable everyMinute, Runnable hourly) {
        }

        @Override
        public void close() {
        }
    }
}
