package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.AuditAction;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.project.Project;
import io.pyhoma.domain.model.project.ProjectMember;
import io.pyhoma.domain.model.project.ProjectTask;
import io.pyhoma.domain.model.project.ProjectTaskDependency;
import io.pyhoma.domain.model.project.ProjectTaskStatus;
import io.pyhoma.domain.project.ProjectScheduler;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.NotificationService.Severity;
import io.pyhoma.domain.support.BusinessException;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.Validator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ProjectService extends AbstractService {

    public record GanttData(Project project, List<ProjectTask> tasks, List<ProjectTaskDependency> dependencies,
                            Set<Long> criticalTaskIds) {
    }

    public ProjectService(Services services) {
        super(services);
    }

    public PageResult<Project> search(String text, Long stateId, boolean trash, int page, int size) {
        require(Module.PROJECT, Right.READ);
        Filter f = Filter.and(trash ? Filter.isTrue("deleted") : Filter.isFalse("deleted"), security().orgFilter(),
                Filter.search(text, "name", "code", "description"),
                stateId == null ? Filter.ALL : Filter.eq("stateId", stateId));
        return repo(Project.class).page(Query.where(f).orderBy(Sort.asc("planStart"), Sort.asc("name")).page(page, size));
    }

    public List<Project> portfolio() {
        require(Module.PROJECT, Right.READ);
        return repo(Project.class).find(Query.where(Filter.and(Filter.isFalse("deleted"), security().orgFilter()))
                .orderBy(Sort.asc("planStart"), Sort.asc("name")));
    }

    public Project get(Long id) {
        require(Module.PROJECT, Right.READ);
        Project p = repo(Project.class).getById(id);
        checkVisible(p);
        return p;
    }

    public Project save(Project project) {
        require(Module.PROJECT, project.isNew() ? Right.CREATE : Right.UPDATE);
        return saveInternal(project);
    }

    Project saveInternal(Project project) {
        project.setName(Texts.clean(project.getName()));
        project.setCode(Texts.clean(project.getCode()));
        if (project.getOrgUnitId() == null) {
            project.setOrgUnitId(security().current().activeOrgUnitId());
        }
        Validator.create().required("name", project.getName())
                .range("priority", project.getPriority(), 1, 6)
                .range("progress", project.getProgress(), 0, 100)
                .check(project.getPlanStart() == null || project.getPlanEnd() == null
                        || !project.getPlanEnd().isBefore(project.getPlanStart()), "planEnd", "validation.endBeforeStart")
                .throwIfInvalid();
        return tx(() -> {
            if (project.getCode() != null) {
                boolean taken = repo(Project.class).find(Filter.eq("code", project.getCode())).stream()
                        .anyMatch(p -> !Objects.equals(p.getId(), project.getId()));
                if (taken) {
                    throw new BusinessException("error.duplicateCode", project.getCode());
                }
            }
            return persist(project);
        });
    }

    public void trash(Long id, boolean deleted) {
        require(Module.PROJECT, Right.DELETE);
        txRun(() -> {
            Project p = get(id);
            p.setDeleted(deleted);
            saveQuietly(p);
            services.audit().record(p, deleted ? AuditAction.DELETE : AuditAction.RESTORE, null, null, null);
        });
    }

    public void purgeProject(Long id) {
        require(Module.PROJECT, Right.PURGE);
        txRun(() -> {
            Project p = get(id);
            List<Long> taskIds = tasks(id).stream().map(ProjectTask::getId).toList();
            repo(ProjectTaskDependency.class).deleteWhere(Filter.in("taskId", taskIds));
            repo(ProjectTask.class).deleteWhere(Filter.eq("projectId", id));
            repo(ProjectMember.class).deleteWhere(Filter.eq("projectId", id));
            purge(p);
        });
    }

    public List<ProjectMember> members(Long projectId) {
        return repo(ProjectMember.class).find(Query.where(Filter.eq("projectId", projectId)).orderBy(Sort.asc("id")));
    }

    public ProjectMember addMember(Long projectId, Long userId, String role) {
        require(Module.PROJECT, Right.UPDATE);
        return tx(() -> {
            get(projectId);
            if (repo(ProjectMember.class).exists(Filter.and(Filter.eq("projectId", projectId), Filter.eq("userId", userId)))) {
                throw new BusinessException("error.alreadyMember");
            }
            ProjectMember m = new ProjectMember();
            m.setProjectId(projectId);
            m.setUserId(userId);
            m.setRoleName(Texts.clean(role));
            return insert(m);
        });
    }

    public void removeMember(Long memberId) {
        require(Module.PROJECT, Right.UPDATE);
        txRun(() -> purge(repo(ProjectMember.class).getById(memberId)));
    }

    public List<ProjectTask> tasks(Long projectId) {
        return repo(ProjectTask.class).find(Query.where(Filter.eq("projectId", projectId))
                .orderBy(Sort.asc("sortOrder"), Sort.asc("planStart"), Sort.asc("id")));
    }

    public ProjectTask task(Long taskId) {
        ProjectTask t = repo(ProjectTask.class).getById(taskId);
        get(t.getProjectId());
        return t;
    }

    public Map<ProjectTaskStatus, List<ProjectTask>> board(Long projectId, Long assigneeId) {
        require(Module.PROJECT, Right.READ);
        List<Long> projectIds = projectId != null ? List.of(projectId) : portfolio().stream().map(Project::getId).toList();
        Filter f = Filter.and(Filter.in("projectId", projectIds), assigneeId == null ? Filter.ALL : Filter.eq("assigneeId", assigneeId));
        Map<ProjectTaskStatus, List<ProjectTask>> board = new EnumMap<>(ProjectTaskStatus.class);
        for (ProjectTaskStatus s : ProjectTaskStatus.values()) {
            board.put(s, new ArrayList<>());
        }
        for (ProjectTask t : repo(ProjectTask.class).find(Query.where(f).orderBy(Sort.asc("planEnd"), Sort.asc("id")))) {
            board.get(t.getStatus()).add(t);
        }
        return board;
    }

    public List<ProjectTask> myTasks() {
        return repo(ProjectTask.class).find(Query.where(Filter.and(Filter.eq("assigneeId", currentUserId()),
                Filter.ne("status", ProjectTaskStatus.DONE))).orderBy(Sort.asc("planEnd")));
    }

    public ProjectTask saveTask(ProjectTask task) {
        require(Module.PROJECT, Right.UPDATE);
        return saveTaskInternal(task, true);
    }

    ProjectTask saveTaskInternal(ProjectTask task, boolean reschedule) {
        task.setName(Texts.clean(task.getName()));
        if (task.isMilestone() && task.getPlanStart() != null) {
            task.setPlanEnd(task.getPlanStart());
        }
        Validator.create().required("name", task.getName()).required("projectId", task.getProjectId())
                .range("progress", task.getProgress(), 0, 100)
                .positive("effortHours", task.getEffortHours())
                .check(task.getPlanStart() == null || task.getPlanEnd() == null
                        || !task.getPlanEnd().isBefore(task.getPlanStart()), "planEnd", "validation.endBeforeStart")
                .throwIfInvalid();
        return tx(() -> {
            get(task.getProjectId());
            if (task.getParentId() != null) {
                ProjectTask parent = repo(ProjectTask.class).getById(task.getParentId());
                if (!Objects.equals(parent.getProjectId(), task.getProjectId()) || Objects.equals(parent.getId(), task.getId())) {
                    throw new BusinessException("error.invalidParent");
                }
            }
            Long previousAssignee = task.isNew() ? null : repo(ProjectTask.class).getById(task.getId()).getAssigneeId();
            applyStatusEffects(task);
            if (task.isNew() && (task.getSortOrder() == null || task.getSortOrder() == 0)) {
                task.setSortOrder((int) repo(ProjectTask.class).count(Filter.eq("projectId", task.getProjectId())) + 1);
            }
            ProjectTask saved = persist(task);
            if (reschedule) {
                reschedule(saved.getProjectId());
            }
            rollup(saved.getProjectId());
            if (saved.getAssigneeId() != null && !Objects.equals(saved.getAssigneeId(), previousAssignee)) {
                Project p = repo(Project.class).getById(saved.getProjectId());
                services.notifications().notify(List.of(saved.getAssigneeId()), Severity.INFO, "notif.projectTask.assigned",
                        List.of(saved.getName(), p.getName()), "Project", p.getId());
            }
            return repo(ProjectTask.class).getById(saved.getId());
        });
    }

    private void applyStatusEffects(ProjectTask task) {
        LocalDate today = today();
        if (task.getStatus() == ProjectTaskStatus.DONE) {
            task.setProgress(100);
            if (task.getRealEnd() == null) {
                task.setRealEnd(today);
            }
            if (task.getRealStart() == null) {
                task.setRealStart(task.getPlanStart() != null && task.getPlanStart().isBefore(today) ? task.getPlanStart() : today);
            }
        } else {
            task.setRealEnd(null);
            if (task.getStatus() != ProjectTaskStatus.TODO && task.getRealStart() == null) {
                task.setRealStart(today);
            }
            if (task.getProgress() != null && task.getProgress() >= 100) {
                task.setProgress(90);
            }
        }
    }

    public ProjectTask moveTask(Long taskId, ProjectTaskStatus status) {
        ProjectTask t = task(taskId);
        t.setStatus(status);
        if (status == ProjectTaskStatus.TODO) {
            t.setProgress(0);
        }
        return saveTask(t);
    }

    public ProjectTask reschedule(Long taskId, LocalDate start, LocalDate end) {
        ProjectTask t = task(taskId);
        t.setPlanStart(start);
        t.setPlanEnd(end);
        return saveTask(t);
    }

    public void deleteTask(Long taskId) {
        require(Module.PROJECT, Right.UPDATE);
        txRun(() -> {
            ProjectTask t = task(taskId);
            for (ProjectTask child : repo(ProjectTask.class).find(Filter.eq("parentId", taskId))) {
                child.setParentId(t.getParentId());
                saveQuietly(child);
            }
            repo(ProjectTaskDependency.class).deleteWhere(Filter.or(Filter.eq("taskId", taskId), Filter.eq("predecessorId", taskId)));
            purge(t);
            rollup(t.getProjectId());
        });
    }

    public List<ProjectTaskDependency> dependencies(Long projectId) {
        List<Long> ids = tasks(projectId).stream().map(ProjectTask::getId).toList();
        return repo(ProjectTaskDependency.class).find(Filter.in("taskId", ids));
    }

    public ProjectTaskDependency addDependency(Long taskId, Long predecessorId, int lagDays) {
        require(Module.PROJECT, Right.UPDATE);
        return tx(() -> {
            ProjectTask task = task(taskId);
            ProjectTask pred = task(predecessorId);
            if (!Objects.equals(task.getProjectId(), pred.getProjectId())) {
                throw new BusinessException("error.dependencyOtherProject");
            }
            List<ProjectTaskDependency> deps = dependencies(task.getProjectId());
            if (deps.stream().anyMatch(d -> d.getTaskId().equals(taskId) && d.getPredecessorId().equals(predecessorId))) {
                throw new BusinessException("error.alreadyLinked");
            }
            if (ProjectScheduler.createsCycle(taskId, predecessorId, deps)) {
                throw new BusinessException("error.dependencyCycle");
            }
            ProjectTaskDependency d = new ProjectTaskDependency();
            d.setTaskId(taskId);
            d.setPredecessorId(predecessorId);
            d.setLagDays(Math.max(0, lagDays));
            ProjectTaskDependency saved = insert(d);
            reschedule(task.getProjectId());
            rollup(task.getProjectId());
            return saved;
        });
    }

    public void removeDependency(Long dependencyId) {
        require(Module.PROJECT, Right.UPDATE);
        txRun(() -> purge(repo(ProjectTaskDependency.class).getById(dependencyId)));
    }

    public int reschedule(Long projectId) {
        List<ProjectTask> tasks = tasks(projectId);
        List<ProjectTask> moved = ProjectScheduler.propagate(tasks, dependencies(projectId));
        for (ProjectTask t : moved) {
            update(t);
        }
        return moved.size();
    }

    void rollup(Long projectId) {
        List<ProjectTask> tasks = tasks(projectId);
        for (ProjectTask parent : tasks) {
            List<ProjectTask> children = tasks.stream().filter(t -> Objects.equals(t.getParentId(), parent.getId())).toList();
            if (children.isEmpty()) {
                continue;
            }
            int progress = ProjectScheduler.weightedProgress(children);
            LocalDate start = children.stream().map(ProjectTask::getPlanStart).filter(Objects::nonNull).min(LocalDate::compareTo).orElse(parent.getPlanStart());
            LocalDate end = children.stream().map(ProjectTask::getPlanEnd).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(parent.getPlanEnd());
            if (progress != parent.getProgress() || !Objects.equals(start, parent.getPlanStart()) || !Objects.equals(end, parent.getPlanEnd())) {
                parent.setProgress(progress);
                parent.setPlanStart(start);
                parent.setPlanEnd(end);
                saveQuietly(parent);
            }
        }
        Project project = repo(Project.class).getById(projectId);
        List<ProjectTask> leaves = tasks.stream()
                .filter(t -> tasks.stream().noneMatch(c -> Objects.equals(c.getParentId(), t.getId()))).toList();
        int progress = leaves.isEmpty() ? project.getProgress() : ProjectScheduler.weightedProgress(leaves);
        LocalDate minStart = tasks.stream().map(ProjectTask::getPlanStart).filter(Objects::nonNull).min(LocalDate::compareTo).orElse(null);
        LocalDate maxEnd = tasks.stream().map(ProjectTask::getPlanEnd).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
        boolean changed = progress != project.getProgress();
        project.setProgress(progress);
        if (minStart != null && (project.getPlanStart() == null || minStart.isBefore(project.getPlanStart()))) {
            project.setPlanStart(minStart);
            changed = true;
        }
        if (maxEnd != null && (project.getPlanEnd() == null || maxEnd.isAfter(project.getPlanEnd()))) {
            project.setPlanEnd(maxEnd);
            changed = true;
        }
        if (progress > 0 && project.getRealStart() == null) {
            project.setRealStart(today());
            changed = true;
        }
        if (progress == 100 && project.getRealEnd() == null) {
            project.setRealEnd(today());
            changed = true;
        }
        if (changed) {
            saveQuietly(project);
        }
    }

    public GanttData gantt(Long projectId) {
        Project p = get(projectId);
        List<ProjectTask> tasks = tasks(projectId);
        List<ProjectTaskDependency> deps = dependencies(projectId);
        return new GanttData(p, tasks, deps, ProjectScheduler.criticalPath(tasks, deps));
    }

    public List<ProjectTask> lateTasks(Long projectId) {
        LocalDate today = today();
        return tasks(projectId).stream().filter(t -> t.getStatus() != ProjectTaskStatus.DONE && t.getPlanEnd() != null
                && t.getPlanEnd().isBefore(today)).toList();
    }
}
