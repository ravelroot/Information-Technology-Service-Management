package io.pyhoma.desktop.ui.components;

import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;

public final class PickerField<V> extends HBox {

    private String emptyText = I18n.t("picker.none");

    private final ObjectProperty<V> value = new SimpleObjectProperty<>();
    private final Supplier<List<Option<V>>> source;
    private final Label text = new Label();
    private final boolean allowEmpty;
    private List<Option<V>> options = List.of();
    private boolean editable = true;

    public PickerField(Supplier<List<Option<V>>> source, boolean allowEmpty) {
        this.source = source;
        this.allowEmpty = allowEmpty;
        getStyleClass().add("picker-field");
        setAlignment(Pos.CENTER_LEFT);
        setFocusTraversable(true);
        text.getStyleClass().add("picker-text");
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);
        getChildren().addAll(text, Icons.of("chevron-down", 14));
        setOnMouseClicked(e -> open());
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.DOWN) {
                open();
                e.consume();
            }
        });
        value.addListener((o, a, b) -> refreshText());
        refreshText();
    }

    public ObjectProperty<V> valueProperty() {
        return value;
    }

    public V getValue() {
        return value.get();
    }

    public void setValue(V v) {
        value.set(v);
        refreshText();
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("readonly"), !editable);
    }

    public void onChange(Consumer<V> listener) {
        value.addListener((o, a, b) -> listener.accept(b));
    }

    private List<Option<V>> options() {
        if (options.isEmpty()) {
            options = source.get();
        }
        return options;
    }

    public void reload() {
        options = List.of();
        refreshText();
    }

    private void refreshText() {
        V v = value.get();
        if (v == null) {
            text.setText(emptyText);
            text.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("empty"), true);
            return;
        }
        String label = options().stream().filter(o -> Objects.equals(o.value(), v)).map(Option::label).findFirst()
                .orElse(String.valueOf(v));
        text.setText(label);
        text.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("empty"), false);
    }

    private void open() {
        if (!editable || isDisabled()) {
            return;
        }
        Popup popup = new Popup();
        popup.setAutoHide(true);
        TextField filter = new TextField();
        filter.setPromptText(I18n.t("picker.search"));
        ListView<Option<V>> list = new ListView<>();
        list.setPrefHeight(260);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Option<V> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(item.label());
                if (item.color() != null) {
                    Circle c = new Circle(4);
                    c.setStyle("-fx-fill: " + item.color() + ";");
                    setGraphic(c);
                } else {
                    setGraphic(null);
                }
            }
        });
        Runnable apply = () -> {
            List<Option<V>> filtered = new ArrayList<>();
            if (allowEmpty) {
                filtered.add(new Option<>(null, emptyText));
            }
            for (Option<V> o : options()) {
                if (o.matches(filter.getText())) {
                    filtered.add(o);
                }
            }
            list.setItems(FXCollections.observableArrayList(filtered));
            list.getSelectionModel().selectFirst();
        };
        filter.textProperty().addListener((o, a, b) -> apply.run());
        apply.run();
        for (int i = 0; i < list.getItems().size(); i++) {
            if (Objects.equals(list.getItems().get(i).value(), value.get())) {
                list.getSelectionModel().select(i);
                list.scrollTo(Math.max(0, i - 3));
            }
        }
        Consumer<Option<V>> choose = o -> {
            if (o != null) {
                setValue(o.value());
            }
            popup.hide();
            requestFocus();
        };
        list.setOnMouseClicked(e -> choose.accept(list.getSelectionModel().getSelectedItem()));
        filter.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                list.getSelectionModel().selectNext();
                e.consume();
            } else if (e.getCode() == KeyCode.UP) {
                list.getSelectionModel().selectPrevious();
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                choose.accept(list.getSelectionModel().getSelectedItem());
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
            }
        });
        VBox box = new VBox(6, filter, list);
        box.getStyleClass().add("picker-popup");
        box.getStylesheets().setAll(getScene().getStylesheets());
        box.setPrefWidth(Math.max(getWidth(), 260));
        popup.getContent().add(box);
        Bounds b = localToScreen(getBoundsInLocal());
        popup.show(this, b.getMinX(), b.getMaxY() + 4);
        filter.requestFocus();
    }

    public static <V> PickerField<V> compact(Supplier<List<Option<V>>> source, String prompt) {
        PickerField<V> p = new PickerField<>(source, true);
        p.getStyleClass().add("compact");
        p.emptyText = I18n.t("picker.any");
        p.refreshText();
        if (prompt != null) {
            p.text.setText(prompt);
        }
        return p;
    }

    Button asButton() {
        Button b = new Button(text.getText());
        b.setOnAction(e -> open());
        return b;
    }
}
