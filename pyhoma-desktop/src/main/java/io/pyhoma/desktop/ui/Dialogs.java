package io.pyhoma.desktop.ui;

import java.util.Optional;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

public final class Dialogs {

    private static Window owner;

    private Dialogs() {
    }

    public static void setOwner(Window w) {
        owner = w;
    }

    public static Window owner() {
        return owner;
    }

    private static <R> Dialog<R> base(String title) {
        Dialog<R> d = new Dialog<>();
        d.setTitle(title);
        if (owner != null && owner.isShowing()) {
            d.initOwner(owner);
        }
        d.initModality(Modality.WINDOW_MODAL);
        DialogPane pane = d.getDialogPane();
        pane.getStylesheets().setAll(Theme.stylesheets());
        pane.getStyleClass().add("ph-dialog");
        return d;
    }

    private static VBox body(String title, Node content) {
        Label heading = Ui.label(title, "dialog-title");
        VBox box = new VBox(14, heading);
        if (content != null) {
            box.getChildren().add(content);
        }
        box.setPadding(new Insets(4, 4, 0, 4));
        return box;
    }

    public static boolean confirm(String title, String message, String confirmText, boolean destructive) {
        Dialog<ButtonType> d = base(title);
        Label text = new Label(message);
        text.setWrapText(true);
        text.setMaxWidth(420);
        d.getDialogPane().setContent(body(title, text));
        ButtonType ok = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ok);
        relabelCancel(d.getDialogPane());
        d.getDialogPane().lookupButton(ok).getStyleClass().add(destructive ? "danger" : "primary");
        return d.showAndWait().filter(b -> b == ok).isPresent();
    }

    public static void info(String title, String message) {
        Dialog<ButtonType> d = base(title);
        Label text = new Label(message);
        text.setWrapText(true);
        text.setMaxWidth(460);
        d.getDialogPane().setContent(body(title, text));
        d.getDialogPane().getButtonTypes().add(ButtonType.OK);
        d.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().add("primary");
        d.showAndWait();
    }

    public static void error(Throwable e) {
        System.getLogger("pyhoma.ui").log(System.Logger.Level.DEBUG, "UI error", e);
        Runnable show = () -> {
            Dialog<ButtonType> d = base(I18n.t("dialog.error.title"));
            Label text = new Label(I18n.error(e));
            text.setWrapText(true);
            text.setMaxWidth(460);
            d.getDialogPane().setContent(body(I18n.t("dialog.error.title"), text));
            d.getDialogPane().getButtonTypes().add(ButtonType.OK);
            d.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().add("primary");
            d.showAndWait();
        };
        if (Platform.isFxApplicationThread()) {
            show.run();
        } else {
            Platform.runLater(show);
        }
    }

    public static Optional<String> prompt(String title, String label, String initial, boolean multiline) {
        Dialog<String> d = base(title);
        TextInputControl input = multiline ? new TextArea(initial) : new TextField(initial);
        if (input instanceof TextArea a) {
            a.setPrefRowCount(5);
            a.setWrapText(true);
        }
        input.setPrefWidth(420);
        VBox content = new VBox(6, Ui.label(label, "field-label"), input);
        d.getDialogPane().setContent(body(title, content));
        ButtonType ok = new ButtonType(I18n.t("action.confirm"), ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ok);
        relabelCancel(d.getDialogPane());
        d.getDialogPane().lookupButton(ok).getStyleClass().add("primary");
        d.setResultConverter(b -> b == ok ? input.getText() : null);
        Platform.runLater(input::requestFocus);
        return d.showAndWait().filter(s -> !s.isBlank());
    }

    public static boolean form(String title, Node content, String okText, Supplier<Boolean> onOk) {
        Dialog<ButtonType> d = base(title);
        d.getDialogPane().setContent(body(title, content));
        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ok);
        relabelCancel(d.getDialogPane());
        Node okButton = d.getDialogPane().lookupButton(ok);
        okButton.getStyleClass().add("primary");
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            try {
                if (!onOk.get()) {
                    ev.consume();
                }
            } catch (RuntimeException ex) {
                ev.consume();
                error(ex);
            }
        });
        return d.showAndWait().filter(b -> b == ok).isPresent();
    }

    private static void relabelCancel(DialogPane pane) {
        Node cancel = pane.lookupButton(ButtonType.CANCEL);
        if (cancel instanceof javafx.scene.control.Button b) {
            b.setText(I18n.t("action.cancel"));
            b.getStyleClass().add("ghost");
        }
    }
}
