package io.pyhoma.domain.model.core;

public enum DropdownKind {
    LOCATION(true, false),
    ITIL_CATEGORY(true, false),
    KB_CATEGORY(true, false),
    MANUFACTURER(false, false),
    ASSET_MODEL(false, false),
    ASSET_STATE(false, true),
    OPERATING_SYSTEM(false, false),
    REQUEST_SOURCE(false, false),
    SOLUTION_TYPE(false, false),
    TASK_CATEGORY(false, false),
    SOFTWARE_CATEGORY(false, false),
    SUPPLIER_TYPE(false, false),
    CONTRACT_TYPE(false, false),
    DOCUMENT_CATEGORY(false, false),
    PROJECT_STATE(false, true);

    private final boolean hierarchical;
    private final boolean colored;

    DropdownKind(boolean hierarchical, boolean colored) {
        this.hierarchical = hierarchical;
        this.colored = colored;
    }

    public boolean isHierarchical() {
        return hierarchical;
    }

    public boolean isColored() {
        return colored;
    }
}
