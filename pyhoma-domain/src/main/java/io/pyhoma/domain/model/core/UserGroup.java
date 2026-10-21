package io.pyhoma.domain.model.core;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.LongText;
import io.pyhoma.domain.model.Named;
import io.pyhoma.domain.model.OrgScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_group", indexes = {
        @Index(name = "ix_group_org_unit_id", columnList = "org_unit_id")
})
public class UserGroup extends BaseEntity implements OrgScoped, Named {

    @Column(nullable = false)
    private Long orgUnitId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column
    private Long parentId;

    @Column(nullable = false)
    private boolean assignable = true;

    @Column(nullable = false)
    private boolean requester = true;

    @Column(length = 150)
    private String email;

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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public boolean isAssignable() {
        return assignable;
    }

    public void setAssignable(boolean assignable) {
        this.assignable = assignable;
    }

    public boolean isRequester() {
        return requester;
    }

    public void setRequester(boolean requester) {
        this.requester = requester;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
