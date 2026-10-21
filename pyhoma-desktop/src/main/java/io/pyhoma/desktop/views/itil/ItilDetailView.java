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
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.PeopleField;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.desktop.ui.components.PageHeader;
import io.pyhoma.desktop.ui.components.PickerField;
import io.pyhoma.desktop.views.common.DocumentsPanel;
import io.pyhoma.desktop.views.common.HistoryPanel;
import io.pyhoma.desktop.views.common.Markdown;
import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.itil.ApprovalStatus;
import io.pyhoma.domain.model.itil.Change;
import io.pyhoma.domain.model.itil.ChangeType;
import io.pyhoma.domain.model.itil.ItilApproval;
import io.pyhoma.domain.model.itil.ItilItem;
import io.pyhoma.domain.model.itil.ItilObserver;
import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.ItilTask;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.Problem;
import io.pyhoma.domain.model.itil.RelationType;
import io.pyhoma.domain.model.itil.RiskLevel;
import io.pyhoma.domain.model.itil.TaskStatus;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.model.itil.TicketType;
import io.pyhoma.domain.model.kb.KbArticle;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.ItilService;
import io.pyhoma.domain.service.RelatedItem;
import io.pyhoma.domain.service.TimelineEntry;
import io.pyhoma.domain.support.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ItilDetailView extends View {

    private final ItilType type;
    private Long id;
    private ItilItem item;
    private final BorderPane root = new BorderPane();
    private final PageHeader header = new PageHeader("", null);
    private final TabPane tabs = new TabPane();
    private final VBox side = new VBox(14);
    private Form sideForm;
    private Form detailForm;

    public ItilDetailView(ItilType type, Long id) {
        this.type = type;
        this.id = id;
    }

    public static ItilDetailView create(ItilType type) {
        return new ItilDetailView(type, null);
    }

    @SuppressWarnings("unchecked")
    private ItilService<ItilItem> service() {
        return (ItilService<ItilItem>) App.services().itil(type);
    }

    private String key() {
        return type.name().toLowerCase(Locale.ROOT);
    }

    private boolean canEdit() {
        return App.can(service().module(), id == null ? Right.CREATE : Right.UPDATE);
    }

    private boolean isRequester() {
        return item != null && Objects.equals(item.getRequesterId(), App.session().userId());
    }

    @Override
    public String title() {
        return id == null ? I18n.t("itil." + key() + ".new") : I18n.t("type." + type.entityClass().getSimpleName()) + " #" + id;
    }

    @Override
    protected Node build() {
        header.back(() -> App.nav().back());
        tabs.getStyleClass().add("detail-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        ScrollPane sideScroll = new ScrollPane(side);
        sideScroll.setFitToWidth(true);
        sideScroll.getStyleClass().add("side-scroll");
        sideScroll.setPrefWidth(340);
        sideScroll.setMinWidth(300);
        side.getStyleClass().add("side-panel");
        HBox body = new HBox(18, tabs, sideScroll);
        HBox.setHgrow(tabs, Priority.ALWAYS);
        root.setTop(header);
        root.setCenter(body);
        BorderPane.setMargin(body, new javafx.geometry.Insets(16, 0, 0, 0));
        root.getStyleClass().add("page");
        return root;
    }

    @Override
    public void onShow() {
        load();
    }

    private void load() {
        if (id == null) {
            Async.run(() -> service().draft(), d -> {
                item = d;
                render();
            });
        } else {
            Async.run(() -> service().get(id), i -> {
                item = i;
                render();
            });
        }
    }

    private void render() {
        boolean creating = item.isNew();
        header.setTitle(creating ? I18n.t("itil." + key() + ".new") : "#" + item.getId() + "  " + item.getTitle());
        header.setSubtitle(creating ? I18n.t("itil.newHint") : I18n.t("itil.openedBy", App.lookups().user(item.getRequesterId()),
                I18n.dateTime(item.getOpenedAt())));
        if (creating) {
            header.tags();
        } else {
            List<Node> tags = new ArrayList<>(List.of(Badge.status(item.getStatus()), Badge.priority(item.getPriority())));
            if (item instanceof Ticket t) {
                tags.add(Badge.of(I18n.e(t.getTicketType()), "slate"));
            }
            if (item.isDeleted()) {
                tags.add(Badge.of(I18n.t("itil.inTrash"), "red"));
            }
            header.tags(tags.toArray(Node[]::new));
        }
        header.actions(actions().toArray(Node[]::new));
        renderSide();
        renderTabs();
    }

    private List<Node> actions() {
        List<Node> nodes = new ArrayList<>();
        if (item.isNew()) {
            nodes.add(Ui.primary(I18n.t("action.create"), "check", this::save));
            return nodes;
        }
        boolean tech = App.can(service().module(), Right.UPDATE);
        if (tech && item.getStatus().isOpen()) {
            if (!Objects.equals(item.getAssigneeId(), App.session().userId())) {
                nodes.add(Ui.button(I18n.t("itil.take"), "user-plus", () -> run(() -> service().takeIntoAccount(id),
                        I18n.t("itil.taken"))));
            }
            if (type != ItilType.CHANGE) {
                nodes.add(Ui.button(I18n.t("itil.solve"), "circle-check", this::proposeSolution));
            }
        }
        if (tech) {
            MenuButton status = new MenuButton(I18n.t("itil.changeStatus"));
            status.setGraphic(Icons.of("circle-dot", 15));
            for (ItilStatus s : ItilStatus.workflow(type)) {
                MenuItem mi = new MenuItem(I18n.e(s));
                mi.setDisable(s == item.getStatus());
                mi.setOnAction(e -> changeStatus(s));
                status.getItems().add(mi);
            }
            nodes.add(status);
        }
        if (canEdit()) {
            nodes.add(Ui.primary(I18n.t("action.save"), "save", this::save));
        }
        MenuButton more = new MenuButton();
        more.setGraphic(Icons.of("ellipsis", 16));
        more.getStyleClass().add("icon-menu");
        if (App.can(service().module(), Right.DELETE)) {
            MenuItem trash = new MenuItem(I18n.t(item.isDeleted() ? "action.restore" : "action.trash"));
            trash.setOnAction(e -> {
                if (item.isDeleted()) {
                    run(() -> service().restore(id), I18n.t("toast.restored"));
                } else if (Dialogs.confirm(I18n.t("action.trash"), I18n.t("confirm.trash"), I18n.t("action.trash"), true)) {
                    Async.exec(() -> service().trash(id), () -> {
                        Toasts.success(I18n.t("toast.trashed"));
                        App.nav().back();
                    });
                }
            });
            more.getItems().add(trash);
        }
        if (App.can(service().module(), Right.PURGE)) {
            MenuItem purge = new MenuItem(I18n.t("action.purge"));
            purge.setOnAction(e -> {
                if (Dialogs.confirm(I18n.t("action.purge"), I18n.t("confirm.purge"), I18n.t("action.purge"), true)) {
                    Async.exec(() -> service().purgeItem(id), () -> {
                        Toasts.success(I18n.t("toast.purged"));
                        App.nav().back();
                    });
                }
            });
            more.getItems().addAll(new SeparatorMenuItem(), purge);
        }
        MenuItem refresh = new MenuItem(I18n.t("action.refresh"));
        refresh.setOnAction(e -> load());
        more.getItems().add(0, refresh);
        nodes.add(more);
        return nodes;
    }

    private void renderSide() {
        side.getChildren().clear();
        boolean readOnly = !canEdit();
        boolean self = App.session().isSelfService();
        sideForm = new Form(1).readOnly(readOnly);
        if (item instanceof Ticket t) {
            sideForm.choice("ticketType", () -> Option.ofEnum(TicketType.values()), t::getTicketType, t::setTicketType, false, true);
        }
        if (item instanceof Change c) {
            sideForm.choice("changeType", () -> Option.ofEnum(ChangeType.values()), c::getChangeType, c::setChangeType, false, true)
                    .choice("risk", () -> Option.ofEnum(RiskLevel.values()), c::getRisk, c::setRisk, false, true);
        }
        sideForm.choice("categoryId", () -> Option.of(App.lookups().dropdown(DropdownKind.ITIL_CATEGORY)),
                        item::getCategoryId, item::setCategoryId, true, false)
                .choice("urgency", () -> Option.levels("level.", 5), item::getUrgency, item::setUrgency, false, true)
                .choice("impact", () -> Option.levels("level.", 5), item::getImpact, item::setImpact, false, true);
        if (!self) {
            sideForm.choice("priority", () -> Option.levels("priority.", 6), item::getPriority, item::setPriority, false, true);
        }
        if (!self || !item.isNew()) {
            sideForm.choice("requesterId", ItilDetailView::userOptions, item::getRequesterId, item::setRequesterId, true, false);
        }
        if (!self) {
            sideForm.choice("assigneeGroupId", () -> Option.of(App.lookups().groups()), item::getAssigneeGroupId,
                            item::setAssigneeGroupId, true, false)
                    .choice("assigneeId", ItilDetailView::technicianOptions, item::getAssigneeId, item::setAssigneeId, true, false);
        }
        sideForm.choice("locationId", () -> Option.of(App.lookups().dropdown(DropdownKind.LOCATION)), item::getLocationId,
                item::setLocationId, true, false);
        if (item instanceof Ticket t && !self) {
            sideForm.choice("sourceId", () -> Option.of(App.lookups().dropdown(DropdownKind.REQUEST_SOURCE)), t::getSourceId,
                    t::setSourceId, true, false);
        }
        if (item instanceof Change c) {
            sideForm.dateTime("plannedStartAt", c::getPlannedStartAt, c::setPlannedStartAt)
                    .dateTime("plannedEndAt", c::getPlannedEndAt, c::setPlannedEndAt);
        }
        if (!item.isNew() && !self) {
            sideForm.dateTime("dueAt", item::getDueAt, item::setDueAt);
        }
        sideForm.load();
        side.getChildren().add(Ui.panel(I18n.t("itil.properties"), sideForm.node()));
        if (!item.isNew()) {
            side.getChildren().add(Ui.panel(I18n.t("itil.facts"), facts()));
            if (item instanceof Ticket t && t.getStatus() == ItilStatus.CLOSED && isRequester()) {
                side.getChildren().add(Ui.panel(I18n.t("itil.satisfaction"), satisfaction(t)));
            }
        }
    }

    private Node facts() {
        VBox v = new VBox(10);
        v.getChildren().add(Ui.fact(I18n.t("field.orgUnitId"), App.lookups().orgUnit(item.getOrgUnitId())));
        if (item instanceof Ticket t) {
            Node tto = slaRow(App.services().tickets().ttoState(t), t.getTtoDueAt());
            Node ttr = slaRow(App.services().tickets().ttrState(t), t.getDueAt());
            v.getChildren().addAll(Ui.fact(I18n.t("field.ttoDueAt"), tto), Ui.fact(I18n.t("field.ttrDueAt"), ttr));
            if (t.getApprovalStatus() != ApprovalStatus.NONE) {
                v.getChildren().add(Ui.fact(I18n.t("field.approvalStatus"), Badge.of(I18n.e(t.getApprovalStatus()),
                        t.getApprovalStatus() == ApprovalStatus.ACCEPTED ? "green" : t.getApprovalStatus() == ApprovalStatus.REFUSED ? "red" : "amber")));
            }
        }
        if (item instanceof Change c && c.getApprovalStatus() != ApprovalStatus.NONE) {
            v.getChildren().add(Ui.fact(I18n.t("field.approvalStatus"), Badge.of(I18n.e(c.getApprovalStatus()),
                    c.getApprovalStatus() == ApprovalStatus.ACCEPTED ? "green" : c.getApprovalStatus() == ApprovalStatus.REFUSED ? "red" : "amber")));
        }
        v.getChildren().add(Ui.fact(I18n.t("field.actionTimeMinutes"), I18n.minutes(item.getActionTimeMinutes())));
        v.getChildren().add(Ui.fact(I18n.t("field.lastActivityAt"), I18n.relative(item.getLastActivityAt())));
        if (item.getSolvedAt() != null) {
            v.getChildren().add(Ui.fact(I18n.t("field.solvedAt"), I18n.dateTime(item.getSolvedAt())));
        }
        if (item.getClosedAt() != null) {
            v.getChildren().add(Ui.fact(I18n.t("field.closedAt"), I18n.dateTime(item.getClosedAt())));
        }
        return v;
    }

    private static Node slaRow(io.pyhoma.domain.itil.SlaState state, java.time.LocalDateTime due) {
        HBox h = new HBox(8, Badge.sla(state), Ui.label(I18n.dateTime(due), "fact-value"));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private Node satisfaction(Ticket t) {
        if (t.getSatisfaction() != null) {
            return Ui.label("★".repeat(t.getSatisfaction()) + "☆".repeat(5 - t.getSatisfaction()), "stars");
        }
        HBox stars = new HBox(4);
        for (int i = 1; i <= 5; i++) {
            int score = i;
            Button b = Ui.button("★", null, () -> Dialogs.prompt(I18n.t("itil.rate"), I18n.t("itil.rateComment"), "", true)
                    .or(() -> java.util.Optional.of(""))
                    .ifPresent(c -> run(() -> App.services().tickets().rate(id, score, c), I18n.t("itil.rated"))), "star-button");
            stars.getChildren().add(b);
        }
        return new VBox(6, Ui.muted(I18n.t("itil.rateHint")), stars);
    }

    private void renderTabs() {
        int selected = Math.max(0, tabs.getSelectionModel().getSelectedIndex());
        tabs.getTabs().clear();
        if (!item.isNew()) {
            tabs.getTabs().add(tab(I18n.t("itil.tab.followup"), "message-square", timelineTab()));
        }
        tabs.getTabs().add(tab(I18n.t("itil.tab.description"), "file-text", descriptionTab()));
        if (!item.isNew()) {
            tabs.getTabs().add(tab(I18n.t("itil.tab.people"), "users", peopleTab()));
        }
        if (!item.isNew() && !App.session().isSelfService()) {
            tabs.getTabs().add(tab(I18n.t("itil.tab.tasks"), "list-checks", tasksTab()));
            if (type != ItilType.PROBLEM) {
                tabs.getTabs().add(tab(I18n.t("itil.tab.approvals"), "shield-check", approvalsTab()));
            }
            tabs.getTabs().add(tab(I18n.t("itil.tab.links"), "link", linksTab()));
            DocumentsPanel docs = new DocumentsPanel(type.entityClass().getSimpleName(), id);
            docs.load();
            tabs.getTabs().add(tab(I18n.t("tab.documents"), "paperclip", docs));
            HistoryPanel history = new HistoryPanel(type.entityClass().getSimpleName(), id);
            history.load();
            tabs.getTabs().add(tab(I18n.t("tab.history"), "history", history));
        }
        tabs.getSelectionModel().select(Math.min(selected, tabs.getTabs().size() - 1));
    }

    static Tab tab(String text, String icon, Node content) {
        Tab t = new Tab(text, content);
        t.setGraphic(Icons.of(icon, 14));
        return t;
    }

    private Node descriptionTab() {
        detailForm = new Form(2).readOnly(!canEdit());
        detailForm.text("title", item::getTitle, item::setTitle, 2, true)
                .area("content", item::getContent, item::setContent, 8, false);
        if (item instanceof Problem p) {
            detailForm.section(I18n.t("itil.analysis"))
                    .area("symptomContent", p::getSymptomContent, p::setSymptomContent, 3, false)
                    .area("causeContent", p::getCauseContent, p::setCauseContent, 3, false)
                    .area("impactContent", p::getImpactContent, p::setImpactContent, 3, false);
        }
        if (item instanceof Change c) {
            detailForm.section(I18n.t("itil.planning"))
                    .area("impactContent", c::getImpactContent, c::setImpactContent, 3, false)
                    .area("rolloutPlan", c::getRolloutPlan, c::setRolloutPlan, 4, false)
                    .area("backoutPlan", c::getBackoutPlan, c::setBackoutPlan, 3, false)
                    .area("checklist", c::getChecklist, c::setChecklist, 3, false);
        }
        detailForm.load();
        ScrollPane sp = new ScrollPane(Ui.style(new VBox(detailForm.node()), "tab-body"));
        sp.setFitToWidth(true);
        if (item.isNew()) {
            javafx.application.Platform.runLater(detailForm::focusFirst);
        }
        return sp;
    }

    private void save() {
        if (!sideForm.commit() || !detailForm.commit()) {
            return;
        }
        Async.run(() -> service().save(item), saved -> {
            boolean created = id == null;
            id = saved.getId();
            if (created && saved instanceof Ticket t) {
                io.pyhoma.desktop.views.assets.AssetDetailView.linkPendingAsset(t);
            }
            item = saved;
            Toasts.success(created ? I18n.t("itil.created", saved.reference()) : I18n.t("toast.saved"));
            render();
        }, e -> {
            if (e instanceof ValidationException v && (sideForm.showErrors(v) | detailForm.showErrors(v))) {
                return;
            }
            Dialogs.error(e);
        });
    }

    private void changeStatus(ItilStatus status) {
        if (status == ItilStatus.SOLVED && type != ItilType.CHANGE) {
            proposeSolution();
            return;
        }
        run(() -> service().changeStatus(id, status), I18n.t("itil.statusChanged", I18n.e(status)));
    }

    private void run(java.util.concurrent.Callable<?> action, String toast) {
        Async.run(action, r -> {
            if (toast != null) {
                Toasts.success(toast);
            }
            load();
        });
    }

    private void run(Runnable action, String toast) {
        run(() -> {
            action.run();
            return Boolean.TRUE;
        }, toast);
    }

    private Node timelineTab() {
        VBox list = new VBox(12);
        list.getStyleClass().add("timeline");
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("timeline-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        Async.run(() -> App.services().itilActivity().timeline(type, id), entries -> {
            for (TimelineEntry e : entries) {
                list.getChildren().add(entry(e));
            }
            scroll.setVvalue(1.0);
        });
        VBox box = new VBox(12, scroll);
        if (item.getStatus() != ItilStatus.CLOSED && item.getStatus() != ItilStatus.CANCELED) {
            box.getChildren().add(composer());
        }
        box.getStyleClass().add("tab-body");
        return box;
    }

    private Node entry(TimelineEntry e) {
        if (e.kind() == TimelineEntry.Kind.STATUS) {
            Label l = Ui.label(I18n.t("timeline.status", App.lookups().user(e.authorId()),
                    HistoryPanel.resolve("status", e.content(), null), HistoryPanel.resolve("status", e.state(), null)), "muted");
            HBox line = new HBox(8, Icons.of("circle-dot", 13), l, Ui.spacer(), Ui.label(I18n.dateTime(e.at()), "muted"));
            line.setAlignment(Pos.CENTER_LEFT);
            line.getStyleClass().add("timeline-status");
            return line;
        }
        String author = e.authorId() == null ? "Pyhoma" : App.lookups().user(e.authorId());
        String kindKey = "timeline.kind." + e.kind().name();
        HBox head = new HBox(8, Ui.label(author, "timeline-author"), Ui.label(I18n.t(kindKey), "timeline-kind"));
        head.setAlignment(Pos.CENTER_LEFT);
        if (e.privateNote()) {
            head.getChildren().add(Badge.of(I18n.t("timeline.private"), "amber"));
        }
        if (e.kind() == TimelineEntry.Kind.TASK) {
            head.getChildren().add(Badge.of(I18n.t("enum.TaskStatus." + e.state()), "DONE".equals(e.state()) ? "green" : "blue"));
            if (e.minutes() > 0) {
                head.getChildren().add(Ui.label(I18n.minutes(e.minutes()), "muted"));
            }
        }
        if (e.kind() == TimelineEntry.Kind.SOLUTION || e.kind() == TimelineEntry.Kind.APPROVAL) {
            String state = e.state();
            String enumType = e.kind() == TimelineEntry.Kind.SOLUTION ? "SolutionStatus" : "ApprovalStatus";
            String tone = switch (state) {
                case "ACCEPTED" -> "green";
                case "REFUSED" -> "red";
                default -> "amber";
            };
            head.getChildren().add(Badge.of(I18n.t("enum." + enumType + "." + state), tone));
        }
        head.getChildren().addAll(Ui.spacer(), Ui.label(I18n.dateTime(e.at()), "muted"));
        Node content = Markdown.render(e.content() == null ? "" : e.content());
        VBox bubble = new VBox(8, head, content);
        bubble.getStyleClass().addAll("timeline-bubble", "kind-" + e.kind().name().toLowerCase(Locale.ROOT));
        if (e.kind() == TimelineEntry.Kind.TASK && e.assigneeId() != null) {
            String plan = e.plannedStart() == null ? "" : "  •  " + I18n.dateTime(e.plannedStart());
            bubble.getChildren().add(Ui.label(I18n.t("timeline.taskFor", App.lookups().user(e.assigneeId())) + plan, "muted"));
        }
        if (e.kind() == TimelineEntry.Kind.SOLUTION && "PROPOSED".equals(e.state())
                && (isRequester() || App.can(service().module(), Right.UPDATE))) {
            HBox buttons = Ui.row(8,
                    Ui.primary(I18n.t("solution.accept"), "check", () -> run(() -> App.services().itilActivity()
                            .acceptSolution(type, id), I18n.t("solution.accepted"))),
                    Ui.button(I18n.t("solution.refuse"), "x", () -> Dialogs.prompt(I18n.t("solution.refuse"),
                            I18n.t("solution.refuseReason"), "", true).ifPresent(reason -> run(() -> App.services().itilActivity()
                            .refuseSolution(type, id, reason), I18n.t("solution.refused")))));
            bubble.getChildren().add(buttons);
        }
        if (e.kind() == TimelineEntry.Kind.APPROVAL && "WAITING".equals(e.state())
                && (Objects.equals(e.assigneeId(), App.session().userId()) || App.can(service().module(), Right.APPROVE))) {
            HBox buttons = Ui.row(8,
                    Ui.primary(I18n.t("approval.accept"), "check", () -> answerApproval(e.id(), true)),
                    Ui.button(I18n.t("approval.refuse"), "x", () -> answerApproval(e.id(), false)));
            bubble.getChildren().add(buttons);
        }
        if (e.kind() == TimelineEntry.Kind.APPROVAL && e.assigneeId() != null) {
            bubble.getChildren().add(Ui.label(I18n.t("timeline.approver", App.lookups().user(e.assigneeId())), "muted"));
        }
        HBox row = new HBox(12, new Avatar(author, 32), bubble);
        HBox.setHgrow(bubble, Priority.ALWAYS);
        row.getStyleClass().add("timeline-row");
        return row;
    }

    private void answerApproval(Long approvalId, boolean accept) {
        Dialogs.prompt(I18n.t(accept ? "approval.accept" : "approval.refuse"), I18n.t("approval.comment"), "", true)
                .or(() -> accept ? java.util.Optional.of("") : java.util.Optional.empty())
                .ifPresent(c -> run(() -> App.services().itilActivity().answerApproval(approvalId, accept, c),
                        I18n.t(accept ? "approval.accepted" : "approval.refused")));
    }

    private Node composer() {
        TextArea text = new TextArea();
        text.setPromptText(I18n.t("composer.prompt"));
        text.setPrefRowCount(3);
        text.setWrapText(true);
        CheckBox privateNote = new CheckBox(I18n.t("composer.private"));
        boolean tech = App.can(service().module(), Right.UPDATE);
        privateNote.setVisible(tech);
        privateNote.setManaged(tech);
        Button send = Ui.primary(I18n.t("composer.send"), "message-square", () -> {
            String t = text.getText();
            if (t == null || t.isBlank()) {
                return;
            }
            Async.exec(() -> App.services().itilActivity().addFollowup(type, id, t, privateNote.isSelected()), () -> {
                text.clear();
                Toasts.success(I18n.t("composer.sent"));
                load();
            });
        });
        HBox actions = Ui.row(8, privateNote, Ui.spacer());
        if (tech) {
            actions.getChildren().add(Ui.button(I18n.t("itil.addTask"), "list-checks", () -> editTask(newTask())));
            if (type != ItilType.CHANGE && item.getStatus().isOpen()) {
                actions.getChildren().add(Ui.button(I18n.t("itil.solve"), "circle-check", this::proposeSolution));
            }
        }
        actions.getChildren().add(send);
        VBox box = new VBox(8, text, actions);
        box.getStyleClass().add("composer");
        return box;
    }

    private void proposeSolution() {
        Holder h = new Holder();
        Form f = new Form(1)
                .choice("solutionTypeId", () -> Option.of(App.lookups().dropdown(DropdownKind.SOLUTION_TYPE)),
                        () -> h.typeId, v -> h.typeId = v, true, false)
                .area("content", () -> h.text, v -> h.text = v, 7, true);
        f.load();
        PickerField<Long> kb = new PickerField<>(() -> {
            List<Option<Long>> list = new ArrayList<>();
            App.services().knowledgeBase().search(null, null, false, false, 0, 500).items()
                    .forEach(a -> list.add(new Option<>(a.getId(), a.getTitle())));
            return list;
        }, true);
        kb.onChange(articleId -> {
            if (articleId != null) {
                Async.run(() -> App.services().knowledgeBase().get(articleId), (KbArticle a) ->
                        ((TextArea) f.box("content").control()).setText(a.getContent()));
            }
        });
        VBox content = new VBox(12, f.node(), Ui.fact(I18n.t("solution.fromKb"), kb));
        content.setPrefWidth(560);
        Dialogs.form(I18n.t("itil.solve"), content, I18n.t("solution.submit"), () -> {
            f.commit();
            try {
                App.services().itilActivity().proposeSolution(type, id, h.text, h.typeId);
                Toasts.success(I18n.t("solution.proposed"));
                load();
                return true;
            } catch (ValidationException e) {
                f.showErrors(e);
                return false;
            }
        });
    }

    private static final class Holder {
        Long typeId;
        String text;
        Long userId;
        Long groupId;
        RelationType relation = RelationType.LINKED;
        ItilType otherType = ItilType.TICKET;
        Integer otherId;
    }

    private ItilTask newTask() {
        ItilTask t = new ItilTask();
        t.setItemType(type);
        t.setItemId(id);
        t.setTechnicianId(App.session().userId());
        t.setStatus(TaskStatus.TODO);
        return t;
    }

    private Node tasksTab() {
        DataTable<ItilTask> table = new DataTable<>();
        table.node(I18n.t("field.status"), 110, t -> Badge.of(I18n.e(t.getStatus()), t.getStatus() == TaskStatus.DONE ? "green" : "blue"))
                .col(I18n.t("field.content"), 320, t -> io.pyhoma.domain.support.Texts.abbreviate(t.getContent(), 120))
                .col(I18n.t("field.technicianId"), 160, t -> App.lookups().user(t.getTechnicianId()))
                .col(I18n.t("field.plannedStartAt"), 140, t -> I18n.dateTime(t.getPlannedStartAt()))
                .col(I18n.t("field.durationMinutes"), 100, "num", t -> I18n.minutes(t.getDurationMinutes()));
        table.placeholder(new EmptyState("list-checks", I18n.t("tasks.empty"), I18n.t("tasks.emptyHint"), null));
        table.onOpen(this::editTask);
        Runnable reload = () -> Async.run(() -> App.services().itilActivity().tasks(type, id), table::setRows);
        reload.run();
        HBox actions = Ui.row(8, Ui.spacer(),
                Ui.button(I18n.t("tasks.complete"), "check", () -> {
                    ItilTask t = table.selected();
                    if (t != null) {
                        run(() -> App.services().itilActivity().completeTask(t.getId()), I18n.t("tasks.completed"));
                    }
                }),
                Ui.button(I18n.t("action.delete"), "trash-2", () -> {
                    ItilTask t = table.selected();
                    if (t != null && Dialogs.confirm(I18n.t("action.delete"), I18n.t("confirm.delete"), I18n.t("action.delete"), true)) {
                        run(() -> App.services().itilActivity().deleteTask(t.getId()), I18n.t("toast.deleted"));
                    }
                }, "ghost"),
                Ui.primary(I18n.t("itil.addTask"), "plus", () -> editTask(newTask())));
        VBox.setVgrow(table, Priority.ALWAYS);
        return Ui.style(new VBox(10, actions, table), "tab-body");
    }

    private void editTask(ItilTask task) {
        Form f = new Form(2)
                .area("content", task::getContent, task::setContent, 4, true)
                .choice("status", () -> Option.ofEnum(TaskStatus.values()), task::getStatus, task::setStatus, false, true)
                .choice("technicianId", ItilDetailView::technicianOptions, task::getTechnicianId, task::setTechnicianId, true, false)
                .choice("categoryId", () -> Option.of(App.lookups().dropdown(DropdownKind.TASK_CATEGORY)), task::getCategoryId,
                        task::setCategoryId, true, false)
                .integer("durationMinutes", task::getDurationMinutes, task::setDurationMinutes)
                .dateTime("plannedStartAt", task::getPlannedStartAt, task::setPlannedStartAt)
                .dateTime("plannedEndAt", task::getPlannedEndAt, task::setPlannedEndAt)
                .check("privateNote", task::isPrivateNote, task::setPrivateNote);
        f.load();
        Node content = f.node();
        ((javafx.scene.layout.Region) content).setPrefWidth(620);
        Dialogs.form(I18n.t(task.isNew() ? "itil.addTask" : "tasks.edit"), content, I18n.t("action.save"), () -> {
            if (!f.commit()) {
                return false;
            }
            try {
                App.services().itilActivity().saveTask(task);
                Toasts.success(I18n.t("toast.saved"));
                load();
                return true;
            } catch (ValidationException e) {
                f.showErrors(e);
                return false;
            }
        });
    }

    private Node approvalsTab() {
        DataTable<ItilApproval> table = new DataTable<>();
        table.col(I18n.t("field.approverId"), 180, a -> App.lookups().user(a.getApproverId()))
                .node(I18n.t("field.status"), 120, a -> Badge.of(I18n.e(a.getStatus()), a.getStatus() == ApprovalStatus.ACCEPTED
                        ? "green" : a.getStatus() == ApprovalStatus.REFUSED ? "red" : "amber"))
                .col(I18n.t("field.requestComment"), 240, ItilApproval::getRequestComment)
                .col(I18n.t("field.answerComment"), 240, ItilApproval::getAnswerComment)
                .col(I18n.t("field.answeredAt"), 140, a -> I18n.dateTime(a.getAnsweredAt()));
        table.placeholder(new EmptyState("shield-check", I18n.t("approvals.empty"), I18n.t("approvals.emptyHint"), null));
        Async.run(() -> App.services().itilActivity().approvals(type, id), table::setRows);
        HBox actions = Ui.row(8, Ui.spacer(), Ui.primary(I18n.t("approvals.request"), "shield-check", () -> {
            Holder h = new Holder();
            Form f = new Form(1)
                    .choice("approverId", ItilDetailView::userOptions, () -> h.userId, v -> h.userId = v, false, true)
                    .area("requestComment", () -> h.text, v -> h.text = v, 3, false);
            f.load();
            ((javafx.scene.layout.Region) f.node()).setPrefWidth(460);
            Dialogs.form(I18n.t("approvals.request"), f.node(), I18n.t("approvals.send"), () -> {
                f.commit();
                if (h.userId == null) {
                    return false;
                }
                App.services().itilActivity().requestApproval(type, id, h.userId, h.text);
                Toasts.success(I18n.t("approvals.requested"));
                load();
                return true;
            });
        }));
        VBox.setVgrow(table, Priority.ALWAYS);
        return Ui.style(new VBox(10, actions, table), "tab-body");
    }

    private Node linksTab() {
        DataTable<RelatedItem> related = new DataTable<>();
        related.col(I18n.t("links.relation"), 150, r -> I18n.e(r.relationType()))
                .col(I18n.t("links.type"), 100, r -> I18n.t("type." + r.type().entityClass().getSimpleName()))
                .col("#", 70, "num", RelatedItem::id)
                .col(I18n.t("field.title"), 320, RelatedItem::title)
                .node(I18n.t("field.status"), 130, r -> Badge.status(r.status()));
        related.placeholder(new EmptyState("link", I18n.t("links.empty"), null, null));
        related.onOpen(r -> App.nav().open(new ItilDetailView(r.type(), r.id())));
        DataTable<Asset> assets = new DataTable<>();
        assets.col(I18n.t("field.name"), 200, Asset::getName)
                .col(I18n.t("field.assetType"), 120, a -> I18n.e(a.getAssetType()))
                .col(I18n.t("field.assetTag"), 130, "mono", Asset::getAssetTag)
                .col(I18n.t("field.serialNumber"), 150, "mono", Asset::getSerialNumber)
                .col(I18n.t("field.userId"), 160, a -> App.lookups().user(a.getUserId()));
        assets.placeholder(new EmptyState("monitor", I18n.t("links.noAssets"), null, null));
        assets.onOpen(a -> App.nav().open(new io.pyhoma.desktop.views.assets.AssetDetailView(a.getId())));
        Runnable reload = () -> {
            Async.run(() -> App.services().itilActivity().related(type, id), related::setRows);
            Async.run(() -> App.services().itilActivity().assets(type, id), assets::setRows);
        };
        reload.run();
        HBox relActions = Ui.row(8, Ui.label(I18n.t("links.items"), "panel-title"), Ui.spacer(),
                Ui.button(I18n.t("links.remove"), "unlink", () -> {
                    RelatedItem r = related.selected();
                    if (r != null) {
                        Async.exec(() -> App.services().itilActivity().unlink(r.relationId()), reload);
                    }
                }, "ghost"),
                Ui.button(I18n.t("links.add"), "link", () -> addLink(reload)));
        HBox assetActions = Ui.row(8, Ui.label(I18n.t("links.assets"), "panel-title"), Ui.spacer(),
                Ui.button(I18n.t("links.remove"), "unlink", () -> {
                    Asset a = assets.selected();
                    if (a != null) {
                        Async.exec(() -> App.services().itilActivity().unlinkAsset(type, id, a.getId()), reload);
                    }
                }, "ghost"),
                Ui.button(I18n.t("links.addAsset"), "monitor", () -> addAsset(reload)));
        related.setPrefHeight(220);
        assets.setPrefHeight(220);
        return Ui.style(new VBox(10, relActions, related, assetActions, assets), "tab-body");
    }

    private void addLink(Runnable reload) {
        Holder h = new Holder();
        Form f = new Form(2)
                .choice("relationType", () -> Option.ofEnum(RelationType.values()), () -> h.relation, v -> h.relation = v, false, true)
                .choice("itemType", () -> Option.ofEnum(ItilType.values()), () -> h.otherType, v -> h.otherType = v, false, true)
                .integer("itemId", () -> h.otherId, v -> h.otherId = v);
        f.load();
        ((javafx.scene.layout.Region) f.node()).setPrefWidth(480);
        Dialogs.form(I18n.t("links.add"), f.node(), I18n.t("links.add"), () -> {
            if (!f.commit() || h.otherId == null) {
                return false;
            }
            App.services().itilActivity().link(type, id, h.otherType, h.otherId.longValue(), h.relation);
            reload.run();
            return true;
        });
    }

    private void addAsset(Runnable reload) {
        PickerField<Long> picker = new PickerField<>(() -> {
            List<Option<Long>> list = new ArrayList<>();
            App.services().assets().search(io.pyhoma.domain.service.AssetService.AssetSearch.of(null, null)
                            .withSize(2000)).items()
                    .forEach(a -> list.add(new Option<>(a.getId(), a.getName(), a.getAssetTag(), null)));
            return list;
        }, false);
        picker.setPrefWidth(420);
        Dialogs.form(I18n.t("links.addAsset"), picker, I18n.t("links.add"), () -> {
            if (picker.getValue() == null) {
                return false;
            }
            App.services().itilActivity().linkAsset(type, id, picker.getValue());
            reload.run();
            return true;
        });
    }

    private Node peopleTab() {
        PeopleField assignees = new PeopleField(PeopleField::activeUsers, I18n.t("people.addAssignee"));
        PeopleField followers = new PeopleField(PeopleField::activeUsers, I18n.t("people.addFollower"));
        Button save = Ui.primary(I18n.t("people.save"), "save", () -> Async.run(
                () -> service().setPeople(id, assignees.getValues(), followers.getValues()), p -> {
                    Toasts.success(I18n.t("people.saved"));
                    load();
                }, Dialogs::error));
        save.setDisable(true);
        Runnable dirty = () -> save.setDisable(item.getStatus().isTerminal());
        assignees.onChange(dirty);
        followers.onChange(dirty);
        Async.run(() -> service().people(id), p -> {
            assignees.setValues(p.assignees());
            followers.setValues(p.followers());
        });
        VBox people = new VBox(8,
                Ui.label(I18n.t("field.assignees"), "field-label"), assignees, Ui.muted(I18n.t("people.assigneesHint")),
                Ui.label(I18n.t("field.followers"), "field-label"), followers, Ui.muted(I18n.t("people.followersHint")),
                Ui.row(8, Ui.spacer(), save));
        VBox.setMargin(people.getChildren().get(3), new javafx.geometry.Insets(12, 0, 0, 0));
        VBox body = new VBox(18, Ui.panel(I18n.t("people.panel"), people));
        if (!App.session().isSelfService()) {
            body.getChildren().add(groupsPanel());
        }
        ScrollPane sp = new ScrollPane(body);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("tab-scroll");
        return Ui.style(new VBox(sp), "tab-body");
    }

    private Node groupsPanel() {
        DataTable<ItilObserver> table = new DataTable<>();
        table.col(I18n.t("field.groupId"), 260, o -> App.lookups().group(o.getGroupId()));
        table.placeholder(new EmptyState("users", I18n.t("observers.empty"), I18n.t("observers.emptyHint"), null));
        table.setPrefHeight(200);
        Runnable reload = () -> Async.run(() -> App.services().itilActivity().observers(type, id).stream()
                .filter(o -> o.getGroupId() != null).toList(), table::setRows);
        reload.run();
        Button remove = Ui.button(I18n.t("observers.remove"), "x", () -> {
            ItilObserver o = table.selected();
            if (o != null) {
                Async.exec(() -> App.services().itilActivity().removeObserver(o.getId()), reload);
            }
        }, "ghost");
        Button add = Ui.button(I18n.t("observers.add"), "plus", () -> {
            Holder h = new Holder();
            Form f = new Form(1)
                    .choice("groupId", () -> Option.of(App.lookups().groups()), () -> h.groupId, v -> h.groupId = v, true, true);
            f.load();
            ((javafx.scene.layout.Region) f.node()).setPrefWidth(420);
            Dialogs.form(I18n.t("observers.add"), f.node(), I18n.t("observers.add"), () -> {
                f.commit();
                if (h.groupId == null) {
                    return false;
                }
                App.services().itilActivity().addObserver(type, id, null, h.groupId);
                reload.run();
                return true;
            });
        });
        boolean tech = App.can(service().module(), Right.UPDATE);
        remove.setVisible(tech);
        add.setVisible(tech);
        return Ui.panel(I18n.t("observers.groups"), table, remove, add);
    }

    static List<Option<Long>> userOptions() {
        List<Option<Long>> list = new ArrayList<>();
        for (User u : App.services().users().activeUsers()) {
            list.add(new Option<>(u.getId(), u.getFullName(), u.getLogin(), null));
        }
        return list;
    }

    static List<Option<Long>> technicianOptions() {
        List<Option<Long>> list = new ArrayList<>();
        for (User u : App.lookups().technicians()) {
            list.add(new Option<>(u.getId(), u.getFullName(), u.getJobTitle(), null));
        }
        return list;
    }
}
