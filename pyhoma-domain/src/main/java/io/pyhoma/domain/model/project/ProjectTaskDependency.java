package io.pyhoma.domain.model.project;

import io.pyhoma.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_project_dependency", indexes = {
        @Index(name = "ix_project_dependency_task_id", columnList = "task_id"),
        @Index(name = "ix_project_dependency_pre_d848", columnList = "predecessor_id")
})
public class ProjectTaskDependency extends BaseEntity {

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Long predecessorId;

    @Column(nullable = false)
    private Integer lagDays = 0;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getPredecessorId() {
        return predecessorId;
    }

    public void setPredecessorId(Long predecessorId) {
        this.predecessorId = predecessorId;
    }

    public Integer getLagDays() {
        return lagDays;
    }

    public void setLagDays(Integer lagDays) {
        this.lagDays = lagDays;
    }
}
