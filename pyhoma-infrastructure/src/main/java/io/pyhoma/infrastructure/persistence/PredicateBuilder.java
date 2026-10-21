package io.pyhoma.infrastructure.persistence;

import io.pyhoma.domain.query.Filter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

final class PredicateBuilder {

    private static final int IN_CHUNK = 500;
    private static final char ESCAPE = '!';

    private PredicateBuilder() {
    }

    static Predicate build(CriteriaBuilder cb, Root<?> root, Filter filter) {
        return switch (filter) {
            case Filter.All all -> cb.conjunction();
            case Filter.Not not -> not.filter() instanceof Filter.All ? cb.disjunction() : cb.not(build(cb, root, not.filter()));
            case Filter.And and -> cb.and(and.filters().stream().map(f -> build(cb, root, f)).toArray(Predicate[]::new));
            case Filter.Or or -> cb.or(or.filters().stream().map(f -> build(cb, root, f)).toArray(Predicate[]::new));
            case Filter.Condition c -> condition(cb, root, c);
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate condition(CriteriaBuilder cb, Root<?> root, Filter.Condition c) {
        Path<Object> path = root.get(c.field());
        Object v = c.value();
        return switch (c.operator()) {
            case EQ -> cb.equal(path, v);
            case NE -> cb.notEqual(path, v);
            case GT -> cb.greaterThan((Expression) path, (Comparable) v);
            case GE -> cb.greaterThanOrEqualTo((Expression) path, (Comparable) v);
            case LT -> cb.lessThan((Expression) path, (Comparable) v);
            case LE -> cb.lessThanOrEqualTo((Expression) path, (Comparable) v);
            case IN -> in(cb, path, (Collection<?>) v);
            case NOT_IN -> cb.and(cb.isNotNull(path), cb.not(in(cb, path, (Collection<?>) v)));
            case LIKE -> cb.like(cb.lower(path.as(String.class)), "%" + escape(String.valueOf(v).toLowerCase(Locale.ROOT)) + "%", ESCAPE);
            case IS_NULL -> cb.isNull(path);
            case NOT_NULL -> cb.isNotNull(path);
        };
    }

    private static Predicate in(CriteriaBuilder cb, Path<Object> path, Collection<?> values) {
        if (values.isEmpty()) {
            return cb.disjunction();
        }
        List<Object> all = new ArrayList<>(values);
        List<Predicate> chunks = new ArrayList<>();
        for (int i = 0; i < all.size(); i += IN_CHUNK) {
            CriteriaBuilder.In<Object> in = cb.in(path);
            for (Object o : all.subList(i, Math.min(all.size(), i + IN_CHUNK))) {
                in.value(o);
            }
            chunks.add(in);
        }
        return chunks.size() == 1 ? chunks.get(0) : cb.or(chunks.toArray(Predicate[]::new));
    }

    static String escape(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
