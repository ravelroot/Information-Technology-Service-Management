package io.pyhoma.domain.support;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class Validator {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
    private static final Pattern MAC = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$");

    private final Map<String, String> errors = new LinkedHashMap<>();

    public static Validator create() {
        return new Validator();
    }

    public Validator required(String field, Object value) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            errors.putIfAbsent(field, "validation.required");
        }
        return this;
    }

    public Validator maxLength(String field, String value, int max) {
        if (value != null && value.length() > max) {
            errors.putIfAbsent(field, "validation.tooLong");
        }
        return this;
    }

    public Validator email(String field, String value) {
        if (value != null && !value.isBlank() && !EMAIL.matcher(value.trim()).matches()) {
            errors.putIfAbsent(field, "validation.email");
        }
        return this;
    }

    public Validator ipAddress(String field, String value) {
        if (value != null && !value.isBlank() && !IPV4.matcher(value.trim()).matches() && !value.contains(":")) {
            errors.putIfAbsent(field, "validation.ip");
        }
        return this;
    }

    public Validator macAddress(String field, String value) {
        if (value != null && !value.isBlank() && !MAC.matcher(value.trim()).matches()) {
            errors.putIfAbsent(field, "validation.mac");
        }
        return this;
    }

    public Validator range(String field, Integer value, int min, int max) {
        if (value != null && (value < min || value > max)) {
            errors.putIfAbsent(field, "validation.range");
        }
        return this;
    }

    public Validator positive(String field, Number value) {
        if (value != null && value.doubleValue() < 0) {
            errors.putIfAbsent(field, "validation.positive");
        }
        return this;
    }

    public Validator check(boolean condition, String field, String messageKey) {
        if (!condition) {
            errors.putIfAbsent(field, messageKey);
        }
        return this;
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public void throwIfInvalid() {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
