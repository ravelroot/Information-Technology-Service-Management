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
import java.time.LocalDateTime;

@Entity
@Table(name = "pyh_asset", indexes = {
        @Index(name = "ix_asset_org_unit_id", columnList = "org_unit_id"),
        @Index(name = "ix_asset_asset_type", columnList = "asset_type"),
        @Index(name = "ix_asset_serial_number", columnList = "serial_number"),
        @Index(name = "ix_asset_asset_tag", columnList = "asset_tag"),
        @Index(name = "ix_asset_state_id", columnList = "state_id"),
        @Index(name = "ix_asset_location_id", columnList = "location_id"),
        @Index(name = "ix_asset_user_id", columnList = "user_id")
})
public class Asset extends BaseEntity implements OrgScoped, SoftDeletable, Named {

    @Column(nullable = false)
    private Long orgUnitId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssetType assetType;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 120)
    private String serialNumber;

    @Column(length = 120)
    private String assetTag;

    @Column
    private Long stateId;

    @Column
    private Long locationId;

    @Column
    private Long manufacturerId;

    @Column
    private Long modelId;

    @Column
    private Long userId;

    @Column
    private Long groupId;

    @Column
    private Long techUserId;

    @Column
    private Long techGroupId;

    @Column
    private Long supplierId;

    @Column
    private LocalDate purchaseDate;

    @Column
    private LocalDate warrantyExpiresOn;

    @Column(precision = 15, scale = 2)
    private BigDecimal purchaseCost;

    @Column(length = 150)
    private String hostname;

    @Column(length = 60)
    private String ipAddress;

    @Column(length = 60)
    private String macAddress;

    @Column
    private Long operatingSystemId;

    @Column(length = 80)
    private String osVersion;

    @Column(length = 150)
    private String cpu;

    @Column
    private Integer ramMb;

    @Column
    private Integer storageGb;

    @Column
    private LocalDateTime lastInventoryAt;

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

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public Long getStateId() {
        return stateId;
    }

    public void setStateId(Long stateId) {
        this.stateId = stateId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Long getManufacturerId() {
        return manufacturerId;
    }

    public void setManufacturerId(Long manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getTechUserId() {
        return techUserId;
    }

    public void setTechUserId(Long techUserId) {
        this.techUserId = techUserId;
    }

    public Long getTechGroupId() {
        return techGroupId;
    }

    public void setTechGroupId(Long techGroupId) {
        this.techGroupId = techGroupId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public LocalDate getWarrantyExpiresOn() {
        return warrantyExpiresOn;
    }

    public void setWarrantyExpiresOn(LocalDate warrantyExpiresOn) {
        this.warrantyExpiresOn = warrantyExpiresOn;
    }

    public BigDecimal getPurchaseCost() {
        return purchaseCost;
    }

    public void setPurchaseCost(BigDecimal purchaseCost) {
        this.purchaseCost = purchaseCost;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public Long getOperatingSystemId() {
        return operatingSystemId;
    }

    public void setOperatingSystemId(Long operatingSystemId) {
        this.operatingSystemId = operatingSystemId;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }

    public Integer getRamMb() {
        return ramMb;
    }

    public void setRamMb(Integer ramMb) {
        this.ramMb = ramMb;
    }

    public Integer getStorageGb() {
        return storageGb;
    }

    public void setStorageGb(Integer storageGb) {
        this.storageGb = storageGb;
    }

    public LocalDateTime getLastInventoryAt() {
        return lastInventoryAt;
    }

    public void setLastInventoryAt(LocalDateTime lastInventoryAt) {
        this.lastInventoryAt = lastInventoryAt;
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
