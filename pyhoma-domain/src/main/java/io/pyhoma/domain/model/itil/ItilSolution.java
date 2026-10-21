package io.pyhoma.domain.model.itil;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.LongText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "pyh_itil_solution", indexes = {
        @Index(name = "ix_itil_solution_item_id", columnList = "item_id")
})
public class ItilSolution extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItilType itemType;

    @Column(nullable = false)
    private Long itemId;

    @Column
    private Long authorId;

    @Column
    private Long solutionTypeId;

    @LongText
    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SolutionStatus status = SolutionStatus.PROPOSED;

    @Column
    private Long answeredById;

    @Column
    private LocalDateTime answeredAt;

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

    public Long getSolutionTypeId() {
        return solutionTypeId;
    }

    public void setSolutionTypeId(Long solutionTypeId) {
        this.solutionTypeId = solutionTypeId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public SolutionStatus getStatus() {
        return status;
    }

    public void setStatus(SolutionStatus status) {
        this.status = status;
    }

    public Long getAnsweredById() {
        return answeredById;
    }

    public void setAnsweredById(Long answeredById) {
        this.answeredById = answeredById;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }
}
