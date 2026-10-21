package io.pyhoma.desktop.views.itil;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Avatar;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.PickerField;
import io.pyhoma.desktop.ui.components.Segmented;
import io.pyhoma.desktop.views.common.ListPage;
import io.pyhoma.domain.itil.SlaState;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.itil.ItilItem;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.model.itil.TicketType;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.ItilSearch;
import io.pyhoma.domain.service.ItilSearch.View;
import io.pyhoma.domain.service.ItilService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ItilListView extends ListPage<ItilItem> {

    private final ItilType type;
    private View view;
    private final PickerField<TicketType> ticketType = PickerField.compact(
            () -> Option.ofEnum(TicketType.values()), null);
    private final PickerField<Integer> priority = PickerField.compact(() -> Option.levels("priority.min.", 6), null);
    private final PickerField<Long> category = PickerField.compact(
            () -> Option.of(App.lookups().dropdown(DropdownKind.ITIL_CATEGORY)), null);
    private final PickerField<Long> assignee = PickerField.compact(() -> {
        List<Option<Long>> list = new ArrayList<>();
        App.lookups().technicians().forEach(u -> list.add(new Option<>(u.getId(), u.getFullName())));
        return list;
    }, null);

    public ItilListView(ItilType type) {
        this.type = type;
        this.view = App.session().isSelfService() ? View.MINE_REQUESTED : View.OPEN;
    }

    private ItilService<? extends ItilItem> service() {
        return App.services().itil(type);
    }

    private String key() {
        return type.name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected String titleText() {
        return App.session().isSelfService() ? I18n.t("nav.MY_TICKETS") : I18n.t("itil." + key() + ".plural");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("itil." + key() + ".subtitle");
    }

    @Override
    protected String exportName() {
        return I18n.t("itil." + key() + ".plural").toLowerCase(Locale.ROOT);
    }

    @Override
    protected Node top() {
        List<View> views = App.session().isSelfService()
                ? List.of(View.MINE_REQUESTED, View.OPEN, View.CLOSED)
                : new ArrayList<>(List.of(View.OPEN, View.MINE_ASSIGNED, View.MY_GROUPS, View.UNASSIGNED, View.OVERDUE,
                        View.SOLVED, View.CLOSED, View.ALL));
        if (!App.session().isSelfService() && App.can(service().module(), Right.DELETE)) {
            views.add(View.TRASH);
        }
        return new Segmented<>(views, v -> I18n.t("itil.view." + v.name()), view, v -> {
            view = v;
            loadPage(0);
        });
    }

    @Override
    protected Node[] filters() {
        if (App.session().isSelfService()) {
            return new Node[0];
        }
        priority.onChange(v -> loadPage(0));
        category.onChange(v -> loadPage(0));
        assignee.onChange(v -> loadPage(0));
        List<Node> nodes = new ArrayList<>();
        if (type == ItilType.TICKET) {
            ticketType.onChange(v -> loadPage(0));
            nodes.add(labeled("filter.type", ticketType));
        }
        nodes.add(labeled("filter.priority", priority));
        nodes.add(labeled("filter.category", category));
        nodes.add(labeled("filter.assignee", assignee));
        return nodes.toArray(Node[]::new);
    }

    static Node labeled(String key, Node picker) {
        javafx.scene.control.Label label = Ui.label(I18n.t(key), "filter-label");
        label.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        HBox box = new HBox(6, label, picker);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    @Override
    protected Node[] actions() {
        if (!App.can(service().module(), Right.CREATE)) {
            return new Node[0];
        }
        return new Node[] {Ui.primary(I18n.t("itil." + key() + ".new"), "plus",
                () -> ItilQuickForm.open(type, io.pyhoma.domain.model.itil.ItilStatus.NEW, ItilQuickForm::openCreated))};
    }

    @Override
    protected Node emptyState() {
        return new EmptyState(type == ItilType.TICKET ? "ticket" : "inbox", I18n.t("itil.empty"), I18n.t("itil.emptyHint"), null);
    }

    @Override
    protected void columns(DataTable<ItilItem> t) {
        t.col("#", 64, "num", ItilItem::getId)
                .col(I18n.t("field.title"), 320, ItilItem::getTitle)
                .node(I18n.t("field.status"), 140, i -> Badge.status(i.getStatus()), i -> I18n.e(i.getStatus()))
                .node(I18n.t("field.priority"), 120, i -> Badge.priority(i.getPriority()), i -> I18n.priority(i.getPriority()));
        if (type == ItilType.TICKET) {
            t.node(I18n.t("field.dueAt"), 110, i -> slaCell((Ticket) i), i -> I18n.dateTime(i.getDueAt()));
        } else {
            t.col(I18n.t("field.lastActivityAt"), 120, i -> I18n.relative(i.getLastActivityAt()));
        }
        t.node(I18n.t("field.assigneeId"), 170, ItilListView::assigneeCell, i -> App.lookups().user(i.getAssigneeId()))
                .col(I18n.t("field.requesterId"), 150, i -> App.lookups().user(i.getRequesterId()));
        if (type == ItilType.TICKET) {
            t.col(I18n.t("field.ticketType"), 100, i -> I18n.e(((Ticket) i).getTicketType()));
        }
        t.col(I18n.t("field.categoryId"), 160, i -> App.lookups().dd(DropdownKind.ITIL_CATEGORY, i.getCategoryId()))
                .col(I18n.t("field.assigneeGroupId"), 150, i -> App.lookups().group(i.getAssigneeGroupId()));
    }

    static Node assigneeCell(ItilItem i) {
        if (i.getAssigneeId() == null) {
            return Ui.label(i.getAssigneeGroupId() == null ? I18n.t("itil.unassigned") : "—", "muted");
        }
        String name = App.lookups().user(i.getAssigneeId());
        Label l = new Label(name);
        HBox box = new HBox(8, new Avatar(name, 22), l);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    static Node slaCell(Ticket t) {
        if (t.getDueAt() == null) {
            return Ui.label("—", "muted");
        }
        SlaState state = App.services().tickets().ttrState(t);
        Badge b = Badge.sla(state);
        ((Label) b.getChildren().get(1)).setText(state.isProblem() || state == SlaState.AT_RISK || state == SlaState.ON_TIME
                ? I18n.relative(t.getDueAt()) : I18n.e(state));
        return b;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected PageResult<ItilItem> load(String text, int page, int size) {
        ItilSearch s = ItilSearch.create().view(view).text(text).ticketType(ticketType.getValue())
                .minPriority(priority.getValue()).categoryId(category.getValue()).assigneeId(assignee.getValue())
                .page(page, size);
        return (PageResult<ItilItem>) (PageResult<?>) service().search(s);
    }

    @Override
    protected void open(ItilItem row) {
        App.nav().open(new ItilDetailView(type, row.getId()));
    }
}
