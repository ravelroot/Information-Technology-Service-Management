package io.pyhoma.desktop.ui.components;

import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.domain.itil.SlaState;
import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.project.ProjectTaskStatus;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

public final class Badge extends HBox {

    private Badge(String text, String tone, String hexColor) {
        super(6);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().addAll("badge", "tone-" + tone);
        Circle led = new Circle(3.5);
        led.getStyleClass().add("led");
        if (hexColor != null) {
            led.setStyle("-fx-fill: " + hexColor + ";");
        }
        Label l = new Label(text);
        l.getStyleClass().add("badge-text");
        getChildren().addAll(led, l);
        setMaxWidth(USE_PREF_SIZE);
    }

    public static Badge of(String text, String tone) {
        return new Badge(text, tone, null);
    }

    public static Badge colored(String text, String hexColor) {
        return new Badge(text, "custom", null);
    }

    public static String tone(ItilStatus s) {
        return switch (s) {
            case NEW -> "blue";
            case ACCEPTED, EVALUATION, APPROVAL -> "violet";
            case ASSIGNED -> "teal";
            case PLANNED, TESTING, QUALIFICATION -> "cyan";
            case WAITING, REVIEW -> "amber";
            case APPLIED, SOLVED -> "green";
            case CLOSED -> "gray";
            case CANCELED -> "red";
        };
    }

    public static Badge status(ItilStatus s) {
        return of(I18n.e(s), tone(s));
    }

    public static String tone(ProjectTaskStatus s) {
        return switch (s) {
            case TODO -> "gray";
            case IN_PROGRESS -> "blue";
            case REVIEW -> "violet";
            case BLOCKED -> "red";
            case DONE -> "green";
        };
    }

    public static Badge status(ProjectTaskStatus s) {
        return of(I18n.e(s), tone(s));
    }

    public static String priorityTone(Integer p) {
        if (p == null) {
            return "gray";
        }
        return switch (p) {
            case 1 -> "gray";
            case 2 -> "slate";
            case 3 -> "blue";
            case 4 -> "amber";
            case 5 -> "red";
            default -> "crimson";
        };
    }

    public static Badge priority(Integer p) {
        return of(I18n.priority(p), priorityTone(p));
    }

    public static Badge sla(SlaState s) {
        String tone = switch (s) {
            case NONE -> "gray";
            case ON_TIME, MET -> "green";
            case AT_RISK -> "amber";
            case BREACHED, MISSED -> "red";
        };
        return of(I18n.e(s), tone);
    }
}
