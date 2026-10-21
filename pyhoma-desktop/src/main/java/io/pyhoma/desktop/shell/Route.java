package io.pyhoma.desktop.shell;

import io.pyhoma.domain.model.core.Module;

public enum Route {
    HOME(Section.MAIN, "house", null),
    DASHBOARD(Section.MAIN, "layout-dashboard", Module.TICKET),
    TICKETS(Section.SERVICE, "ticket", Module.TICKET),
    KANBAN(Section.SERVICE, "square-kanban", Module.TICKET),
    PROBLEMS(Section.SERVICE, "triangle-alert", Module.PROBLEM),
    CHANGES(Section.SERVICE, "git-pull-request-arrow", Module.CHANGE),
    PLANNING(Section.SERVICE, "calendar-clock", Module.TICKET),
    RECURRING(Section.SERVICE, "repeat", Module.RECURRING_TICKET),
    PROJECTS(Section.PROJECTS, "folder-kanban", Module.PROJECT),
    PORTFOLIO(Section.PROJECTS, "chart-gantt", Module.PROJECT),
    ASSETS(Section.INVENTORY, "monitor-smartphone", Module.ASSET),
    SOFTWARE(Section.INVENTORY, "app-window", Module.SOFTWARE),
    CONTRACTS(Section.INVENTORY, "file-signature", Module.CONTRACT),
    SUPPLIERS(Section.INVENTORY, "truck", Module.SUPPLIER),
    DOCUMENTS(Section.INVENTORY, "file-text", Module.DOCUMENT),
    KNOWLEDGE(Section.KNOWLEDGE, "book-open", Module.KNOWLEDGE_BASE),
    REPORTS(Section.KNOWLEDGE, "chart-column", Module.REPORT),
    USERS(Section.ADMIN, "users", Module.USER),
    GROUPS(Section.ADMIN, "users-round", Module.GROUP),
    PROFILES(Section.ADMIN, "shield-check", Module.PROFILE),
    ORG_UNITS(Section.ADMIN, "building-2", Module.ORG_UNIT),
    DROPDOWNS(Section.ADMIN, "list", Module.DROPDOWN),
    RULES(Section.ADMIN, "workflow", Module.RULE),
    SLAS(Section.ADMIN, "timer", Module.SLA),
    AUDIT(Section.ADMIN, "history", Module.AUDIT),
    SETTINGS(Section.ADMIN, "settings", Module.SETTINGS);

    public enum Section { MAIN, SERVICE, PROJECTS, INVENTORY, KNOWLEDGE, ADMIN }

    private final Section section;
    private final String icon;
    private final Module module;

    Route(Section section, String icon, Module module) {
        this.section = section;
        this.icon = icon;
        this.module = module;
    }

    public Section section() {
        return section;
    }

    public String icon() {
        return icon;
    }

    public Module module() {
        return module;
    }
}
