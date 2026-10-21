package io.pyhoma.domain.model.core;

import io.pyhoma.domain.security.Right;

public enum Module {
    TICKET(Section.ASSISTANCE, Right.CRUD | Right.PURGE | Right.ASSIGN | Right.SEE_ALL | Right.APPROVE),
    PROBLEM(Section.ASSISTANCE, Right.CRUD | Right.PURGE),
    CHANGE(Section.ASSISTANCE, Right.CRUD | Right.PURGE | Right.APPROVE),
    RECURRING_TICKET(Section.ASSISTANCE, Right.CRUD),
    ASSET(Section.ASSETS, Right.CRUD | Right.PURGE),
    SOFTWARE(Section.ASSETS, Right.CRUD | Right.PURGE),
    CONTRACT(Section.MANAGEMENT, Right.CRUD | Right.PURGE),
    SUPPLIER(Section.MANAGEMENT, Right.CRUD),
    DOCUMENT(Section.MANAGEMENT, Right.CRUD | Right.PURGE),
    PROJECT(Section.TOOLS, Right.CRUD | Right.PURGE),
    KNOWLEDGE_BASE(Section.TOOLS, Right.CRUD | Right.PURGE),
    REPORT(Section.TOOLS, Right.READ),
    USER(Section.ADMINISTRATION, Right.CRUD),
    GROUP(Section.ADMINISTRATION, Right.CRUD),
    PROFILE(Section.ADMINISTRATION, Right.CRUD),
    ORG_UNIT(Section.ADMINISTRATION, Right.CRUD),
    DROPDOWN(Section.ADMINISTRATION, Right.CRUD),
    RULE(Section.ADMINISTRATION, Right.CRUD),
    SLA(Section.ADMINISTRATION, Right.CRUD),
    SETTINGS(Section.ADMINISTRATION, Right.READ | Right.UPDATE),
    AUDIT(Section.ADMINISTRATION, Right.READ);

    public enum Section { ASSISTANCE, ASSETS, MANAGEMENT, TOOLS, ADMINISTRATION }

    private final Section section;
    private final int allowedRights;

    Module(Section section, int allowedRights) {
        this.section = section;
        this.allowedRights = allowedRights;
    }

    public Section section() {
        return section;
    }

    public int allowedRights() {
        return allowedRights;
    }

    public boolean supports(int right) {
        return (allowedRights & right) == right;
    }
}
