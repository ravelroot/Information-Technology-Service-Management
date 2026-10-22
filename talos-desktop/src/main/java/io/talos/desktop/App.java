package io.talos.desktop;

import io.talos.desktop.config.AppConfig;
import io.talos.desktop.shell.Navigator;
import io.talos.desktop.ui.LookupCache;
import io.talos.domain.model.core.Module;
import io.talos.domain.port.BackendProvider;
import io.talos.domain.security.UserSession;
import io.talos.domain.service.Services;

public final class App {

    private static AppConfig config;
    private static Services services;
    private static BackendProvider.Backend backend;
    private static LookupCache lookups;
    private static Navigator navigator;
    private static boolean demo;

    private App() {
    }

    public static AppConfig config() {
        return config;
    }

    public static void setConfig(AppConfig c) {
        config = c;
    }

    public static Services services() {
        return services;
    }

    public static void connect(BackendProvider.Backend b, Services s) {
        backend = b;
        services = s;
        lookups = new LookupCache(s);
    }

    public static BackendProvider.Backend backend() {
        return backend;
    }

    public static LookupCache lookups() {
        return lookups;
    }

    public static Navigator nav() {
        return navigator;
    }

    public static void setNavigator(Navigator n) {
        navigator = n;
    }

    public static UserSession session() {
        return services.security().current();
    }

    public static boolean can(Module module, int right) {
        return services != null && services.security().can(module, right);
    }

    public static boolean demo() {
        return demo;
    }

    public static void setDemo(boolean value) {
        demo = value;
    }

    public static void shutdown() {
        if (backend != null) {
            try {
                backend.close();
            } catch (RuntimeException ignored) {
            }
        }
    }
}
