package io.pyhoma.desktop.views.itil;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.desktop.views.common.ListPage;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.itil.Periodicity;
import io.pyhoma.domain.model.itil.RecurringTicket;
import io.pyhoma.domain.model.itil.TicketType;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.ValidationException;
import java.util.List;
import javafx.scene.Node;

public class RecurringTicketsView extends ListPage<RecurringTicket> {

    @Override
    protected String titleText() {
        return I18n.t("nav.RECURRING");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("recurring.subtitle");
    }

    @Override
    protected void columns(DataTable<RecurringTicket> t) {
        t.col(I18n.t("field.name"), 220, RecurringTicket::getName)
                .col(I18n.t("field.title"), 260, RecurringTicket::getTitle)
                .col(I18n.t("field.periodicity"), 150, r -> I18n.t("recurring.every", r.getIntervalCount(), I18n.e(r.getPeriodicity())))
                .col(I18n.t("field.assigneeGroupId"), 160, r -> App.lookups().group(r.getAssigneeGroupId()))
                .col(I18n.t("field.nextRunAt"), 140, r -> I18n.dateTime(r.getNextRunAt()))
                .col(I18n.t("field.lastRunAt"), 140, r -> I18n.dateTime(r.getLastRunAt()))
                .node(I18n.t("field.active"), 90, r -> Badge.of(I18n.t(r.isActive() ? "common.active" : "common.inactive"),
                        r.isActive() ? "green" : "gray"));
    }

    @Override
    protected PageResult<RecurringTicket> load(String text, int page, int size) {
        List<RecurringTicket> all = App.services().recurringTickets().all().stream()
                .filter(r -> text == null || text.isBlank() || Texts.fold(r.getName() + " " + r.getTitle()).contains(Texts.fold(text)))
                .toList();
        return new PageResult<>(all, all.size(), 0, -1);
    }

    @Override
    protected Node[] actions() {
        if (!App.can(Module.RECURRING_TICKET, Right.CREATE)) {
            return new Node[0];
        }
        return new Node[] {
            Ui.button(I18n.t("recurring.runNow"), "play", () -> {
                RecurringTicket r = table.selected();
                if (r != null) {
                    Async.run(() -> App.services().recurringTickets().runNow(r.getId()), t -> {
                        Toasts.success(I18n.t("itil.created", t.reference()));
                        reload();
                    });
                }
            }),
            Ui.primary(I18n.t("recurring.new"), "plus", () -> edit(new RecurringTicket()))};
    }

    @Override
    protected void open(RecurringTicket row) {
        edit(row);
    }

    private void edit(RecurringTicket r) {
        boolean canEdit = App.can(Module.RECURRING_TICKET, r.isNew() ? Right.CREATE : Right.UPDATE);
        Form f = new Form(2).readOnly(!canEdit)
                .text("name", r::getName, r::setName, 1, true)
                .check("active", r::isActive, r::setActive)
                .text("title", r::getTitle, r::setTitle, 2, true)
                .area("content", r::getContent, r::setContent, 3, false)
                .choice("ticketType", () -> Option.ofEnum(TicketType.values()), r::getTicketType, r::setTicketType, false, true)
                .choice("categoryId", () -> Option.of(App.lookups().dropdown(DropdownKind.ITIL_CATEGORY)), r::getCategoryId,
                        r::setCategoryId, true, false)
                .choice("urgency", () -> Option.levels("level.", 5), r::getUrgency, r::setUrgency, false, true)
                .choice("impact", () -> Option.levels("level.", 5), r::getImpact, r::setImpact, false, true)
                .choice("assigneeGroupId", () -> Option.of(App.lookups().groups()), r::getAssigneeGroupId, r::setAssigneeGroupId, true, false)
                .choice("assigneeId", ItilDetailView::technicianOptions, r::getAssigneeId, r::setAssigneeId, true, false)
                .choice("periodicity", () -> Option.ofEnum(Periodicity.values()), r::getPeriodicity, r::setPeriodicity, false, true)
                .integer("intervalCount", r::getIntervalCount, r::setIntervalCount)
                .dateTime("nextRunAt", r::getNextRunAt, r::setNextRunAt);
        f.load();
        ((javafx.scene.layout.Region) f.node()).setPrefWidth(640);
        if (!canEdit) {
            Dialogs.info(r.getName(), "");
            return;
        }
        Dialogs.form(I18n.t(r.isNew() ? "recurring.new" : "recurring.edit"), f.node(), I18n.t("action.save"), () -> {
            if (!f.commit()) {
                return false;
            }
            try {
                App.services().recurringTickets().save(r);
                Toasts.success(I18n.t("toast.saved"));
                reload();
                return true;
            } catch (ValidationException e) {
                f.showErrors(e);
                return false;
            }
        });
    }
}
