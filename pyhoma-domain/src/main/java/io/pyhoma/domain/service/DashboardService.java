package io.pyhoma.domain.service;

import io.pyhoma.domain.itil.SlaState;
import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.asset.AssetType;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.model.project.Project;
import io.pyhoma.domain.model.project.ProjectTask;
import io.pyhoma.domain.model.project.ProjectTaskStatus;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.security.Right;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;

public class DashboardService extends AbstractService {

    public record Overview(long openTickets, long unassigned, long overdue, long atRisk, long solvedToday, long myOpen,
                           long myGroupsQueue, double avgResolutionHours, double satisfaction, long openProblems,
                           long openChanges, long assets, long warrantyExpiring, long contractsExpiring,
                           long licensesOverUsed, long activeProjects, long lateTasks, long pendingApprovals) {
    }

    public record DayPoint(LocalDate day, long opened, long solved) {
    }

    public DashboardService(Services services) {
        super(services);
    }

    public Overview overview() {
        TicketService t = services.tickets();
        boolean tickets = can(Module.TICKET, Right.READ);
        LocalDateTime now = now();
        long open = tickets ? t.count(ItilSearch.create().view(ItilSearch.View.OPEN)) : -1;
        long unassigned = tickets ? t.count(ItilSearch.create().view(ItilSearch.View.UNASSIGNED)) : -1;
        long overdue = tickets ? t.count(ItilSearch.create().view(ItilSearch.View.OVERDUE)) : -1;
        long myOpen = tickets ? t.count(ItilSearch.create().view(ItilSearch.View.MINE_ASSIGNED)) : -1;
        long myGroups = tickets ? t.count(ItilSearch.create().view(ItilSearch.View.MY_GROUPS)) : -1;
        long atRisk = 0;
        long solvedToday = 0;
        double avg = 0;
        double satisfaction = 0;
        if (tickets) {
            Filter openFilter = t.filter(ItilSearch.create().view(ItilSearch.View.OPEN));
            for (Ticket x : repo(Ticket.class).find(Query.where(Filter.and(openFilter, Filter.notNull("dueAt"))))) {
                if (services.slas().ttrState(x) == SlaState.AT_RISK) {
                    atRisk++;
                }
            }
            Filter visible = t.filter(ItilSearch.create().view(ItilSearch.View.ALL));
            solvedToday = repo(Ticket.class).count(Filter.and(visible, Filter.ge("solvedAt", today().atStartOfDay())));
            List<Ticket> recent = repo(Ticket.class).find(Filter.and(visible, Filter.ge("solvedAt", now.minusDays(30))));
            OptionalDouble hours = recent.stream().filter(x -> x.getOpenedAt() != null)
                    .mapToLong(x -> Duration.between(x.getOpenedAt(), x.getSolvedAt()).toMinutes()).average();
            avg = hours.isPresent() ? hours.getAsDouble() / 60.0 : 0;
            OptionalDouble sat = recent.stream().filter(x -> x.getSatisfaction() != null).mapToInt(Ticket::getSatisfaction).average();
            satisfaction = sat.isPresent() ? sat.getAsDouble() : 0;
        }
        long problems = can(Module.PROBLEM, Right.READ) ? services.problems().count(ItilSearch.create().view(ItilSearch.View.OPEN)) : -1;
        long changes = can(Module.CHANGE, Right.READ) ? services.changes().count(ItilSearch.create().view(ItilSearch.View.OPEN)) : -1;
        int alertDays = services.settings().getInt(SettingKeys.EXPIRATION_ALERT_DAYS, 30);
        boolean assets = can(Module.ASSET, Right.READ);
        long assetCount = assets ? repo(Asset.class).count(Filter.and(Filter.isFalse("deleted"), security().orgFilter())) : -1;
        long warranty = assets ? services.assets().warrantyExpiring(alertDays).size() : -1;
        long contracts = can(Module.CONTRACT, Right.READ) ? services.contracts().expiring(alertDays).size() : -1;
        long licenses = can(Module.SOFTWARE, Right.READ)
                ? services.software().compliance().stream().filter(SoftwareService.LicenseUsage::overUsed).count() : -1;
        long projects = -1;
        long late = -1;
        if (can(Module.PROJECT, Right.READ)) {
            List<Project> portfolio = services.projects().portfolio();
            projects = portfolio.stream().filter(p -> p.getProgress() < 100).count();
            List<Long> ids = portfolio.stream().map(Project::getId).toList();
            late = repo(ProjectTask.class).count(Filter.and(Filter.in("projectId", ids), Filter.ne("status", ProjectTaskStatus.DONE),
                    Filter.lt("planEnd", today())));
        }
        long approvals = security().current().isSystem() ? 0 : services.itilActivity().myPendingApprovals().size();
        return new Overview(open, unassigned, overdue, atRisk, solvedToday, myOpen, myGroups, avg, satisfaction, problems,
                changes, assetCount, warranty, contracts, licenses, projects, late, approvals);
    }

    public Map<ItilStatus, Long> ticketsByStatus() {
        Map<ItilStatus, Long> result = new EnumMap<>(ItilStatus.class);
        if (!can(Module.TICKET, Right.READ)) {
            return result;
        }
        Filter f = services.tickets().filter(ItilSearch.create().view(ItilSearch.View.ALL));
        Map<Object, Long> counts = repo(Ticket.class).countBy("status", Filter.and(f,
                Filter.or(Filter.in("status", ItilStatus.open(io.pyhoma.domain.model.itil.ItilType.TICKET)),
                        Filter.ge("solvedAt", now().minusDays(30)))));
        for (ItilStatus s : ItilStatus.workflow(io.pyhoma.domain.model.itil.ItilType.TICKET)) {
            result.put(s, counts.getOrDefault(s, 0L));
        }
        return result;
    }

    public Map<Integer, Long> ticketsByPriority() {
        Map<Integer, Long> result = new TreeMap<>();
        if (!can(Module.TICKET, Right.READ)) {
            return result;
        }
        Filter f = services.tickets().filter(ItilSearch.create().view(ItilSearch.View.OPEN));
        for (int p = 1; p <= 6; p++) {
            result.put(p, 0L);
        }
        repo(Ticket.class).countBy("priority", f).forEach((k, v) -> result.put(((Number) k).intValue(), v));
        return result;
    }

    public List<DayPoint> trend(int days) {
        List<DayPoint> result = new ArrayList<>();
        if (!can(Module.TICKET, Right.READ)) {
            return result;
        }
        LocalDate first = today().minusDays(days - 1L);
        Filter visible = services.tickets().filter(ItilSearch.create().view(ItilSearch.View.ALL));
        Map<LocalDate, long[]> perDay = new TreeMap<>();
        for (int i = 0; i < days; i++) {
            perDay.put(first.plusDays(i), new long[2]);
        }
        for (Ticket t : repo(Ticket.class).find(Filter.and(visible, Filter.or(Filter.ge("openedAt", first.atStartOfDay()),
                Filter.ge("solvedAt", first.atStartOfDay()))))) {
            if (t.getOpenedAt() != null && perDay.containsKey(t.getOpenedAt().toLocalDate())) {
                perDay.get(t.getOpenedAt().toLocalDate())[0]++;
            }
            if (t.getSolvedAt() != null && perDay.containsKey(t.getSolvedAt().toLocalDate())) {
                perDay.get(t.getSolvedAt().toLocalDate())[1]++;
            }
        }
        perDay.forEach((d, v) -> result.add(new DayPoint(d, v[0], v[1])));
        return result;
    }

    public Map<String, Long> openByCategory(int max) {
        if (!can(Module.TICKET, Right.READ)) {
            return Map.of();
        }
        Map<Long, String> names = services.dropdowns().names(DropdownKind.ITIL_CATEGORY);
        Filter f = services.tickets().filter(ItilSearch.create().view(ItilSearch.View.OPEN));
        return top(repo(Ticket.class).countBy("categoryId", f), names, max);
    }

    public Map<String, Long> technicianLoad(int max) {
        if (!can(Module.TICKET, Right.READ)) {
            return Map.of();
        }
        Map<Long, String> names = services.lookups().names(User.class);
        Filter f = services.tickets().filter(ItilSearch.create().view(ItilSearch.View.OPEN));
        return top(repo(Ticket.class).countBy("assigneeId", f), names, max);
    }

    public Map<AssetType, Long> assetsByType() {
        return can(Module.ASSET, Right.READ) ? services.assets().countByType() : Map.of();
    }

    public Map<String, Long> assetsByState() {
        if (!can(Module.ASSET, Right.READ)) {
            return Map.of();
        }
        Map<Long, String> names = services.dropdowns().names(DropdownKind.ASSET_STATE);
        Map<Object, Long> raw = new LinkedHashMap<>();
        services.assets().countByState().forEach(raw::put);
        return top(raw, names, 10);
    }

    static Map<String, Long> top(Map<Object, Long> counts, Map<Long, String> names, int max) {
        Map<String, Long> result = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<Object, Long>comparingByValue().reversed())
                .limit(max)
                .forEach(e -> result.merge(e.getKey() == null ? "—" : names.getOrDefault((Long) e.getKey(), "#" + e.getKey()),
                        e.getValue(), Long::sum));
        return result;
    }
}
