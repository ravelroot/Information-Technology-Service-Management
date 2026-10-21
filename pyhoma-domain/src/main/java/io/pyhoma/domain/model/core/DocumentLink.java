package io.pyhoma.domain.model.core;

import io.pyhoma.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_document_link", indexes = {
        @Index(name = "ix_document_link_document_id", columnList = "document_id"),
        @Index(name = "ix_document_link_item_id", columnList = "item_id")
})
public class DocumentLink extends BaseEntity {

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false, length = 40)
    private String itemType;

    @Column(nullable = false)
    private Long itemId;

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
}
