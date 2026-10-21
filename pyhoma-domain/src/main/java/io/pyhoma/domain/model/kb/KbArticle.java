package io.pyhoma.domain.model.kb;

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
@Table(name = "pyh_kb_article", indexes = {
        @Index(name = "ix_kb_article_org_unit_id", columnList = "org_unit_id"),
        @Index(name = "ix_kb_article_category_id", columnList = "category_id")
})
public class KbArticle extends BaseEntity implements OrgScoped, SoftDeletable, Named {

    @Column(nullable = false)
    private Long orgUnitId;

    @Column
    private Long categoryId;

    @Column(nullable = false, length = 255)
    private String title;

    @LongText
    @Column
    private String content;

    @Column(nullable = false)
    private boolean faq;

    @Column(nullable = false)
    private boolean published = true;

    @Column
    private Long authorId;

    @Column
    private Long updatedById;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @Column(nullable = false)
    private boolean deleted;

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isFaq() {
        return faq;
    }

    public void setFaq(boolean faq) {
        this.faq = faq;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public Long getUpdatedById() {
        return updatedById;
    }

    public void setUpdatedById(Long updatedById) {
        this.updatedById = updatedById;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String displayName() {
        return title;
    }
}
