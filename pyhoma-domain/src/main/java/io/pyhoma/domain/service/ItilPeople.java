package io.pyhoma.domain.service;

import java.util.List;

public record ItilPeople(List<Long> assignees, List<Long> followers) {

    public ItilPeople {
        assignees = assignees == null ? List.of() : List.copyOf(assignees);
        followers = followers == null ? List.of() : List.copyOf(followers);
    }
}
