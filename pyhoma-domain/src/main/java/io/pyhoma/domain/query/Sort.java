package io.pyhoma.domain.query;

public record Sort(String field, boolean ascending) {

    public static Sort asc(String field) {
        return new Sort(field, true);
    }

    public static Sort desc(String field) {
        return new Sort(field, false);
    }
}
