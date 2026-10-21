package io.pyhoma.domain.model.asset;

import io.pyhoma.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_contract_item", indexes = {
        @Index(name = "ix_contract_item_contract_id", columnList = "contract_id"),
        @Index(name = "ix_contract_item_asset_id", columnList = "asset_id")
})
public class ContractItem extends BaseEntity {

    @Column(nullable = false)
    private Long contractId;

    @Column(nullable = false)
    private Long assetId;

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }
}
