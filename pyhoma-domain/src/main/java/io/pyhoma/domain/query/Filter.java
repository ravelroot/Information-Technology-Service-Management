package io.pyhoma.domain.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public sealed interface Filter permits Filter.All, Filter.Condition, Filter.And, Filter.Or, Filter.Not {

    record All() implements Filter {
    }

    record Condition(String field, Operator operator, Object value) implements Filter {
        public Condition {
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(operator, "operator");
        }
    }

    record And(List<Filter> filters) implements Filter {
        public And {
            filters = List.copyOf(filters);
        }
    }

    record Or(List<Filter> filters) implements Filter {
        public Or {
            filters = List.copyOf(filters);
        }
    }

    record Not(Filter filter) implements Filter {
    }

    Filter ALL = new All();

    static Filter all() {
        return ALL;
    }

    static Filter eq(String field, Object value) {
        return value == null ? isNull(field) : new Condition(field, Operator.EQ, value);
    }

    static Filter ne(String field, Object value) {
        return value == null ? notNull(field) : new Condition(field, Operator.NE, value);
    }

    static Filter gt(String field, Comparable<?> value) {
        return new Condition(field, Operator.GT, value);
    }

    static Filter ge(String field, Comparable<?> value) {
        return new Condition(field, Operator.GE, value);
    }

    static Filter lt(String field, Comparable<?> value) {
        return new Condition(field, Operator.LT, value);
    }

    static Filter le(String field, Comparable<?> value) {
        return new Condition(field, Operator.LE, value);
    }

    static Filter in(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return new Not(ALL);
        }
        return new Condition(field, Operator.IN, List.copyOf(values));
    }

    static Filter notIn(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return ALL;
        }
        return new Condition(field, Operator.NOT_IN, List.copyOf(values));
    }

    static Filter like(String field, String text) {
        return new Condition(field, Operator.LIKE, text);
    }

    static Filter isNull(String field) {
        return new Condition(field, Operator.IS_NULL, null);
    }

    static Filter notNull(String field) {
        return new Condition(field, Operator.NOT_NULL, null);
    }

    static Filter isTrue(String field) {
        return new Condition(field, Operator.EQ, Boolean.TRUE);
    }

    static Filter isFalse(String field) {
        return new Condition(field, Operator.EQ, Boolean.FALSE);
    }

    static Filter between(String field, Comparable<?> from, Comparable<?> to) {
        return and(ge(field, from), le(field, to));
    }

    static Filter not(Filter filter) {
        return new Not(filter);
    }

    static Filter and(Filter... filters) {
        return and(List.of(filters));
    }

    static Filter and(List<Filter> filters) {
        List<Filter> parts = new ArrayList<>();
        for (Filter f : filters) {
            if (f != null && !(f instanceof All)) {
                parts.add(f);
            }
        }
        if (parts.isEmpty()) {
            return ALL;
        }
        return parts.size() == 1 ? parts.get(0) : new And(parts);
    }

    static Filter or(Filter... filters) {
        return or(List.of(filters));
    }

    static Filter or(List<Filter> filters) {
        List<Filter> parts = new ArrayList<>();
        for (Filter f : filters) {
            if (f == null) {
                continue;
            }
            if (f instanceof All) {
                return ALL;
            }
            parts.add(f);
        }
        if (parts.isEmpty()) {
            return new Not(ALL);
        }
        return parts.size() == 1 ? parts.get(0) : new Or(parts);
    }

    static Filter search(String text, String... fields) {
        if (text == null || text.isBlank()) {
            return ALL;
        }
        List<Filter> parts = new ArrayList<>();
        for (String f : fields) {
            parts.add(like(f, text.trim()));
        }
        return or(parts);
    }

    default Filter and(Filter other) {
        return Filter.and(this, other);
    }
}
