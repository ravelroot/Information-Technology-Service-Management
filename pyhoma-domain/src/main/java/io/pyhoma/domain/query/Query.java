package io.pyhoma.domain.query;

import java.util.ArrayList;
import java.util.List;

public record Query(Filter filter, List<Sort> sorts, int offset, int limit) {

    public static final int UNLIMITED = -1;

    public Query {
        filter = filter == null ? Filter.ALL : filter;
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }

    public static Query all() {
        return new Query(Filter.ALL, List.of(), 0, UNLIMITED);
    }

    public static Query where(Filter filter) {
        return new Query(filter, List.of(), 0, UNLIMITED);
    }

    public Query orderBy(Sort... sort) {
        List<Sort> s = new ArrayList<>(sorts);
        s.addAll(List.of(sort));
        return new Query(filter, s, offset, limit);
    }

    public Query page(int pageIndex, int pageSize) {
        return new Query(filter, sorts, Math.max(0, pageIndex) * pageSize, pageSize);
    }

    public Query limit(int max) {
        return new Query(filter, sorts, offset, max);
    }

    public Query and(Filter extra) {
        return new Query(Filter.and(filter, extra), sorts, offset, limit);
    }

    public boolean isPaged() {
        return limit > 0;
    }
}
