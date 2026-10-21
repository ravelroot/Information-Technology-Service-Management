package io.pyhoma.domain.model.asset;

import io.pyhoma.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "pyh_software_install", indexes = {
        @Index(name = "ix_software_install_asset_id", columnList = "asset_id"),
        @Index(name = "ix_software_install_softw_c3f5", columnList = "software_id"),
        @Index(name = "ix_software_install_license_id", columnList = "license_id")
})
public class SoftwareInstallation extends BaseEntity {

    @Column(nullable = false)
    private Long assetId;

    @Column(nullable = false)
    private Long softwareId;

    @Column(length = 60)
    private String softwareVersion;

    @Column
    private Long licenseId;

    @Column
    private LocalDate installedOn;

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Long getSoftwareId() {
        return softwareId;
    }

    public void setSoftwareId(Long softwareId) {
        this.softwareId = softwareId;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public Long getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(Long licenseId) {
        this.licenseId = licenseId;
    }

    public LocalDate getInstalledOn() {
        return installedOn;
    }

    public void setInstalledOn(LocalDate installedOn) {
        this.installedOn = installedOn;
    }
}
