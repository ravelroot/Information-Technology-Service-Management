package io.pyhoma.domain.rules;

import io.pyhoma.domain.itil.PriorityMatrix;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.model.itil.TicketType;
import io.pyhoma.domain.model.rules.RuleAction;
import io.pyhoma.domain.model.rules.RuleCriterion;
import io.pyhoma.domain.model.rules.RuleField;
import io.pyhoma.domain.support.Texts;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class RuleEngine {

    private RuleEngine() {
    }

    public static RuleOutcome run(List<RuleDefinition> rules, Ticket ticket, Set<Long> requesterGroups) {
        RuleOutcome outcome = new RuleOutcome();
        for (RuleDefinition def : rules) {
            if (!def.rule().isActive() || def.criteria().isEmpty()) {
                continue;
            }
            if (matches(def, ticket, requesterGroups)) {
                outcome.appliedRules().add(def.rule().getName());
                for (RuleAction action : def.actions()) {
                    apply(action, ticket, outcome);
                }
                if (def.rule().isStopProcessing()) {
                    break;
                }
            }
        }
        if (!outcome.prioritySet() && outcome.matchedAny()) {
            ticket.setPriority(PriorityMatrix.compute(ticket.getUrgency(), ticket.getImpact()));
        }
        return outcome;
    }

    public static boolean matches(RuleDefinition def, Ticket ticket, Set<Long> requesterGroups) {
        boolean all = def.rule().isMatchAll();
        for (RuleCriterion c : def.criteria()) {
            boolean ok = matches(c, ticket, requesterGroups);
            if (all && !ok) {
                return false;
            }
            if (!all && ok) {
                return true;
            }
        }
        return all;
    }

    static boolean matches(RuleCriterion c, Ticket t, Set<Long> requesterGroups) {
        if (c.getField() == RuleField.REQUESTER_GROUP) {
            return matchesCollection(c, requesterGroups);
        }
        Object value = value(c.getField(), t);
        return compare(c, value);
    }

    private static boolean matchesCollection(RuleCriterion c, Collection<Long> values) {
        return switch (c.getMatchOperator()) {
            case IS_NOT -> values.stream().noneMatch(v -> compare(withOp(c), v));
            case IS_EMPTY -> values.isEmpty();
            default -> values.stream().anyMatch(v -> compare(c, v));
        };
    }

    private static RuleCriterion withOp(RuleCriterion c) {
        RuleCriterion copy = new RuleCriterion();
        copy.setField(c.getField());
        copy.setPattern(c.getPattern());
        copy.setMatchOperator(io.pyhoma.domain.model.rules.RuleOperator.IS);
        return copy;
    }

    private static Object value(RuleField field, Ticket t) {
        return switch (field) {
            case TITLE -> t.getTitle();
            case CONTENT -> t.getContent();
            case TICKET_TYPE -> t.getTicketType() == null ? null : t.getTicketType().name();
            case URGENCY -> t.getUrgency();
            case IMPACT -> t.getImpact();
            case PRIORITY -> t.getPriority();
            case CATEGORY -> t.getCategoryId();
            case LOCATION -> t.getLocationId();
            case REQUESTER -> t.getRequesterId();
            case ORG_UNIT -> t.getOrgUnitId();
            case SOURCE -> t.getSourceId();
            case REQUESTER_GROUP -> null;
        };
    }

    private static boolean compare(RuleCriterion c, Object value) {
        String pattern = c.getPattern() == null ? "" : c.getPattern().trim();
        String text = value == null ? "" : value.toString();
        String a = Texts.fold(text);
        String b = Texts.fold(pattern);
        return switch (c.getMatchOperator()) {
            case IS -> value != null && a.equals(b);
            case IS_NOT -> value == null || !a.equals(b);
            case CONTAINS -> !b.isEmpty() && a.contains(b);
            case NOT_CONTAINS -> b.isEmpty() || !a.contains(b);
            case STARTS_WITH -> !b.isEmpty() && a.startsWith(b);
            case ENDS_WITH -> !b.isEmpty() && a.endsWith(b);
            case REGEX -> regex(pattern, text);
            case GREATER_THAN -> numeric(value) != null && numeric(pattern) != null && numeric(value) > numeric(pattern);
            case LESS_THAN -> numeric(value) != null && numeric(pattern) != null && numeric(value) < numeric(pattern);
            case IS_EMPTY -> text.isBlank();
        };
    }

    public static final int MAX_PATTERN_LENGTH = 300;
    private static final int MAX_INPUT_LENGTH = 20_000;
    private static final long REGEX_BUDGET_NANOS = 50_000_000L;

    static boolean regex(String pattern, String text) {
        if (pattern.length() > MAX_PATTERN_LENGTH) {
            return false;
        }
        try {
            CharSequence input = new BudgetedText(text.length() > MAX_INPUT_LENGTH ? text.substring(0, MAX_INPUT_LENGTH) : text,
                    System.nanoTime() + REGEX_BUDGET_NANOS);
            return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(input).find();
        } catch (PatternSyntaxException | RegexTimeout e) {
            return false;
        }
    }

    private static final class RegexTimeout extends RuntimeException {
        private static final long serialVersionUID = 1L;

        RegexTimeout() {
            super(null, null, false, false);
        }
    }

    private static final class BudgetedText implements CharSequence {
        private final String text;
        private final long deadline;
        private int reads;

        BudgetedText(String text, long deadline) {
            this.text = text;
            this.deadline = deadline;
        }

        @Override
        public char charAt(int index) {
            if ((++reads & 0x3FF) == 0 && System.nanoTime() > deadline) {
                throw new RegexTimeout();
            }
            return text.charAt(index);
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new BudgetedText(text.substring(start, end), deadline);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private static Double numeric(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return Double.parseDouble(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void apply(RuleAction action, Ticket t, RuleOutcome outcome) {
        String v = action.getActionValue() == null ? "" : action.getActionValue().trim();
        Long id = parseLong(v);
        Integer level = id == null ? null : (int) Math.max(1, Math.min(6, id));
        switch (action.getActionType()) {
            case ASSIGN_USER -> t.setAssigneeId(id);
            case ASSIGN_GROUP -> t.setAssigneeGroupId(id);
            case SET_CATEGORY -> t.setCategoryId(id);
            case SET_LOCATION -> t.setLocationId(id);
            case SET_TICKET_TYPE -> {
                try {
                    t.setTicketType(TicketType.valueOf(v));
                } catch (IllegalArgumentException ignored) {
                    return;
                }
            }
            case SET_URGENCY -> t.setUrgency(level == null ? t.getUrgency() : Math.min(5, level));
            case SET_IMPACT -> t.setImpact(level == null ? t.getImpact() : Math.min(5, level));
            case SET_PRIORITY -> {
                if (level != null) {
                    t.setPriority(level);
                    outcome.markPrioritySet();
                }
            }
            case SET_SLA_TTO -> t.setSlaTtoId(id);
            case SET_SLA_TTR -> t.setSlaTtrId(id);
            case ADD_OBSERVER -> {
                if (id != null) {
                    outcome.observers().add(id);
                }
            }
            case REQUEST_APPROVAL -> {
                if (id != null) {
                    outcome.approvers().add(id);
                }
            }
        }
        outcome.changes().add(action.getActionType().name() + " = " + v);
    }

    private static Long parseLong(String v) {
        try {
            return v.isEmpty() ? null : Long.valueOf(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
