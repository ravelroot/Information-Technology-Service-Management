package io.pyhoma.domain.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.pyhoma.domain.model.project.ProjectTask;
import io.pyhoma.domain.model.project.ProjectTaskDependency;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProjectSchedulerTest {

    private static final LocalDate D0 = LocalDate.of(2026, 3, 2);

    private static ProjectTask task(long id, int start, int end) {
        ProjectTask t = new ProjectTask();
        t.setId(id);
        t.setPlanStart(D0.plusDays(start));
        t.setPlanEnd(D0.plusDays(end));
        t.setProgress(0);
        return t;
    }

    private static ProjectTaskDependency dep(long task, long pred) {
        ProjectTaskDependency d = new ProjectTaskDependency();
        d.setTaskId(task);
        d.setPredecessorId(pred);
        d.setLagDays(0);
        return d;
    }

    @Test
    void detectsCycles() {
        List<ProjectTaskDependency> deps = List.of(dep(2, 1), dep(3, 2));
        assertTrue(ProjectScheduler.createsCycle(1L, 3L, deps), "3 -> 1 closes 1 -> 2 -> 3");
        assertFalse(ProjectScheduler.createsCycle(3L, 1L, deps));
        assertTrue(ProjectScheduler.createsCycle(1L, 1L, deps));
    }

    @Test
    void propagatesDatesKeepingDuration() {
        ProjectTask a = task(1, 0, 4);
        ProjectTask b = task(2, 2, 3);
        ProjectTask c = task(3, 1, 1);
        List<ProjectTask> moved = ProjectScheduler.propagate(List.of(a, b, c), List.of(dep(2, 1), dep(3, 2)));
        assertEquals(2, moved.size());
        assertEquals(D0.plusDays(5), b.getPlanStart());
        assertEquals(D0.plusDays(6), b.getPlanEnd());
        assertEquals(D0.plusDays(7), c.getPlanStart());
    }

    @Test
    void criticalPathFollowsTheLongestChain() {
        ProjectTask a = task(1, 0, 4);
        ProjectTask b = task(2, 5, 9);
        ProjectTask c = task(3, 5, 6);
        ProjectTask d = task(4, 10, 12);
        Set<Long> critical = ProjectScheduler.criticalPath(List.of(a, b, c, d), List.of(dep(2, 1), dep(3, 1), dep(4, 2), dep(4, 3)));
        assertEquals(Set.of(1L, 2L, 4L), critical);
    }

    @Test
    void weightedProgress() {
        ProjectTask a = task(1, 0, 9);
        a.setProgress(100);
        ProjectTask b = task(2, 0, 9);
        b.setProgress(0);
        assertEquals(50, ProjectScheduler.weightedProgress(List.of(a, b)));
    }
}
