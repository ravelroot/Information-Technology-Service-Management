package io.pyhoma.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.pyhoma.domain.model.core.DropdownItem;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.model.itil.TicketType;
import io.pyhoma.domain.support.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketPeopleTest {

    private TestFixture f;
    private Services s;
    private User ana;
    private User bruno;
    private User joana;
    private User carla;

    @BeforeEach
    void setUp() {
        f = TestFixture.installed(false);
        s = f.services;
        ana = f.user("ana", "Técnico");
        bruno = f.user("bruno", "Técnico");
        joana = f.user("joana", "Autoatendimento");
        carla = f.user("carla", "Autoatendimento");
    }

    private Ticket draft(String title) {
        Ticket t = s.tickets().draft();
        t.setTitle(title);
        t.setTicketType(TicketType.REQUEST);
        t.setContent("Explicação do chamado");
        return t;
    }

    @Test
    void anyoneCanAssignSeveralPeopleAndFollowers() {
        f.loginAs("joana");
        Ticket created = s.tickets().createWithPeople(draft("Acesso ao sistema financeiro"), ItilStatus.NEW,
                List.of(ana.getId(), bruno.getId()), List.of(carla.getId(), ana.getId()));
        assertEquals(ana.getId(), created.getAssigneeId());
        assertEquals(ItilStatus.ASSIGNED, created.getStatus());
        ItilPeople people = s.tickets().people(created.getId());
        assertEquals(List.of(ana.getId(), bruno.getId()), people.assignees());
        assertEquals(List.of(carla.getId()), people.followers());

        f.loginAs("bruno");
        assertTrue(s.tickets().search(ItilSearch.create().view(ItilSearch.View.MINE_ASSIGNED)).items().stream()
                .anyMatch(t -> t.getId().equals(created.getId())));
        assertEquals(List.of(ana.getId(), bruno.getId()),
                s.tickets().cards(List.of(s.tickets().get(created.getId()))).get(created.getId()).assignees());

        f.loginAs("carla");
        assertEquals(created.getId(), s.tickets().get(created.getId()).getId());
        assertTrue(s.itilActivity().observers(ItilType.TICKET, created.getId()).stream()
                .allMatch(o -> o.getUserId().equals(carla.getId())));
    }

    @Test
    void requesterCanChangeThePeopleOfItsTicket() {
        f.loginAs("joana");
        Ticket created = s.tickets().createWithPeople(draft("Notebook não liga"), ItilStatus.NEW, List.of(), List.of());
        assertNull(created.getAssigneeId());
        ItilPeople after = s.tickets().setPeople(created.getId(), List.of(bruno.getId(), ana.getId()), List.of(carla.getId()));
        assertEquals(List.of(bruno.getId(), ana.getId()), after.assignees());
        assertEquals(bruno.getId(), s.tickets().get(created.getId()).getAssigneeId());
        assertEquals(ItilStatus.ASSIGNED, s.tickets().get(created.getId()).getStatus());

        ItilPeople cleared = s.tickets().setPeople(created.getId(), List.of(ana.getId()), List.of());
        assertEquals(List.of(ana.getId()), cleared.assignees());
        assertEquals(List.of(), cleared.followers());
    }

    @Test
    void inactiveOrUnknownPeopleAreRejected() {
        f.loginAs("joana");
        assertThrows(BusinessException.class, () -> s.tickets().createWithPeople(draft("Teste"), ItilStatus.NEW,
                List.of(999_999L), List.of()));
    }

    @Test
    void adminCreatesAndDeletesTicketCategories() {
        f.loginAdmin();
        DropdownItem parent = new DropdownItem();
        parent.setKind(DropdownKind.ITIL_CATEGORY);
        parent.setName("Categoria de teste");
        parent = s.dropdowns().save(parent);
        DropdownItem child = new DropdownItem();
        child.setKind(DropdownKind.ITIL_CATEGORY);
        child.setName("Subcategoria de teste");
        child.setParentId(parent.getId());
        child = s.dropdowns().save(child);

        Ticket t = draft("Ramal mudo");
        t.setCategoryId(child.getId());
        Ticket created = s.tickets().create(t);
        assertEquals(1, s.dropdowns().categoryUsage(parent.getId()));

        s.dropdowns().delete(parent.getId());
        assertNull(s.tickets().get(created.getId()).getCategoryId());
        assertTrue(s.dropdowns().list(DropdownKind.ITIL_CATEGORY, false).stream()
                .noneMatch(d -> d.getName().equals("Categoria de teste") || d.getName().equals("Subcategoria de teste")));

        f.loginAs("joana");
        Long id = s.dropdowns().list(DropdownKind.ITIL_CATEGORY, false).get(0).getId();
        assertThrows(BusinessException.class, () -> s.dropdowns().delete(id));
    }
}
