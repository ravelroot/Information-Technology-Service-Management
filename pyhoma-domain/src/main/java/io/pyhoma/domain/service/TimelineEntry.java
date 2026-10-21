package io.pyhoma.domain.service;

import java.time.LocalDateTime;

public record TimelineEntry(
        Kind kind,
        Long id,
        LocalDateTime at,
        Long authorId,
        String content,
        boolean privateNote,
        String state,
        Long assigneeId,
        int minutes,
        LocalDateTime plannedStart,
        LocalDateTime plannedEnd) {

    public enum Kind { CREATED, FOLLOWUP, TASK, SOLUTION, APPROVAL, STATUS }
}
