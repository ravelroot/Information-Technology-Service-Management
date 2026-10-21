package io.pyhoma.domain.model.itil;

public enum ItilType {
    TICKET(Ticket.class),
    PROBLEM(Problem.class),
    CHANGE(Change.class);

    private final Class<? extends ItilItem> entityClass;

    ItilType(Class<? extends ItilItem> entityClass) {
        this.entityClass = entityClass;
    }

    public Class<? extends ItilItem> entityClass() {
        return entityClass;
    }

    public static ItilType of(Class<?> type) {
        for (ItilType t : values()) {
            if (t.entityClass.equals(type)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Not an ITIL class: " + type);
    }
}
