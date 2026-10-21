package io.pyhoma.domain.model.itil;

import io.pyhoma.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_itil_asset", indexes = {
        @Index(name = "ix_itil_asset_item_id", columnList = "item_id"),
        @Index(name = "ix_itil_asset_asset_id", columnList = "asset_id")
})
public class ItilAsset extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItilType itemType;

    @Column(nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private Long assetId;

    public ItilType getItemType() {
        return itemType;
    }

    public void setItemType(ItilType itemType) {
        this.itemType = itemType;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }
}
