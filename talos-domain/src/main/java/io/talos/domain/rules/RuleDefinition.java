package io.talos.domain.rules;

import io.talos.domain.model.rules.Rule;
import io.talos.domain.model.rules.RuleAction;
import io.talos.domain.model.rules.RuleCriterion;
import java.util.List;

public record RuleDefinition(Rule rule, List<RuleCriterion> criteria, List<RuleAction> actions) {

    public RuleDefinition {
        criteria = List.copyOf(criteria);
        actions = List.copyOf(actions);
    }
}
