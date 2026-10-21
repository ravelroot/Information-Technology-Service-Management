package io.pyhoma.domain.model.rules;

public enum RuleField {
    TITLE(false), CONTENT(false), TICKET_TYPE(false), URGENCY(false), IMPACT(false), PRIORITY(false),
    CATEGORY(true), LOCATION(true), REQUESTER(true), REQUESTER_GROUP(true), ORG_UNIT(true), SOURCE(true);

    private final boolean reference;

    RuleField(boolean reference) {
        this.reference = reference;
    }

    public boolean isReference() {
        return reference;
    }
}
