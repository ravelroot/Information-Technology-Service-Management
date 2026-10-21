package io.pyhoma.domain.model.asset;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.LongText;
import io.pyhoma.domain.model.Named;
import io.pyhoma.domain.model.OrgScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pyh_software_license", indexes = {
        @Index(name = "ix_software_license_org_u_8556", columnList = "org_unit_id"),
        @Index(name = "ix_software_license_softw_68f2", columnList = "software_id")
})
public class SoftwareLicense extends BaseEntity implements OrgScoped, Named {

    @Column(nullable = false)
    private Long orgUnitId;

    @Column(nullable = false)
    private Long softwareId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 300)
    private String licenseKey;

    @Column
    private Integer seats;

    @Column
    private LocalDate expiresOn;

    @Column
    private Long supplierId;

    @Column(precision = 15, scale = 2)
    private BigDecimal purchaseCost;

    @LongText
    @Column
    private String notes;

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public Long getSoftwareId() {
        return softwareId;
    }

    public void setSoftwareId(Long softwareId) {
        this.softwareId = softwareId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public Integer getSeats() {
        return seats;
    }

    public void setSeats(Integer seats) {
        this.seats = seats;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public BigDecimal getPurchaseCost() {
        return purchaseCost;
    }

    public void setPurchaseCost(BigDecimal purchaseCost) {
        this.purchaseCost = purchaseCost;
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
