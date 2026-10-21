package io.pyhoma.domain.rules;

import io.pyhoma.domain.model.rules.Rule;
import io.pyhoma.domain.model.rules.RuleAction;
import io.pyhoma.domain.model.rules.RuleCriterion;
import java.util.List;

public record RuleDefinition(Rule rule, List<RuleCriterion> criteria, List<RuleAction> actions) {

    public RuleDefinition {
        criteria = List.copyOf(criteria);
        actions = List.copyOf(actions);
    }
}
