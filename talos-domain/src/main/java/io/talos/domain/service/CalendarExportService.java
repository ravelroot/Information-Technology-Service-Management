package io.talos.domain.service;

import io.talos.domain.model.itil.Ticket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Exporta os prazos dos chamados abertos no formato iCalendar (RFC 5545), para assinatura ou
 * importação no Outlook, Google Agenda e Apple Calendário.
 */
public class CalendarExportService extends AbstractService {

    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT);

    public CalendarExportService(Services services) {
        super(services);
    }

    /** Prazos dos chamados abertos visíveis ao usuário; {@code assigneeId} nulo = todos. */
    public String openTicketDeadlines(Long assigneeId, Function<String, String> labels) {
        ItilSearch s = ItilSearch.create().view(ItilSearch.View.OPEN).sort("dueAt", true).page(0, 2000);
        if (assigneeId != null) {
            s.assigneeId(assigneeId);
        }
        List<Ticket> tickets = services.tickets().search(s).items().stream().filter(t -> t.getDueAt() != null).toList();
        return render(tickets, services.settings().get(SettingKeys.ORGANIZATION_NAME, "Talos."), now(), labels);
    }

    static String render(List<Ticket> tickets, String org, LocalDateTime now, Function<String, String> labels) {
        StringBuilder b = new StringBuilder();
        line(b, "BEGIN:VCALENDAR");
        line(b, "VERSION:2.0");
        line(b, "PRODID:-//Talos.//Talos " + Services.VERSION + "//PT");
        line(b, "CALSCALE:GREGORIAN");
        line(b, "METHOD:PUBLISH");
        line(b, "X-WR-CALNAME:" + escape(org + " — " + labels.apply("ics.calendarName")));
        String stamp = utc(now);
        String host = org.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        for (Ticket t : tickets) {
            line(b, "BEGIN:VEVENT");
            line(b, "UID:talos-ticket-" + t.getId() + "@" + (host.isEmpty() ? "talos" : host));
            line(b, "DTSTAMP:" + stamp);
            line(b, "DTSTART:" + utc(t.getDueAt().minusMinutes(30)));
            line(b, "DTEND:" + utc(t.getDueAt()));
            line(b, "SUMMARY:" + escape(t.reference() + " " + t.getTitle()));
            String status = t.getStatus() == null ? "" : labels.apply("enum.ItilStatus." + t.getStatus().name());
            String priority = t.getPriority() == null ? "" : labels.apply("priority." + t.getPriority());
            line(b, "DESCRIPTION:" + escape(labels.apply("ics.status") + ": " + status + "\n"
                    + labels.apply("ics.priority") + ": " + priority));
            line(b, "CATEGORIES:" + escape(labels.apply("type.Ticket")));
            line(b, "PRIORITY:" + icsPriority(t.getPriority()));
            line(b, "BEGIN:VALARM");
            line(b, "ACTION:DISPLAY");
            line(b, "DESCRIPTION:" + escape(t.reference()));
            line(b, "TRIGGER:-PT1H");
            line(b, "END:VALARM");
            line(b, "END:VEVENT");
        }
        line(b, "END:VCALENDAR");
        return b.toString();
    }

    private static int icsPriority(Integer p) {
        if (p == null) {
            return 0;
        }
        return p >= 5 ? 1 : p == 4 ? 3 : p == 3 ? 5 : 9;
    }

    private static String utc(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).format(UTC);
    }

    static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
                .replace("\r\n", "\\n").replace("\n", "\\n");
    }

    /** Dobra linhas em 75 octetos, como pede a RFC 5545. */
    static void line(StringBuilder b, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= 75) {
            b.append(content).append("\r\n");
            return;
        }
        int count = 0;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < content.length(); ) {
            int cp = content.codePointAt(i);
            int len = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
            if (count + len > (b.isEmpty() && cur.isEmpty() ? 75 : 74) && count > 0) {
                b.append(cur).append("\r\n ");
                cur.setLength(0);
                count = 0;
            }
            cur.appendCodePoint(cp);
            count += len;
            i += Character.charCount(cp);
        }
        b.append(cur).append("\r\n");
    }
}
