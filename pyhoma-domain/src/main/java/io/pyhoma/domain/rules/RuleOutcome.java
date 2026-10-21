package io.pyhoma.domain.rules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RuleOutcome {

    private final List<String> appliedRules = new ArrayList<>();
    private final List<String> changes = new ArrayList<>();
    private final Set<Long> observers = new LinkedHashSet<>();
    private final Set<Long> approvers = new LinkedHashSet<>();
    private boolean prioritySet;

    public List<String> appliedRules() {
        return appliedRules;
    }

    public List<String> changes() {
        return changes;
    }

    public Set<Long> observers() {
        return observers;
    }

    public Set<Long> approvers() {
        return approvers;
    }

    public boolean prioritySet() {
        return prioritySet;
    }

    void markPrioritySet() {
        this.prioritySet = true;
    }

    public boolean matchedAny() {
        return !appliedRules.isEmpty();
    }
}
