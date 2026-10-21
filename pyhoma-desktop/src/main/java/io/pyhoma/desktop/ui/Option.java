package io.pyhoma.desktop.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Option<V>(V value, String label, String detail, String color) {

    public Option(V value, String label) {
        this(value, label, null, null);
    }

    public static List<Option<Long>> of(Map<Long, String> names) {
        List<Option<Long>> list = new ArrayList<>();
        names.forEach((k, v) -> list.add(new Option<>(k, v)));
        list.sort((a, b) -> a.label().compareToIgnoreCase(b.label()));
        return list;
    }

    public static <E extends Enum<E>> List<Option<E>> ofEnum(E[] values) {
        List<Option<E>> list = new ArrayList<>();
        for (E e : values) {
            list.add(new Option<>(e, I18n.e(e)));
        }
        return list;
    }

    public static List<Option<Integer>> levels(String prefix, int max) {
        List<Option<Integer>> list = new ArrayList<>();
        for (int i = max; i >= 1; i--) {
            list.add(new Option<>(i, I18n.t(prefix + i)));
        }
        return list;
    }

    public boolean matches(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = io.pyhoma.domain.support.Texts.fold(query);
        return io.pyhoma.domain.support.Texts.fold(label).contains(q)
                || (detail != null && io.pyhoma.domain.support.Texts.fold(detail).contains(q));
    }

    public boolean is(Object v) {
        return Objects.equals(value, v);
    }

    @Override
    public String toString() {
        return label;
    }
}
