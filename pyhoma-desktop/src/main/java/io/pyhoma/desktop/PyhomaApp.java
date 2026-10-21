package io.pyhoma.desktop;

import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.Theme;
import java.io.InputStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class PyhomaApp extends Application {

    @Override
    public void init() {
        Theme.loadFonts();
    }

    @Override
    public void start(Stage stage) {
        for (String size : new String[] {"16", "32", "48", "64", "128", "256"}) {
            try (InputStream in = PyhomaApp.class.getResourceAsStream("/io/pyhoma/desktop/images/icon-" + size + ".png")) {
                if (in != null) {
                    stage.getIcons().add(new Image(in));
                }
            } catch (Exception ignored) {
            }
        }
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.getLogger("pyhoma").log(System.Logger.Level.ERROR, "Unexpected error", e);
            if (Platform.isFxApplicationThread()) {
                Dialogs.error(e);
            }
        });
        stage.setOnHidden(e -> {
            App.shutdown();
            Platform.exit();
            System.exit(0);
        });
        new StartupFlow(stage).start();
        StartMenuShortcut.ensure();
    }

    @Override
    public void stop() {
        App.shutdown();
    }
}
