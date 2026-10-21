package io.pyhoma.domain.memory;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.port.Repository;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.support.ConcurrentUpdateException;
import io.pyhoma.domain.support.Fields;
import io.pyhoma.domain.support.Texts;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

final class InMemoryRepository<T extends BaseEntity> implements Repository<T> {

    private final Class<T> type;
    private final ReentrantLock lock;
    private TreeMap<Long, T> rows = new TreeMap<>();
    private final AtomicLong sequence = new AtomicLong();

    InMemoryRepository(Class<T> type, ReentrantLock lock) {
        this.type = type;
        this.lock = lock;
    }

    @Override
    public Class<T> entityType() {
        return type;
    }

    @Override
    public Optional<T> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        lock.lock();
        try {
            T row = rows.get(id);
            return row == null ? Optional.empty() : Optional.of(Fields.copy(row));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<T> findAllById(Collection<Long> ids) {
        lock.lock();
        try {
            List<T> result = new ArrayList<>();
            for (Long id : ids) {
                T row = id == null ? null : rows.get(id);
                if (row != null) {
                    result.add(Fields.copy(row));
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<T> find(Query query) {
        lock.lock();
        try {
            List<T> matching = filtered(query.filter());
            matching.sort(comparator(query.sorts()));
            int from = Math.min(query.offset(), matching.size());
            int to = query.isPaged() ? Math.min(matching.size(), from + query.limit()) : matching.size();
            return matching.subList(from, to).stream().map(Fields::copy).toList();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public PageResult<T> page(Query query) {
        lock.lock();
        try {
            long total = filtered(query.filter()).size();
            return new PageResult<>(find(query), total, query.offset(), query.limit());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long count(Filter filter) {
        lock.lock();
        try {
            return filtered(filter).size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<Object, Long> countBy(String field, Filter filter) {
        lock.lock();
        try {
            Map<Object, Long> result = new LinkedHashMap<>();
            for (T row : filtered(filter)) {
                result.merge(Fields.get(row, field), 1L, Long::sum);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T save(T entity) {
        lock.lock();
        try {
            T copy = Fields.copy(entity);
            if (copy.getId() == null) {
                copy.setId(sequence.incrementAndGet());
                copy.setVersion(0L);
            } else {
                T current = rows.get(copy.getId());
                if (current == null) {
                    throw new ConcurrentUpdateException(type.getSimpleName(), copy.getId());
                }
                if (copy.getVersion() != null && !copy.getVersion().equals(current.getVersion())) {
                    throw new ConcurrentUpdateException(type.getSimpleName(), copy.getId());
                }
                copy.setVersion(current.getVersion() + 1);
            }
            rows.put(copy.getId(), copy);
            return Fields.copy(copy);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete(T entity) {
        lock.lock();
        try {
            rows.remove(entity.getId());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int deleteWhere(Filter filter) {
        lock.lock();
        try {
            List<T> victims = filtered(filter);
            victims.forEach(v -> rows.remove(v.getId()));
            return victims.size();
        } finally {
            lock.unlock();
        }
    }

    Object snapshot() {
        return new Object[] {new TreeMap<>(rows), sequence.get()};
    }

    @SuppressWarnings("unchecked")
    void restore(Object snapshot) {
        if (snapshot == null) {
            rows = new TreeMap<>();
            return;
        }
        Object[] s = (Object[]) snapshot;
        rows = (TreeMap<Long, T>) s[0];
        sequence.set((Long) s[1]);
    }

    private List<T> filtered(Filter filter) {
        List<T> result = new ArrayList<>();
        for (T row : rows.values()) {
            if (matches(row, filter)) {
                result.add(row);
            }
        }
        return result;
    }

    static boolean matches(Object row, Filter filter) {
        return switch (filter) {
            case Filter.All all -> true;
            case Filter.Not not -> !matches(row, not.filter());
            case Filter.And and -> and.filters().stream().allMatch(f -> matches(row, f));
            case Filter.Or or -> or.filters().stream().anyMatch(f -> matches(row, f));
            case Filter.Condition c -> evaluate(Fields.get(row, c.field()), c);
        };
    }

    private static boolean evaluate(Object actual, Filter.Condition c) {
        Object expected = c.value();
        return switch (c.operator()) {
            case EQ -> equal(actual, expected);
            case NE -> actual != null && !equal(actual, expected);
            case GT -> actual != null && compare(actual, expected) > 0;
            case GE -> actual != null && compare(actual, expected) >= 0;
            case LT -> actual != null && compare(actual, expected) < 0;
            case LE -> actual != null && compare(actual, expected) <= 0;
            case IN -> ((Collection<?>) expected).stream().anyMatch(e -> equal(actual, e));
            case NOT_IN -> actual != null && ((Collection<?>) expected).stream().noneMatch(e -> equal(actual, e));
            case LIKE -> actual != null && Texts.fold(actual.toString()).contains(Texts.fold(String.valueOf(expected)));
            case IS_NULL -> actual == null;
            case NOT_NULL -> actual != null;
        };
    }

    private static boolean equal(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a instanceof Number na && b instanceof Number nb) {
            return new BigDecimal(na.toString()).compareTo(new BigDecimal(nb.toString())) == 0;
        }
        return a.equals(b);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compare(Object a, Object b) {
        if (a instanceof Number na && b instanceof Number nb) {
            return new BigDecimal(na.toString()).compareTo(new BigDecimal(nb.toString()));
        }
        return ((Comparable) a).compareTo(b);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Comparator<T> comparator(List<Sort> sorts) {
        Comparator<T> result = null;
        for (Sort s : sorts) {
            Comparator<T> c = (x, y) -> {
                Object a = Fields.get(x, s.field());
                Object b = Fields.get(y, s.field());
                if (a == null || b == null) {
                    return a == b ? 0 : (a == null ? 1 : -1);
                }
                if (a instanceof String sa && b instanceof String sb) {
                    return Texts.fold(sa).compareTo(Texts.fold(sb));
                }
                return ((Comparable) a).compareTo(b);
            };
            if (!s.ascending()) {
                c = c.reversed();
            }
            result = result == null ? c : result.thenComparing(c);
        }
        Comparator<T> byId = Comparator.comparing(BaseEntity::getId);
        return result == null ? byId : result.thenComparing(byId);
    }
}
