package io.talos.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.talos.domain.model.core.User;
import io.talos.domain.model.itil.ApprovalStatus;
import io.talos.domain.model.itil.Change;
import io.talos.domain.model.itil.ItilStatus;
import io.talos.domain.model.itil.ItilType;
import io.talos.domain.model.itil.Ticket;
import io.talos.domain.model.itil.TicketType;
import io.talos.domain.support.BusinessException;
import io.talos.domain.support.Csv;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegressionFixesTest {

    private TestFixture f;
    private Services s;
    private User tech;
    private User joana;
    private User carla;

    @BeforeEach
    void setUp() {
        f = TestFixture.installed(false);
        s = f.services;
        tech = f.user("tech", "Técnico");
        joana = f.user("joana", "Autoatendimento");
        carla = f.user("carla", "Autoatendimento");
    }

    @Test
    void csvKeepsQuotesInsideUnquotedCellsAndRejectsImpossibleDates() {
        List<List<String>> rows = Csv.parse("Monitor 24\" Dell;SN1\nMouse;SN2\n");
        assertEquals(2, rows.size());
        assertEquals("Monitor 24\" Dell", rows.get(0).get(0));
        assertEquals("SN2", rows.get(1).get(1));
        assertThrows(BusinessException.class, () -> Csv.toDate("31/02/2026"));
        assertEquals(LocalDate.of(2028, 2, 29), Csv.toDate("29/02/2028"));
    }

    @Test
    void searchWithHugeNumberDoesNotCrash() {
        assertDoesNotThrow(() -> s.tickets().search(ItilSearch.create().view(ItilSearch.View.ALL).text("12345678901234567890")));
    }

    @Test
    void followerCannotRewriteThePeopleOfSomeoneElsesTicket() {
        f.loginAs("joana");
        Ticket t = s.tickets().draft();
        t.setTitle("Cadeira quebrada");
        t.setContent("Sala 3");
        t.setTicketType(TicketType.REQUEST);
        Ticket created = s.tickets().createWithPeople(t, ItilStatus.NEW, List.of(tech.getId()), List.of(carla.getId()));
        f.loginAs("carla");
        assertThrows(BusinessException.class, () -> s.tickets().setPeople(created.getId(), List.of(), List.of()));
        f.loginAs("tech");
        assertEquals(List.of(tech.getId()), s.tickets().setPeople(created.getId(), List.of(tech.getId()), List.of()).assignees());
    }

    @Test
    void changeCanBeApprovedAfterAnEarlierRefusal() {
        f.loginAdmin();
        Change c = s.changes().draft();
        c.setTitle("Trocar o switch do 2º andar");
        Change change = s.changes().create(c);
        s.itilActivity().requestApproval(ItilType.CHANGE, change.getId(), tech.getId(), "primeira");
        f.loginAs("tech");
        s.itilActivity().answerApproval(s.itilActivity().myPendingApprovals().get(0).getId(), false, "falta o plano");
        f.loginAdmin();
        assertEquals(ApprovalStatus.REFUSED, s.changes().get(change.getId()).getApprovalStatus());
        s.itilActivity().requestApproval(ItilType.CHANGE, change.getId(), tech.getId(), "segunda");
        f.loginAs("tech");
        s.itilActivity().answerApproval(s.itilActivity().myPendingApprovals().get(0).getId(), true, "ok");
        f.loginAdmin();
        assertEquals(ApprovalStatus.ACCEPTED, s.changes().get(change.getId()).getApprovalStatus());
    }

    @Test
    void savingCannotMoveItemsToTheTrashWithoutTheDeleteRight() {
        f.loginAs("tech");
        Ticket t = s.tickets().draft();
        t.setTitle("Notebook lento");
        t.setTicketType(TicketType.INCIDENT);
        Ticket created = s.tickets().create(t);
        created.setDeleted(true);
        s.tickets().save(created);
        assertTrue(s.tickets().search(ItilSearch.create().view(ItilSearch.View.ALL)).items().stream()
                .anyMatch(x -> x.getId().equals(created.getId())));
    }
}
