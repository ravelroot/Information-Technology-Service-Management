package io.pyhoma.domain.model.itil;

import io.pyhoma.domain.model.itil.ItilItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "pyh_ticket")
public class Ticket extends ItilItem {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketType ticketType = TicketType.INCIDENT;

    @Column
    private Long sourceId;

    @Column
    private Long slaTtoId;

    @Column
    private Long slaTtrId;

    @Column
    private LocalDateTime ttoDueAt;

    @Column
    private LocalDateTime takenAt;

    @Column
    private LocalDateTime waitingSince;

    @Column(nullable = false)
    private Integer waitingMinutes = 0;

    @Column(nullable = false)
    private boolean slaWarned;

    @Column(nullable = false)
    private boolean slaBreachNotified;

    @Column
    private Integer satisfaction;

    @Column(length = 2000)
    private String satisfactionComment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.NONE;

    @Column
    private Long recurringId;

    public TicketType getTicketType() {
        return ticketType;
    }

    public void setTicketType(TicketType ticketType) {
        this.ticketType = ticketType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getSlaTtoId() {
        return slaTtoId;
    }

    public void setSlaTtoId(Long slaTtoId) {
        this.slaTtoId = slaTtoId;
    }

    public Long getSlaTtrId() {
        return slaTtrId;
    }

    public void setSlaTtrId(Long slaTtrId) {
        this.slaTtrId = slaTtrId;
    }

    public LocalDateTime getTtoDueAt() {
        return ttoDueAt;
    }

    public void setTtoDueAt(LocalDateTime ttoDueAt) {
        this.ttoDueAt = ttoDueAt;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(LocalDateTime takenAt) {
        this.takenAt = takenAt;
    }

    public LocalDateTime getWaitingSince() {
        return waitingSince;
    }

    public void setWaitingSince(LocalDateTime waitingSince) {
        this.waitingSince = waitingSince;
    }

    public Integer getWaitingMinutes() {
        return waitingMinutes;
    }

    public void setWaitingMinutes(Integer waitingMinutes) {
        this.waitingMinutes = waitingMinutes;
    }

    public boolean isSlaWarned() {
        return slaWarned;
    }

    public void setSlaWarned(boolean slaWarned) {
        this.slaWarned = slaWarned;
    }

    public boolean isSlaBreachNotified() {
        return slaBreachNotified;
    }

    public void setSlaBreachNotified(boolean slaBreachNotified) {
        this.slaBreachNotified = slaBreachNotified;
    }

    public Integer getSatisfaction() {
        return satisfaction;
    }

    public void setSatisfaction(Integer satisfaction) {
        this.satisfaction = satisfaction;
    }

    public String getSatisfactionComment() {
        return satisfactionComment;
    }

    public void setSatisfactionComment(String satisfactionComment) {
        this.satisfactionComment = satisfactionComment;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public Long getRecurringId() {
        return recurringId;
    }

    public void setRecurringId(Long recurringId) {
        this.recurringId = recurringId;
    }

    @Override
    public ItilType itilType() {
        return ItilType.TICKET;
    }
}
