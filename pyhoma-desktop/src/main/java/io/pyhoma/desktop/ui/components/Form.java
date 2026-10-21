package io.pyhoma.desktop.ui.components;

import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.domain.support.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public final class Form {

    private final GridPane grid = new GridPane();
    private final int columns;
    private int col;
    private int row;
    private final List<Binding> bindings = new ArrayList<>();
    private final Map<String, FieldBox> boxes = new LinkedHashMap<>();
    private boolean readOnly;

    private interface Binding {
        void load();

        String commit();

        String field();
    }

    public static final class FieldBox extends VBox {
        private final Label error = Ui.label("", "field-error");
        private final Node control;

        FieldBox(String label, Node control, boolean required) {
            super(5);
            this.control = control;
            getStyleClass().add("field-box");
            Label l = Ui.label(label + (required ? " *" : ""), "field-label");
            l.setLabelFor(control);
            error.setManaged(false);
            error.setVisible(false);
            error.setWrapText(true);
            getChildren().addAll(l, control, error);
            if (control instanceof Control c) {
                c.setMaxWidth(Double.MAX_VALUE);
            }
        }

        void setError(String message) {
            boolean has = message != null;
            error.setText(has ? message : "");
            error.setManaged(has);
            error.setVisible(has);
            control.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("invalid"), has);
        }

        public Node control() {
            return control;
        }
    }

    public Form(int columns) {
        this.columns = columns;
        grid.getStyleClass().add("form-grid");
        grid.setHgap(18);
        grid.setVgap(14);
        for (int i = 0; i < columns; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columns);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }
    }

    public Node node() {
        return grid;
    }

    public Form readOnly(boolean value) {
        this.readOnly = value;
        return this;
    }

    private FieldBox place(String field, Node control, int span, boolean required) {
        FieldBox box = new FieldBox(I18n.field(field), control, required);
        int s = Math.min(span, columns);
        if (col + s > columns) {
            col = 0;
            row++;
        }
        grid.add(box, col, row, s, 1);
        col += s;
        if (col >= columns) {
            col = 0;
            row++;
        }
        boxes.put(field, box);
        if (readOnly) {
            setReadOnly(control);
        }
        return box;
    }

    private static void setReadOnly(Node control) {
        if (control instanceof TextInputControl t) {
            t.setEditable(false);
        } else if (control instanceof PickerField<?> p) {
            p.setEditable(false);
        } else if (control instanceof HBox h) {
            h.getChildren().forEach(Form::setReadOnly);
        } else if (control instanceof DatePicker d) {
            d.setDisable(true);
        } else if (control instanceof CheckBox c) {
            c.setDisable(true);
        }
    }

    public Form section(String title) {
        if (col != 0) {
            col = 0;
            row++;
        }
        Label l = Ui.label(title, "form-section");
        grid.add(l, 0, row, columns, 1);
        row++;
        return this;
    }

    public Form custom(String field, Node node, int span) {
        place(field, node, span, false);
        return this;
    }

    public Form text(String field, Supplier<String> get, Consumer<String> set, int span, boolean required) {
        TextField tf = new TextField();
        place(field, tf, span, required);
        bindings.add(new Binding() {
            public void load() {
                tf.setText(get.get());
            }

            public String commit() {
                set.accept(tf.getText() == null || tf.getText().isBlank() ? null : tf.getText().trim());
                return null;
            }

            public String field() {
                return field;
            }
        });
        return this;
    }

    public Form text(String field, Supplier<String> get, Consumer<String> set) {
        return text(field, get, set, 1, false);
    }

    public Form code(String field, Supplier<String> get, Consumer<String> set) {
        text(field, get, set, 1, false);
        boxes.get(field).control().getStyleClass().add("mono");
        return this;
    }

    public Form area(String field, Supplier<String> get, Consumer<String> set, int rows, boolean required) {
        TextArea ta = new TextArea();
        ta.setWrapText(true);
        ta.setPrefRowCount(rows);
        place(field, ta, columns, required);
        bindings.add(new Binding() {
            public void load() {
                ta.setText(get.get());
            }

            public String commit() {
                set.accept(ta.getText() == null || ta.getText().isBlank() ? null : ta.getText());
                return null;
            }

            public String field() {
                return field;
            }
        });
        return this;
    }

    public Form integer(String field, Supplier<Integer> get, Consumer<Integer> set) {
        TextField tf = new TextField();
        tf.getStyleClass().add("num");
        place(field, tf, 1, false);
        bindings.add(new Binding() {
            public void load() {
                Integer v = get.get();
                tf.setText(v == null ? "" : v.toString());
            }

            public String commit() {
                String t = tf.getText() == null ? "" : tf.getText().trim();
                if (t.isEmpty()) {
                    set.accept(null);
                    return null;
                }
                try {
                    set.accept(Integer.valueOf(t));
                    return null;
                } catch (NumberFormatException e) {
                    return "validation.number";
                }
            }

            public String field() {
                return field;
            }
        });
        return this;
    }

    public Form decimal(String field, Supplier<BigDecimal> get, Consumer<BigDecimal> set) {
        TextField tf = new TextField();
        tf.getStyleClass().add("num");
        place(field, tf, 1, false);
        bindings.add(new Binding() {
            public void load() {
                BigDecimal v = get.get();
                tf.setText(v == null ? "" : v.toPlainString().replace('.', I18n.isPortuguese() ? ',' : '.'));
            }

            public String commit() {
                String t = tf.getText() == null ? "" : tf.getText().trim();
                if (t.isEmpty()) {
                    set.accept(null);
                    return null;
                }
                try {
                    String normalized = I18n.isPortuguese() ? t.replace(".", "").replace(',', '.') : t.replace(",", "");
                    set.accept(new BigDecimal(normalized));
                    return null;
                } catch (NumberFormatException e) {
                    return "validation.number";
                }
            }

            public String field() {
                return field;
            }
        });
        return this;
    }

    public Form check(String field, Supplier<Boolean> get, Consumer<Boolean> set) {
        CheckBox cb = new CheckBox(I18n.t("field." + field + ".check"));
        if (cb.getText().startsWith("field.")) {
            cb.setText(I18n.t("common.yes"));
        }
        place(field, cb, 1, false);
        bindings.add(new Binding() {
            public void load() {
                cb.setSelected(Boolean.TRUE.equals(get.get()));
            }

            public String commit() {
                set.accept(cb.isSelected());
                return null;
            }

            public String field() {
                return field;
            }
        });
        return this;
    }

    public <V> Form choice(String field, Supplier<List<Option<V>>> options, Supplier<V> get, Consumer<V> set,
                           boolean allowEmpty, boolean required) {
        PickerField<V> picker = new PickerField<>(options, allowEmpty);
        place(field, picker, 1, required);
        bindings.add(new Binding() {
            public void load() {
                picker.setValue(get.get());
            }

            public String commit() {
                set.accept(picker.getValue());
                return null;
            }

            public String field() {
                return field;
            }
        });
        return this;
    }

    public <V> PickerField<V> pickerOf(String field) {
        @SuppressWarnings("unchecked")
        PickerField<V> p = (PickerField<V>) boxes.get(field).control();
        return p;
    }

    public Form date(String field, Supplier<LocalDate> get, Consumer<LocalDate> set) {
        DatePicker dp = datePicker();
        place(field, dp, 1, false);
        bindings.add(new Binding() {
            public void load() {
                dp.setValue(get.get());
            }

            public String commit() {
                try {
                    String raw = dp.getEditor().getText();
                    set.accept(raw == null || raw.isBlank() ? null : dp.getConverter().fromString(raw));
                    return null;
                } catch (DateTimeParseException e) {
                    return "validation.date";
                }
            }

            public String field() {
                return field;
            }
        });
        return this;
    }

    public Form dateTime(String field, Supplier<LocalDateTime> get, Consumer<LocalDateTime> set) {
        DatePicker dp = datePicker();
        TextField time = new TextField();
        time.setPromptText("HH:mm");
        time.setPrefColumnCount(5);
        time.getStyleClass().add("num");
        HBox box = new HBox(6, dp, time);
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(dp, Priority.ALWAYS);
        place(field, box, 1, false);
        bindings.add(new Binding() {
            public void load() {
                LocalDateTime v = get.get();
                dp.setValue(v == null ? null : v.toLocalDate());
                time.setText(v == null ? "" : v.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }

            public String commit() {
                try {
                    String raw = dp.getEditor().getText();
                    LocalDate d = raw == null || raw.isBlank() ? null : dp.getConverter().fromString(raw);
                    if (d == null) {
                        set.accept(null);
                        return null;
                    }
                    String t = time.getText() == null || time.getText().isBlank() ? "09:00" : time.getText().trim();
                    set.accept(LocalDateTime.of(d, LocalTime.parse(t.length() == 4 ? "0" + t : t)));
                    return null;
                } catch (DateTimeParseException e) {
                    return "validation.date";
                }
            }

            public String field() {
                return field;
            }
        });
        return this;
    }

    static DatePicker datePicker() {
        DatePicker dp = new DatePicker();
        dp.setMaxWidth(Double.MAX_VALUE);
        dp.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate d) {
                return d == null ? "" : I18n.dateFormatter().format(d);
            }

            @Override
            public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(s.trim(), I18n.dateFormatter());
                } catch (DateTimeParseException e) {
                    return LocalDate.parse(s.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                }
            }
        });
        dp.setPromptText(I18n.isPortuguese() ? "dd/mm/aaaa" : "Mmm d, yyyy");
        return dp;
    }

    public Form load() {
        bindings.forEach(Binding::load);
        boxes.values().forEach(b -> b.setError(null));
        return this;
    }

    public boolean commit() {
        boolean ok = true;
        boxes.values().forEach(b -> b.setError(null));
        for (Binding b : bindings) {
            String err = b.commit();
            if (err != null) {
                ok = false;
                boxes.get(b.field()).setError(I18n.t(err));
            }
        }
        return ok;
    }

    public boolean showErrors(ValidationException e) {
        boolean any = false;
        for (Map.Entry<String, String> entry : e.fieldErrors().entrySet()) {
            FieldBox box = boxes.get(entry.getKey());
            if (box != null) {
                box.setError(I18n.t(entry.getValue()));
                any = true;
            }
        }
        return any;
    }

    public FieldBox box(String field) {
        return boxes.get(field);
    }

    public void focusFirst() {
        if (!boxes.isEmpty()) {
            boxes.values().iterator().next().control().requestFocus();
        }
    }
}
