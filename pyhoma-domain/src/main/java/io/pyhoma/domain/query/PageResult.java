package io.pyhoma.domain.query;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> items, long total, int offset, int limit) {

    public PageResult {
        items = List.copyOf(items);
    }

    public int pageCount() {
        if (limit <= 0) {
            return 1;
        }
        return (int) Math.max(1, (total + limit - 1) / limit);
    }

    public int pageIndex() {
        return limit <= 0 ? 0 : offset / limit;
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), total, offset, limit);
    }
}
