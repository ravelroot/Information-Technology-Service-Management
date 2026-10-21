package io.pyhoma.domain.model.itil;

import java.util.List;

public enum ItilStatus {
    NEW(false),
    ACCEPTED(false),
    EVALUATION(false),
    APPROVAL(false),
    ASSIGNED(false),
    PLANNED(false),
    WAITING(false),
    TESTING(false),
    QUALIFICATION(false),
    APPLIED(false),
    REVIEW(false),
    SOLVED(true),
    CLOSED(true),
    CANCELED(true);

    private final boolean terminal;

    ItilStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean isOpen() {
        return !terminal;
    }

    private static final List<ItilStatus> TICKET =
            List.of(NEW, ASSIGNED, PLANNED, WAITING, SOLVED, CLOSED);
    private static final List<ItilStatus> PROBLEM =
            List.of(NEW, ACCEPTED, ASSIGNED, PLANNED, WAITING, SOLVED, CLOSED);
    private static final List<ItilStatus> CHANGE =
            List.of(NEW, EVALUATION, APPROVAL, ACCEPTED, WAITING, TESTING, QUALIFICATION, APPLIED, REVIEW, CLOSED, CANCELED);

    public static List<ItilStatus> workflow(ItilType type) {
        return switch (type) {
            case TICKET -> TICKET;
            case PROBLEM -> PROBLEM;
            case CHANGE -> CHANGE;
        };
    }

    public static List<ItilStatus> open(ItilType type) {
        return workflow(type).stream().filter(ItilStatus::isOpen).toList();
    }
}
