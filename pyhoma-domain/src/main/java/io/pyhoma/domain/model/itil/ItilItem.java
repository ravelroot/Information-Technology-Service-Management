package io.pyhoma.domain.model.itil;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.LongText;
import io.pyhoma.domain.model.Named;
import io.pyhoma.domain.model.OrgScoped;
import io.pyhoma.domain.model.SoftDeletable;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class ItilItem extends BaseEntity implements OrgScoped, SoftDeletable, Named {

    @Column(nullable = false)
    private Long orgUnitId;

    @Column(nullable = false, length = 255)
    private String title;

    @LongText
    @Column
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItilStatus status = ItilStatus.NEW;

    @Column(nullable = false)
    private Integer urgency = 3;

    @Column(nullable = false)
    private Integer impact = 3;

    @Column(nullable = false)
    private Integer priority = 3;

    @Column
    private Long categoryId;

    @Column
    private Long locationId;

    @Column
    private Long requesterId;

    @Column
    private Long assigneeId;

    @Column
    private Long assigneeGroupId;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    @Column
    private LocalDateTime dueAt;

    @Column
    private LocalDateTime solvedAt;

    @Column
    private LocalDateTime closedAt;

    @Column
    private LocalDateTime lastActivityAt;

    @Column(nullable = false)
    private Integer actionTimeMinutes = 0;

    @Column(nullable = false)
    private boolean deleted;

    public abstract ItilType itilType();

    @Override
    public String displayName() {
        return title;
    }

    public String reference() {
        return getId() == null ? "#-" : "#" + getId();
    }

    @Override
    public Long getOrgUnitId() {
        return orgUnitId;
    }

    @Override
    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ItilStatus getStatus() {
        return status;
    }

    public void setStatus(ItilStatus status) {
        this.status = status;
    }

    public Integer getUrgency() {
        return urgency;
    }

    public void setUrgency(Integer urgency) {
        this.urgency = urgency;
    }

    public Integer getImpact() {
        return impact;
    }

    public void setImpact(Integer impact) {
        this.impact = impact;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getAssigneeGroupId() {
        return assigneeGroupId;
    }

    public void setAssigneeGroupId(Long assigneeGroupId) {
        this.assigneeGroupId = assigneeGroupId;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public LocalDateTime getSolvedAt() {
        return solvedAt;
    }

    public void setSolvedAt(LocalDateTime solvedAt) {
        this.solvedAt = solvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public Integer getActionTimeMinutes() {
        return actionTimeMinutes;
    }

    public void setActionTimeMinutes(Integer actionTimeMinutes) {
        this.actionTimeMinutes = actionTimeMinutes;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
