package io.pyhoma.desktop.ui.components;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.domain.model.core.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

public final class PeopleField extends FlowPane {

    private final List<Long> values = new ArrayList<>();
    private final Supplier<List<Option<Long>>> source;
    private List<Option<Long>> options = List.of();
    private final String addText;
    private Runnable onChange = () -> { };

    public PeopleField(Supplier<List<Option<Long>>> source, String addText) {
        super(6, 6);
        this.source = source;
        this.addText = addText;
        getStyleClass().add("people-field");
        setAlignment(Pos.CENTER_LEFT);
        render();
    }

    public static List<Option<Long>> activeUsers() {
        List<Option<Long>> list = new ArrayList<>();
        for (User u : App.services().users().activeUsers()) {
            list.add(new Option<>(u.getId(), u.getFullName(), u.getJobTitle() == null ? u.getLogin() : u.getJobTitle(), null));
        }
        return list;
    }

    public List<Long> getValues() {
        return List.copyOf(values);
    }

    public void setValues(Collection<Long> ids) {
        values.clear();
        if (ids != null) {
            for (Long id : ids) {
                if (id != null && !values.contains(id)) {
                    values.add(id);
                }
            }
        }
        render();
    }

    public void onChange(Runnable listener) {
        this.onChange = listener == null ? () -> { } : listener;
    }

    private List<Option<Long>> options() {
        if (options.isEmpty()) {
            options = source.get();
        }
        return options;
    }

    private String name(Long id) {
        return options().stream().filter(o -> Objects.equals(o.value(), id)).map(Option::label).findFirst()
                .orElseGet(() -> App.lookups().user(id));
    }

    private void render() {
        getChildren().clear();
        for (Long id : values) {
            String n = name(id);
            Label l = new Label(n);
            l.getStyleClass().add("people-chip-text");
            Button x = new Button();
            x.setGraphic(Icons.of("x", 12));
            x.getStyleClass().addAll("people-chip-remove");
            x.setTooltip(new Tooltip(I18n.t("people.remove", n)));
            x.setFocusTraversable(false);
            x.setOnAction(e -> {
                values.remove(id);
                render();
                onChange.run();
            });
            HBox chip = new HBox(6, new Avatar(n, 20), l, x);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.getStyleClass().add("people-chip");
            getChildren().add(chip);
        }
        Button add = new Button(addText);
        add.setGraphic(Icons.of("user-plus", 14));
        add.getStyleClass().add("people-add");
        add.setOnAction(e -> open(add));
        getChildren().add(add);
    }

    private void open(Button anchor) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        TextField filter = new TextField();
        filter.setPromptText(I18n.t("picker.search"));
        ListView<Option<Long>> list = new ListView<>();
        list.setPrefHeight(240);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Option<Long> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label main = new Label(item.label());
                VBox text = item.detail() == null ? new VBox(main) : new VBox(0, main, io.pyhoma.desktop.ui.Ui.muted(item.detail()));
                HBox row = new HBox(8, new Avatar(item.label(), 22), text);
                row.setAlignment(Pos.CENTER_LEFT);
                setText(null);
                setGraphic(row);
            }
        });
        Runnable apply = () -> {
            List<Option<Long>> filtered = new ArrayList<>();
            for (Option<Long> o : options()) {
                if (!values.contains(o.value()) && o.matches(filter.getText())) {
                    filtered.add(o);
                }
            }
            list.setItems(FXCollections.observableArrayList(filtered));
            list.getSelectionModel().selectFirst();
        };
        filter.textProperty().addListener((o, a, b) -> apply.run());
        apply.run();
        Runnable choose = () -> {
            Option<Long> o = list.getSelectionModel().getSelectedItem();
            if (o != null) {
                values.add(o.value());
                render();
                onChange.run();
            }
            popup.hide();
        };
        list.setOnMouseClicked(e -> choose.run());
        filter.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                list.getSelectionModel().selectNext();
                e.consume();
            } else if (e.getCode() == KeyCode.UP) {
                list.getSelectionModel().selectPrevious();
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                choose.run();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
            }
        });
        VBox box = new VBox(6, filter, list);
        box.getStyleClass().add("picker-popup");
        box.getStylesheets().setAll(getScene().getStylesheets());
        box.setPrefWidth(300);
        popup.getContent().add(box);
        Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
        popup.show(anchor, b.getMinX(), b.getMaxY() + 4);
        filter.requestFocus();
    }
}
