package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.Setting;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.security.Right;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SettingsService extends AbstractService {

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private volatile boolean loaded;

    public SettingsService(Services services) {
        super(services);
    }

    public String get(String key, String defaultValue) {
        ensureLoaded();
        return cache.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    public Map<String, String> all() {
        ensureLoaded();
        return new LinkedHashMap<>(cache);
    }

    public void set(String key, String value) {
        require(Module.SETTINGS, Right.UPDATE);
        checkBounds(key, value);
        setInternal(key, value);
    }

    private static void checkBounds(String key, String value) {
        int[] range = switch (key) {
            case SettingKeys.MAX_FAILED_LOGINS -> new int[] {3, 10};
            case SettingKeys.LOCK_MINUTES -> new int[] {5, 1440};
            case SettingKeys.SESSION_IDLE_MINUTES -> new int[] {1, 240};
            default -> null;
        };
        if (range == null) {
            return;
        }
        int v;
        try {
            v = Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            throw new io.pyhoma.domain.support.ValidationException(java.util.Map.of(key, "validation.number"));
        }
        if (v < range[0] || v > range[1]) {
            throw new io.pyhoma.domain.support.BusinessException("error.settingRange", range[0], range[1]);
        }
    }

    void setInternal(String key, String value) {
        txRun(() -> {
            Setting s = repo(Setting.class).findFirst(Filter.eq("settingKey", key)).orElseGet(Setting::new);
            String old = s.getSettingValue();
            if (s.isNew()) {
                s.setSettingKey(key);
            }
            s.setSettingValue(value);
            if (s.isNew()) {
                insert(s);
            } else if (old == null || !old.equals(value)) {
                update(s);
            }
        });
        if (value == null) {
            cache.remove(key);
        } else {
            cache.put(key, value);
        }
    }

    public void reload() {
        loaded = false;
        ensureLoaded();
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (this) {
            if (loaded) {
                return;
            }
            cache.clear();
            for (Setting s : repo(Setting.class).find(Query.all())) {
                if (s.getSettingValue() != null) {
                    cache.put(s.getSettingKey(), s.getSettingValue());
                }
            }
            loaded = true;
        }
    }
}
