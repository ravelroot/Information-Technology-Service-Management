package io.pyhoma.domain.project;

import io.pyhoma.domain.model.project.ProjectTask;
import io.pyhoma.domain.model.project.ProjectTaskDependency;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProjectScheduler {

    private ProjectScheduler() {
    }

    public static boolean createsCycle(Long taskId, Long predecessorId, List<ProjectTaskDependency> deps) {
        if (taskId.equals(predecessorId)) {
            return true;
        }
        Map<Long, List<Long>> successors = successors(deps);
        Deque<Long> stack = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        stack.push(taskId);
        while (!stack.isEmpty()) {
            Long current = stack.pop();
            if (current.equals(predecessorId)) {
                return true;
            }
            if (seen.add(current)) {
                successors.getOrDefault(current, List.of()).forEach(stack::push);
            }
        }
        return false;
    }

    public static List<ProjectTask> propagate(List<ProjectTask> tasks, List<ProjectTaskDependency> deps) {
        Map<Long, ProjectTask> byId = new HashMap<>();
        tasks.forEach(t -> byId.put(t.getId(), t));
        Map<Long, List<ProjectTaskDependency>> incoming = new HashMap<>();
        for (ProjectTaskDependency d : deps) {
            incoming.computeIfAbsent(d.getTaskId(), k -> new ArrayList<>()).add(d);
        }
        Set<ProjectTask> changed = new LinkedHashSet<>();
        for (Long id : topologicalOrder(tasks, deps)) {
            ProjectTask task = byId.get(id);
            if (task == null || task.getPlanStart() == null) {
                continue;
            }
            LocalDate earliest = null;
            for (ProjectTaskDependency d : incoming.getOrDefault(id, List.of())) {
                ProjectTask pred = byId.get(d.getPredecessorId());
                if (pred == null || pred.getPlanEnd() == null) {
                    continue;
                }
                LocalDate candidate = pred.getPlanEnd().plusDays(1 + Math.max(0, d.getLagDays() == null ? 0 : d.getLagDays()));
                if (pred.isMilestone()) {
                    candidate = candidate.minusDays(1);
                }
                if (earliest == null || candidate.isAfter(earliest)) {
                    earliest = candidate;
                }
            }
            if (earliest != null && earliest.isAfter(task.getPlanStart())) {
                long shift = ChronoUnit.DAYS.between(task.getPlanStart(), earliest);
                task.setPlanStart(earliest);
                task.setPlanEnd(task.getPlanEnd() == null ? earliest : task.getPlanEnd().plusDays(shift));
                changed.add(task);
            }
        }
        return new ArrayList<>(changed);
    }

    public static Set<Long> criticalPath(List<ProjectTask> tasks, List<ProjectTaskDependency> deps) {
        Map<Long, ProjectTask> byId = new HashMap<>();
        for (ProjectTask t : tasks) {
            if (t.getPlanStart() != null && t.getPlanEnd() != null) {
                byId.put(t.getId(), t);
            }
        }
        if (byId.isEmpty()) {
            return Set.of();
        }
        List<ProjectTaskDependency> valid = deps.stream()
                .filter(d -> byId.containsKey(d.getTaskId()) && byId.containsKey(d.getPredecessorId())).toList();
        List<Long> order = topologicalOrder(new ArrayList<>(byId.values()), valid);
        Map<Long, Long> duration = new HashMap<>();
        Map<Long, Long> es = new HashMap<>();
        Map<Long, Long> ef = new HashMap<>();
        Map<Long, List<ProjectTaskDependency>> incoming = new HashMap<>();
        Map<Long, List<ProjectTaskDependency>> outgoing = new HashMap<>();
        for (ProjectTaskDependency d : valid) {
            incoming.computeIfAbsent(d.getTaskId(), k -> new ArrayList<>()).add(d);
            outgoing.computeIfAbsent(d.getPredecessorId(), k -> new ArrayList<>()).add(d);
        }
        long origin = byId.values().stream().mapToLong(t -> t.getPlanStart().toEpochDay()).min().orElse(0);
        for (Long id : order) {
            ProjectTask t = byId.get(id);
            long dur = t.isMilestone() ? 0 : ChronoUnit.DAYS.between(t.getPlanStart(), t.getPlanEnd()) + 1;
            duration.put(id, dur);
            long start = t.getPlanStart().toEpochDay() - origin;
            for (ProjectTaskDependency d : incoming.getOrDefault(id, List.of())) {
                start = Math.max(start, ef.get(d.getPredecessorId()) + lag(d));
            }
            es.put(id, start);
            ef.put(id, start + dur);
        }
        long projectEnd = ef.values().stream().mapToLong(Long::longValue).max().orElse(0);
        Map<Long, Long> lf = new HashMap<>();
        Map<Long, Long> ls = new HashMap<>();
        for (int i = order.size() - 1; i >= 0; i--) {
            Long id = order.get(i);
            long finish = projectEnd;
            for (ProjectTaskDependency d : outgoing.getOrDefault(id, List.of())) {
                finish = Math.min(finish, ls.get(d.getTaskId()) - lag(d));
            }
            lf.put(id, finish);
            ls.put(id, finish - duration.get(id));
        }
        Set<Long> critical = new LinkedHashSet<>();
        for (Long id : order) {
            if (ls.get(id) - es.get(id) <= 0) {
                critical.add(id);
            }
        }
        return critical;
    }

    private static long lag(ProjectTaskDependency d) {
        return d.getLagDays() == null ? 0 : Math.max(0, d.getLagDays());
    }

    static List<Long> topologicalOrder(List<ProjectTask> tasks, List<ProjectTaskDependency> deps) {
        Map<Long, Integer> indegree = new HashMap<>();
        Set<Long> ids = new LinkedHashSet<>();
        tasks.stream().sorted((a, b) -> {
            if (a.getPlanStart() == null || b.getPlanStart() == null) {
                return a.getPlanStart() == null ? (b.getPlanStart() == null ? 0 : 1) : -1;
            }
            return a.getPlanStart().compareTo(b.getPlanStart());
        }).forEach(t -> ids.add(t.getId()));
        ids.forEach(id -> indegree.put(id, 0));
        Map<Long, List<Long>> successors = successors(deps);
        for (ProjectTaskDependency d : deps) {
            if (ids.contains(d.getTaskId()) && ids.contains(d.getPredecessorId())) {
                indegree.merge(d.getTaskId(), 1, Integer::sum);
            }
        }
        Deque<Long> queue = new ArrayDeque<>();
        ids.stream().filter(id -> indegree.get(id) == 0).forEach(queue::add);
        List<Long> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            order.add(id);
            for (Long s : successors.getOrDefault(id, List.of())) {
                if (indegree.containsKey(s) && indegree.merge(s, -1, Integer::sum) == 0) {
                    queue.add(s);
                }
            }
        }
        for (Long id : ids) {
            if (!order.contains(id)) {
                order.add(id);
            }
        }
        return order;
    }

    private static Map<Long, List<Long>> successors(List<ProjectTaskDependency> deps) {
        Map<Long, List<Long>> map = new HashMap<>();
        for (ProjectTaskDependency d : deps) {
            map.computeIfAbsent(d.getPredecessorId(), k -> new ArrayList<>()).add(d.getTaskId());
        }
        return map;
    }

    public static int weightedProgress(List<ProjectTask> tasks) {
        long weight = 0;
        long done = 0;
        for (ProjectTask t : tasks) {
            long w = t.getPlanStart() != null && t.getPlanEnd() != null && !t.isMilestone()
                    ? ChronoUnit.DAYS.between(t.getPlanStart(), t.getPlanEnd()) + 1 : 1;
            weight += w;
            done += w * (t.getProgress() == null ? 0 : t.getProgress());
        }
        return weight == 0 ? 0 : (int) Math.round((double) done / weight);
    }
}
