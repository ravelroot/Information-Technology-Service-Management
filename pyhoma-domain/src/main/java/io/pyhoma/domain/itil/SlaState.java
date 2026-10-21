package io.pyhoma.domain.itil;

public enum SlaState {
    NONE,
    ON_TIME,
    AT_RISK,
    BREACHED,
    MET,
    MISSED;

    public boolean isProblem() {
        return this == BREACHED || this == MISSED;
    }
}
