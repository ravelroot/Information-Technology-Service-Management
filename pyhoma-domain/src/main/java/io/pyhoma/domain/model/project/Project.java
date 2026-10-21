package io.pyhoma.domain.model.project;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.LongText;
import io.pyhoma.domain.model.Named;
import io.pyhoma.domain.model.OrgScoped;
import io.pyhoma.domain.model.SoftDeletable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "pyh_project", indexes = {
        @Index(name = "ix_project_org_unit_id", columnList = "org_unit_id")
})
public class Project extends BaseEntity implements OrgScoped, SoftDeletable, Named {

    @Column(nullable = false)
    private Long orgUnitId;

    @Column(length = 40)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @LongText
    @Column
    private String description;

    @Column
    private Long stateId;

    @Column(nullable = false)
    private Integer priority = 3;

    @Column
    private Long managerId;

    @Column
    private Long groupId;

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
    private Long parentId;

    @Column(length = 20)
    private String color;

    @Column(nullable = false)
    private boolean deleted;

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public Long getStateId() {
        return stateId;
    }

    public void setStateId(Long stateId) {
        this.stateId = stateId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String displayName() {
        return name;
    }
}
