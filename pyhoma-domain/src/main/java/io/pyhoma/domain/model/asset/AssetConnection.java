package io.pyhoma.domain.model.asset;

import io.pyhoma.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_asset_connection", indexes = {
        @Index(name = "ix_asset_connection_asset_id", columnList = "asset_id"),
        @Index(name = "ix_asset_connection_conne_9ac6", columnList = "connected_asset_id")
})
public class AssetConnection extends BaseEntity {

    @Column(nullable = false)
    private Long assetId;

    @Column(nullable = false)
    private Long connectedAssetId;

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Long getConnectedAssetId() {
        return connectedAssetId;
    }

    public void setConnectedAssetId(Long connectedAssetId) {
        this.connectedAssetId = connectedAssetId;
    }
}
