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

@Entity
@Table(name = "pyh_sla", indexes = {
        @Index(name = "ix_sla_org_unit_id", columnList = "org_unit_id")
})
public class Sla extends BaseEntity implements OrgScoped, Named {

    @Column(nullable = false)
    private Long orgUnitId;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SlaType slaType;

    @Column(nullable = false)
    private Integer durationMinutes = 0;

    @Column(nullable = false)
    private boolean businessHours = true;

    @Column
    private Integer priority;

    @Column(nullable = false)
    private boolean active = true;

    @LongText
    @Column
    private String notes;

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

    public SlaType getSlaType() {
        return slaType;
    }

    public void setSlaType(SlaType slaType) {
        this.slaType = slaType;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public boolean isBusinessHours() {
        return businessHours;
    }

    public void setBusinessHours(boolean businessHours) {
        this.businessHours = businessHours;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String displayName() {
        return name;
    }
}
