package io.pyhoma.domain.model.rules;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.Named;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_rule")
public class Rule extends BaseEntity implements Named {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RuleTrigger triggerEvent = RuleTrigger.ON_CREATE;

    @Column(nullable = false)
    private Integer ranking = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean matchAll = true;

    @Column(nullable = false)
    private boolean stopProcessing;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RuleTrigger getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(RuleTrigger triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    public Integer getRanking() {
        return ranking;
    }

    public void setRanking(Integer ranking) {
        this.ranking = ranking;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isMatchAll() {
        return matchAll;
    }

    public void setMatchAll(boolean matchAll) {
        this.matchAll = matchAll;
    }

    public boolean isStopProcessing() {
        return stopProcessing;
    }

    public void setStopProcessing(boolean stopProcessing) {
        this.stopProcessing = stopProcessing;
    }

    @Override
    public String displayName() {
        return name;
    }
}
