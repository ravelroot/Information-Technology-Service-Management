package io.pyhoma.desktop.views.projects;

import io.pyhoma.desktop.App;
import io.pyhoma.desktop.ui.components.GanttChart;
import io.pyhoma.domain.model.project.ProjectTask;
import io.pyhoma.domain.model.project.ProjectTaskDependency;
import io.pyhoma.domain.service.ProjectService.GanttData;
import io.pyhoma.domain.support.Texts;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

final class GanttSupport {

    private GanttSupport() {
    }

    static List<GanttChart.Bar> bars(GanttData data) {
        Map<Long, List<ProjectTask>> children = data.tasks().stream().filter(t -> t.getParentId() != null)
                .collect(Collectors.groupingBy(ProjectTask::getParentId));
        List<GanttChart.Bar> result = new ArrayList<>();
        List<ProjectTask> roots = data.tasks().stream().filter(t -> t.getParentId() == null
                || data.tasks().stream().noneMatch(p -> p.getId().equals(t.getParentId()))).toList();
        for (ProjectTask t : sorted(roots)) {
            add(result, t, 0, children, data);
        }
        return result;
    }

    private static List<ProjectTask> sorted(List<ProjectTask> list) {
        return list.stream().sorted(Comparator.comparing(ProjectTask::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ProjectTask::getPlanStart, Comparator.nullsLast(LocalDate::compareTo))).toList();
    }

    private static void add(List<GanttChart.Bar> out, ProjectTask t, int depth, Map<Long, List<ProjectTask>> children, GanttData data) {
        List<ProjectTask> kids = children.getOrDefault(t.getId(), List.of());
        boolean late = !t.getStatus().isDone() && t.getPlanEnd() != null && t.getPlanEnd().isBefore(LocalDate.now());
        String who = t.getAssigneeId() == null ? null : Texts.abbreviate(App.lookups().user(t.getAssigneeId()), 28);
        out.add(new GanttChart.Bar(t.getId(), t.getName(), who, t.getPlanStart(), t.getPlanEnd(), t.getProgress(), t.isMilestone(),
                data.project().getColor(), depth, data.criticalTaskIds().contains(t.getId()), late, !kids.isEmpty()));
        for (ProjectTask k : sorted(kids)) {
            add(out, k, depth + 1, children, data);
        }
    }

    static List<GanttChart.Link> links(List<ProjectTaskDependency> deps) {
        return deps.stream().filter(Objects::nonNull).map(d -> new GanttChart.Link(d.getPredecessorId(), d.getTaskId())).toList();
    }
}
