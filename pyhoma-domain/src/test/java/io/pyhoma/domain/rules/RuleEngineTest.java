package io.pyhoma.domain.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.model.itil.TicketType;
import io.pyhoma.domain.model.rules.Rule;
import io.pyhoma.domain.model.rules.RuleAction;
import io.pyhoma.domain.model.rules.RuleActionType;
import io.pyhoma.domain.model.rules.RuleCriterion;
import io.pyhoma.domain.model.rules.RuleField;
import io.pyhoma.domain.model.rules.RuleOperator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RuleEngineTest {

    private static Rule rule(String name, boolean all, boolean stop) {
        Rule r = new Rule();
        r.setName(name);
        r.setMatchAll(all);
        r.setStopProcessing(stop);
        r.setActive(true);
        return r;
    }

    private static RuleCriterion c(RuleField f, RuleOperator op, String pattern) {
        RuleCriterion c = new RuleCriterion();
        c.setField(f);
        c.setMatchOperator(op);
        c.setPattern(pattern);
        return c;
    }

    private static RuleAction a(RuleActionType t, String v) {
        RuleAction a = new RuleAction();
        a.setActionType(t);
        a.setActionValue(v);
        return a;
    }

    private static Ticket ticket(String title) {
        Ticket t = new Ticket();
        t.setTitle(title);
        t.setUrgency(2);
        t.setImpact(2);
        t.setTicketType(TicketType.INCIDENT);
        return t;
    }

    @Test
    void regexCriterionAssignsGroupAndRecomputesPriority() {
        RuleDefinition printers = new RuleDefinition(rule("printers", true, false),
                List.of(c(RuleField.TITLE, RuleOperator.REGEX, "impressora|toner")),
                List.of(a(RuleActionType.ASSIGN_GROUP, "7"), a(RuleActionType.SET_URGENCY, "5")));
        Ticket t = ticket("Impressora sem TONER");
        RuleOutcome outcome = RuleEngine.run(List.of(printers), t, Set.of());
        assertEquals(List.of("printers"), outcome.appliedRules());
        assertEquals(7L, t.getAssigneeGroupId());
        assertEquals(5, t.getUrgency());
        assertEquals(3, t.getPriority(), "urgency 5 x impact 2");
    }

    @Test
    void stopProcessingEndsTheRun() {
        RuleDefinition first = new RuleDefinition(rule("first", true, true),
                List.of(c(RuleField.TITLE, RuleOperator.CONTAINS, "vpn")), List.of(a(RuleActionType.ASSIGN_USER, "1")));
        RuleDefinition second = new RuleDefinition(rule("second", true, false),
                List.of(c(RuleField.TITLE, RuleOperator.CONTAINS, "vpn")), List.of(a(RuleActionType.ASSIGN_USER, "2")));
        Ticket t = ticket("VPN caiu");
        RuleEngine.run(List.of(first, second), t, Set.of());
        assertEquals(1L, t.getAssigneeId());
    }

    @Test
    void anyModeAndGroupCriterion() {
        RuleDefinition def = new RuleDefinition(rule("any", false, false),
                List.of(c(RuleField.TITLE, RuleOperator.STARTS_WITH, "zzz"), c(RuleField.REQUESTER_GROUP, RuleOperator.IS, "9")),
                List.of(a(RuleActionType.ADD_OBSERVER, "4"), a(RuleActionType.SET_PRIORITY, "6")));
        Ticket t = ticket("Erro");
        RuleOutcome outcome = RuleEngine.run(List.of(def), t, Set.of(3L, 9L));
        assertTrue(outcome.observers().contains(4L));
        assertEquals(6, t.getPriority(), "explicit priority wins");
    }

    @Test
    void noMatchLeavesTicketUntouched() {
        RuleDefinition def = new RuleDefinition(rule("none", true, false),
                List.of(c(RuleField.URGENCY, RuleOperator.GREATER_THAN, "3")), List.of(a(RuleActionType.ASSIGN_GROUP, "1")));
        Ticket t = ticket("Nada");
        RuleEngine.run(List.of(def), t, Set.of());
        assertNull(t.getAssigneeGroupId());
        assertEquals(3, t.getPriority());
    }

    @org.junit.jupiter.api.Test
    void catastrophicRegexIsCutShort() {
        long start = System.nanoTime();
        boolean matched = RuleEngine.regex("(a+)+$", "a".repeat(40) + "!");
        long millis = (System.nanoTime() - start) / 1_000_000;
        org.junit.jupiter.api.Assertions.assertFalse(matched);
        org.junit.jupiter.api.Assertions.assertTrue(millis < 2000, "took " + millis + " ms");
        org.junit.jupiter.api.Assertions.assertTrue(RuleEngine.regex("impressora|printer", "A impressora parou"));
    }
}
