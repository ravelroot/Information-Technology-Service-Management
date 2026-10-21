package io.pyhoma.domain.model.itil;

import io.pyhoma.domain.model.LongText;
import io.pyhoma.domain.model.itil.ItilItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "pyh_change")
public class Change extends ItilItem {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChangeType changeType = ChangeType.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel risk = RiskLevel.MEDIUM;

    @LongText
    @Column
    private String impactContent;

    @LongText
    @Column
    private String rolloutPlan;

    @LongText
    @Column
    private String backoutPlan;

    @LongText
    @Column
    private String checklist;

    @Column
    private LocalDateTime plannedStartAt;

    @Column
    private LocalDateTime plannedEndAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.NONE;

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public RiskLevel getRisk() {
        return risk;
    }

    public void setRisk(RiskLevel risk) {
        this.risk = risk;
    }

    public String getImpactContent() {
        return impactContent;
    }

    public void setImpactContent(String impactContent) {
        this.impactContent = impactContent;
    }

    public String getRolloutPlan() {
        return rolloutPlan;
    }

    public void setRolloutPlan(String rolloutPlan) {
        this.rolloutPlan = rolloutPlan;
    }

    public String getBackoutPlan() {
        return backoutPlan;
    }

    public void setBackoutPlan(String backoutPlan) {
        this.backoutPlan = backoutPlan;
    }

    public String getChecklist() {
        return checklist;
    }

    public void setChecklist(String checklist) {
        this.checklist = checklist;
    }

    public LocalDateTime getPlannedStartAt() {
        return plannedStartAt;
    }

    public void setPlannedStartAt(LocalDateTime plannedStartAt) {
        this.plannedStartAt = plannedStartAt;
    }

    public LocalDateTime getPlannedEndAt() {
        return plannedEndAt;
    }

    public void setPlannedEndAt(LocalDateTime plannedEndAt) {
        this.plannedEndAt = plannedEndAt;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    @Override
    public ItilType itilType() {
        return ItilType.CHANGE;
    }
}
