package io.pyhoma.desktop.shell;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Theme;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.domain.service.SearchService.Hit;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public final class GlobalSearch {

    private GlobalSearch() {
    }

    private static String icon(String type) {
        return switch (type) {
            case "Ticket" -> "ticket";
            case "Problem" -> "triangle-alert";
            case "Change" -> "git-pull-request-arrow";
            case "Asset" -> "monitor-smartphone";
            case "User" -> "user";
            case "KbArticle" -> "book-open";
            case "Project" -> "folder-kanban";
            case "Software" -> "app-window";
            case "Contract" -> "file-signature";
            case "Supplier" -> "truck";
            default -> "circle-dot";
        };
    }

    public static void show(Stage owner) {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        TextField query = new TextField();
        query.setPromptText(I18n.t("search.prompt"));
        query.getStyleClass().add("palette-input");
        HBox inputRow = new HBox(10, Icons.of("search", 18), query);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.getStyleClass().add("palette-input-row");
        HBox.setHgrow(query, Priority.ALWAYS);
        ListView<Hit> results = new ListView<>();
        results.getStyleClass().add("palette-results");
        results.setPrefHeight(380);
        Label hint = Ui.label(I18n.t("search.hint"), "muted");
        results.setPlaceholder(hint);
        results.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Hit h, boolean empty) {
                super.updateItem(h, empty);
                if (empty || h == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label title = Ui.label(h.title(), "palette-title");
                Label sub = Ui.label(I18n.t("type." + h.type()) + (h.subtitle() == null ? "" : "  •  " + h.subtitle()), "muted");
                VBox texts = new VBox(1, title, sub);
                HBox row = new HBox(12, Icons.of(icon(h.type()), 16), texts);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
        PauseTransition debounce = new PauseTransition(Duration.millis(220));
        debounce.setOnFinished(e -> {
            String q = query.getText();
            Async.run(() -> App.services().search().search(q, 6), hits -> {
                if (q.equals(query.getText())) {
                    results.setItems(FXCollections.observableArrayList(hits));
                    results.getSelectionModel().selectFirst();
                    if (hits.isEmpty() && q.trim().length() >= 2) {
                        results.setPlaceholder(Ui.label(I18n.t("search.none", q), "muted"));
                    }
                }
            });
        });
        query.textProperty().addListener((o, a, b) -> debounce.playFromStart());
        Runnable openSelected = () -> {
            Hit h = results.getSelectionModel().getSelectedItem();
            if (h != null) {
                stage.close();
                App.nav().openItem(h.type(), h.id());
            }
        };
        query.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case DOWN -> {
                    results.getSelectionModel().selectNext();
                    e.consume();
                }
                case UP -> {
                    results.getSelectionModel().selectPrevious();
                    e.consume();
                }
                case ENTER -> openSelected.run();
                case ESCAPE -> stage.close();
                default -> { }
            }
        });
        results.setOnMouseClicked(e -> openSelected.run());
        results.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                openSelected.run();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });
        VBox box = new VBox(inputRow, results);
        box.getStyleClass().add("palette");
        box.setPrefWidth(640);
        VBox.setVgrow(results, Priority.ALWAYS);
        Scene scene = new Scene(box);
        scene.setFill(Color.TRANSPARENT);
        Theme.install(scene);
        stage.setScene(scene);
        stage.focusedProperty().addListener((o, a, focused) -> {
            if (!focused) {
                stage.close();
            }
        });
        stage.setOnShown(e -> {
            stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2);
            stage.setY(owner.getY() + 110);
            query.requestFocus();
        });
        stage.show();
    }
}
