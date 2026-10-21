package io.pyhoma.domain.model.asset;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.LongText;
import io.pyhoma.domain.model.Named;
import io.pyhoma.domain.model.OrgScoped;
import io.pyhoma.domain.model.SoftDeletable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pyh_contract", indexes = {
        @Index(name = "ix_contract_org_unit_id", columnList = "org_unit_id"),
        @Index(name = "ix_contract_supplier_id", columnList = "supplier_id")
})
public class Contract extends BaseEntity implements OrgScoped, SoftDeletable, Named {

    @Column(nullable = false)
    private Long orgUnitId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 80)
    private String contractNumber;

    @Column
    private Long typeId;

    @Column
    private Long supplierId;

    @Column
    private LocalDate startDate;

    @Column
    private Integer durationMonths;

    @Column
    private Integer noticeMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RenewalType renewal = RenewalType.NONE;

    @Column(precision = 15, scale = 2)
    private BigDecimal cost;

    @LongText
    @Column
    private String notes;

    @Column(nullable = false)
    private boolean deleted;

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

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public Integer getNoticeMonths() {
        return noticeMonths;
    }

    public void setNoticeMonths(Integer noticeMonths) {
        this.noticeMonths = noticeMonths;
    }

    public RenewalType getRenewal() {
        return renewal;
    }

    public void setRenewal(RenewalType renewal) {
        this.renewal = renewal;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
