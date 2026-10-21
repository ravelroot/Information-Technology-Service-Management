package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.model.rules.Rule;
import io.pyhoma.domain.model.rules.RuleAction;
import io.pyhoma.domain.model.rules.RuleCriterion;
import io.pyhoma.domain.model.rules.RuleTrigger;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.rules.RuleDefinition;
import io.pyhoma.domain.rules.RuleEngine;
import io.pyhoma.domain.rules.RuleOutcome;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.Fields;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.Validator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class RuleService extends AbstractService {

    public RuleService(Services services) {
        super(services);
    }

    public List<RuleDefinition> all() {
        List<RuleDefinition> result = new ArrayList<>();
        for (Rule r : repo(Rule.class).find(Query.all().orderBy(Sort.asc("ranking"), Sort.asc("id")))) {
            result.add(load(r));
        }
        return result;
    }

    public RuleDefinition get(Long id) {
        return load(repo(Rule.class).getById(id));
    }

    private RuleDefinition load(Rule r) {
        List<RuleCriterion> criteria = repo(RuleCriterion.class).find(Query.where(Filter.eq("ruleId", r.getId())).orderBy(Sort.asc("id")));
        List<RuleAction> actions = repo(RuleAction.class).find(Query.where(Filter.eq("ruleId", r.getId())).orderBy(Sort.asc("id")));
        return new RuleDefinition(r, criteria, actions);
    }

    public RuleDefinition save(RuleDefinition def) {
        require(Module.RULE, def.rule().isNew() ? Right.CREATE : Right.UPDATE);
        return saveInternal(def);
    }

    RuleDefinition saveInternal(RuleDefinition def) {
        Rule rule = def.rule();
        rule.setName(Texts.clean(rule.getName()));
        Validator.create().required("name", rule.getName()).required("triggerEvent", rule.getTriggerEvent())
                .check(!def.criteria().isEmpty(), "criteria", "validation.atLeastOneCriterion")
                .check(!def.actions().isEmpty(), "actions", "validation.atLeastOneAction")
                .check(def.criteria().stream().allMatch(RuleService::validPattern), "criteria", "validation.pattern")
                .throwIfInvalid();
        return tx(() -> {
            if (rule.isNew() && (rule.getRanking() == null || rule.getRanking() == 0)) {
                rule.setRanking((int) repo(Rule.class).count(Filter.ALL) + 1);
            }
            Rule saved = persist(rule);
            repo(RuleCriterion.class).deleteWhere(Filter.eq("ruleId", saved.getId()));
            repo(RuleAction.class).deleteWhere(Filter.eq("ruleId", saved.getId()));
            for (RuleCriterion c : def.criteria()) {
                RuleCriterion copy = Fields.copy(c);
                copy.setId(null);
                copy.setVersion(null);
                copy.setRuleId(saved.getId());
                saveQuietly(copy);
            }
            for (RuleAction a : def.actions()) {
                RuleAction copy = Fields.copy(a);
                copy.setId(null);
                copy.setVersion(null);
                copy.setRuleId(saved.getId());
                saveQuietly(copy);
            }
            return load(saved);
        });
    }

    public void delete(Long id) {
        require(Module.RULE, Right.DELETE);
        txRun(() -> {
            repo(RuleCriterion.class).deleteWhere(Filter.eq("ruleId", id));
            repo(RuleAction.class).deleteWhere(Filter.eq("ruleId", id));
            purge(repo(Rule.class).getById(id));
        });
    }

    public void move(Long id, boolean up) {
        require(Module.RULE, Right.UPDATE);
        txRun(() -> {
            List<Rule> rules = repo(Rule.class).find(Query.all().orderBy(Sort.asc("ranking"), Sort.asc("id")));
            for (int i = 0; i < rules.size(); i++) {
                if (rules.get(i).getId().equals(id)) {
                    int j = up ? i - 1 : i + 1;
                    if (j < 0 || j >= rules.size()) {
                        return;
                    }
                    Rule a = rules.get(i);
                    Rule b = rules.get(j);
                    for (int k = 0; k < rules.size(); k++) {
                        rules.get(k).setRanking(k + 1);
                    }
                    a.setRanking(j + 1);
                    b.setRanking(i + 1);
                    rules.forEach(this::saveQuietly);
                    return;
                }
            }
        });
    }

    public RuleOutcome apply(Ticket ticket, RuleTrigger trigger) {
        List<RuleDefinition> active = new ArrayList<>();
        for (Rule r : repo(Rule.class).find(Query.where(Filter.and(Filter.eq("triggerEvent", trigger), Filter.isTrue("active")))
                .orderBy(Sort.asc("ranking"), Sort.asc("id")))) {
            active.add(load(r));
        }
        if (active.isEmpty()) {
            return new RuleOutcome();
        }
        HashSet<Long> groups = new HashSet<>();
        if (ticket.getRequesterId() != null) {
            groups.addAll(services.users().groupIds(ticket.getRequesterId()));
        }
        return RuleEngine.run(active, ticket, groups);
    }

    public RuleOutcome test(RuleDefinition def, Ticket sample) {
        Ticket copy = Fields.copy(sample);
        HashSet<Long> groups = new HashSet<>();
        if (copy.getRequesterId() != null) {
            groups.addAll(services.users().groupIds(copy.getRequesterId()));
        }
        Rule r = Fields.copy(def.rule());
        r.setActive(true);
        return RuleEngine.run(List.of(new RuleDefinition(r, def.criteria(), def.actions())), copy, groups);
    }

    private static boolean validPattern(io.pyhoma.domain.model.rules.RuleCriterion c) {
        String p = c.getPattern();
        if (p == null) {
            return true;
        }
        if (p.length() > io.pyhoma.domain.rules.RuleEngine.MAX_PATTERN_LENGTH) {
            return false;
        }
        if (c.getMatchOperator() == io.pyhoma.domain.model.rules.RuleOperator.REGEX) {
            try {
                java.util.regex.Pattern.compile(p);
            } catch (java.util.regex.PatternSyntaxException e) {
                return false;
            }
        }
        return true;
    }
}
