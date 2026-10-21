package io.pyhoma.domain.service;

import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.asset.AssetType;
import io.pyhoma.domain.model.asset.Contract;
import io.pyhoma.domain.model.asset.Supplier;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.Right;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReportService extends AbstractService {

    public enum ReportKind {
        TICKETS_BY_MONTH, TICKETS_BY_CATEGORY, TECHNICIAN_PERFORMANCE, SLA_COMPLIANCE, SATISFACTION,
        ASSETS_BY_TYPE, ASSETS_BY_LOCATION, WARRANTY_EXPIRATION, CONTRACT_EXPIRATION, LICENSE_COMPLIANCE
    }

    public enum ChartKind { NONE, BAR, STACKED_BAR, LINE, PIE }

    public record Report(ReportKind kind, List<String> columns, List<List<String>> rows, ChartKind chart,
                         List<String> categories, Map<String, List<Number>> series) {
    }

    public ReportService(Services services) {
        super(services);
    }

    public Report run(ReportKind kind, LocalDate from, LocalDate to) {
        require(Module.REPORT, Right.READ);
        LocalDate start = from == null ? today().minusMonths(11).withDayOfMonth(1) : from;
        LocalDate end = to == null ? today() : to;
        return switch (kind) {
            case TICKETS_BY_MONTH -> ticketsByMonth(start, end);
            case TICKETS_BY_CATEGORY -> ticketsByCategory(start, end);
            case TECHNICIAN_PERFORMANCE -> technicians(start, end);
            case SLA_COMPLIANCE -> slaCompliance(start, end);
            case SATISFACTION -> satisfaction(start, end);
            case ASSETS_BY_TYPE -> assetsByType();
            case ASSETS_BY_LOCATION -> assetsByLocation();
            case WARRANTY_EXPIRATION -> warranty();
            case CONTRACT_EXPIRATION -> contracts();
            case LICENSE_COMPLIANCE -> licenses();
        };
    }

    private List<Ticket> tickets(LocalDate from, LocalDate to) {
        Filter visible = services.tickets().filter(ItilSearch.create().view(ItilSearch.View.ALL));
        return repo(Ticket.class).find(Filter.and(visible, Filter.ge("openedAt", from.atStartOfDay()),
                Filter.lt("openedAt", to.plusDays(1).atStartOfDay())));
    }

    private Report ticketsByMonth(LocalDate from, LocalDate to) {
        Map<YearMonth, long[]> months = new TreeMap<>();
        for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
            months.put(m, new long[3]);
        }
        for (Ticket t : tickets(from, to)) {
            long[] v = months.get(YearMonth.from(t.getOpenedAt()));
            if (v != null) {
                v[0]++;
                if (t.getSolvedAt() != null) {
                    v[1]++;
                }
                if (t.getTicketType() == io.pyhoma.domain.model.itil.TicketType.INCIDENT) {
                    v[2]++;
                }
            }
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> cats = new ArrayList<>();
        List<Number> opened = new ArrayList<>();
        List<Number> solved = new ArrayList<>();
        months.forEach((m, v) -> {
            rows.add(List.of(m.toString(), String.valueOf(v[0]), String.valueOf(v[1]), String.valueOf(v[2]), String.valueOf(v[0] - v[2])));
            cats.add(m.toString());
            opened.add(v[0]);
            solved.add(v[1]);
        });
        Map<String, List<Number>> series = new LinkedHashMap<>();
        series.put("report.series.opened", opened);
        series.put("report.series.solved", solved);
        return new Report(ReportKind.TICKETS_BY_MONTH, List.of("month", "opened", "solved", "incidents", "requests"),
                rows, ChartKind.LINE, cats, series);
    }

    private Report ticketsByCategory(LocalDate from, LocalDate to) {
        Map<Long, String> names = services.dropdowns().names(DropdownKind.ITIL_CATEGORY);
        Map<String, long[]> stats = new TreeMap<>();
        for (Ticket t : tickets(from, to)) {
            String name = t.getCategoryId() == null ? "—" : names.getOrDefault(t.getCategoryId(), "#" + t.getCategoryId());
            long[] v = stats.computeIfAbsent(name, k -> new long[3]);
            v[0]++;
            if (t.getSolvedAt() != null) {
                v[1]++;
                v[2] += Duration.between(t.getOpenedAt(), t.getSolvedAt()).toMinutes();
            }
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> cats = new ArrayList<>();
        List<Number> values = new ArrayList<>();
        stats.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0])).forEach(e -> {
            long[] v = e.getValue();
            rows.add(List.of(e.getKey(), String.valueOf(v[0]), String.valueOf(v[1]), hours(v[1] == 0 ? 0 : v[2] / v[1])));
            cats.add(e.getKey());
            values.add(v[0]);
        });
        return new Report(ReportKind.TICKETS_BY_CATEGORY, List.of("category", "opened", "solved", "avgResolutionHours"),
                rows, ChartKind.BAR, cats, Map.of("report.series.opened", values));
    }

    private Report technicians(LocalDate from, LocalDate to) {
        Map<Long, String> names = services.lookups().names(User.class);
        Map<Long, long[]> stats = new HashMap<>();
        for (Ticket t : tickets(from, to)) {
            if (t.getAssigneeId() == null) {
                continue;
            }
            long[] v = stats.computeIfAbsent(t.getAssigneeId(), k -> new long[6]);
            v[0]++;
            if (t.getSolvedAt() != null) {
                v[1]++;
                v[2] += Duration.between(t.getOpenedAt(), t.getSolvedAt()).toMinutes();
            }
            if (t.getSatisfaction() != null) {
                v[3] += t.getSatisfaction();
                v[4]++;
            }
            v[5] += t.getActionTimeMinutes() == null ? 0 : t.getActionTimeMinutes();
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> cats = new ArrayList<>();
        List<Number> assigned = new ArrayList<>();
        List<Number> solved = new ArrayList<>();
        stats.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1])).forEach(e -> {
            long[] v = e.getValue();
            String name = names.getOrDefault(e.getKey(), "#" + e.getKey());
            rows.add(List.of(name, String.valueOf(v[0]), String.valueOf(v[1]), hours(v[1] == 0 ? 0 : v[2] / v[1]),
                    v[4] == 0 ? "—" : String.format(java.util.Locale.ROOT, "%.1f", (double) v[3] / v[4]), hours(v[5])));
            cats.add(name);
            assigned.add(v[0]);
            solved.add(v[1]);
        });
        Map<String, List<Number>> series = new LinkedHashMap<>();
        series.put("report.series.assigned", assigned);
        series.put("report.series.solved", solved);
        return new Report(ReportKind.TECHNICIAN_PERFORMANCE,
                List.of("technician", "assigned", "solved", "avgResolutionHours", "satisfaction", "timeSpentHours"),
                rows, ChartKind.BAR, cats, series);
    }

    private Report slaCompliance(LocalDate from, LocalDate to) {
        Map<YearMonth, long[]> months = new TreeMap<>();
        for (Ticket t : tickets(from, to)) {
            if (t.getDueAt() == null || t.getSolvedAt() == null) {
                continue;
            }
            long[] v = months.computeIfAbsent(YearMonth.from(t.getOpenedAt()), k -> new long[2]);
            if (t.getSolvedAt().isAfter(t.getDueAt())) {
                v[1]++;
            } else {
                v[0]++;
            }
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> cats = new ArrayList<>();
        List<Number> met = new ArrayList<>();
        List<Number> missed = new ArrayList<>();
        months.forEach((m, v) -> {
            long total = v[0] + v[1];
            rows.add(List.of(m.toString(), String.valueOf(total), String.valueOf(v[0]), String.valueOf(v[1]),
                    total == 0 ? "—" : (v[0] * 100 / total) + "%"));
            cats.add(m.toString());
            met.add(v[0]);
            missed.add(v[1]);
        });
        Map<String, List<Number>> series = new LinkedHashMap<>();
        series.put("report.series.met", met);
        series.put("report.series.missed", missed);
        return new Report(ReportKind.SLA_COMPLIANCE, List.of("month", "withSla", "met", "missed", "compliance"),
                rows, ChartKind.STACKED_BAR, cats, series);
    }

    private Report satisfaction(LocalDate from, LocalDate to) {
        long[] stars = new long[6];
        for (Ticket t : tickets(from, to)) {
            if (t.getSatisfaction() != null) {
                stars[Math.max(1, Math.min(5, t.getSatisfaction()))]++;
            }
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> cats = new ArrayList<>();
        List<Number> values = new ArrayList<>();
        for (int i = 5; i >= 1; i--) {
            String label = "★".repeat(i);
            rows.add(List.of(label, String.valueOf(stars[i])));
            cats.add(label);
            values.add(stars[i]);
        }
        return new Report(ReportKind.SATISFACTION, List.of("rating", "tickets"), rows, ChartKind.PIE, cats,
                Map.of("report.series.tickets", values));
    }

    private Report assetsByType() {
        require(Module.ASSET, Right.READ);
        Map<AssetType, Long> counts = services.assets().countByType();
        List<List<String>> rows = new ArrayList<>();
        List<String> cats = new ArrayList<>();
        List<Number> values = new ArrayList<>();
        counts.forEach((type, n) -> {
            rows.add(List.of("enum.AssetType." + type.name(), String.valueOf(n)));
            cats.add("enum.AssetType." + type.name());
            values.add(n);
        });
        return new Report(ReportKind.ASSETS_BY_TYPE, List.of("type", "assets"), rows, ChartKind.PIE, cats,
                Map.of("report.series.assets", values));
    }

    private Report assetsByLocation() {
        require(Module.ASSET, Right.READ);
        Map<Long, String> names = services.dropdowns().names(DropdownKind.LOCATION);
        Map<Object, Long> counts = repo(Asset.class).countBy("locationId", Filter.and(Filter.isFalse("deleted"), security().orgFilter()));
        Map<String, Long> top = DashboardService.top(counts, names, 50);
        List<List<String>> rows = new ArrayList<>();
        top.forEach((k, v) -> rows.add(List.of(k, String.valueOf(v))));
        return new Report(ReportKind.ASSETS_BY_LOCATION, List.of("location", "assets"), rows, ChartKind.BAR,
                new ArrayList<>(top.keySet()), Map.of("report.series.assets", new ArrayList<>(top.values())));
    }

    private Report warranty() {
        require(Module.ASSET, Right.READ);
        Map<Long, String> locations = services.dropdowns().names(DropdownKind.LOCATION);
        List<List<String>> rows = new ArrayList<>();
        for (Asset a : services.assets().warrantyExpiring(90)) {
            rows.add(List.of(a.getName(), a.getAssetTag() == null ? "" : a.getAssetTag(), "enum.AssetType." + a.getAssetType().name(),
                    a.getLocationId() == null ? "" : locations.getOrDefault(a.getLocationId(), ""), a.getWarrantyExpiresOn().toString()));
        }
        return new Report(ReportKind.WARRANTY_EXPIRATION, List.of("asset", "assetTag", "type", "location", "warrantyEnd"),
                rows, ChartKind.NONE, List.of(), Map.of());
    }

    private Report contracts() {
        require(Module.CONTRACT, Right.READ);
        Map<Long, String> suppliers = services.lookups().names(Supplier.class);
        List<List<String>> rows = new ArrayList<>();
        for (Contract c : services.contracts().expiring(90)) {
            rows.add(List.of(c.getName(), c.getContractNumber() == null ? "" : c.getContractNumber(),
                    c.getSupplierId() == null ? "" : suppliers.getOrDefault(c.getSupplierId(), ""),
                    String.valueOf(ContractService.endDate(c)), String.valueOf(ContractService.noticeDate(c)),
                    "enum.RenewalType." + c.getRenewal().name()));
        }
        return new Report(ReportKind.CONTRACT_EXPIRATION, List.of("contract", "number", "supplier", "endDate", "noticeDate", "renewal"),
                rows, ChartKind.NONE, List.of(), Map.of());
    }

    private Report licenses() {
        require(Module.SOFTWARE, Right.READ);
        List<List<String>> rows = new ArrayList<>();
        List<String> cats = new ArrayList<>();
        List<Number> used = new ArrayList<>();
        List<Number> seats = new ArrayList<>();
        for (SoftwareService.LicenseUsage u : services.software().compliance()) {
            rows.add(List.of(u.softwareName() == null ? "" : u.softwareName(), u.license().getName(),
                    u.unlimited() ? "∞" : String.valueOf(u.license().getSeats()), String.valueOf(u.used()),
                    u.overUsed() ? "report.value.overUsed" : "report.value.compliant",
                    u.license().getExpiresOn() == null ? "" : u.license().getExpiresOn().toString()));
            cats.add(u.license().getName());
            used.add(u.used());
            seats.add(u.unlimited() ? 0 : u.license().getSeats());
        }
        Map<String, List<Number>> series = new LinkedHashMap<>();
        series.put("report.series.seats", seats);
        series.put("report.series.used", used);
        return new Report(ReportKind.LICENSE_COMPLIANCE, List.of("software", "license", "seats", "used", "status", "expiresOn"),
                rows, ChartKind.BAR, cats, series);
    }

    private static String hours(long minutes) {
        return String.format(java.util.Locale.ROOT, "%.1f", minutes / 60.0);
    }

    List<Ticket> latest(int max) {
        return repo(Ticket.class).find(Query.all().orderBy(Sort.desc("openedAt")).limit(max));
    }
}
