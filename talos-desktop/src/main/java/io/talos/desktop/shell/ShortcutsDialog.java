package io.talos.desktop.shell;

import io.talos.desktop.ui.Dialogs;
import io.talos.desktop.ui.I18n;
import io.talos.desktop.ui.Ui;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Guia de atalhos de teclado (F1). */
public final class ShortcutsDialog {

    private ShortcutsDialog() {
    }

    /** Grupos de atalhos: chave do título e pares (teclas, chave da descrição). */
    static final String[][][] GROUPS = {
        {{"shortcuts.group.general"}, {"Ctrl K", "shortcuts.palette"}, {"Ctrl N", "shortcuts.newTicket"},
            {"F1", "shortcuts.help"}, {"F5", "shortcuts.refresh"}, {"Esc", "shortcuts.close"}},
        {{"shortcuts.group.view"}, {"Ctrl +", "shortcuts.zoomIn"}, {"Ctrl −", "shortcuts.zoomOut"},
            {"Ctrl 0", "shortcuts.zoomReset"}, {"Ctrl + rolagem", "shortcuts.zoomWheel"}},
        {{"shortcuts.group.lists"}, {"↑ ↓", "shortcuts.move"}, {"Enter", "shortcuts.open"},
            {"Ctrl Shift N", "shortcuts.tickets"}},
    };

    public static Node content() {
        VBox box = new VBox(2);
        box.setPrefWidth(460);
        for (String[][] group : GROUPS) {
            box.getChildren().add(Ui.label(I18n.t(group[0][0]), "shortcut-group"));
            for (int i = 1; i < group.length; i++) {
                String keys = group[i][0].replace("rolagem", I18n.t("shortcuts.wheel"));
                HBox chips = new HBox(4);
                for (String k : keys.split(" ")) {
                    chips.getChildren().add(Ui.label(k, "kbd", "kbd-lg"));
                }
                chips.setAlignment(Pos.CENTER_RIGHT);
                Label what = new Label(I18n.t(group[i][1]));
                HBox row = new HBox(12, what, Ui.spacer(), chips);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("shortcut-row");
                box.getChildren().add(row);
            }
        }
        return box;
    }

    public static javafx.scene.control.Dialog<javafx.scene.control.ButtonType> show() {
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> d = Dialogs.infoDialog(I18n.t("shortcuts.title"), content());
        d.show();
        return d;
    }
}
