package io.pyhoma.domain.model.project;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.LongText;
import io.pyhoma.domain.model.Named;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "pyh_project_task", indexes = {
        @Index(name = "ix_project_task_project_id", columnList = "project_id"),
        @Index(name = "ix_project_task_assignee_id", columnList = "assignee_id")
})
public class ProjectTask extends BaseEntity implements Named {

    @Column(nullable = false)
    private Long projectId;

    @Column
    private Long parentId;

    @Column(nullable = false, length = 200)
    private String name;

    @LongText
    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectTaskStatus status = ProjectTaskStatus.TODO;

    @Column
    private LocalDate planStart;

    @Column
    private LocalDate planEnd;

    @Column
    private LocalDate realStart;

    @Column
    private LocalDate realEnd;

    @Column(nullable = false)
    private Integer progress = 0;

    @Column
    private Long assigneeId;

    @Column
    private Integer effortHours;

    @Column(nullable = false)
    private boolean milestone;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectTaskStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectTaskStatus status) {
        this.status = status;
    }

    public LocalDate getPlanStart() {
        return planStart;
    }

    public void setPlanStart(LocalDate planStart) {
        this.planStart = planStart;
    }

    public LocalDate getPlanEnd() {
        return planEnd;
    }

    public void setPlanEnd(LocalDate planEnd) {
        this.planEnd = planEnd;
    }

    public LocalDate getRealStart() {
        return realStart;
    }

    public void setRealStart(LocalDate realStart) {
        this.realStart = realStart;
    }

    public LocalDate getRealEnd() {
        return realEnd;
    }

    public void setRealEnd(LocalDate realEnd) {
        this.realEnd = realEnd;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Integer getEffortHours() {
        return effortHours;
    }

    public void setEffortHours(Integer effortHours) {
        this.effortHours = effortHours;
    }

    public boolean isMilestone() {
        return milestone;
    }

    public void setMilestone(boolean milestone) {
        this.milestone = milestone;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Override
    public String displayName() {
        return name;
    }
}
