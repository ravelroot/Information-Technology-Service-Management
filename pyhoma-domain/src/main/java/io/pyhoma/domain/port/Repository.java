package io.pyhoma.domain.port;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.support.NotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Repository<T extends BaseEntity> {

    Class<T> entityType();

    Optional<T> findById(Long id);

    default T getById(Long id) {
        return findById(id).orElseThrow(() -> new NotFoundException(entityType().getSimpleName(), id));
    }

    List<T> findAllById(Collection<Long> ids);

    List<T> find(Query query);

    default List<T> find(Filter filter) {
        return find(Query.where(filter));
    }

    default Optional<T> findFirst(Query query) {
        List<T> rows = find(query.limit(1));
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    default Optional<T> findFirst(Filter filter) {
        return findFirst(Query.where(filter));
    }

    PageResult<T> page(Query query);

    long count(Filter filter);

    default boolean exists(Filter filter) {
        return count(filter) > 0;
    }

    Map<Object, Long> countBy(String field, Filter filter);

    T save(T entity);

    void delete(T entity);

    int deleteWhere(Filter filter);
}
