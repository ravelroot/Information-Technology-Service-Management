package io.pyhoma.domain.model.rules;

import io.pyhoma.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "pyh_rule_criterion", indexes = {
        @Index(name = "ix_rule_criterion_rule_id", columnList = "rule_id")
})
public class RuleCriterion extends BaseEntity {

    @Column(nullable = false)
    private Long ruleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RuleField field;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RuleOperator matchOperator;

    @Column(length = 500)
    private String pattern;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public RuleField getField() {
        return field;
    }

    public void setField(RuleField field) {
        this.field = field;
    }

    public RuleOperator getMatchOperator() {
        return matchOperator;
    }

    public void setMatchOperator(RuleOperator matchOperator) {
        this.matchOperator = matchOperator;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}
