package io.pyhoma.domain.service;

import java.util.List;

public record ItilCard(List<Long> assignees, int tasksDone, int tasksTotal) {

    public static final ItilCard EMPTY = new ItilCard(List.of(), 0, 0);
}
