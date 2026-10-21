package io.pyhoma.domain.support;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Fields {

    private static final Map<Class<?>, List<Field>> CACHE = new ConcurrentHashMap<>();

    private Fields() {
    }

    public static List<Field> of(Class<?> type) {
        return CACHE.computeIfAbsent(type, Fields::scan);
    }

    private static List<Field> scan(Class<?> type) {
        List<Class<?>> chain = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            chain.add(c);
        }
        Collections.reverse(chain);
        List<Field> result = new ArrayList<>();
        for (Class<?> c : chain) {
            for (Field f : c.getDeclaredFields()) {
                int m = f.getModifiers();
                if (Modifier.isStatic(m) || Modifier.isTransient(m) || f.isSynthetic()) {
                    continue;
                }
                f.setAccessible(true);
                result.add(f);
            }
        }
        return List.copyOf(result);
    }

    public static Field field(Class<?> type, String name) {
        for (Field f : of(type)) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unknown attribute " + type.getSimpleName() + "." + name);
    }

    public static Object get(Object target, String name) {
        try {
            return field(target.getClass(), name).get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void set(Object target, String name, Object value) {
        try {
            field(target.getClass(), name).set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T copy(T source) {
        try {
            var ctor = source.getClass().getDeclaredConstructor();
            ctor.setAccessible(true);
            T target = (T) ctor.newInstance();
            for (Field f : of(source.getClass())) {
                f.set(target, f.get(source));
            }
            return target;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot copy " + source.getClass(), e);
        }
    }
}
