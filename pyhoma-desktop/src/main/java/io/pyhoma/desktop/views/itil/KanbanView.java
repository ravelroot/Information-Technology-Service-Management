package io.pyhoma.desktop.views.itil;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.shell.View;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Avatar;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.KanbanBoard;
import io.pyhoma.desktop.ui.components.PageHeader;
import io.pyhoma.desktop.ui.components.PickerField;
import io.pyhoma.desktop.ui.components.Segmented;
import io.pyhoma.desktop.views.projects.ProjectDetailView;
import io.pyhoma.domain.itil.SlaState;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.itil.ItilItem;
import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.model.project.ProjectTask;
import io.pyhoma.domain.model.project.ProjectTaskStatus;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.ItilSearch;
import io.pyhoma.domain.service.ItilCard;
import io.pyhoma.domain.service.ItilService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class KanbanView extends View {

    private enum Board { TICKET, PROBLEM, CHANGE, PROJECT }

    private Board board = Board.TICKET;
    private final StackPane holder = new StackPane();
    private final CheckBox mine = new CheckBox();
    private final PickerField<Long> project = PickerField.compact(() -> Option.of(App.lookups().projects()), null);
    private HBox projectFilter;
    private final StackPane createSlot = new StackPane();
    private Long highlightId;
    private Segmented<Board> tabs;

    @Override
    public String title() {
        return I18n.t("nav.KANBAN");
    }

    @Override
    protected Node build() {
        List<Board> boards = new ArrayList<>();
        boards.add(Board.TICKET);
        if (App.can(Module.PROBLEM, Right.READ)) {
            boards.add(Board.PROBLEM);
        }
        if (App.can(Module.CHANGE, Right.READ)) {
            boards.add(Board.CHANGE);
        }
        if (App.can(Module.PROJECT, Right.READ)) {
            boards.add(Board.PROJECT);
        }
        PageHeader header = new PageHeader(I18n.t("nav.KANBAN"), I18n.t("kanban.subtitle"))
                .actions(Ui.iconButton("refresh-cw", I18n.t("action.refresh"), this::onShow), createSlot);
        mine.setText(I18n.t("kanban.onlyMine"));
        mine.setOnAction(e -> onShow());
        project.onChange(v -> onShow());
        projectFilter = ItilListView.labeled("kanban.project", project) instanceof HBox h ? h : new HBox();
        tabs = new Segmented<>(boards, b -> I18n.t("kanban.board." + b.name()), board, b -> {
            board = b;
            onShow();
        });
        HBox toolbar = Ui.row(12, tabs, Ui.spacer(), projectFilter, mine);
        toolbar.getStyleClass().add("toolbar");
        VBox.setVgrow(holder, Priority.ALWAYS);
        VBox root = new VBox(16, header, toolbar, holder);
        root.getStyleClass().add("page");
        return root;
    }

    @Override
    public void onShow() {
        projectFilter.setVisible(board == Board.PROJECT);
        projectFilter.setManaged(board == Board.PROJECT);
        createSlot.getChildren().clear();
        if (board != Board.PROJECT && CategoryDialog.allowed()) {
            createSlot.getChildren().add(CategoryDialog.button(this::onShow));
        }
        if (board != Board.PROJECT && ItilQuickForm.canCreate(ItilType.valueOf(board.name()))) {
            ItilType t = ItilType.valueOf(board.name());
            createSlot.getChildren().add(Ui.primary(I18n.t("itil." + t.name().toLowerCase(Locale.ROOT) + ".new"), "plus",
                    () -> quickCreate(t, ItilStatus.NEW)));
        }
        if (board == Board.PROJECT) {
            showProjectBoard();
        } else {
            showItilBoard(ItilType.valueOf(board.name()));
        }
    }

    private void showItilBoard(ItilType type) {
        List<KanbanBoard.Column<ItilStatus>> cols = new ArrayList<>();
        for (ItilStatus s : ItilStatus.workflow(type)) {
            cols.add(new KanbanBoard.Column<>(s, I18n.e(s), Badge.tone(s)));
        }
        Map<Long, ItilCard> info = new HashMap<>();
        KanbanBoard<ItilItem, ItilStatus> kb = new KanbanBoard<>(cols, i -> itilCard(type, i, info.getOrDefault(i.getId(), ItilCard.EMPTY)),
                ItilItem::getId);
        kb.onOpen(i -> App.nav().open(new ItilDetailView(type, i.getId())));
        if (ItilQuickForm.canCreate(type)) {
            kb.onAdd(status -> quickCreate(type, status), status -> status.isOpen()
                    && (status == ItilStatus.NEW || App.can(ItilQuickForm.module(type), Right.UPDATE)),
                    I18n.t("kanban.add." + type.name()));
        }
        kb.stats(KanbanView::laneStats);
        kb.cardStyles(i -> overdue(i) ? List.of("card-overdue") : List.of());
        kb.sorts(List.of(
                new KanbanBoard.SortOption<>(I18n.t("kanban.sort.priority"),
                        Comparator.comparing(ItilItem::getPriority, Comparator.nullsLast(Comparator.reverseOrder()))),
                new KanbanBoard.SortOption<>(I18n.t("kanban.sort.created"),
                        Comparator.comparing(ItilItem::getOpenedAt, Comparator.nullsLast(Comparator.reverseOrder()))),
                new KanbanBoard.SortOption<>(I18n.t("kanban.sort.due"),
                        Comparator.comparing(ItilItem::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()))),
                new KanbanBoard.SortOption<>(I18n.t("kanban.sort.title"),
                        Comparator.comparing(ItilItem::getTitle, String.CASE_INSENSITIVE_ORDER))));
        kb.onMove((i, status) -> move(type, i, status));
        holder.getChildren().setAll(kb);
        ItilSearch s = ItilSearch.create();
        if (mine.isSelected()) {
            s.assigneeId(App.session().userId());
        }
        Async.run(() -> {
            @SuppressWarnings("unchecked")
            ItilService<ItilItem> service = (ItilService<ItilItem>) App.services().itil(type);
            Map<ItilStatus, List<ItilItem>> m = service.board(s);
            List<ItilItem> all = new ArrayList<>();
            m.values().forEach(all::addAll);
            return new Object[] {m, service.cards(all)};
        }, r -> {
            @SuppressWarnings("unchecked")
            Map<ItilStatus, List<ItilItem>> m = (Map<ItilStatus, List<ItilItem>>) r[0];
            @SuppressWarnings("unchecked")
            Map<Long, ItilCard> cards = (Map<Long, ItilCard>) r[1];
            info.putAll(cards);
            kb.setItems(m);
            if (highlightId != null) {
                kb.highlight(highlightId);
                highlightId = null;
            }
        });
    }

    private void move(ItilType type, ItilItem i, ItilStatus status) {
        if (status == i.getStatus()) {
            return;
        }
        if (status == ItilStatus.SOLVED && type != ItilType.CHANGE) {
            App.nav().open(new ItilDetailView(type, i.getId()));
            Toasts.info(I18n.t("kanban.solveHint"));
            return;
        }
        Async.run(() -> App.services().itil(type).changeStatus(i.getId(), status), r -> {
            Toasts.success(I18n.t("itil.statusChanged", I18n.e(status)));
            onShow();
        }, e -> {
            Dialogs.error(e);
            onShow();
        });
    }

    private static boolean overdue(ItilItem i) {
        return i.getDueAt() != null && i.getStatus().isOpen() && i.getDueAt().isBefore(LocalDateTime.now());
    }

    private static List<KanbanBoard.LaneStat> laneStats(List<ItilItem> list) {
        long minutes = list.stream().mapToLong(i -> i.getActionTimeMinutes() == null ? 0 : i.getActionTimeMinutes()).sum();
        long late = list.stream().filter(KanbanView::overdue).count();
        return List.of(
                new KanbanBoard.LaneStat("layers", String.valueOf(list.size()), I18n.t("kanban.stat.count"), null),
                new KanbanBoard.LaneStat("clock", compact(minutes), I18n.t("kanban.stat.time"), null),
                new KanbanBoard.LaneStat("bell", String.valueOf(late), I18n.t("kanban.stat.late"), late > 0 ? "lane-stat-alert" : null));
    }

    static String compact(long minutes) {
        if (minutes < 60) {
            return minutes + "min";
        }
        if (minutes < 60 * 24) {
            return (minutes / 60) + "h";
        }
        return (minutes / (60 * 24)) + "d";
    }

    static String clock(Integer minutes) {
        int m = minutes == null ? 0 : minutes;
        return String.format("%02d:%02d:00", m / 60, m % 60);
    }

    private void quickCreate(ItilType type, ItilStatus status) {
        ItilQuickForm.open(type, status, created -> {
            highlightId = created.getId();
            Board target = Board.valueOf(created.itilType().name());
            if (target == board) {
                onShow();
                return;
            }
            tabs.select(target);
            if (board != target) {
                board = target;
                onShow();
            }
        });
    }

    private Node itilCard(ItilType type, ItilItem i, ItilCard info) {
        HBox chips = new HBox(6);
        chips.setAlignment(Pos.CENTER_LEFT);
        chips.getStyleClass().add("card-chips");
        if (i.getCategoryId() != null) {
            String name = App.lookups().dd(DropdownKind.ITIL_CATEGORY, i.getCategoryId());
            Label cat = chip(leaf(name), "card-chip-category");
            Region box = new Region();
            box.getStyleClass().add("card-chip-box");
            String color = CategoryDialog.colorOf(i.getCategoryId());
            if (color != null) {
                box.setStyle("-fx-border-color: " + color + "; -fx-background-color: " + color + "33;");
            }
            cat.setGraphic(box);
            cat.setTooltip(new Tooltip(name));
            chips.getChildren().add(cat);
        }
        if (i instanceof Ticket t) {
            chips.getChildren().add(chip(I18n.e(t.getTicketType()), "card-chip-type", "tag"));
        } else if (i instanceof io.pyhoma.domain.model.itil.Change c) {
            chips.getChildren().add(chip(I18n.e(c.getChangeType()), "card-chip-type", "tag"));
        }
        Button more = new Button();
        more.setGraphic(Icons.of("ellipsis", 16));
        more.getStyleClass().add("card-more");
        more.setOnAction(e -> cardMenu(type, i).show(more, javafx.geometry.Side.BOTTOM, 0, 2));
        HBox top = new HBox(6, chips, Ui.spacer(), more);
        top.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(chips, Priority.ALWAYS);
        chips.setMinWidth(0);
        Text ref = new Text("#" + i.getId() + " ");
        ref.getStyleClass().add("card-ref-text");
        Text name = new Text(i.getTitle());
        name.getStyleClass().add("card-title-text");
        TextFlow title = new TextFlow(ref, name);
        title.getStyleClass().add("card-title-flow");
        title.setMaxHeight(64);
        HBox people = new HBox(-6);
        people.setAlignment(Pos.CENTER_LEFT);
        List<Long> who = info.assignees();
        for (int k = 0; k < Math.min(4, who.size()); k++) {
            Avatar a = new Avatar(App.lookups().user(who.get(k)), 26);
            a.getStyleClass().add("card-avatar");
            people.getChildren().add(a);
        }
        if (who.size() > 4) {
            Label extra = Ui.label("+" + (who.size() - 4), "card-avatar-more");
            people.getChildren().add(extra);
        }
        if (who.isEmpty()) {
            Button assign = new Button();
            assign.setGraphic(Icons.of("user-plus", 15));
            assign.getStyleClass().add("card-assign");
            assign.setTooltip(new Tooltip(I18n.t("people.edit")));
            assign.setOnAction(e -> PeopleDialog.open(type, i.getId(), "#" + i.getId(), this::onShow));
            people.getChildren().add(assign);
        }
        HBox right = new HBox(8);
        right.setAlignment(Pos.CENTER_RIGHT);
        if (i.getDueAt() != null && i.getStatus().isOpen()) {
            Label due = Ui.label(shortDate(i.getDueAt()), "card-due-date");
            if (overdue(i)) {
                due.getStyleClass().add("late");
            } else if (i instanceof Ticket t && App.services().tickets().ttrState(t) == SlaState.AT_RISK) {
                due.getStyleClass().add("risk");
            }
            due.setTooltip(new Tooltip(I18n.t("kanban.due", I18n.dateTime(i.getDueAt()))));
            right.getChildren().add(due);
        } else {
            Label cal = new Label();
            cal.setGraphic(Icons.of("calendar", 15));
            cal.getStyleClass().add("card-icon");
            cal.setTooltip(new Tooltip(I18n.t("kanban.noDue")));
            right.getChildren().add(cal);
        }
        Label flag = new Label();
        flag.setGraphic(Icons.of("flag", 15));
        flag.getStyleClass().addAll("card-flag", "card-flag-" + flagLevel(i.getPriority()));
        flag.setTooltip(new Tooltip(I18n.t("field.priority") + ": " + I18n.priority(i.getPriority())));
        right.getChildren().add(flag);
        HBox middle = new HBox(8, people, Ui.spacer(), right);
        middle.setAlignment(Pos.CENTER_LEFT);
        Label time = new Label(clock(i.getActionTimeMinutes()));
        time.setGraphic(Icons.of("play", 12));
        time.getStyleClass().add("card-time");
        time.setTooltip(new Tooltip(I18n.t("kanban.stat.time")));
        Label tasks = new Label(info.tasksDone() + "/" + info.tasksTotal());
        tasks.setGraphic(Icons.of("corner-down-right", 13));
        tasks.getStyleClass().add("card-tasks");
        tasks.setTooltip(new Tooltip(I18n.t("kanban.tasks")));
        HBox bottom = new HBox(16, time, tasks);
        bottom.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(8, top, title, middle, bottom);
        if (info.tasksTotal() > 0) {
            Region track = new Region();
            track.getStyleClass().add("card-progress-track");
            Region fill = new Region();
            fill.getStyleClass().add("card-progress-fill");
            StackPane bar = new StackPane(track, fill);
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            fill.maxWidthProperty().bind(bar.widthProperty().multiply((double) info.tasksDone() / info.tasksTotal()));
            bar.getStyleClass().add("card-progress");
            card.getChildren().add(bar);
        }
        return card;
    }

    private ContextMenu cardMenu(ItilType type, ItilItem i) {
        ContextMenu menu = new ContextMenu();
        MenuItem open = new MenuItem(I18n.t("kanban.card.open"), Icons.of("external-link", 14));
        open.setOnAction(e -> App.nav().open(new ItilDetailView(type, i.getId())));
        MenuItem people = new MenuItem(I18n.t("people.edit"), Icons.of("users", 14));
        people.setOnAction(e -> PeopleDialog.open(type, i.getId(), "#" + i.getId(), this::onShow));
        menu.getItems().addAll(open, people);
        if (App.can(ItilQuickForm.module(type), Right.UPDATE)) {
            Menu moveTo = new Menu(I18n.t("kanban.card.move"));
            moveTo.setGraphic(Icons.of("chevron-right", 14));
            for (ItilStatus st : ItilStatus.workflow(type)) {
                if (st == i.getStatus() || st.isTerminal()) {
                    continue;
                }
                MenuItem mi = new MenuItem(I18n.e(st));
                mi.setOnAction(e -> move(type, i, st));
                moveTo.getItems().add(mi);
            }
            menu.getItems().addAll(new SeparatorMenuItem(), moveTo);
        }
        return menu;
    }

    private static Label chip(String text, String style, String icon) {
        Label l = chip(text, style);
        l.setGraphic(Icons.of(icon, 12));
        return l;
    }

    private static Label chip(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().addAll("card-chip", style);
        l.setMinWidth(0);
        return l;
    }

    private static String leaf(String completeName) {
        if (completeName == null) {
            return "";
        }
        int k = completeName.lastIndexOf(" > ");
        return k < 0 ? completeName : completeName.substring(k + 3);
    }

    private static String flagLevel(Integer priority) {
        int p = priority == null ? 3 : priority;
        return p >= 5 ? "high" : p == 4 ? "medium" : "low";
    }

    private static String shortDate(LocalDateTime when) {
        String s = DateTimeFormatter.ofPattern("d MMM", I18n.locale()).format(when);
        return s.replace(".", "");
    }

    private void showProjectBoard() {
        List<KanbanBoard.Column<ProjectTaskStatus>> cols = new ArrayList<>();
        for (ProjectTaskStatus s : ProjectTaskStatus.values()) {
            cols.add(new KanbanBoard.Column<>(s, I18n.e(s), Badge.tone(s)));
        }
        KanbanBoard<ProjectTask, ProjectTaskStatus> kb = new KanbanBoard<>(cols, KanbanView::taskCard, ProjectTask::getId);
        kb.onOpen(t -> App.nav().open(new ProjectDetailView(t.getProjectId())));
        kb.onMove((t, status) -> {
            if (status == t.getStatus()) {
                return;
            }
            Async.run(() -> App.services().projects().moveTask(t.getId(), status), r -> onShow(), e -> {
                Dialogs.error(e);
                onShow();
            });
        });
        holder.getChildren().setAll(kb);
        Long p = project.getValue();
        Long assignee = mine.isSelected() ? App.session().userId() : null;
        Async.run(() -> App.services().projects().board(p, assignee), kb::setItems);
    }

    public static Node taskCard(ProjectTask t) {
        Label proj = Ui.label(io.pyhoma.desktop.ui.LookupCache.name(App.lookups().projects(), t.getProjectId()), "card-ref");
        Label title = Ui.label(t.getName(), "card-title");
        title.setWrapText(true);
        javafx.scene.control.ProgressBar bar = new javafx.scene.control.ProgressBar(t.getProgress() / 100.0);
        bar.getStyleClass().add("thin-progress");
        bar.setMaxWidth(Double.MAX_VALUE);
        Label dates = Ui.label(I18n.date(t.getPlanEnd()), "card-meta");
        if (t.getPlanEnd() != null && t.getPlanEnd().isBefore(java.time.LocalDate.now()) && !t.getStatus().isDone()) {
            dates.getStyleClass().add("late");
        }
        HBox bottom = new HBox(8, dates, Ui.spacer(), Ui.label(t.getProgress() + "%", "card-meta"));
        bottom.setAlignment(Pos.CENTER_LEFT);
        if (t.getAssigneeId() != null) {
            bottom.getChildren().add(new Avatar(App.lookups().user(t.getAssigneeId()), 22));
        }
        return new VBox(8, proj, title, bar, bottom);
    }
}
