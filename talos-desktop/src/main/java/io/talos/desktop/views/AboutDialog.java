package io.talos.desktop.views;

import io.talos.desktop.App;
import io.talos.desktop.shell.MainWindow;
import io.talos.desktop.ui.Dialogs;
import io.talos.desktop.ui.I18n;
import io.talos.desktop.ui.Ui;
import io.talos.domain.service.Services;
import javafx.scene.layout.VBox;

public final class AboutDialog {

    private AboutDialog() {
    }

    public static void show() {
        VBox box = new VBox(10, MainWindow.brand(30), Ui.muted(I18n.t("about.tagline")),
                Ui.fact(I18n.t("about.version"), Services.VERSION),
                Ui.fact(I18n.t("about.database"), (App.demo() ? I18n.t("demo.footer") : App.services().store().describe())),
                Ui.fact(I18n.t("about.java"), System.getProperty("java.version") + " • JavaFX " + System.getProperty("javafx.version", "")),
                Ui.fact(I18n.t("about.config"), App.config().file().toString()),
                Ui.muted(I18n.t("about.licenses")));
        box.setPrefWidth(440);
        Dialogs.form(I18n.t("menu.about"), box, I18n.t("action.close"), () -> true);
    }
}
