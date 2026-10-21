package io.pyhoma.domain.model.core;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.LongText;
import io.pyhoma.domain.model.Named;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_dropdown", indexes = {
        @Index(name = "ix_dropdown_kind", columnList = "kind")
})
public class DropdownItem extends BaseEntity implements Named {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DropdownKind kind;

    @Column(nullable = false, length = 150)
    private String name;

    @Column
    private Long parentId;

    @Column(length = 600)
    private String completeName;

    @Column(length = 20)
    private String color;

    @Column(nullable = false)
    private boolean flag;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @LongText
    @Column
    private String notes;

    public DropdownKind getKind() {
        return kind;
    }

    public void setKind(DropdownKind kind) {
        this.kind = kind;
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

    public String getCompleteName() {
        return completeName;
    }

    public void setCompleteName(String completeName) {
        this.completeName = completeName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
