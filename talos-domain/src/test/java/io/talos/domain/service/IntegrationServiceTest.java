package io.talos.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.talos.domain.model.itil.ItilStatus;
import io.talos.domain.model.itil.Ticket;
import io.talos.domain.service.IntegrationService.Delivery;
import io.talos.domain.service.IntegrationService.Event;
import io.talos.domain.service.IntegrationService.Kind;
import io.talos.domain.service.IntegrationService.Webhook;
import io.talos.domain.support.AccessDeniedException;
import io.talos.domain.support.ValidationException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntegrationServiceTest {

    private record Sent(URI uri, String body, Map<String, String> headers) {
    }

    private TestFixture f;
    private Services s;
    private final List<Sent> sent = Collections.synchronizedList(new ArrayList<>());
    private CountDownLatch latch;

    @BeforeEach
    void setUp() {
        f = TestFixture.installed(false);
        s = f.services;
        s.integrations().setTransport((uri, body, headers) -> {
            sent.add(new Sent(uri, body, headers));
            if (latch != null) {
                latch.countDown();
            }
            return uri.getPath().contains("fail") ? 500 : 200;
        });
    }

    private Webhook hook(String name, Kind kind, String url, Event... events) {
        return new Webhook(null, name, kind, url, "s3cret", true,
                events.length == 0 ? EnumSet.noneOf(Event.class) : EnumSet.copyOf(List.of(events)));
    }

    @Test
    void savesListsAndDeletesWebhooks() {
        Webhook saved = s.integrations().save(hook("Canal TI", Kind.SLACK, "https://hooks.slack.com/services/T/B/x",
                Event.ITEM_CREATED, Event.ITEM_SOLVED));
        assertEquals(1, s.integrations().webhooks().size());
        Webhook loaded = s.integrations().webhooks().get(0);
        assertEquals(saved.id(), loaded.id());
        assertEquals("Canal TI", loaded.name());
        assertEquals(EnumSet.of(Event.ITEM_CREATED, Event.ITEM_SOLVED), loaded.events());
        s.integrations().setEnabled(saved.id(), false);
        assertFalse(s.integrations().webhooks().get(0).enabled());
        s.integrations().delete(saved.id());
        assertTrue(s.integrations().webhooks().isEmpty());
    }

    @Test
    void rejectsInsecureOrInvalidUrls() {
        assertThrows(ValidationException.class, () -> s.integrations().save(
                hook("x", Kind.GENERIC, "http://example.com/hook", Event.ITEM_CREATED)));
        assertThrows(ValidationException.class, () -> s.integrations().save(
                hook("x", Kind.GENERIC, "https://user:pw@example.com/hook", Event.ITEM_CREATED)));
        assertThrows(ValidationException.class, () -> s.integrations().save(
                hook("x", Kind.GENERIC, "https://example.com/hook")));
        assertTrue(IntegrationService.isAllowedUrl("http://localhost:8080/hook"));
    }

    @Test
    void onlyAdministratorsConfigureIntegrations() {
        f.user("tech", "Técnico");
        f.loginAs("tech");
        assertThrows(AccessDeniedException.class, () -> s.integrations().save(
                hook("x", Kind.SLACK, "https://hooks.slack.com/services/a", Event.ITEM_CREATED)));
    }

    @Test
    void publishesTicketEventsToSubscribedWebhooks() throws Exception {
        s.integrations().save(hook("Geral", Kind.GENERIC, "https://example.com/hook", Event.ITEM_CREATED, Event.ITEM_SOLVED));
        s.integrations().save(hook("Só soluções", Kind.TEAMS, "https://example.com/teams", Event.ITEM_SOLVED));
        latch = new CountDownLatch(1);
        Ticket t = s.tickets().draft();
        t.setTitle("Impressora parada");
        t.setContent("details");
        t.setUrgency(3);
        t.setImpact(3);
        Ticket created = s.tickets().create(t);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        Sent first = sent.get(0);
        assertEquals("/hook", first.uri().getPath());
        assertTrue(first.body().contains("\"event\":\"item.created\""), first.body());
        assertTrue(first.body().contains("Impressora parada"));
        String expected = "sha256=" + IntegrationService.sign("s3cret", first.body());
        assertEquals(expected, first.headers().get("X-Talos-Signature"));
        assertEquals("item.created", first.headers().get("X-Talos-Event"));
        assertEquals(created.getId(), Long.valueOf(created.getId()));
        assertFalse(sent.stream().anyMatch(x -> x.uri().getPath().equals("/teams")));
    }

    @Test
    void testDeliveryReportsHttpFailures() throws Exception {
        Delivery ok = s.integrations().test(hook("ok", Kind.DISCORD, "https://example.com/ok", Event.ITEM_CREATED))
                .get(5, TimeUnit.SECONDS);
        assertTrue(ok.ok());
        assertTrue(sent.get(0).body().contains("\"embeds\""));
        Delivery bad = s.integrations().test(hook("bad", Kind.SLACK, "https://example.com/fail", Event.ITEM_CREATED))
                .get(5, TimeUnit.SECONDS);
        assertFalse(bad.ok());
        assertEquals(500, bad.httpStatus());
        assertEquals(2, s.integrations().deliveries().size());
    }

    @Test
    void jsonIsEscaped() {
        assertEquals("\"a\\\"b\\nc\\\\\"", IntegrationService.str("a\"b\nc\\"));
    }

    @Test
    void exportsOpenTicketDeadlinesAsICalendar() {
        Ticket t = s.tickets().draft();
        t.setTitle("Trocar toner; andar 2, sala 4");
        t.setContent("details");
        t.setUrgency(4);
        t.setImpact(4);
        Ticket created = s.tickets().create(t);
        created.setDueAt(java.time.LocalDateTime.now(f.clock).plusDays(1));
        s.tickets().save(created);
        String ics = s.calendarExport().openTicketDeadlines(null, k -> k);
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(ics.contains("BEGIN:VEVENT"));
        assertTrue(ics.contains("Trocar toner\\; andar 2\\, sala 4"), ics);
        assertTrue(ics.trim().endsWith("END:VCALENDAR"));
        for (String line : ics.split("\r\n")) {
            assertTrue(line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 75, line);
        }
        assertEquals(ItilStatus.NEW, created.getStatus());
    }
}
