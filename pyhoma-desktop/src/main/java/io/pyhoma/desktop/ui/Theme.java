package io.pyhoma.desktop.ui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.text.Font;

public final class Theme {

    public enum Mode { LIGHT, DARK }

    private static Mode mode = Mode.LIGHT;
    private static final List<Scene> SCENES = new ArrayList<>();

    private Theme() {
    }

    public static void loadFonts() {
        for (String f : List.of("IBMPlexSans-400", "IBMPlexSans-500", "IBMPlexSans-600", "IBMPlexSans-700",
                "IBMPlexMono-400", "IBMPlexMono-500")) {
            try (InputStream in = Theme.class.getResourceAsStream("/io/pyhoma/desktop/fonts/" + f + ".ttf")) {
                Font.loadFont(in, 13);
            } catch (Exception ignored) {
            }
        }
    }

    public static Mode mode() {
        return mode;
    }

    public static void setMode(Mode m) {
        mode = m;
        SCENES.removeIf(s -> s.getWindow() == null && s.getRoot() == null);
        SCENES.forEach(Theme::apply);
    }

    public static void install(Scene scene) {
        if (!SCENES.contains(scene)) {
            SCENES.add(scene);
        }
        apply(scene);
    }

    private static void apply(Scene scene) {
        scene.getStylesheets().setAll(css("pyhoma.css"), css(mode == Mode.DARK ? "dark.css" : "light.css"));
    }

    static String css(String name) {
        return Theme.class.getResource("/io/pyhoma/desktop/css/" + name).toExternalForm();
    }

    public static List<String> stylesheets() {
        return List.of(css("pyhoma.css"), css(mode == Mode.DARK ? "dark.css" : "light.css"));
    }
}
