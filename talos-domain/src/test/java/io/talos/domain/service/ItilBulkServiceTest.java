package io.talos.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.talos.domain.model.core.User;
import io.talos.domain.model.itil.ItilStatus;
import io.talos.domain.model.itil.ItilType;
import io.talos.domain.model.itil.RelationType;
import io.talos.domain.model.itil.Ticket;
import io.talos.domain.model.itil.TicketType;
import io.talos.domain.service.ItilBulkService.BulkResult;
import io.talos.domain.service.ReportService.Report;
import io.talos.domain.service.ReportService.ReportKind;
import io.talos.domain.support.AccessDeniedException;
import io.talos.domain.support.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItilBulkServiceTest {

    private TestFixture f;
    private Services s;
    private User ana;
    private User joana;

    @BeforeEach
    void setUp() {
        f = TestFixture.installed(false);
        s = f.services;
        ana = f.user("ana", "Técnico");
        joana = f.user("joana", "Autoatendimento");
    }

    private Ticket ticket(String title) {
        Ticket t = s.tickets().draft();
        t.setTitle(title);
        t.setContent("Detalhes de " + title);
        t.setTicketType(TicketType.INCIDENT);
        return s.tickets().create(t);
    }

    @Test
    void bulkAssignAndStatusReportPerItemFailures() {
        Ticket a = ticket("Impressora do financeiro sem toner");
        Ticket b = ticket("VPN cai a cada dez minutos");
        BulkResult assigned = s.itilBulk().assign(ItilType.TICKET, List.of(a.getId(), b.getId(), 999_999L), ana.getId(), null);
        assertEquals(2, assigned.succeeded());
        assertEquals(1, assigned.failures().size());
        assertEquals(999_999L, assigned.failures().get(0).id());
        assertEquals(ana.getId(), s.tickets().get(a.getId()).getAssigneeId());
        assertEquals(ItilStatus.ASSIGNED, s.tickets().get(b.getId()).getStatus());

        BulkResult planned = s.itilBulk().changeStatus(ItilType.TICKET, List.of(a.getId(), b.getId()), ItilStatus.PLANNED);
        assertTrue(planned.allSucceeded());
        assertEquals(ItilStatus.PLANNED, s.tickets().get(a.getId()).getStatus());

        BulkResult solved = s.itilBulk().changeStatus(ItilType.TICKET, List.of(a.getId()), ItilStatus.SOLVED);
        assertEquals(0, solved.succeeded());
        assertEquals(1, solved.failures().size());
    }

    @Test
    void bulkUrgencyRecomputesPriorityAndBulkTrashHidesItems() {
        Ticket a = ticket("Sistema de ponto lento");
        Ticket b = ticket("Wi-Fi da sala de reunião instável");
        BulkResult r = s.itilBulk().changeUrgencyImpact(ItilType.TICKET, List.of(a.getId(), b.getId()), 5, 5);
        assertEquals(2, r.succeeded());
        assertTrue(s.tickets().get(a.getId()).getPriority() >= 5);

        BulkResult notes = s.itilBulk().addFollowup(ItilType.TICKET, List.of(a.getId(), b.getId()), "Equipe avisada", false);
        assertEquals(2, notes.succeeded());
        assertThrows(BusinessException.class,
                () -> s.itilBulk().addFollowup(ItilType.TICKET, List.of(a.getId()), "   ", false));

        assertEquals(2, s.itilBulk().trash(ItilType.TICKET, List.of(a.getId(), b.getId())).succeeded());
        assertTrue(s.tickets().search(ItilSearch.create().view(ItilSearch.View.ALL)).items().stream()
                .noneMatch(t -> t.getId().equals(a.getId()) || t.getId().equals(b.getId())));
        assertEquals(2, s.itilBulk().restore(ItilType.TICKET, List.of(a.getId(), b.getId())).succeeded());
        assertFalse(s.tickets().get(a.getId()).isDeleted());
    }

    @Test
    void selfServiceCannotRunBulkActions() {
        Ticket a = ticket("Monitor piscando");
        f.loginAs("joana");
        assertThrows(AccessDeniedException.class,
                () -> s.itilBulk().changeStatus(ItilType.TICKET, List.of(a.getId()), ItilStatus.PLANNED));
        assertThrows(AccessDeniedException.class, () -> s.itilBulk().trash(ItilType.TICKET, List.of(a.getId())));
    }

    @Test
    void mergeCopiesHistoryAndClosesDuplicates() {
        f.loginAs("joana");
        Ticket target = ticket("Outlook não abre no notebook");
        f.loginAdmin();
        Ticket dup = ticket("Outlook não abre");
        s.itilActivity().addFollowup(ItilType.TICKET, dup.getId(), "Erro 0x800 ao iniciar", false);
        s.itilActivity().addFollowup(ItilType.TICKET, dup.getId(), "Nota interna", true);

        int merged = s.itilBulk().merge(ItilType.TICKET, target.getId(), List.of(dup.getId(), target.getId()), null);
        assertEquals(1, merged);

        Ticket closed = s.tickets().get(dup.getId());
        assertEquals(ItilStatus.CLOSED, closed.getStatus());
        assertTrue(s.itilActivity().related(ItilType.TICKET, target.getId()).stream()
                .anyMatch(r -> r.relationType() == RelationType.DUPLICATES && r.id().equals(dup.getId())));
        List<TimelineEntry> timeline = s.itilActivity().timeline(ItilType.TICKET, target.getId());
        assertTrue(timeline.stream().anyMatch(e -> e.content() != null && e.content().contains("Erro 0x800")));
        assertTrue(timeline.stream().anyMatch(e -> e.content() != null && e.content().contains(dup.reference())));
        assertTrue(s.tickets().people(target.getId()).followers().contains(f.services.security().current().userId()));

        assertThrows(BusinessException.class,
                () -> s.itilBulk().merge(ItilType.TICKET, target.getId(), List.of(dup.getId()), null));
        assertThrows(BusinessException.class,
                () -> s.itilBulk().merge(ItilType.TICKET, target.getId(), List.of(), null));
    }

    @Test
    void bulkAssignKeepsTheOtherFieldAndMergeRejectsTrashedItems() {
        io.talos.domain.model.core.UserGroup g = new io.talos.domain.model.core.UserGroup();
        g.setName("Infraestrutura");
        g.setOrgUnitId(s.orgUnits().root().getId());
        Long group = s.groups().save(g).getId();
        Ticket a = ticket("Backup noturno falhou");
        s.tickets().assign(a.getId(), null, group);
        s.itilBulk().assign(ItilType.TICKET, List.of(a.getId()), ana.getId(), null);
        assertEquals(group, s.tickets().get(a.getId()).getAssigneeGroupId());
        assertEquals(ana.getId(), s.tickets().get(a.getId()).getAssigneeId());
        assertEquals(0, s.itilBulk().assign(ItilType.TICKET, List.of(a.getId()), null, null).total());

        Ticket b = ticket("Backup noturno com erro");
        s.tickets().trash(b.getId());
        assertThrows(BusinessException.class, () -> s.itilBulk().merge(ItilType.TICKET, a.getId(), List.of(b.getId()), null));
        assertThrows(BusinessException.class, () -> s.itilBulk().merge(ItilType.TICKET, b.getId(), List.of(a.getId()), null));
    }

    @Test
    void mergeMovesTasksAndTimeToTheMainItem() {
        Ticket main = ticket("Servidor de arquivos lento");
        Ticket dup = ticket("Servidor de arquivos travando");
        io.talos.domain.model.itil.ItilTask task = new io.talos.domain.model.itil.ItilTask();
        task.setItemType(ItilType.TICKET);
        task.setItemId(dup.getId());
        task.setContent("Verificar disco");
        task.setDurationMinutes(45);
        s.itilActivity().saveTask(task);
        assertEquals(45, s.tickets().get(dup.getId()).getActionTimeMinutes());
        s.itilBulk().merge(ItilType.TICKET, main.getId(), List.of(dup.getId()), null);
        assertEquals(1, s.itilActivity().tasks(ItilType.TICKET, main.getId()).size());
        assertTrue(s.itilActivity().tasks(ItilType.TICKET, dup.getId()).isEmpty());
        assertEquals(45, s.tickets().get(main.getId()).getActionTimeMinutes());
        assertEquals(0, s.tickets().get(dup.getId()).getActionTimeMinutes());
    }

    @Test
    void selfServiceDuplicateBelongsToWhoCreatedIt() {
        f.loginAs("joana");
        Ticket mine = ticket("Mouse sem fio parou");
        Ticket copy = s.tickets().duplicate(mine.getId());
        assertEquals(joana.getId(), copy.getRequesterId());
        assertEquals(copy.getId(), s.tickets().get(copy.getId()).getId());
    }

    @Test
    void duplicateCreatesFreshCopyLinkedToTheOriginal() {
        Ticket original = ticket("Instalar o pacote Office");
        s.tickets().assign(original.getId(), ana.getId(), null);
        Ticket copy = s.tickets().duplicate(original.getId());
        assertNotEquals(original.getId(), copy.getId());
        assertEquals(original.getTitle(), copy.getTitle());
        assertEquals(ItilStatus.NEW, copy.getStatus());
        assertNull(copy.getAssigneeId());
        assertEquals(TicketType.INCIDENT, copy.getTicketType());
        assertTrue(s.itilActivity().related(ItilType.TICKET, copy.getId()).stream()
                .anyMatch(r -> r.id().equals(original.getId())));
    }

    @Test
    void similarFindsOpenItemsWithMatchingWords() {
        ticket("Impressora do RH não imprime");
        ticket("Troca de senha do e-mail");
        Ticket solved = ticket("Impressora do RH atolando papel");
        s.tickets().trash(solved.getId());
        List<ItilBulkService.Similar> found = s.itilBulk().similar(ItilType.TICKET, "impressora rh nao imprime", null, 5);
        assertEquals(1, found.size());
        assertTrue(found.get(0).title().startsWith("Impressora do RH não"));
        assertTrue(s.itilBulk().similar(ItilType.TICKET, "a de", null, 5).isEmpty());
    }

    @Test
    void newReportsRun() {
        Ticket a = ticket("Teclado sem resposta");
        s.tickets().takeIntoAccount(a.getId());
        f.clock.advance(java.time.Duration.ofHours(30));
        ticket("Mouse com duplo clique");
        Report aging = s.reports().run(ReportKind.BACKLOG_AGING, null, null);
        assertEquals(5, aging.rows().size());
        assertEquals("1", aging.rows().get(0).get(1));
        assertEquals("1", aging.rows().get(1).get(1));
        Report response = s.reports().run(ReportKind.FIRST_RESPONSE, null, null);
        assertTrue(response.rows().stream().anyMatch(row -> row.get(1).equals("1")));
        Report requesters = s.reports().run(ReportKind.TOP_REQUESTERS, null, null);
        assertEquals("2", requesters.rows().get(0).get(1));
    }
}
