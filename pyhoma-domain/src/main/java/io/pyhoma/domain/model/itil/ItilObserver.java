package io.pyhoma.domain.model.itil;

import io.pyhoma.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_itil_observer", indexes = {
        @Index(name = "ix_itil_observer_item_id", columnList = "item_id"),
        @Index(name = "ix_itil_observer_user_id", columnList = "user_id")
})
public class ItilObserver extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItilType itemType;

    @Column(nullable = false)
    private Long itemId;

    @Column
    private Long userId;

    @Column
    private Long groupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItilActorRole actorRole = ItilActorRole.FOLLOWER;

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

    public ItilActorRole getActorRole() {
        return actorRole;
    }

    public void setActorRole(ItilActorRole actorRole) {
        this.actorRole = actorRole;
    }
}
