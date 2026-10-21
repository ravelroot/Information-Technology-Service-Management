package io.pyhoma.desktop.views.projects;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.shell.View;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Icons;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.desktop.ui.components.GanttChart;
import io.pyhoma.desktop.ui.components.KanbanBoard;
import io.pyhoma.desktop.ui.components.PageHeader;
import io.pyhoma.desktop.ui.components.Segmented;
import io.pyhoma.desktop.views.common.DocumentsPanel;
import io.pyhoma.desktop.views.common.HistoryPanel;
import io.pyhoma.desktop.views.itil.KanbanView;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.project.Project;
import io.pyhoma.domain.model.project.ProjectMember;
import io.pyhoma.domain.model.project.ProjectTask;
import io.pyhoma.domain.model.project.ProjectTaskDependency;
import io.pyhoma.domain.model.project.ProjectTaskStatus;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.ProjectService.GanttData;
import io.pyhoma.domain.support.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ProjectDetailView extends View {

    private Long id;
    private Project project;
    private GanttData data;
    private final PageHeader header = new PageHeader("", null);
    private final TabPane tabs = new TabPane();
    private final GanttChart gantt = new GanttChart();
    private final DataTable<ProjectTask> tasks = new DataTable<>();
    private Form form;

    public ProjectDetailView(Long id) {
        this.id = id;
    }

    private boolean canEdit() {
        return App.can(Module.PROJECT, id == null ? Right.CREATE : Right.UPDATE);
    }

    @Override
    public String title() {
        return project == null || project.getName() == null ? I18n.t("projects.new") : project.getName();
    }

    @Override
    protected Node build() {
        header.back(() -> App.nav().back());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("detail-tabs");
        gantt.onOpen(this::editTaskById);
        gantt.onMove((taskId, start, end) -> Async.run(() -> App.services().projects().reschedule(taskId, start, end), t -> {
            Toasts.success(I18n.t("gantt.rescheduled", t.getName()));
            load();
        }, e -> {
            Dialogs.error(e);
            load();
        }));
        tasks.col(I18n.t("field.name"), 260, t -> (t.getParentId() != null ? "    " : "") + t.getName())
                .node(I18n.t("field.status"), 120, t -> Badge.status(t.getStatus()), t -> I18n.e(t.getStatus()))
                .col(I18n.t("field.assigneeId"), 160, t -> App.lookups().user(t.getAssigneeId()))
                .col(I18n.t("field.planStart"), 110, t -> I18n.date(t.getPlanStart()))
                .col(I18n.t("field.planEnd"), 110, t -> I18n.date(t.getPlanEnd()))
                .col(I18n.t("field.progress"), 80, "num", t -> t.getProgress() + "%")
                .col(I18n.t("field.effortHours"), 90, "num", t -> t.getEffortHours() == null ? "" : t.getEffortHours() + " h");
        tasks.onOpen(this::editTask);
        tasks.placeholder(new EmptyState("list-checks", I18n.t("projects.noTasks"), I18n.t("projects.noTasksHint"), null));
        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(tabs);
        BorderPane.setMargin(tabs, new javafx.geometry.Insets(16, 0, 0, 0));
        root.getStyleClass().add("page");
        return root;
    }

    @Override
    public void onShow() {
        load();
    }

    private void load() {
        if (id == null) {
            project = new Project();
            project.setPriority(3);
            project.setProgress(0);
            project.setColor("#000000");
            data = null;
            render();
            return;
        }
        Async.run(() -> App.services().projects().gantt(id), d -> {
            data = d;
            project = d.project();
            render();
        });
    }

    private void render() {
        boolean creating = project.isNew();
        header.setTitle(creating ? I18n.t("projects.new") : project.getName());
        header.setSubtitle(creating ? null : (project.getCode() == null ? "" : project.getCode() + "  •  ")
                + I18n.date(project.getPlanStart()) + " – " + I18n.date(project.getPlanEnd()));
        if (!creating) {
            header.tags(Badge.colored(App.lookups().dd(DropdownKind.PROJECT_STATE, project.getStateId()),
                    App.lookups().colors(DropdownKind.PROJECT_STATE).get(project.getStateId())),
                    Badge.of(project.getProgress() + "%", project.getProgress() >= 100 ? "green" : "blue"));
        }
        List<Node> actions = new ArrayList<>();
        if (canEdit()) {
            actions.add(Ui.primary(I18n.t(creating ? "action.create" : "action.save"), "save", this::save));
        }
        if (!creating && App.can(Module.PROJECT, Right.DELETE)) {
            actions.add(0, Ui.button(I18n.t("action.trash"), "trash-2", () -> {
                if (Dialogs.confirm(I18n.t("action.trash"), I18n.t("confirm.trash"), I18n.t("action.trash"), true)) {
                    Async.exec(() -> App.services().projects().trash(id, true), () -> App.nav().back());
                }
            }, "ghost"));
        }
        header.actions(actions.toArray(Node[]::new));
        int sel = Math.max(0, tabs.getSelectionModel().getSelectedIndex());
        tabs.getTabs().clear();
        tabs.getTabs().add(tab("projects.tab.overview", "info", overview()));
        if (!creating) {
            tabs.getTabs().add(tab("projects.tab.gantt", "chart-gantt", ganttTab()));
            tabs.getTabs().add(tab("projects.tab.tasks", "list-checks", tasksTab()));
            tabs.getTabs().add(tab("projects.tab.kanban", "square-kanban", kanbanTab()));
            tabs.getTabs().add(tab("projects.tab.team", "users", teamTab()));
            DocumentsPanel docs = new DocumentsPanel("Project", id);
            docs.load();
            tabs.getTabs().add(tab("tab.documents", "paperclip", docs));
            HistoryPanel h = new HistoryPanel("Project", id);
            h.load();
            tabs.getTabs().add(tab("tab.history", "history", h));
            gantt.setData(GanttSupport.bars(data), GanttSupport.links(data.dependencies()));
            tasks.setRows(GanttSupport.bars(data).stream().map(b -> taskById(b.id())).filter(Objects::nonNull).toList());
        }
        tabs.getSelectionModel().select(Math.min(sel, tabs.getTabs().size() - 1));
    }

    private static Tab tab(String key, String icon, Node content) {
        Tab t = new Tab(I18n.t(key), content);
        t.setGraphic(Icons.of(icon, 14));
        return t;
    }

    private ProjectTask taskById(Long taskId) {
        return data == null ? null : data.tasks().stream().filter(t -> t.getId().equals(taskId)).findFirst().orElse(null);
    }

    private Node overview() {
        form = new Form(3).readOnly(!canEdit())
                .text("name", project::getName, project::setName, 2, true)
                .code("code", project::getCode, project::setCode)
                .choice("stateId", () -> colored(DropdownKind.PROJECT_STATE), project::getStateId, project::setStateId, true, false)
                .choice("priority", () -> Option.levels("priority.", 6), project::getPriority, project::setPriority, false, true)
                .choice("managerId", ProjectDetailView::users, project::getManagerId, project::setManagerId, true, false)
                .choice("groupId", () -> Option.of(App.lookups().groups()), project::getGroupId, project::setGroupId, true, false)
                .date("planStart", project::getPlanStart, project::setPlanStart)
                .date("planEnd", project::getPlanEnd, project::setPlanEnd)
                .date("realStart", project::getRealStart, project::setRealStart)
                .date("realEnd", project::getRealEnd, project::setRealEnd)
                .text("color", project::getColor, project::setColor)
                .area("description", project::getDescription, project::setDescription, 6, false);
        form.load();
        VBox content = new VBox(18, form.node());
        if (data != null) {
            long late = data.tasks().stream().filter(t -> !t.getStatus().isDone() && t.getPlanEnd() != null
                    && t.getPlanEnd().isBefore(java.time.LocalDate.now())).count();
            Map<ProjectTaskStatus, Long> byStatus = data.tasks().stream()
                    .collect(Collectors.groupingBy(ProjectTask::getStatus, Collectors.counting()));
            HBox strip = new HBox(28,
                    Ui.fact(I18n.t("projects.stat.tasks"), String.valueOf(data.tasks().size())),
                    Ui.fact(I18n.t("projects.stat.done"), String.valueOf(byStatus.getOrDefault(ProjectTaskStatus.DONE, 0L))),
                    Ui.fact(I18n.t("projects.stat.inProgress"), String.valueOf(byStatus.getOrDefault(ProjectTaskStatus.IN_PROGRESS, 0L))),
                    Ui.fact(I18n.t("projects.stat.late"), String.valueOf(late)),
                    Ui.fact(I18n.t("projects.stat.critical"), String.valueOf(data.criticalTaskIds().size())));
            strip.getStyleClass().add("stat-strip");
            content.getChildren().add(0, strip);
        }
        ScrollPane sp = new ScrollPane(Ui.style(content, "tab-body"));
        sp.setFitToWidth(true);
        return sp;
    }

    static List<Option<Long>> colored(DropdownKind kind) {
        List<Option<Long>> list = new ArrayList<>();
        Map<Long, String> colors = App.lookups().colors(kind);
        App.lookups().dropdown(kind).forEach((k, v) -> list.add(new Option<>(k, v, null, colors.get(k))));
        return list;
    }

    static List<Option<Long>> users() {
        List<Option<Long>> list = new ArrayList<>();
        for (User u : App.services().users().activeUsers()) {
            list.add(new Option<>(u.getId(), u.getFullName()));
        }
        return list;
    }

    private void save() {
        if (!form.commit()) {
            return;
        }
        Async.run(() -> App.services().projects().save(project), saved -> {
            id = saved.getId();
            Toasts.success(I18n.t("toast.saved"));
            App.lookups().refreshAsync(null);
            load();
        }, e -> {
            if (e instanceof ValidationException v && form.showErrors(v)) {
                return;
            }
            Dialogs.error(e);
        });
    }

    private Node ganttTab() {
        Segmented<GanttChart.Scale> zoom = new Segmented<>(List.of(GanttChart.Scale.values()),
                s -> I18n.t("gantt.scale." + s.name()), GanttChart.Scale.DAY, gantt::setScale);
        CheckBox critical = new CheckBox(I18n.t("gantt.critical"));
        critical.setSelected(true);
        critical.setOnAction(e -> gantt.setShowCritical(critical.isSelected()));
        HBox toolbar = Ui.row(10, zoom, critical, Ui.spacer(), Ui.button(I18n.t("gantt.today"), "crosshair", gantt::scrollToToday, "ghost"));
        if (canEdit()) {
            toolbar.getChildren().addAll(
                    Ui.button(I18n.t("gantt.reschedule"), "refresh-cw", () -> Async.run(() -> App.services().projects().reschedule(id),
                            n -> {
                                Toasts.success(I18n.t("gantt.rescheduledCount", n));
                                load();
                            })),
                    Ui.button(I18n.t("gantt.addDependency"), "git-branch", this::addDependency),
                    Ui.primary(I18n.t("projects.addTask"), "plus", () -> editTask(newTask())));
        }
        VBox panel = new VBox(gantt);
        panel.getStyleClass().add("table-panel");
        VBox.setVgrow(gantt, Priority.ALWAYS);
        VBox.setVgrow(panel, Priority.ALWAYS);
        javafx.application.Platform.runLater(gantt::scrollToToday);
        return Ui.style(new VBox(10, toolbar, panel), "tab-body");
    }

    private Node tasksTab() {
        HBox actions = Ui.row(8, Ui.spacer());
        if (canEdit()) {
            actions.getChildren().addAll(
                    Ui.button(I18n.t("action.delete"), "trash-2", () -> {
                        ProjectTask t = tasks.selected();
                        if (t != null && Dialogs.confirm(I18n.t("action.delete"), I18n.t("confirm.delete"), I18n.t("action.delete"), true)) {
                            Async.exec(() -> App.services().projects().deleteTask(t.getId()), this::load);
                        }
                    }, "ghost"),
                    Ui.primary(I18n.t("projects.addTask"), "plus", () -> editTask(newTask())));
        }
        VBox.setVgrow(tasks, Priority.ALWAYS);
        return Ui.style(new VBox(10, actions, tasks), "tab-body");
    }

    private Node kanbanTab() {
        List<KanbanBoard.Column<ProjectTaskStatus>> cols = new ArrayList<>();
        for (ProjectTaskStatus s : ProjectTaskStatus.values()) {
            cols.add(new KanbanBoard.Column<>(s, I18n.e(s), Badge.tone(s)));
        }
        KanbanBoard<ProjectTask, ProjectTaskStatus> kb = new KanbanBoard<>(cols, ProjectDetailView::card, ProjectTask::getId);
        kb.onOpen(this::editTask);
        kb.onMove((t, s) -> Async.run(() -> App.services().projects().moveTask(t.getId(), s), r -> load(), e -> {
            Dialogs.error(e);
            load();
        }));
        Map<ProjectTaskStatus, List<ProjectTask>> map = new java.util.EnumMap<>(ProjectTaskStatus.class);
        for (ProjectTask t : data.tasks()) {
            map.computeIfAbsent(t.getStatus(), k -> new ArrayList<>()).add(t);
        }
        kb.setItems(map);
        VBox box = new VBox(kb);
        VBox.setVgrow(kb, Priority.ALWAYS);
        return Ui.style(box, "tab-body");
    }

    private static Node card(ProjectTask t) {
        return KanbanView.taskCard(t);
    }

    private Node teamTab() {
        DataTable<ProjectMember> members = new DataTable<>();
        members.col(I18n.t("field.userId"), 240, m -> App.lookups().user(m.getUserId()))
                .col(I18n.t("field.roleName"), 220, ProjectMember::getRoleName);
        members.placeholder(new EmptyState("users", I18n.t("projects.noMembers"), null, null));
        Runnable reload = () -> Async.run(() -> App.services().projects().members(id), members::setRows);
        reload.run();
        HBox actions = Ui.row(8, Ui.spacer());
        if (canEdit()) {
            actions.getChildren().addAll(
                    Ui.button(I18n.t("projects.removeMember"), "x", () -> {
                        ProjectMember m = members.selected();
                        if (m != null) {
                            Async.exec(() -> App.services().projects().removeMember(m.getId()), reload);
                        }
                    }, "ghost"),
                    Ui.primary(I18n.t("projects.addMember"), "user-plus", () -> {
                        Long[] user = new Long[1];
                        String[] role = new String[1];
                        Form f = new Form(1)
                                .choice("userId", ProjectDetailView::users, () -> user[0], v -> user[0] = v, false, true)
                                .text("roleName", () -> role[0], v -> role[0] = v);
                        f.load();
                        ((javafx.scene.layout.Region) f.node()).setPrefWidth(400);
                        Dialogs.form(I18n.t("projects.addMember"), f.node(), I18n.t("action.add"), () -> {
                            f.commit();
                            if (user[0] == null) {
                                return false;
                            }
                            App.services().projects().addMember(id, user[0], role[0]);
                            reload.run();
                            return true;
                        });
                    }));
        }
        VBox.setVgrow(members, Priority.ALWAYS);
        return Ui.style(new VBox(10, actions, members), "tab-body");
    }

    private ProjectTask newTask() {
        ProjectTask t = new ProjectTask();
        t.setProjectId(id);
        t.setStatus(ProjectTaskStatus.TODO);
        t.setProgress(0);
        t.setPlanStart(java.time.LocalDate.now());
        t.setPlanEnd(java.time.LocalDate.now().plusDays(4));
        return t;
    }

    private void editTaskById(Long taskId) {
        ProjectTask t = taskById(taskId);
        if (t != null) {
            editTask(t);
        }
    }

    private void editTask(ProjectTask task) {
        boolean editable = canEdit();
        Form f = new Form(2).readOnly(!editable)
                .text("name", task::getName, task::setName, 2, true)
                .choice("status", () -> Option.ofEnum(ProjectTaskStatus.values()), task::getStatus, task::setStatus, false, true)
                .choice("assigneeId", ProjectDetailView::users, task::getAssigneeId, task::setAssigneeId, true, false)
                .date("planStart", task::getPlanStart, task::setPlanStart)
                .date("planEnd", task::getPlanEnd, task::setPlanEnd)
                .integer("progress", task::getProgress, task::setProgress)
                .integer("effortHours", task::getEffortHours, task::setEffortHours)
                .choice("parentId", () -> {
                    List<Option<Long>> list = new ArrayList<>();
                    data.tasks().stream().filter(x -> !Objects.equals(x.getId(), task.getId()))
                            .forEach(x -> list.add(new Option<>(x.getId(), x.getName())));
                    return list;
                }, task::getParentId, task::setParentId, true, false)
                .check("milestone", task::isMilestone, task::setMilestone)
                .area("description", task::getDescription, task::setDescription, 4, false);
        f.load();
        ((javafx.scene.layout.Region) f.node()).setPrefWidth(620);
        if (!editable) {
            Dialogs.form(task.getName(), f.node(), I18n.t("action.close"), () -> true);
            return;
        }
        Dialogs.form(I18n.t(task.isNew() ? "projects.addTask" : "projects.editTask"), f.node(), I18n.t("action.save"), () -> {
            if (!f.commit()) {
                return false;
            }
            try {
                App.services().projects().saveTask(task);
                Toasts.success(I18n.t("toast.saved"));
                load();
                return true;
            } catch (ValidationException e) {
                f.showErrors(e);
                return false;
            }
        });
    }

    private void addDependency() {
        Long[] values = new Long[2];
        Integer[] lag = {0};
        java.util.function.Supplier<List<Option<Long>>> taskOptions = () -> {
            List<Option<Long>> list = new ArrayList<>();
            data.tasks().forEach(t -> list.add(new Option<>(t.getId(), t.getName())));
            return list;
        };
        Form f = new Form(1)
                .choice("predecessorId", taskOptions, () -> values[0], v -> values[0] = v, false, true)
                .choice("taskId", taskOptions, () -> values[1], v -> values[1] = v, false, true)
                .integer("lagDays", () -> lag[0], v -> lag[0] = v);
        f.load();
        ((javafx.scene.layout.Region) f.node()).setPrefWidth(440);
        Node hint = Ui.muted(I18n.t("gantt.dependencyHint"));
        Dialogs.form(I18n.t("gantt.addDependency"), new VBox(10, hint, f.node()), I18n.t("action.add"), () -> {
            if (!f.commit() || values[0] == null || values[1] == null) {
                return false;
            }
            App.services().projects().addDependency(values[1], values[0], lag[0] == null ? 0 : lag[0]);
            Toasts.success(I18n.t("gantt.dependencyAdded"));
            load();
            return true;
        });
    }

    List<ProjectTaskDependency> dependencies() {
        return data == null ? List.of() : data.dependencies();
    }
}
