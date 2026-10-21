package io.pyhoma.desktop.ui;

import io.pyhoma.domain.support.BusinessException;
import io.pyhoma.domain.support.ValidationException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class I18n {

    public static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static Locale locale = PT_BR;
    private static ResourceBundle bundle = load(PT_BR);

    private I18n() {
    }

    private static ResourceBundle load(Locale l) {
        return ResourceBundle.getBundle("io.pyhoma.desktop.i18n.messages", l, ResourceBundle.Control.getNoFallbackControl(
                ResourceBundle.Control.FORMAT_PROPERTIES));
    }

    public static void setLanguage(String tag) {
        locale = tag != null && tag.toLowerCase(Locale.ROOT).startsWith("en") ? Locale.ENGLISH : PT_BR;
        Locale.setDefault(locale);
        bundle = load(locale);
    }

    public static Locale locale() {
        return locale;
    }

    public static boolean isPortuguese() {
        return locale.getLanguage().equals("pt");
    }

    public static String languageTag() {
        return isPortuguese() ? "pt-BR" : "en";
    }

    public static boolean has(String key) {
        return bundle.containsKey(key);
    }

    public static String t(String key, Object... args) {
        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
        if (args.length == 0) {
            return pattern;
        }
        Object[] formatted = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            if (a instanceof LocalDateTime dt) {
                formatted[i] = dateTime(dt);
            } else if (a instanceof LocalDate d) {
                formatted[i] = date(d);
            } else if (a instanceof Enum<?> en) {
                formatted[i] = e(en);
            } else {
                formatted[i] = a == null ? "" : a.toString();
            }
        }
        return new MessageFormat(pattern.replace("'", "''"), locale).format(formatted);
    }

    public static String e(Enum<?> value) {
        if (value == null) {
            return "—";
        }
        return t("enum." + value.getDeclaringClass().getSimpleName() + "." + value.name());
    }

    public static String field(String name) {
        String key = "field." + name;
        if (has(key)) {
            return t(key);
        }
        String spaced = name.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(locale);
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    public static String priority(Integer p) {
        return p == null ? "—" : t("priority." + p);
    }

    public static String error(Throwable error) {
        Throwable e = error;
        while (e.getCause() != null && !(e instanceof BusinessException)) {
            e = e.getCause();
        }
        if (e instanceof ValidationException v) {
            StringBuilder sb = new StringBuilder(t("error.validation.title"));
            v.fieldErrors().forEach((f, k) -> sb.append("\n• ").append(field(f)).append(": ").append(t(k)));
            return sb.toString();
        }
        if (e instanceof BusinessException b) {
            Object[] args = b.args();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof String s) {
                    if (s.matches("[A-Z_]+") && has("module." + s)) {
                        args[i] = t("module." + s);
                    } else if (s.matches("[A-Z_]+") && has("right." + s)) {
                        args[i] = t("right." + s);
                    } else if (has("type." + s)) {
                        args[i] = t("type." + s);
                    }
                }
            }
            return t(b.messageKey(), args);
        }
        if (e instanceof UserMessage m) {
            return m.getMessage();
        }
        System.getLogger("pyhoma").log(System.Logger.Level.ERROR, "Unexpected error", error);
        return t("error.unexpected");
    }

    public static DateTimeFormatter dateFormatter() {
        return DateTimeFormatter.ofPattern(isPortuguese() ? "dd/MM/yyyy" : "MMM d, yyyy", locale);
    }

    public static DateTimeFormatter dateTimeFormatter() {
        return DateTimeFormatter.ofPattern(isPortuguese() ? "dd/MM/yyyy HH:mm" : "MMM d, yyyy h:mm a", locale);
    }

    public static String date(LocalDate d) {
        return d == null ? "—" : dateFormatter().format(d);
    }

    public static String dateTime(LocalDateTime d) {
        return d == null ? "—" : dateTimeFormatter().format(d);
    }

    public static String relative(LocalDateTime when) {
        if (when == null) {
            return "—";
        }
        Duration d = Duration.between(when, LocalDateTime.now());
        boolean future = d.isNegative();
        long minutes = Math.abs(d.toMinutes());
        String amount;
        if (minutes < 1) {
            return t("time.now");
        } else if (minutes < 60) {
            amount = t("time.minutes", minutes);
        } else if (minutes < 60 * 24) {
            amount = t("time.hours", minutes / 60);
        } else {
            amount = t("time.days", minutes / (60 * 24));
        }
        return future ? t("time.in", amount) : t("time.ago", amount);
    }

    public static String minutes(Integer minutes) {
        if (minutes == null || minutes == 0) {
            return "0 min";
        }
        int h = minutes / 60;
        int m = minutes % 60;
        return h == 0 ? m + " min" : (m == 0 ? h + " h" : h + " h " + m + " min");
    }
}
