package io.pyhoma.desktop.ui.components;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

public final class DataTable<T> extends TableView<T> {

    private Consumer<T> onOpen;
    private final List<String> exportHeaders = new java.util.ArrayList<>();
    private final List<Function<T, ?>> exportValues = new java.util.ArrayList<>();
    private final java.util.Map<TableColumn<T, ?>, double[]> widths = new java.util.HashMap<>();

    public DataTable() {
        getStyleClass().add("data-table");
        setFixedCellSize(42);
        setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        widthProperty().addListener((o, a, b) -> fitColumns());
        getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty() && onOpen != null) {
                    onOpen.accept(row.getItem());
                }
            });
            return row;
        });
        setOnKeyPressed(e -> {
            T item = getSelectionModel().getSelectedItem();
            if (e.getCode() == KeyCode.ENTER && item != null && onOpen != null) {
                onOpen.accept(item);
            }
        });
    }

    private boolean selectFirst;

    public DataTable<T> autoSelectFirst() {
        this.selectFirst = true;
        return this;
    }

    public DataTable<T> onOpen(Consumer<T> handler) {
        this.onOpen = handler;
        return this;
    }

    public DataTable<T> placeholder(Node node) {
        setPlaceholder(node);
        return this;
    }

    public DataTable<T> menu(ContextMenu menu) {
        setContextMenu(menu);
        return this;
    }

    public DataTable<T> col(String header, double width, Function<T, ?> value) {
        TableColumn<T, Object> c = new TableColumn<>(header);
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(value.apply(cd.getValue())));
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setGraphic(null);
            }
        });
        size(c, width, false);
        getColumns().add(c);
        exportHeaders.add(header);
        exportValues.add(value);
        return this;
    }

    public DataTable<T> col(String header, double width, String style, Function<T, ?> value) {
        col(header, width, value);
        getColumns().get(getColumns().size() - 1).getStyleClass().add(style);
        return this;
    }

    public DataTable<T> node(String header, double width, Function<T, Node> graphic, Function<T, ?> text) {
        node(header, width, graphic);
        exportHeaders.add(header);
        exportValues.add(text);
        return this;
    }

    public DataTable<T> node(String header, double width, Function<T, Node> graphic) {
        TableColumn<T, T> c = new TableColumn<>(header);
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : graphic.apply(item));
            }
        });
        c.setSortable(false);
        size(c, width, true);
        getColumns().add(c);
        return this;
    }

    private void size(TableColumn<T, ?> c, double width, boolean fixed) {
        double w = width > 0 ? width : 120;
        double min = fixed ? w : Math.min(w, w >= 250 ? 230 : 90);
        widths.put(c, new double[] {w, min});
        c.setPrefWidth(w);
        c.setMinWidth(Math.min(min, 60));
        fitColumns();
    }

    private void fitColumns() {
        double available = getWidth() - 22;
        if (available <= 0 || widths.isEmpty()) {
            return;
        }
        double total = 0;
        double shrinkable = 0;
        TableColumn<T, ?> widest = null;
        for (TableColumn<T, ?> c : getColumns()) {
            double[] w = widths.get(c);
            if (w == null || !c.isVisible()) {
                continue;
            }
            total += w[0];
            shrinkable += w[0] - w[1];
            if (widest == null || w[0] > widths.get(widest)[0]) {
                widest = c;
            }
        }
        double excess = total - available;
        for (TableColumn<T, ?> c : getColumns()) {
            double[] w = widths.get(c);
            if (w == null) {
                continue;
            }
            double size = w[0];
            if (excess < 0 && c == widest) {
                size = w[0] - excess;
            } else if (excess > 0 && shrinkable > 0) {
                size = w[0] - Math.min(excess, shrinkable) * (w[0] - w[1]) / shrinkable;
            }
            c.setPrefWidth(Math.floor(size));
        }
    }

    public void setRows(List<T> rows) {
        T selected = getSelectionModel().getSelectedItem();
        getItems().setAll(FXCollections.observableArrayList(rows));
        if (selected != null) {
            for (T r : rows) {
                if (r.equals(selected)) {
                    getSelectionModel().select(r);
                    break;
                }
            }
        }
        if (selectFirst && getSelectionModel().isEmpty() && !rows.isEmpty()) {
            getSelectionModel().selectFirst();
        }
    }

    public List<String> exportHeaders() {
        return List.copyOf(exportHeaders);
    }

    public List<String> exportRow(T row) {
        List<String> cells = new java.util.ArrayList<>();
        for (Function<T, ?> f : exportValues) {
            Object v = f.apply(row);
            cells.add(v == null ? "" : v.toString());
        }
        return cells;
    }

    public T selected() {
        return getSelectionModel().getSelectedItem();
    }
}
