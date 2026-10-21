package io.pyhoma.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.pyhoma.domain.itil.SlaState;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.itil.ApprovalStatus;
import io.pyhoma.domain.model.itil.Change;
import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.ItilTask;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.support.BusinessException;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketLifecycleTest {

    private TestFixture f;
    private Services s;
    private User tech;
    private User requester;

    @BeforeEach
    void setUp() {
        f = TestFixture.installed(false);
        s = f.services;
        tech = f.user("tech", "Técnico");
        requester = f.user("joana", "Autoatendimento");
    }

    private Ticket openAsRequester(String title, int urgency) {
        f.loginAs("joana");
        Ticket t = s.tickets().draft();
        t.setTitle(title);
        t.setContent("details");
        t.setUrgency(urgency);
        t.setImpact(3);
        return s.tickets().create(t);
    }

    @Test
    void kanbanQuickCreateLandsInTheChosenColumn() {
        f.loginAs("tech");
        Ticket t = s.tickets().draft();
        t.setTitle("Impressora do 2º andar sem toner");
        Ticket planned = s.tickets().createInStatus(t, ItilStatus.PLANNED);
        assertEquals(ItilStatus.PLANNED, planned.getStatus());
        assertTrue(s.tickets().board(io.pyhoma.domain.service.ItilSearch.create()).get(ItilStatus.PLANNED).stream()
                .anyMatch(i -> i.getId().equals(planned.getId())));

        Ticket solved = s.tickets().draft();
        solved.setTitle("Não pode nascer solucionado");
        assertEquals(ItilStatus.NEW, s.tickets().createInStatus(solved, ItilStatus.SOLVED).getStatus());

        Ticket mine = openAsRequester("Mouse quebrado", 3);
        Ticket viaBoard = s.tickets().draft();
        viaBoard.setTitle("Teclado com teclas presas");
        assertEquals(ItilStatus.NEW, s.tickets().createInStatus(viaBoard, ItilStatus.WAITING).getStatus());
        assertNotNull(mine.getId());
    }

    @Test
    void fullLifecycleWithSlaAndSatisfaction() {
        Ticket t = openAsRequester("Notebook não liga", 5);
        assertEquals(ItilStatus.NEW, t.getStatus());
        assertEquals(4, t.getPriority());
        assertNotNull(t.getDueAt(), "TTR SLA applied from priority");
        assertNotNull(t.getTtoDueAt(), "generic TTO applied");
        assertEquals(LocalDateTime.of(2026, 3, 2, 17, 0), t.getDueAt(), "8 business hours from Monday 09:00");

        f.loginAs("tech");
        t = s.tickets().takeIntoAccount(t.getId());
        assertEquals(ItilStatus.ASSIGNED, t.getStatus());
        assertEquals(tech.getId(), t.getAssigneeId());
        assertNotNull(t.getTakenAt());
        assertEquals(SlaState.MET, s.tickets().ttoState(t));

        ItilTask task = new ItilTask();
        task.setItemType(ItilType.TICKET);
        task.setItemId(t.getId());
        task.setContent("Diagnosis");
        task.setDurationMinutes(45);
        s.itilActivity().saveTask(task);
        t = s.tickets().get(t.getId());
        assertEquals(45, t.getActionTimeMinutes());

        s.itilActivity().proposeSolution(ItilType.TICKET, t.getId(), "Battery replaced", null);
        t = s.tickets().get(t.getId());
        assertEquals(ItilStatus.SOLVED, t.getStatus());
        assertNotNull(t.getSolvedAt());

        f.loginAs("joana");
        assertTrue(s.notifications().unreadCount() > 0, "requester was notified");
        s.itilActivity().acceptSolution(ItilType.TICKET, t.getId());
        t = s.tickets().get(t.getId());
        assertEquals(ItilStatus.CLOSED, t.getStatus());
        t = s.tickets().rate(t.getId(), 5, "Great");
        assertEquals(5, t.getSatisfaction());
        assertTrue(s.itilActivity().timeline(ItilType.TICKET, t.getId()).size() >= 4);
    }

    @Test
    void refusedSolutionReopensTicket() {
        Ticket t = openAsRequester("VPN", 3);
        f.loginAs("tech");
        s.tickets().takeIntoAccount(t.getId());
        s.itilActivity().proposeSolution(ItilType.TICKET, t.getId(), "Reboot", null);
        f.loginAs("joana");
        s.itilActivity().refuseSolution(ItilType.TICKET, t.getId(), "Still broken");
        t = s.tickets().get(t.getId());
        assertEquals(ItilStatus.ASSIGNED, t.getStatus());
        assertNull(t.getSolvedAt());
    }

    @Test
    void solvedStatusRequiresSolution() {
        Ticket t = openAsRequester("Mouse", 2);
        f.loginAs("tech");
        Long id = t.getId();
        assertThrows(BusinessException.class, () -> s.tickets().changeStatus(id, ItilStatus.SOLVED));
    }

    @Test
    void waitingPausesTheResolutionClock() {
        Ticket t = openAsRequester("ERP", 5);
        LocalDateTime due = t.getDueAt();
        f.loginAs("tech");
        s.tickets().takeIntoAccount(t.getId());
        s.tickets().changeStatus(t.getId(), ItilStatus.WAITING);
        f.clock.advance(Duration.ofHours(3));
        t = s.tickets().changeStatus(t.getId(), ItilStatus.ASSIGNED);
        assertEquals(LocalDateTime.of(2026, 3, 2, 17, 0), due);
        assertEquals(LocalDateTime.of(2026, 3, 3, 10, 0), t.getDueAt(), "3 business hours pushed to the next day");
        assertEquals(180, t.getWaitingMinutes());
    }

    @Test
    void slaMonitorWarnsThenBreaches() {
        Ticket t = openAsRequester("Server down", 5);
        f.loginAs("tech");
        s.tickets().takeIntoAccount(t.getId());
        f.clock.advance(Duration.ofHours(7));
        s.jobs().runFrequent();
        assertTrue(s.tickets().get(t.getId()).isSlaWarned());
        f.clock.advance(Duration.ofHours(2));
        assertEquals(SlaState.BREACHED, s.tickets().ttrState(s.tickets().get(t.getId())));
        s.jobs().runFrequent();
        assertTrue(s.tickets().get(t.getId()).isSlaBreachNotified());
    }

    @Test
    void selfServiceSeesOnlyOwnTickets() {
        openAsRequester("Mine", 3);
        f.loginAdmin();
        Ticket other = s.tickets().draft();
        other.setTitle("Admin ticket");
        s.tickets().create(other);
        f.loginAs("joana");
        var page = s.tickets().search(ItilSearch.create().view(ItilSearch.View.ALL));
        assertEquals(1, page.total());
        assertEquals("Mine", page.items().get(0).getTitle());
        assertThrows(BusinessException.class, () -> s.tickets().get(other.getId() == null ? 999L : other.getId()));
    }

    @Test
    void changeApprovalDrivesWorkflow() {
        f.loginAdmin();
        Change c = s.changes().draft();
        c.setTitle("Upgrade firewall");
        c = s.changes().create(c);
        s.itilActivity().requestApproval(ItilType.CHANGE, c.getId(), tech.getId(), "please");
        c = s.changes().get(c.getId());
        assertEquals(ItilStatus.APPROVAL, c.getStatus());
        assertEquals(ApprovalStatus.WAITING, c.getApprovalStatus());
        Long changeId = c.getId();
        assertThrows(BusinessException.class, () -> s.changes().changeStatus(changeId, ItilStatus.APPLIED));
        f.loginAs("tech");
        var approval = s.itilActivity().myPendingApprovals().get(0);
        s.itilActivity().answerApproval(approval.getId(), true, "ok");
        f.loginAdmin();
        c = s.changes().get(c.getId());
        assertEquals(ItilStatus.ACCEPTED, c.getStatus());
        assertEquals(ApprovalStatus.ACCEPTED, c.getApprovalStatus());
    }

    @Test
    void businessRuleAssignsGroupOnCreation() {
        f.loginAdmin();
        var group = new io.pyhoma.domain.model.core.UserGroup();
        group.setName("Infra");
        group.setOrgUnitId(s.orgUnits().root().getId());
        group = s.groups().save(group);
        var rule = new io.pyhoma.domain.model.rules.Rule();
        rule.setName("printers");
        var c = new io.pyhoma.domain.model.rules.RuleCriterion();
        c.setField(io.pyhoma.domain.model.rules.RuleField.TITLE);
        c.setMatchOperator(io.pyhoma.domain.model.rules.RuleOperator.CONTAINS);
        c.setPattern("impressora");
        var a = new io.pyhoma.domain.model.rules.RuleAction();
        a.setActionType(io.pyhoma.domain.model.rules.RuleActionType.ASSIGN_GROUP);
        a.setActionValue(String.valueOf(group.getId()));
        s.rules().save(new io.pyhoma.domain.rules.RuleDefinition(rule, java.util.List.of(c), java.util.List.of(a)));
        Ticket t = openAsRequester("Impressora travada", 3);
        assertEquals(group.getId(), t.getAssigneeGroupId());
        assertEquals(ItilStatus.ASSIGNED, t.getStatus());
    }
}
