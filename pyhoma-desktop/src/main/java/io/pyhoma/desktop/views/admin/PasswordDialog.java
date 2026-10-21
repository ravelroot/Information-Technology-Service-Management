package io.pyhoma.desktop.views.admin;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.domain.security.PasswordHasher;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

public final class PasswordDialog {

    private PasswordDialog() {
    }

    public static void changeOwn() {
        PasswordField current = new PasswordField();
        PasswordField next = new PasswordField();
        PasswordField confirm = new PasswordField();
        VBox box = new VBox(8, Ui.label(I18n.t("password.current"), "field-label"), current,
                Ui.label(I18n.t("password.new"), "field-label"), next,
                Ui.label(I18n.t("password.confirm"), "field-label"), confirm,
                Ui.muted(I18n.t("password.rules", PasswordHasher.MIN_LENGTH)));
        box.setPrefWidth(380);
        Dialogs.form(I18n.t("menu.changePassword"), box, I18n.t("action.save"), () -> {
            if (!next.getText().equals(confirm.getText())) {
                Dialogs.info(I18n.t("menu.changePassword"), I18n.t("password.mismatch"));
                return false;
            }
            App.services().auth().changePassword(current.getText(), next.getText());
            Toasts.success(I18n.t("password.changed"));
            return true;
        });
    }

    public static void reset(Long userId) {
        PasswordField next = new PasswordField();
        PasswordField confirm = new PasswordField();
        CheckBox mustChange = new CheckBox(I18n.t("password.mustChange"));
        mustChange.setSelected(true);
        VBox box = new VBox(8, Ui.label(I18n.t("password.new"), "field-label"), next,
                Ui.label(I18n.t("password.confirm"), "field-label"), confirm, mustChange,
                Ui.muted(I18n.t("password.rules", PasswordHasher.MIN_LENGTH)));
        box.setPrefWidth(380);
        Dialogs.form(I18n.t("users.resetPassword"), box, I18n.t("action.save"), () -> {
            if (!next.getText().equals(confirm.getText())) {
                Dialogs.info(I18n.t("users.resetPassword"), I18n.t("password.mismatch"));
                return false;
            }
            App.services().users().resetPassword(userId, next.getText(), mustChange.isSelected());
            Toasts.success(I18n.t("password.changed"));
            return true;
        });
    }

    public static boolean forceChange() {
        PasswordField current = new PasswordField();
        PasswordField next = new PasswordField();
        PasswordField confirm = new PasswordField();
        VBox box = new VBox(8, Ui.muted(I18n.t("password.forced")),
                Ui.label(I18n.t("password.current"), "field-label"), current,
                Ui.label(I18n.t("password.new"), "field-label"), next,
                Ui.label(I18n.t("password.confirm"), "field-label"), confirm);
        box.setPrefWidth(380);
        return Dialogs.form(I18n.t("menu.changePassword"), box, I18n.t("action.save"), () -> {
            if (!next.getText().equals(confirm.getText())) {
                Dialogs.info(I18n.t("menu.changePassword"), I18n.t("password.mismatch"));
                return false;
            }
            App.services().auth().changePassword(current.getText(), next.getText());
            return true;
        });
    }
}
