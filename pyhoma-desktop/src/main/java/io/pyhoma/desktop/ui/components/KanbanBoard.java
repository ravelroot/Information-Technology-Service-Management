package io.pyhoma.desktop.ui.components;

import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Ui;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class KanbanBoard<T, K> extends ScrollPane {

    public record Column<K>(K key, String title, String tone) {
    }

    public record LaneStat(String icon, String text, String tooltip, String style) {
    }

    public record SortOption<T>(String label, Comparator<T> comparator) {
    }

    private final HBox lanes = new HBox(12);
    private final List<Column<K>> columns;
    private final Function<T, Node> cardFactory;
    private final Function<T, Long> idOf;
    private final Map<Long, T> items = new LinkedHashMap<>();
    private final Map<K, List<T>> byColumn = new HashMap<>();
    private final Map<K, SortOption<T>> sortBy = new HashMap<>();
    private final Map<K, VBox> laneNodes = new HashMap<>();
    private BiConsumer<T, K> onMove;
    private Consumer<T> onOpen;
    private Consumer<K> onAdd;
    private Predicate<K> canAdd = k -> true;
    private String addText = "";
    private Function<List<T>, List<LaneStat>> stats = l -> List.of();
    private Function<T, List<String>> cardStyles = t -> List.of();
    private List<SortOption<T>> sorts = List.of();
    private final Map<Long, Node> cardNodes = new HashMap<>();

    public KanbanBoard(List<Column<K>> columns, Function<T, Node> cardFactory, Function<T, Long> idOf) {
        this.columns = new ArrayList<>(columns);
        this.cardFactory = cardFactory;
        this.idOf = idOf;
        getStyleClass().add("kanban");
        lanes.getStyleClass().add("kanban-lanes");
        setContent(lanes);
        setFitToHeight(true);
        setVbarPolicy(ScrollBarPolicy.NEVER);
    }

    public KanbanBoard<T, K> onMove(BiConsumer<T, K> handler) {
        this.onMove = handler;
        return this;
    }

    public KanbanBoard<T, K> onOpen(Consumer<T> handler) {
        this.onOpen = handler;
        return this;
    }

    public KanbanBoard<T, K> onAdd(Consumer<K> handler, Predicate<K> allowed, String text) {
        this.onAdd = handler;
        this.canAdd = allowed;
        this.addText = text;
        return this;
    }

    public KanbanBoard<T, K> stats(Function<List<T>, List<LaneStat>> stats) {
        this.stats = stats;
        return this;
    }

    public KanbanBoard<T, K> cardStyles(Function<T, List<String>> styles) {
        this.cardStyles = styles;
        return this;
    }

    public KanbanBoard<T, K> sorts(List<SortOption<T>> sorts) {
        this.sorts = List.copyOf(sorts);
        return this;
    }

    public void highlight(Long id) {
        Node card = id == null ? null : cardNodes.get(id);
        if (card == null) {
            return;
        }
        card.getStyleClass().add("kanban-card-new");
        javafx.application.Platform.runLater(() -> {
            Node lane = card;
            while (lane != null && !(lane instanceof ScrollPane)) {
                lane = lane.getParent();
            }
            if (lane instanceof ScrollPane sp && sp.getContent() != null) {
                double h = sp.getContent().getBoundsInLocal().getHeight() - sp.getViewportBounds().getHeight();
                if (h > 0) {
                    sp.setVvalue(Math.min(1, card.getBoundsInParent().getMinY() / h));
                }
            }
            double laneX = card.localToScene(0, 0).getX() - lanes.localToScene(0, 0).getX();
            double w = lanes.getBoundsInLocal().getWidth() - getViewportBounds().getWidth();
            if (w > 0) {
                setHvalue(Math.max(0, Math.min(1, (laneX - 40) / w)));
            }
            card.setOpacity(0);
            card.setTranslateY(-8);
            javafx.animation.Timeline in = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(320),
                            new javafx.animation.KeyValue(card.opacityProperty(), 1, javafx.animation.Interpolator.EASE_OUT),
                            new javafx.animation.KeyValue(card.translateYProperty(), 0, javafx.animation.Interpolator.EASE_OUT)));
            javafx.animation.PauseTransition hold = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2.4));
            hold.setOnFinished(e -> card.getStyleClass().remove("kanban-card-new"));
            new javafx.animation.SequentialTransition(in, hold).play();
        });
    }

    public void setItems(Map<K, List<T>> data) {
        items.clear();
        cardNodes.clear();
        byColumn.clear();
        laneNodes.clear();
        lanes.getChildren().clear();
        for (Column<K> c : columns) {
            List<T> list = new ArrayList<>(data.getOrDefault(c.key(), List.of()));
            list.forEach(i -> items.put(idOf.apply(i), i));
            byColumn.put(c.key(), list);
            VBox lane = new VBox();
            lane.getStyleClass().add("lane");
            lane.setPrefWidth(292);
            lane.setMinWidth(264);
            lane.setMaxWidth(292);
            laneNodes.put(c.key(), lane);
            fillLane(c, lane);
            wireDrop(c, lane);
            lanes.getChildren().add(lane);
        }
    }

    private void fillLane(Column<K> column, VBox lane) {
        List<T> list = new ArrayList<>(byColumn.getOrDefault(column.key(), List.of()));
        SortOption<T> sort = sortBy.get(column.key());
        if (sort != null) {
            list.sort(sort.comparator());
        }
        Region led = new Region();
        led.getStyleClass().addAll("lane-led", "tone-" + column.tone());
        Label title = Ui.label(column.title(), "lane-title");
        title.setMinWidth(0);
        Button more = new Button();
        more.setGraphic(Icons.of("ellipsis", 16));
        more.getStyleClass().add("lane-more");
        more.setTooltip(new Tooltip(I18n.t("kanban.laneMenu")));
        more.setOnAction(e -> laneMenu(column, lane).show(more, javafx.geometry.Side.BOTTOM, 0, 4));
        HBox head = new HBox(8, led, title, Ui.spacer(), more);
        head.setAlignment(Pos.CENTER_LEFT);
        HBox statRow = new HBox(14);
        statRow.setAlignment(Pos.CENTER_LEFT);
        statRow.getStyleClass().add("lane-stats");
        for (LaneStat s : stats.apply(list)) {
            Label l = new Label(s.text());
            l.setGraphic(Icons.of(s.icon(), 13));
            l.getStyleClass().add("lane-stat");
            if (s.style() != null) {
                l.getStyleClass().add(s.style());
            }
            if (s.tooltip() != null) {
                l.setTooltip(new Tooltip(s.tooltip()));
            }
            statRow.getChildren().add(l);
        }
        VBox header = new VBox(6, head, statRow);
        header.getStyleClass().add("lane-head");
        VBox cards = new VBox(10);
        cards.getStyleClass().add("lane-cards");
        if (sort != null) {
            Label sorted = new Label(I18n.t("kanban.sortedBy", sort.label()));
            sorted.getStyleClass().add("lane-sort-text");
            Button clear = new Button();
            clear.setGraphic(Icons.of("x", 13));
            clear.getStyleClass().add("lane-sort-clear");
            clear.setTooltip(new Tooltip(I18n.t("kanban.sortClear")));
            clear.setOnAction(e -> {
                sortBy.remove(column.key());
                fillLane(column, lane);
            });
            HBox bar = new HBox(8, sorted, Ui.spacer(), Icons.of("arrow-up-down", 13), clear);
            bar.setAlignment(Pos.CENTER_LEFT);
            bar.getStyleClass().add("lane-sort");
            cards.getChildren().add(bar);
        }
        for (T item : list) {
            cards.getChildren().add(card(item));
        }
        if (list.isEmpty()) {
            Label empty = Ui.label(I18n.t("kanban.laneEmpty"), "lane-empty");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            cards.getChildren().add(empty);
        }
        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("lane-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        lane.getChildren().setAll(header, scroll);
        if (onAdd != null && canAdd.test(column.key())) {
            Button add = new Button(addText);
            add.setGraphic(Icons.of("plus", 15));
            add.getStyleClass().add("lane-add-task");
            add.setMaxWidth(Double.MAX_VALUE);
            add.setAlignment(Pos.CENTER_LEFT);
            add.setOnAction(e -> onAdd.accept(column.key()));
            HBox foot = new HBox(add);
            HBox.setHgrow(add, Priority.ALWAYS);
            foot.getStyleClass().add("lane-foot");
            lane.getChildren().add(foot);
        }
    }

    private ContextMenu laneMenu(Column<K> column, VBox lane) {
        ContextMenu menu = new ContextMenu();
        if (onAdd != null && canAdd.test(column.key())) {
            MenuItem add = new MenuItem(addText, Icons.of("plus", 14));
            add.setOnAction(e -> onAdd.accept(column.key()));
            menu.getItems().addAll(add, new SeparatorMenuItem());
        }
        for (SortOption<T> s : sorts) {
            MenuItem mi = new MenuItem(I18n.t("kanban.sortBy", s.label()), Icons.of("arrow-up-down", 14));
            mi.setOnAction(e -> {
                sortBy.put(column.key(), s);
                fillLane(column, lane);
            });
            menu.getItems().add(mi);
        }
        if (sortBy.containsKey(column.key())) {
            MenuItem clear = new MenuItem(I18n.t("kanban.sortClear"), Icons.of("x", 14));
            clear.setOnAction(e -> {
                sortBy.remove(column.key());
                fillLane(column, lane);
            });
            menu.getItems().add(clear);
        }
        if (menu.getItems().isEmpty()) {
            MenuItem none = new MenuItem(I18n.t("kanban.laneEmpty"));
            none.setDisable(true);
            menu.getItems().add(none);
        }
        return menu;
    }

    private void wireDrop(Column<K> column, VBox lane) {
        lane.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });
        lane.setOnDragEntered(e -> lane.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("drop"), true));
        lane.setOnDragExited(e -> lane.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("drop"), false));
        lane.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean done = false;
            if (db.hasString()) {
                T item = items.get(Long.valueOf(db.getString()));
                if (item != null && onMove != null) {
                    onMove.accept(item, column.key());
                    done = true;
                }
            }
            e.setDropCompleted(done);
            e.consume();
        });
    }

    private Node card(T item) {
        Node content = cardFactory.apply(item);
        VBox card = new VBox(content);
        card.getStyleClass().add("kanban-card");
        card.getStyleClass().addAll(cardStyles.apply(item));
        cardNodes.put(idOf.apply(item), card);
        io.pyhoma.desktop.ui.Motion.hoverLift(card);
        card.setOnDragDetected(e -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(String.valueOf(idOf.apply(item)));
            db.setContent(cc);
            db.setDragView(card.snapshot(null, null));
            e.consume();
        });
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && onOpen != null) {
                onOpen.accept(item);
            }
        });
        return card;
    }
}
