package io.pyhoma.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.pyhoma.domain.model.project.Project;
import org.junit.jupiter.api.Test;

class DemoDataTest {

    @Test
    void demoDataFeedsEveryScreen() {
        TestFixture f = TestFixture.installed(true);
        Services s = f.services;
        var overview = s.dashboard().overview();
        assertTrue(overview.openTickets() > 10, "open tickets: " + overview.openTickets());
        assertTrue(overview.assets() > 40);
        assertEquals(30, s.dashboard().trend(30).size());
        assertFalse(s.dashboard().ticketsByStatus().isEmpty());
        for (ReportService.ReportKind kind : ReportService.ReportKind.values()) {
            assertDoesNotThrow(() -> s.reports().run(kind, null, null), kind.name());
        }
        var board = s.tickets().board(ItilSearch.create());
        assertTrue(board.values().stream().mapToInt(java.util.List::size).sum() > 0);
        Project p = s.projects().portfolio().get(0);
        var gantt = s.projects().gantt(p.getId());
        assertFalse(gantt.tasks().isEmpty());
        assertFalse(gantt.criticalTaskIds().isEmpty());
        assertFalse(s.search().search("notebook", 5).isEmpty());
        assertTrue(s.software().compliance().stream().anyMatch(SoftwareService.LicenseUsage::overUsed));
        s.jobs().runFrequent();
        s.jobs().runHourly();
    }

    @Test
    void englishDemoData() {
        TestFixture f = new TestFixture();
        f.services.installation().install(new InstallationService.InstallRequest("Acme Inc", "admin", "Admin", null,
                TestFixture.ADMIN_PASSWORD, "en", true));
        f.services.auth().login("admin", TestFixture.ADMIN_PASSWORD);
        assertTrue(f.services.profiles().all().stream().anyMatch(p -> p.getName().equals("Technician")));
        assertTrue(f.services.tickets().count(ItilSearch.create().view(ItilSearch.View.ALL)) >= 150);
    }
}
