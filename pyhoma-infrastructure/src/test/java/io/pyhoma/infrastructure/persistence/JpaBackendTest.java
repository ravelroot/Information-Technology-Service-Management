package io.pyhoma.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.port.BackendProvider;
import io.pyhoma.domain.port.DatabaseSettings;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.service.ItilSearch;
import io.pyhoma.domain.service.ReportService;
import io.pyhoma.domain.service.Services;
import io.pyhoma.domain.support.ConcurrentUpdateException;
import io.pyhoma.infrastructure.boot.SpringBackendProvider;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JpaBackendTest {

    private Path folder;

    private BackendProvider.Backend backend;
    private Services services;
    private DatabaseSettings settings;

    @BeforeAll
    void start() throws Exception {
        folder = java.nio.file.Files.createTempDirectory("pyhoma-jpa-test");
        settings = DatabaseSettings.embedded(folder);
        assertTrue(new SpringBackendProvider().testConnection(settings).startsWith("H2"));
        backend = BackendTestSupport.start(settings, folder);
        services = BackendTestSupport.installDemo(backend);
    }

    @AfterAll
    void stop() {
        if (backend != null) {
            backend.close();
        }
    }

    @Test
    void genericFiltersRunOnTheDatabase() {
        var repo = backend.dataStore().repository(Asset.class);
        long total = repo.count(Filter.isFalse("deleted"));
        assertTrue(total > 40);
        assertFalse(repo.find(Query.where(Filter.like("name", "nb-")).orderBy(Sort.desc("name")).limit(5)).isEmpty());
        assertEquals(0, repo.count(Filter.in("id", List.of())));
        assertTrue(repo.countBy("assetType", Filter.ALL).size() > 3);
        assertEquals(0, repo.count(Filter.like("name", "100%_x")), "LIKE wildcards are escaped");
        var page = repo.page(Query.all().orderBy(Sort.asc("id")).page(2, 10));
        assertEquals(10, page.items().size());
        assertEquals(total, page.total());
    }

    @Test
    void ticketWorkflowPersists() {
        Ticket t = services.tickets().draft();
        t.setTitle("Impressora travando papel");
        t.setUrgency(4);
        t = services.tickets().create(t);
        assertNotNull(t.getId());
        assertEquals(ItilStatus.ASSIGNED, t.getStatus(), "demo rule assigns printers to infrastructure");
        services.itilActivity().addFollowup(ItilType.TICKET, t.getId(), "Checking", false);
        services.itilActivity().proposeSolution(ItilType.TICKET, t.getId(), "Roller cleaned", null);
        Ticket solved = services.tickets().get(t.getId());
        assertEquals(ItilStatus.CLOSED, solved.getStatus(), "admin was the requester: auto-accepted");
        assertTrue(services.itilActivity().timeline(ItilType.TICKET, t.getId()).size() >= 3);
        assertFalse(services.audit().history("Ticket", t.getId(), 0, 20).items().isEmpty());
    }

    @Test
    void optimisticLockingAcrossClients() {
        Asset a = services.assets().search(io.pyhoma.domain.service.AssetService.AssetSearch.of(null, null)).items().get(0);
        Asset first = services.assets().get(a.getId());
        Asset second = services.assets().get(a.getId());
        first.setNotes("first");
        services.assets().save(first);
        second.setNotes("second");
        assertThrows(ConcurrentUpdateException.class, () -> services.assets().save(second));
    }

    @Test
    void dashboardsAndReportsRunOnTheDatabase() {
        assertTrue(services.dashboard().overview().openTickets() > 0);
        assertEquals(30, services.dashboard().trend(30).size());
        for (ReportService.ReportKind kind : ReportService.ReportKind.values()) {
            assertDoesNotThrow(() -> services.reports().run(kind, null, null), kind.name());
        }
        assertFalse(services.search().search("notebook", 3).isEmpty());
        assertFalse(services.tickets().board(ItilSearch.create()).isEmpty());
        assertDoesNotThrow(() -> services.jobs().runFrequent());
        assertDoesNotThrow(() -> services.jobs().runHourly());
    }

    @Test
    void restartKeepsDataAndSkipsMigrations() throws Exception {
        long tickets = services.tickets().count(ItilSearch.create().view(ItilSearch.View.ALL));
        backend.close();
        backend = BackendTestSupport.start(settings, folder);
        services = BackendTestSupport.installDemo(backend);
        assertEquals(tickets, services.tickets().count(ItilSearch.create().view(ItilSearch.View.ALL)));
    }
}
