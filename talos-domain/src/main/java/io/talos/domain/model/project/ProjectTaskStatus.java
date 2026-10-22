package io.talos.domain.model.project;

public enum ProjectTaskStatus {
    TODO, IN_PROGRESS, REVIEW, BLOCKED, DONE;

    public boolean isDone() {
        return this == DONE;
    }
}
