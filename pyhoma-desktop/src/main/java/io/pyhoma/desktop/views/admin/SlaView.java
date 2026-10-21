package io.pyhoma.desktop.views.admin;

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
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.desktop.ui.components.PageHeader;
import io.pyhoma.domain.itil.BusinessCalendar;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.itil.Holiday;
import io.pyhoma.domain.model.itil.Sla;
import io.pyhoma.domain.model.itil.SlaType;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.ValidationException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SlaView extends View {

    private final DataTable<Sla> slas = new DataTable<>();
    private final DataTable<Holiday> holidays = new DataTable<>();
    private final VBox calendarBox = new VBox(14);

    @Override
    public String title() {
        return I18n.t("nav.SLAS");
    }

    @Override
    protected Node build() {
        PageHeader header = new PageHeader(I18n.t("nav.SLAS"), I18n.t("sla.subtitle"));
        boolean edit = App.can(Module.SLA, Right.UPDATE);
        slas.col(I18n.t("field.name"), 260, Sla::getName)
                .node(I18n.t("field.slaType"), 190, s -> Badge.of(I18n.e(s.getSlaType()), s.getSlaType() == SlaType.TTO ? "blue" : "violet"))
                .col(I18n.t("field.priority"), 120, s -> s.getPriority() == null ? I18n.t("sla.anyPriority") : I18n.priority(s.getPriority()))
                .col(I18n.t("field.durationMinutes"), 110, "num", s -> I18n.minutes(s.getDurationMinutes()))
                .col(I18n.t("field.businessHours"), 140, s -> I18n.t(s.isBusinessHours() ? "common.yes" : "common.no"))
                .col(I18n.t("field.orgUnitId"), 180, s -> App.lookups().orgUnit(s.getOrgUnitId()))
                .col(I18n.t("field.active"), 80, s -> I18n.t(s.isActive() ? "common.yes" : "common.no"));
        slas.onOpen(this::editSla);
        HBox slaActions = Ui.row(8, Ui.muted(I18n.t("sla.hint")), Ui.spacer());
        if (App.can(Module.SLA, Right.CREATE)) {
            slaActions.getChildren().add(Ui.primary(I18n.t("sla.new"), "plus", () -> {
                Sla s = new Sla();
                s.setOrgUnitId(App.session().activeOrgUnitId());
                s.setSlaType(SlaType.TTR);
                editSla(s);
            }));
        }
        VBox.setVgrow(slas, Priority.ALWAYS);
        VBox slaTab = Ui.style(new VBox(10, slaActions, slas), "tab-body");

        holidays.col(I18n.t("field.name"), 260, Holiday::getName)
                .col(I18n.t("field.holidayDate"), 140, h -> h.isRecurring()
                        ? h.getHolidayDate().getDayOfMonth() + "/" + h.getHolidayDate().getMonthValue() + " (" + I18n.t("sla.yearly") + ")"
                        : I18n.date(h.getHolidayDate()));
        HBox holidayActions = Ui.row(8, Ui.spacer());
        if (edit) {
            holidayActions.getChildren().addAll(
                    Ui.button(I18n.t("action.delete"), "trash-2", () -> {
                        Holiday h = holidays.selected();
                        if (h != null) {
                            Async.exec(() -> App.services().slas().deleteHoliday(h.getId()), this::onShow);
                        }
                    }, "ghost"),
                    Ui.primary(I18n.t("sla.newHoliday"), "plus", this::addHoliday));
        }
        VBox.setVgrow(holidays, Priority.ALWAYS);
        VBox holidayTab = Ui.style(new VBox(10, holidayActions, holidays), "tab-body");
        calendarBox.getStyleClass().add("tab-body");

        TabPane tabs = new TabPane(tab("sla.tab.slas", "timer", slaTab), tab("sla.tab.calendar", "clock", calendarBox),
                tab("sla.tab.holidays", "calendar", holidayTab));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("detail-tabs");
        VBox.setVgrow(tabs, Priority.ALWAYS);
        VBox root = new VBox(16, header, tabs);
        root.getStyleClass().add("page");
        return root;
    }

    private static Tab tab(String key, String icon, Node content) {
        Tab t = new Tab(I18n.t(key), content);
        t.setGraphic(Icons.of(icon, 14));
        return t;
    }

    @Override
    public void onShow() {
        Async.run(() -> App.services().slas().all(), slas::setRows);
        Async.run(() -> App.services().slas().holidays(), holidays::setRows);
        Async.run(() -> App.services().slas().calendar(), this::renderCalendar);
    }

    private void renderCalendar(BusinessCalendar cal) {
        Map<DayOfWeek, CheckBox> days = new EnumMap<>(DayOfWeek.class);
        HBox dayBoxes = new HBox(12);
        for (DayOfWeek d : DayOfWeek.values()) {
            CheckBox cb = new CheckBox(d.getDisplayName(TextStyle.SHORT, I18n.locale()));
            cb.setSelected(cal.workDays().contains(d));
            days.put(d, cb);
            dayBoxes.getChildren().add(cb);
        }
        TextField start = new TextField(cal.dayStart().toString());
        TextField end = new TextField(cal.dayEnd().toString());
        start.setPrefColumnCount(5);
        end.setPrefColumnCount(5);
        boolean edit = App.can(Module.SETTINGS, Right.UPDATE);
        HBox hours = Ui.row(8, Ui.label(I18n.t("sla.opensAt"), "field-label"), start, Ui.label(I18n.t("sla.closesAt"), "field-label"), end);
        VBox content = new VBox(14, Ui.muted(I18n.t("sla.calendarHint")), Ui.label(I18n.t("sla.workDays"), "field-label"), dayBoxes, hours);
        if (edit) {
            content.getChildren().add(Ui.primary(I18n.t("action.save"), "save", () -> {
                Set<DayOfWeek> selected = EnumSet.noneOf(DayOfWeek.class);
                days.forEach((d, cb) -> {
                    if (cb.isSelected()) {
                        selected.add(d);
                    }
                });
                Async.exec(() -> App.services().slas().saveCalendar(selected, LocalTime.parse(start.getText().trim()),
                        LocalTime.parse(end.getText().trim())), () -> Toasts.success(I18n.t("toast.saved")));
            }));
        }
        calendarBox.getChildren().setAll(Ui.panel(I18n.t("sla.tab.calendar"), content));
    }

    private void editSla(Sla s) {
        boolean canEdit = App.can(Module.SLA, s.isNew() ? Right.CREATE : Right.UPDATE);
        Form f = new Form(2).readOnly(!canEdit)
                .text("name", s::getName, s::setName, 2, true)
                .choice("slaType", () -> Option.ofEnum(SlaType.values()), s::getSlaType, s::setSlaType, false, true)
                .choice("priority", () -> Option.levels("priority.", 6), s::getPriority, s::setPriority, true, false)
                .integer("durationMinutes", s::getDurationMinutes, s::setDurationMinutes)
                .choice("orgUnitId", () -> Option.of(App.lookups().orgUnits()), s::getOrgUnitId, s::setOrgUnitId, false, true)
                .check("businessHours", s::isBusinessHours, s::setBusinessHours)
                .check("active", s::isActive, s::setActive)
                .area("notes", s::getNotes, s::setNotes, 2, false);
        f.load();
        ((javafx.scene.layout.Region) f.node()).setPrefWidth(560);
        Dialogs.form(I18n.t(s.isNew() ? "sla.new" : "sla.edit"), f.node(), I18n.t("action.save"), () -> {
            if (!canEdit) {
                return true;
            }
            if (!f.commit()) {
                return false;
            }
            try {
                App.services().slas().save(s);
                Toasts.success(I18n.t("toast.saved"));
                App.lookups().refreshAsync(this::onShow);
                return true;
            } catch (ValidationException e) {
                f.showErrors(e);
                return false;
            }
        });
    }

    private void addHoliday() {
        Holiday h = new Holiday();
        h.setRecurring(true);
        Form f = new Form(1)
                .text("name", h::getName, h::setName, 1, true)
                .date("holidayDate", h::getHolidayDate, h::setHolidayDate)
                .check("recurring", h::isRecurring, h::setRecurring);
        f.load();
        ((javafx.scene.layout.Region) f.node()).setPrefWidth(380);
        Dialogs.form(I18n.t("sla.newHoliday"), f.node(), I18n.t("action.add"), () -> {
            if (!f.commit()) {
                return false;
            }
            try {
                App.services().slas().saveHoliday(h);
                onShow();
                return true;
            } catch (ValidationException e) {
                f.showErrors(e);
                return false;
            }
        });
    }
}
