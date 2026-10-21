package io.pyhoma.desktop.ui;

import java.util.Map;
import java.util.WeakHashMap;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.control.skin.VirtualFlow;
import javafx.util.Duration;

public final class Motion {

    private static final Interpolator OUT = Interpolator.SPLINE(0.22, 1, 0.36, 1);
    private static final Map<Node, Animation> RUNNING = new WeakHashMap<>();
    private static final Map<ScrollPane, DoubleProperty> SCROLL_TARGETS = new WeakHashMap<>();
    private static boolean enabled = true;

    private Motion() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void install(Scene scene) {
        scene.addEventFilter(ScrollEvent.SCROLL, Motion::smoothScroll);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> press(e, 0.97));
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> press(e, 1));
    }

    public static void pageIn(Node page) {
        stop(page);
        page.setOpacity(1);
        page.setTranslateY(0);
        if (!enabled) {
            return;
        }
        java.util.List<Node> parts = new java.util.ArrayList<>();
        if (page instanceof Pane p && p.getChildren().size() <= 8) {
            for (Node child : p.getChildren()) {
                if (child.isManaged() && child.isVisible()) {
                    parts.add(child);
                }
            }
        }
        if (parts.isEmpty()) {
            parts.add(page);
        }
        ParallelTransition all = new ParallelTransition();
        int i = 0;
        for (Node part : parts) {
            stop(part);
            boolean cached = part.isCache();
            part.setCache(true);
            part.setCacheHint(CacheHint.SPEED);
            part.setOpacity(0);
            part.setTranslateY(14);
            Animation a = fadeUp(part, Math.min(i, 4) * 40L, 300);
            a.setOnFinished(e -> {
                part.setCache(cached);
                part.setCacheHint(CacheHint.DEFAULT);
            });
            all.getChildren().add(a);
            i++;
        }
        RUNNING.put(page, all);
        all.play();
    }

    public static void fadeIn(Node node, double millis) {
        stop(node);
        if (!enabled) {
            node.setOpacity(1);
            return;
        }
        boolean cached = node.isCache();
        node.setCache(true);
        node.setCacheHint(CacheHint.SPEED);
        node.setOpacity(0);
        node.setTranslateY(8);
        Timeline t = new Timeline(new KeyFrame(Duration.millis(millis), new KeyValue(node.opacityProperty(), 1, OUT),
                new KeyValue(node.translateYProperty(), 0, OUT)));
        t.setOnFinished(e -> {
            node.setCache(cached);
            node.setCacheHint(CacheHint.DEFAULT);
        });
        RUNNING.put(node, t);
        t.play();
    }

    public static void hoverLift(Node node) {
        hoverShift(node, 0, -2);
    }

    public static void hoverShift(Node node, double dx, double dy) {
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> to(node, dx, dy, 150));
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> to(node, 0, 0, 200));
    }

    public static void countUp(Label label, long target) {
        if (!enabled || target <= 0) {
            label.setText(String.valueOf(target));
            return;
        }
        DoubleProperty v = new SimpleDoubleProperty(0);
        v.addListener((o, a, b) -> label.setText(String.valueOf(Math.round(b.doubleValue()))));
        label.setText("0");
        Timeline t = new Timeline(new KeyFrame(Duration.millis(Math.min(900, 360 + target * 6)), new KeyValue(v, target, OUT)));
        label.getProperties().put("motion.count", v);
        t.play();
    }

    public static DoubleProperty grow(double target) {
        DoubleProperty p = new SimpleDoubleProperty(enabled ? 0 : target);
        if (enabled) {
            Timeline t = new Timeline(new KeyFrame(Duration.millis(620), new KeyValue(p, target, OUT)));
            t.setDelay(Duration.millis(120));
            t.play();
        }
        return p;
    }

    private static Animation fadeUp(Node node, long delay, long millis) {
        Timeline t = new Timeline(new KeyFrame(Duration.millis(millis),
                new KeyValue(node.opacityProperty(), 1, OUT),
                new KeyValue(node.translateYProperty(), 0, OUT)));
        t.setDelay(Duration.millis(delay));
        return t;
    }

    private static void to(Node node, double x, double y, double millis) {
        if (!enabled) {
            return;
        }
        Animation running = RUNNING.remove(node);
        if (running != null) {
            running.stop();
        }
        Timeline t = new Timeline(new KeyFrame(Duration.millis(millis), new KeyValue(node.translateXProperty(), x, OUT),
                new KeyValue(node.translateYProperty(), y, OUT)));
        RUNNING.put(node, t);
        t.play();
    }

    private static void stop(Node node) {
        Animation a = RUNNING.remove(node);
        if (a != null) {
            a.stop();
        }
    }

    private static void press(MouseEvent e, double scale) {
        if (!enabled || !(e.getTarget() instanceof Node n)) {
            return;
        }
        for (Node p = n; p != null; p = p.getParent()) {
            if (p instanceof ButtonBase b) {
                if (b.isDisabled()) {
                    return;
                }
                Timeline t = new Timeline(new KeyFrame(Duration.millis(scale < 1 ? 70 : 160),
                        new KeyValue(b.scaleXProperty(), scale, OUT), new KeyValue(b.scaleYProperty(), scale, OUT)));
                t.play();
                return;
            }
        }
    }

    private static void smoothScroll(ScrollEvent e) {
        if (!enabled || e.isShortcutDown() || e.getDeltaY() == 0 || e.isInertia() || e.getTouchCount() > 0
                || !(e.getTarget() instanceof Node n)) {
            return;
        }
        for (Node p = n; p != null; p = p.getParent()) {
            if (p instanceof TextArea) {
                return;
            }
            if (p instanceof TableView<?> || p instanceof ListView<?> || p instanceof TreeView<?> || p instanceof TreeTableView<?>) {
                if (p.lookup(".virtual-flow") instanceof VirtualFlow<?> flow) {
                    scrollFlow(flow, -e.getDeltaY());
                    e.consume();
                }
                return;
            }
            if (p instanceof ScrollPane sp && scrollable(sp)) {
                if (scrollPane(sp, e.getDeltaY())) {
                    e.consume();
                }
                return;
            }
        }
    }

    private static boolean scrollPane(ScrollPane sp, double deltaY) {
        Node content = sp.getContent();
        double range = content.getLayoutBounds().getHeight() - sp.getViewportBounds().getHeight();
        if (range <= 0) {
            return false;
        }
        DoubleProperty target = SCROLL_TARGETS.computeIfAbsent(sp, k -> new SimpleDoubleProperty(k.getVvalue()));
        Animation running = RUNNING.get(sp);
        double base = running != null && running.getStatus() == Animation.Status.RUNNING ? target.get() : sp.getVvalue();
        double next = Math.max(sp.getVmin(), Math.min(sp.getVmax(), base - deltaY * 1.2 / range));
        target.set(next);
        stop(sp);
        Timeline t = new Timeline(new KeyFrame(Duration.millis(260), new KeyValue(sp.vvalueProperty(), next, OUT)));
        RUNNING.put(sp, t);
        t.play();
        return true;
    }

    private static final Map<VirtualFlow<?>, double[]> FLOW_STATE = new WeakHashMap<>();

    private static void scrollFlow(VirtualFlow<?> flow, double pixels) {
        double[] state = FLOW_STATE.computeIfAbsent(flow, k -> new double[2]);
        Animation running = RUNNING.get(flow);
        if (running == null || running.getStatus() != Animation.Status.RUNNING) {
            state[0] = 0;
            state[1] = 0;
        }
        state[1] += pixels * 1.2;
        stop(flow);
        DoubleProperty pos = new SimpleDoubleProperty(state[0]);
        pos.addListener((o, a, b) -> {
            double step = b.doubleValue() - state[0];
            state[0] = b.doubleValue();
            flow.scrollPixels(step);
        });
        Timeline t = new Timeline(new KeyFrame(Duration.millis(240), new KeyValue(pos, state[1], OUT)));
        flow.getProperties().put("motion.pos", pos);
        RUNNING.put(flow, t);
        t.play();
    }

    private static boolean scrollable(ScrollPane sp) {
        return sp.getVbarPolicy() != ScrollPane.ScrollBarPolicy.NEVER && sp.getContent() != null
                && sp.getContent() instanceof Parent;
    }
}
