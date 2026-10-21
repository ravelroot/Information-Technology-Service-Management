package io.pyhoma.domain.model.itil;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.LongText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_itil_followup", indexes = {
        @Index(name = "ix_itil_followup_item_id", columnList = "item_id")
})
public class ItilFollowup extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItilType itemType;

    @Column(nullable = false)
    private Long itemId;

    @Column
    private Long authorId;

    @LongText
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean privateNote;

    @Column
    private Long sourceId;

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

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isPrivateNote() {
        return privateNote;
    }

    public void setPrivateNote(boolean privateNote) {
        this.privateNote = privateNote;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }
}
