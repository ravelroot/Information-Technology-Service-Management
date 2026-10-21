package io.pyhoma.domain.model.core;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.LongText;
import io.pyhoma.domain.model.Named;
import io.pyhoma.domain.model.OrgScoped;
import io.pyhoma.domain.model.SoftDeletable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_document", indexes = {
        @Index(name = "ix_document_org_unit_id", columnList = "org_unit_id")
})
public class Document extends BaseEntity implements OrgScoped, SoftDeletable, Named {

    @Column(nullable = false)
    private Long orgUnitId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column
    private Long categoryId;

    @Column(nullable = false, length = 1000)
    private String uri;

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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
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
