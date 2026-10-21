package io.pyhoma.desktop.views.admin;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.Async;
import io.pyhoma.desktop.ui.Dialogs;
import io.pyhoma.desktop.ui.I18n;
import io.pyhoma.desktop.ui.Option;
import io.pyhoma.desktop.ui.Toasts;
import io.pyhoma.desktop.ui.Ui;
import io.pyhoma.desktop.ui.components.Badge;
import io.pyhoma.desktop.ui.components.DataTable;
import io.pyhoma.desktop.ui.components.EmptyState;
import io.pyhoma.desktop.ui.components.Form;
import io.pyhoma.desktop.ui.components.PickerField;
import io.pyhoma.desktop.views.itil.ItilListViewHelper;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.model.itil.TicketType;
import io.pyhoma.domain.model.rules.Rule;
import io.pyhoma.domain.model.rules.RuleAction;
import io.pyhoma.domain.model.rules.RuleActionType;
import io.pyhoma.domain.model.rules.RuleCriterion;
import io.pyhoma.domain.model.rules.RuleField;
import io.pyhoma.domain.model.rules.RuleOperator;
import io.pyhoma.domain.model.rules.RuleTrigger;
import io.pyhoma.domain.rules.RuleDefinition;
import io.pyhoma.domain.rules.RuleOutcome;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class RulesView extends MasterDetail {

    private final DataTable<RuleDefinition> rules = new DataTable<>();

    @Override
    protected String titleText() {
        return I18n.t("nav.RULES");
    }

    @Override
    protected String subtitleText() {
        return I18n.t("rules.subtitle");
    }

    @Override
    protected Node[] actions() {
        if (!App.can(Module.RULE, Right.CREATE)) {
            return new Node[0];
        }
        return new Node[] {Ui.primary(I18n.t("rules.new"), "plus", () -> {
            Rule r = new Rule();
            r.setTriggerEvent(RuleTrigger.ON_CREATE);
            edit(new RuleDefinition(r, List.of(), List.of()));
        })};
    }

    @Override
    protected Node master() {
        rules.col("#", 50, "num", d -> d.rule().getRanking())
                .node(I18n.t("field.name"), 240, d -> {
                    HBox h = new HBox(8, new Label(d.rule().getName()));
                    h.setAlignment(Pos.CENTER_LEFT);
                    if (!d.rule().isActive()) {
                        h.getChildren().add(Badge.of(I18n.t("common.inactive"), "gray"));
                    }
                    return h;
                });
        rules.autoSelectFirst();
        rules.getSelectionModel().selectedItemProperty().addListener((o, a, d) -> {
            if (d != null) {
                edit(d);
            }
        });
        rules.placeholder(new EmptyState("workflow", I18n.t("rules.empty"), I18n.t("rules.emptyHint"), null));
        HBox order = Ui.row(6, Ui.spacer(),
                Ui.iconButton("arrow-up", I18n.t("rules.up"), () -> move(true)),
                Ui.iconButton("arrow-down", I18n.t("rules.down"), () -> move(false)));
        VBox.setVgrow(rules, Priority.ALWAYS);
        VBox box = new VBox(6, order, rules);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private void move(boolean up) {
        RuleDefinition d = rules.selected();
        if (d != null && App.can(Module.RULE, Right.UPDATE)) {
            Async.exec(() -> App.services().rules().move(d.rule().getId(), up), this::onShow);
        }
    }

    @Override
    public void onShow() {
        Async.run(() -> App.services().rules().all(), rules::setRows);
    }

    private static List<Option<String>> referenceOptions(RuleField field) {
        List<Option<String>> list = new ArrayList<>();
        java.util.Map<Long, String> names = switch (field) {
            case CATEGORY -> App.lookups().dropdown(DropdownKind.ITIL_CATEGORY);
            case LOCATION -> App.lookups().dropdown(DropdownKind.LOCATION);
            case SOURCE -> App.lookups().dropdown(DropdownKind.REQUEST_SOURCE);
            case REQUESTER -> App.lookups().users();
            case REQUESTER_GROUP -> App.lookups().groups();
            case ORG_UNIT -> App.lookups().orgUnits();
            default -> java.util.Map.of();
        };
        names.forEach((k, v) -> list.add(new Option<>(String.valueOf(k), v)));
        list.sort((a, b) -> a.label().compareToIgnoreCase(b.label()));
        return list;
    }

    private static List<Option<String>> actionOptions(RuleActionType type) {
        List<Option<String>> list = new ArrayList<>();
        java.util.Map<Long, String> names = switch (type) {
            case ASSIGN_USER, ADD_OBSERVER, REQUEST_APPROVAL -> App.lookups().users();
            case ASSIGN_GROUP -> App.lookups().groups();
            case SET_CATEGORY -> App.lookups().dropdown(DropdownKind.ITIL_CATEGORY);
            case SET_LOCATION -> App.lookups().dropdown(DropdownKind.LOCATION);
            case SET_SLA_TTO, SET_SLA_TTR -> App.lookups().slas();
            default -> java.util.Map.of();
        };
        names.forEach((k, v) -> list.add(new Option<>(String.valueOf(k), v)));
        if (type == RuleActionType.SET_TICKET_TYPE) {
            for (TicketType t : TicketType.values()) {
                list.add(new Option<>(t.name(), I18n.e(t)));
            }
        }
        if (type == RuleActionType.SET_URGENCY || type == RuleActionType.SET_IMPACT) {
            for (int i = 5; i >= 1; i--) {
                list.add(new Option<>(String.valueOf(i), I18n.t("level." + i)));
            }
        }
        if (type == RuleActionType.SET_PRIORITY) {
            for (int i = 6; i >= 1; i--) {
                list.add(new Option<>(String.valueOf(i), I18n.t("priority." + i)));
            }
        }
        return list;
    }

    private final class CriterionRow extends HBox {
        final PickerField<RuleField> field = new PickerField<>(() -> Option.ofEnum(RuleField.values()), false);
        final PickerField<RuleOperator> op = new PickerField<>(() -> Option.ofEnum(RuleOperator.values()), false);
        final StackPane valueBox = new StackPane();
        TextField text = new TextField();
        PickerField<String> ref;

        CriterionRow(RuleCriterion c, VBox parent) {
            super(8);
            setAlignment(Pos.CENTER_LEFT);
            getStyleClass().add("rule-row");
            field.setValue(c.getField() == null ? RuleField.TITLE : c.getField());
            op.setValue(c.getMatchOperator() == null ? RuleOperator.CONTAINS : c.getMatchOperator());
            field.setPrefWidth(190);
            op.setPrefWidth(170);
            HBox.setHgrow(valueBox, Priority.ALWAYS);
            field.onChange(f -> rebuildValue(null));
            rebuildValue(c.getPattern());
            getChildren().addAll(Ui.label(I18n.t("rules.if"), "rule-keyword"), field, op, valueBox,
                    Ui.iconButton("x", I18n.t("action.remove"), () -> parent.getChildren().remove(this)));
        }

        void rebuildValue(String value) {
            RuleField f = field.getValue();
            if (f != null && f.isReference()) {
                ref = new PickerField<>(() -> referenceOptions(f), false);
                ref.setValue(value);
                valueBox.getChildren().setAll(ref);
                text = null;
            } else {
                text = new TextField(value);
                text.setPromptText(I18n.t("rules.patternHint"));
                valueBox.getChildren().setAll(text);
                ref = null;
            }
        }

        RuleCriterion toCriterion() {
            RuleCriterion c = new RuleCriterion();
            c.setField(field.getValue());
            c.setMatchOperator(op.getValue());
            c.setPattern(ref != null ? ref.getValue() : text.getText());
            return c;
        }
    }

    private final class ActionRow extends HBox {
        final PickerField<RuleActionType> type = new PickerField<>(() -> Option.ofEnum(RuleActionType.values()), false);
        final StackPane valueBox = new StackPane();
        PickerField<String> value;

        ActionRow(RuleAction a, VBox parent) {
            super(8);
            setAlignment(Pos.CENTER_LEFT);
            getStyleClass().add("rule-row");
            type.setValue(a.getActionType() == null ? RuleActionType.ASSIGN_GROUP : a.getActionType());
            type.setPrefWidth(240);
            HBox.setHgrow(valueBox, Priority.ALWAYS);
            type.onChange(t -> rebuild(null));
            rebuild(a.getActionValue());
            getChildren().addAll(Ui.label(I18n.t("rules.then"), "rule-keyword"), type, valueBox,
                    Ui.iconButton("x", I18n.t("action.remove"), () -> parent.getChildren().remove(this)));
        }

        void rebuild(String v) {
            RuleActionType t = type.getValue();
            value = new PickerField<>(() -> actionOptions(t), false);
            value.setValue(v);
            valueBox.getChildren().setAll(value);
        }

        RuleAction toAction() {
            RuleAction a = new RuleAction();
            a.setActionType(type.getValue());
            a.setActionValue(value.getValue());
            return a;
        }
    }

    private void edit(RuleDefinition def) {
        Rule r = def.rule();
        boolean canEdit = App.can(Module.RULE, r.isNew() ? Right.CREATE : Right.UPDATE);
        Form f = new Form(3).readOnly(!canEdit)
                .text("name", r::getName, r::setName, 2, true)
                .choice("triggerEvent", () -> Option.ofEnum(RuleTrigger.values()), r::getTriggerEvent, r::setTriggerEvent, false, true)
                .text("description", r::getDescription, r::setDescription, 3, false)
                .check("matchAll", r::isMatchAll, r::setMatchAll)
                .check("stopProcessing", r::isStopProcessing, r::setStopProcessing)
                .check("active", r::isActive, r::setActive);
        f.load();
        VBox criteria = new VBox(8);
        for (RuleCriterion c : def.criteria()) {
            criteria.getChildren().add(new CriterionRow(c, criteria));
        }
        VBox actions = new VBox(8);
        for (RuleAction a : def.actions()) {
            actions.getChildren().add(new ActionRow(a, actions));
        }
        if (def.criteria().isEmpty()) {
            criteria.getChildren().add(new CriterionRow(new RuleCriterion(), criteria));
        }
        if (def.actions().isEmpty()) {
            actions.getChildren().add(new ActionRow(new RuleAction(), actions));
        }
        Supplier<RuleDefinition> collect = () -> {
            List<RuleCriterion> cs = new ArrayList<>();
            criteria.getChildren().forEach(n -> cs.add(((CriterionRow) n).toCriterion()));
            List<RuleAction> as = new ArrayList<>();
            actions.getChildren().forEach(n -> as.add(((ActionRow) n).toAction()));
            return new RuleDefinition(r, cs, as);
        };
        VBox criteriaPanel = Ui.panel(I18n.t("rules.criteria"), criteria,
                Ui.ghost(I18n.t("rules.addCriterion"), "plus", () -> criteria.getChildren().add(new CriterionRow(new RuleCriterion(), criteria))));
        VBox actionsPanel = Ui.panel(I18n.t("rules.actions"), actions,
                Ui.ghost(I18n.t("rules.addAction"), "plus", () -> actions.getChildren().add(new ActionRow(new RuleAction(), actions))));

        TextField sampleTitle = new TextField();
        sampleTitle.setPromptText(I18n.t("rules.sampleTitle"));
        PickerField<Long> sampleCategory = new PickerField<>(() -> Option.of(App.lookups().dropdown(DropdownKind.ITIL_CATEGORY)), true);
        PickerField<Long> sampleRequester = new PickerField<>(ItilListViewHelper::users, true);
        Label result = Ui.label("", "rule-result");
        result.setWrapText(true);
        HBox sample = Ui.row(8, sampleTitle, sampleCategory, sampleRequester, Ui.button(I18n.t("rules.test"), "play", () -> {
            if (!f.commit()) {
                return;
            }
            Ticket t = new Ticket();
            t.setTitle(sampleTitle.getText());
            t.setCategoryId(sampleCategory.getValue());
            t.setRequesterId(sampleRequester.getValue());
            t.setUrgency(3);
            t.setImpact(3);
            Async.run(() -> App.services().rules().test(collect.get(), t), (RuleOutcome o) -> result.setText(o.matchedAny()
                    ? I18n.t("rules.matched") + "\n• " + String.join("\n• ", o.changes())
                    : I18n.t("rules.notMatched")));
        }));
        HBox.setHgrow(sampleTitle, Priority.ALWAYS);
        sampleCategory.setPrefWidth(200);
        sampleRequester.setPrefWidth(200);
        VBox testPanel = Ui.panel(I18n.t("rules.tester"), new VBox(10, sample, result));

        HBox buttons = Ui.row(8, Ui.spacer());
        if (!r.isNew() && App.can(Module.RULE, Right.DELETE)) {
            buttons.getChildren().add(Ui.button(I18n.t("action.delete"), "trash-2", () -> {
                if (Dialogs.confirm(I18n.t("action.delete"), I18n.t("confirm.delete"), I18n.t("action.delete"), true)) {
                    Async.exec(() -> App.services().rules().delete(r.getId()), () -> {
                        detail.getChildren().clear();
                        onShow();
                    });
                }
            }, "ghost"));
        }
        if (canEdit) {
            buttons.getChildren().add(Ui.primary(I18n.t("action.save"), "save", () -> {
                if (!f.commit()) {
                    return;
                }
                Async.run(() -> App.services().rules().save(collect.get()), saved -> {
                    Toasts.success(I18n.t("toast.saved"));
                    onShow();
                    edit(saved);
                }, e -> {
                    if (!(e instanceof ValidationException v && f.showErrors(v))) {
                        Dialogs.error(e);
                    }
                });
            }));
        }
        VBox content = new VBox(14, Ui.panel(r.isNew() ? I18n.t("rules.new") : r.getName(), f.node()), criteriaPanel, actionsPanel,
                testPanel, buttons);
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        detail.getChildren().setAll(sp);
    }
}
