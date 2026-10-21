package io.pyhoma.domain.model.rules;

import io.pyhoma.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_rule_action", indexes = {
        @Index(name = "ix_rule_action_rule_id", columnList = "rule_id")
})
public class RuleAction extends BaseEntity {

    @Column(nullable = false)
    private Long ruleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RuleActionType actionType;

    @Column(length = 500)
    private String actionValue;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public RuleActionType getActionType() {
        return actionType;
    }

    public void setActionType(RuleActionType actionType) {
        this.actionType = actionType;
    }

    public String getActionValue() {
        return actionValue;
    }

    public void setActionValue(String actionValue) {
        this.actionValue = actionValue;
    }
}
