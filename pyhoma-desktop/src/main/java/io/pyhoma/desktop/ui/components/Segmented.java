package io.pyhoma.desktop.ui.components;

import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

public final class Segmented<T> extends HBox {

    private final ToggleGroup group = new ToggleGroup();

    public Segmented(List<T> values, java.util.function.Function<T, String> label, T selected, Consumer<T> onChange) {
        getStyleClass().add("segmented");
        setMaxWidth(USE_PREF_SIZE);
        for (T v : values) {
            ToggleButton b = new ToggleButton(label.apply(v));
            b.setUserData(v);
            b.setToggleGroup(group);
            b.setSelected(v.equals(selected));
            getChildren().add(b);
        }
        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {
                group.selectToggle(old);
                return;
            }
            @SuppressWarnings("unchecked")
            T value = (T) now.getUserData();
            onChange.accept(value);
        });
    }

    public void select(T value) {
        for (javafx.scene.control.Toggle t : group.getToggles()) {
            if (value.equals(t.getUserData()) && !t.isSelected()) {
                t.setSelected(true);
            }
        }
    }
}
