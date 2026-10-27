package io.talos.desktop.shell;

import io.talos.desktop.App;
import io.talos.desktop.ui.Async;
import io.talos.desktop.ui.I18n;
import io.talos.desktop.ui.Icons;
import io.talos.desktop.ui.Theme;
import io.talos.desktop.ui.Ui;
import io.talos.domain.model.itil.ItilStatus;
import io.talos.domain.model.itil.ItilType;
import io.talos.domain.security.Right;
import io.talos.domain.service.SearchService.Hit;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

/**
 * Paleta de comandos (Ctrl+K): ações rápidas, navegação para qualquer tela e busca global
 * em chamados, ativos, pessoas, artigos e projetos — tudo pelo teclado.
 */
public final class GlobalSearch {

    /** Uma linha da paleta: um comando ou um resultado de busca. */
    private record Entry(String group, String icon, String title, String hint, Runnable action) {
    }

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

    private static List<Entry> commands() {
        List<Entry> list = new ArrayList<>();
        String actions = I18n.t("palette.group.actions");
        if (App.can(io.talos.domain.model.core.Module.TICKET, Right.CREATE)) {
            list.add(new Entry(actions, "plus", I18n.t("palette.newTicket"), "Ctrl N", () -> newItem(ItilType.TICKET)));
        }
        if (App.can(io.talos.domain.model.core.Module.PROBLEM, Right.CREATE)) {
            list.add(new Entry(actions, "triangle-alert", I18n.t("palette.newProblem"), null, () -> newItem(ItilType.PROBLEM)));
        }
        if (App.can(io.talos.domain.model.core.Module.CHANGE, Right.CREATE)) {
            list.add(new Entry(actions, "git-pull-request-arrow", I18n.t("palette.newChange"), null,
                    () -> newItem(ItilType.CHANGE)));
        }
        list.add(new Entry(actions, Theme.mode() == Theme.Mode.DARK ? "sun" : "moon", I18n.t("palette.toggleTheme"), null,
                MainWindow::toggleTheme));
        list.add(new Entry(actions, "keyboard", I18n.t("palette.shortcuts"), "F1", () -> ShortcutsDialog.show()));
        list.add(new Entry(actions, "refresh-cw", I18n.t("palette.refresh"), "F5", () -> App.nav().refresh()));
        String go = I18n.t("palette.group.goTo");
        for (Route r : MainWindow.visibleRoutes()) {
            list.add(new Entry(go, r.icon(), I18n.t("nav." + r.name()), I18n.t("nav.section." + r.section().name()),
                    () -> App.nav().go(r)));
        }
        return list;
    }

    private static void newItem(ItilType type) {
        io.talos.desktop.views.itil.ItilQuickForm.open(type, ItilStatus.NEW, io.talos.desktop.views.itil.ItilQuickForm::openCreated);
    }

    static String fold(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }

    private static List<Entry> filter(List<Entry> all, String query) {
        String q = fold(query);
        if (q.isEmpty()) {
            return all;
        }
        List<Entry> starts = new ArrayList<>();
        List<Entry> contains = new ArrayList<>();
        for (Entry e : all) {
            String t = fold(e.title());
            if (t.startsWith(q)) {
                starts.add(e);
            } else if (t.contains(q) || fold(e.hint()).contains(q)) {
                contains.add(e);
            }
        }
        starts.addAll(contains);
        return starts.size() > 8 ? starts.subList(0, 8) : starts;
    }

    public static Stage show(Stage owner) {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        List<Entry> commands = commands();

        TextField query = new TextField();
        query.setPromptText(I18n.t("palette.prompt"));
        query.getStyleClass().add("palette-input");
        Label esc = Ui.label("Esc", "kbd");
        HBox inputRow = new HBox(10, Icons.of("command", 18), query, esc);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.getStyleClass().add("palette-input-row");
        HBox.setHgrow(query, Priority.ALWAYS);

        ListView<Entry> results = new ListView<>();
        results.getStyleClass().add("palette-results");
        results.setPrefHeight(400);
        results.setPlaceholder(Ui.label(I18n.t("search.hint"), "muted"));
        results.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Entry e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                int index = getIndex();
                List<Entry> items = getListView().getItems();
                boolean firstOfGroup = index == 0 || !items.get(index - 1).group().equals(e.group());
                Label title = Ui.label(e.title(), "palette-row-title");
                HBox row = new HBox(10, Icons.of(e.icon(), 16), title, Ui.spacer());
                if (e.hint() != null) {
                    boolean keys = e.hint().matches("(Ctrl|Shift|Alt|Esc|F\\d+)\\b.*");
                    row.getChildren().add(Ui.label(e.hint(), keys ? "kbd" : "palette-row-hint"));
                }
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("palette-row");
                row.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), isSelected());
                if (firstOfGroup) {
                    Label group = Ui.label(e.group().toUpperCase(I18n.locale()), "palette-group");
                    setGraphic(new VBox(group, row));
                } else {
                    setGraphic(row);
                }
                setText(null);
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (getGraphic() != null) {
                    getGraphic().lookupAll(".palette-row").forEach(n ->
                            n.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), selected));
                }
            }
        });
        results.setItems(FXCollections.observableArrayList(commands));
        results.getSelectionModel().selectFirst();

        PauseTransition debounce = new PauseTransition(Duration.millis(200));
        debounce.setOnFinished(e -> {
            String q = query.getText();
            if (q.trim().length() < 2) {
                return;
            }
            Async.run(() -> App.services().search().search(q, 6), hits -> {
                if (!q.equals(query.getText())) {
                    return;
                }
                List<Entry> merged = new ArrayList<>(filter(commands, q));
                String group = I18n.t("palette.group.results");
                for (Hit h : hits) {
                    String sub = I18n.t("type." + h.type()) + (h.subtitle() == null ? "" : "  ·  " + h.subtitle());
                    merged.add(new Entry(group, icon(h.type()), h.title(), sub, () -> App.nav().openItem(h.type(), h.id())));
                }
                results.setItems(FXCollections.observableArrayList(merged));
                results.getSelectionModel().selectFirst();
                if (merged.isEmpty()) {
                    results.setPlaceholder(Ui.label(I18n.t("search.none", q), "muted"));
                }
            });
        });
        query.textProperty().addListener((o, a, b) -> {
            results.setItems(FXCollections.observableArrayList(filter(commands, b)));
            results.getSelectionModel().selectFirst();
            debounce.playFromStart();
        });
        Runnable openSelected = () -> {
            Entry h = results.getSelectionModel().getSelectedItem();
            if (h != null) {
                stage.close();
                h.action().run();
            }
        };
        query.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case DOWN -> {
                    results.getSelectionModel().selectNext();
                    results.scrollTo(results.getSelectionModel().getSelectedIndex());
                    e.consume();
                }
                case UP -> {
                    results.getSelectionModel().selectPrevious();
                    results.scrollTo(results.getSelectionModel().getSelectedIndex());
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
        HBox foot = new HBox(Ui.label("↑ ↓  " + I18n.t("palette.foot.navigate")), Ui.label("Enter  " + I18n.t("palette.foot.open")),
                Ui.label("Esc  " + I18n.t("palette.foot.close")), Ui.spacer(), Ui.label(I18n.t("palette.foot.tip")));
        foot.getStyleClass().add("palette-foot");
        VBox box = new VBox(inputRow, results, foot);
        box.getStyleClass().add("palette");
        box.setPrefWidth(660);
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
            stage.setY(owner.getY() + 96);
            query.requestFocus();
        });
        stage.show();
        return stage;
    }
}
