package io.pyhoma.domain.model.itil;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.LongText;
import io.pyhoma.domain.model.Named;
import io.pyhoma.domain.model.OrgScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "pyh_recurring_ticket", indexes = {
        @Index(name = "ix_recurring_ticket_org_u_c5ce", columnList = "org_unit_id")
})
public class RecurringTicket extends BaseEntity implements OrgScoped, Named {

    @Column(nullable = false)
    private Long orgUnitId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, length = 255)
    private String title;

    @LongText
    @Column
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketType ticketType = TicketType.REQUEST;

    @Column
    private Long categoryId;

    @Column(nullable = false)
    private Integer urgency = 3;

    @Column(nullable = false)
    private Integer impact = 3;

    @Column
    private Long assigneeId;

    @Column
    private Long assigneeGroupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Periodicity periodicity = Periodicity.MONTHLY;

    @Column(nullable = false)
    private Integer intervalCount = 1;

    @Column
    private LocalDateTime nextRunAt;

    @Column
    private LocalDateTime lastRunAt;

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public TicketType getTicketType() {
        return ticketType;
    }

    public void setTicketType(TicketType ticketType) {
        this.ticketType = ticketType;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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

    public Periodicity getPeriodicity() {
        return periodicity;
    }

    public void setPeriodicity(Periodicity periodicity) {
        this.periodicity = periodicity;
    }

    public Integer getIntervalCount() {
        return intervalCount;
    }

    public void setIntervalCount(Integer intervalCount) {
        this.intervalCount = intervalCount;
    }

    public LocalDateTime getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(LocalDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(LocalDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    @Override
    public String displayName() {
        return name;
    }
}
