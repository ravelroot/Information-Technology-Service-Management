package io.pyhoma.desktop.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

public final class Icons {

    private static final Properties PATHS = new Properties();

    static {
        try (InputStream in = Icons.class.getResourceAsStream("/io/pyhoma/desktop/icons/lucide.properties")) {
            PATHS.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException("Icons not found", e);
        }
    }

    private Icons() {
    }

    public static Node of(String name) {
        return of(name, 16);
    }

    public static Node of(String name, double size) {
        SVGPath path = new SVGPath();
        path.setContent(PATHS.getProperty(name, PATHS.getProperty("circle-help")));
        path.getStyleClass().add("icon-path");
        Rectangle frame = new Rectangle(24, 24);
        frame.getStyleClass().add("icon-frame");
        frame.setMouseTransparent(true);
        Group inner = new Group(frame, path);
        inner.getTransforms().add(new Scale(size / 24.0, size / 24.0, 0, 0));
        Group outer = new Group(inner);
        outer.getStyleClass().add("icon");
        outer.setMouseTransparent(true);
        return outer;
    }

    public static boolean exists(String name) {
        return PATHS.containsKey(name);
    }
}
