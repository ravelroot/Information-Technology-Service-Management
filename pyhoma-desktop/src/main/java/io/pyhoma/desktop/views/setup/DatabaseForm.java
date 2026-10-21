package io.pyhoma.desktop.views.setup;

import io.pyhoma.desktop.config.AppPaths;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.domain.port.BackendProvider;
import io.pyhoma.domain.port.DatabaseSettings;
import io.pyhoma.domain.port.DatabaseVendor;
import java.io.File;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

public final class DatabaseForm extends VBox {

    private final BackendProvider provider;
    private final ToggleGroup vendors = new ToggleGroup();
    private final TextField host = new TextField("localhost");
    private final TextField port = new TextField();
    private final TextField database = new TextField("pyhoma");
    private final TextField user = new TextField();
    private final PasswordField password = new PasswordField();
    private final TextField extra = new TextField();
    private final TextField folder = new TextField(AppPaths.defaultEmbeddedDatabaseDir().toString());
    private final Label status = Ui.label("", "db-status");
    private final VBox serverFields = new VBox(12);
    private final VBox embeddedFields = new VBox(12);

    public DatabaseForm(BackendProvider provider, DatabaseSettings initial) {
        super(16);
        this.provider = provider;
        getStyleClass().add("db-form");
        FlowPane cards = new FlowPane(10, 10);
        for (DatabaseVendor v : DatabaseVendor.values()) {
            ToggleButton card = new ToggleButton();
            Label name = Ui.label(I18n.e(v), "vendor-name");
            Label hint = Ui.label(I18n.t("db.vendor." + v.name()), "vendor-hint");
            hint.setWrapText(true);
            hint.setPrefWidth(170);
            VBox content = new VBox(4, Icons.of(v.isEmbedded() ? "hard-drive" : "database", 18), name, hint);
            content.setPrefWidth(170);
            card.setGraphic(content);
            card.setUserData(v);
            card.setToggleGroup(vendors);
            card.getStyleClass().add("vendor-card");
            card.setPrefSize(196, 104);
            card.setMinHeight(104);
            cards.getChildren().add(card);
        }
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.addRow(0, Ui.label(I18n.t("db.host"), "field-label"), host, Ui.label(I18n.t("db.port"), "field-label"), port);
        grid.addRow(1, Ui.label(I18n.t("db.database"), "field-label"), database);
        grid.addRow(2, Ui.label(I18n.t("db.user"), "field-label"), user, Ui.label(I18n.t("db.password"), "field-label"), password);
        grid.add(Ui.label(I18n.t("db.extra"), "field-label"), 0, 3);
        grid.add(extra, 1, 3, 3, 1);
        GridPane.setHgrow(host, Priority.ALWAYS);
        GridPane.setHgrow(database, Priority.ALWAYS);
        GridPane.setHgrow(user, Priority.ALWAYS);
        port.setPrefColumnCount(6);
        extra.setPromptText(I18n.t("db.extraHint"));
        serverFields.getChildren().addAll(grid, Ui.muted(I18n.t("db.serverHint")));
        HBox folderRow = Ui.row(8, folder, Ui.button(I18n.t("db.choose"), "folder", () -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File f = chooser.showDialog(getScene() == null ? null : getScene().getWindow());
            if (f != null) {
                folder.setText(f.getAbsolutePath());
            }
        }));
        HBox.setHgrow(folder, Priority.ALWAYS);
        embeddedFields.getChildren().addAll(Ui.label(I18n.t("db.folder"), "field-label"), folderRow, Ui.muted(I18n.t("db.embeddedHint")));

        HBox testRow = Ui.row(10, Ui.button(I18n.t("db.test"), "activity", this::test), status);
        testRow.setAlignment(Pos.CENTER_LEFT);
        status.setWrapText(true);
        getChildren().addAll(cards, embeddedFields, serverFields, testRow);

        vendors.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                vendors.selectToggle(a);
                return;
            }
            DatabaseVendor v = (DatabaseVendor) b.getUserData();
            serverFields.setVisible(!v.isEmbedded());
            serverFields.setManaged(!v.isEmbedded());
            embeddedFields.setVisible(v.isEmbedded());
            embeddedFields.setManaged(v.isEmbedded());
            if (!v.isEmbedded()) {
                port.setText(String.valueOf(v.defaultPort()));
                database.setPromptText(v == DatabaseVendor.ORACLE ? "FREEPDB1" : "pyhoma");
            }
            status.setText("");
        });
        load(initial);
    }

    private void load(DatabaseSettings s) {
        DatabaseVendor vendor = s == null ? DatabaseVendor.H2 : s.vendor();
        for (var t : vendors.getToggles()) {
            if (t.getUserData() == vendor) {
                vendors.selectToggle(t);
            }
        }
        if (s != null && !vendor.isEmbedded()) {
            host.setText(s.host());
            port.setText(s.port() > 0 ? String.valueOf(s.port()) : String.valueOf(vendor.defaultPort()));
            database.setText(s.database());
            user.setText(s.username());
            password.setText(s.password());
            extra.setText(s.extraParameters());
        }
        if (s != null && vendor.isEmbedded() && s.embeddedPath() != null) {
            folder.setText(s.embeddedPath());
        }
    }

    public DatabaseVendor vendor() {
        return (DatabaseVendor) vendors.getSelectedToggle().getUserData();
    }

    public DatabaseSettings settings() {
        DatabaseVendor v = vendor();
        if (v.isEmbedded()) {
            return DatabaseSettings.embedded(new File(folder.getText().trim()).toPath());
        }
        int p;
        try {
            p = Integer.parseInt(port.getText().trim());
        } catch (NumberFormatException e) {
            p = v.defaultPort();
        }
        return new DatabaseSettings(v, host.getText().trim(), p, database.getText().trim(), user.getText().trim(),
                password.getText(), null, extra.getText() == null || extra.getText().isBlank() ? null : extra.getText().trim());
    }

    private void test() {
        DatabaseSettings s = settings();
        status.setText(I18n.t("db.testing"));
        status.getStyleClass().removeAll("ok", "fail");
        Async.run(() -> provider.testConnection(s), info -> {
            status.setText(I18n.t("db.ok", info));
            status.getStyleClass().add("ok");
        }, e -> {
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            status.setText(I18n.t("db.fail", root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage()));
            status.getStyleClass().add("fail");
        });
    }

    public boolean validateFields() {
        if (vendor().isEmbedded()) {
            if (folder.getText() == null || folder.getText().isBlank()) {
                Dialogs.info(I18n.t("db.title"), I18n.t("db.folderRequired"));
                return false;
            }
            return true;
        }
        if (host.getText().isBlank() && (extra.getText() == null || !extra.getText().startsWith("jdbc:"))) {
            Dialogs.info(I18n.t("db.title"), I18n.t("db.hostRequired"));
            return false;
        }
        return true;
    }

    public static Node gap() {
        return Ui.spacer();
    }
}
