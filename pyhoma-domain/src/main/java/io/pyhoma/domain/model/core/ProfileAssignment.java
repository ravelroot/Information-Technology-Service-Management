package io.pyhoma.domain.model.core;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.OrgScoped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_profile_assignment", indexes = {
        @Index(name = "ix_profile_assignment_user_id", columnList = "user_id")
})
public class ProfileAssignment extends BaseEntity implements OrgScoped {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long profileId;

    @Column(nullable = false)
    private Long orgUnitId;

    @Column(nullable = false)
    private boolean includeChildren = true;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public boolean isIncludeChildren() {
        return includeChildren;
    }

    public void setIncludeChildren(boolean includeChildren) {
        this.includeChildren = includeChildren;
    }
}
