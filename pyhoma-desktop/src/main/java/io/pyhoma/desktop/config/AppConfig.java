package io.pyhoma.desktop.config;

import io.pyhoma.domain.port.DatabaseSettings;
import io.pyhoma.domain.port.DatabaseVendor;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public final class AppConfig {

    private final Path file;
    private final SecretProtector protector;
    private final Properties props = new Properties();

    public AppConfig(Path configDir) {
        this.file = configDir.resolve("pyhoma.properties");
        this.protector = new SecretProtector(configDir);
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(r);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + file, e);
            }
        }
    }

    public static AppConfig load() {
        return new AppConfig(AppPaths.configDir());
    }

    public Path file() {
        return file;
    }

    public boolean hasDatabase() {
        return props.getProperty("db.vendor") != null;
    }

    public Optional<DatabaseSettings> database() {
        String vendor = props.getProperty("db.vendor");
        if (vendor == null) {
            return Optional.empty();
        }
        return Optional.of(new DatabaseSettings(DatabaseVendor.valueOf(vendor), props.getProperty("db.host"),
                Integer.parseInt(props.getProperty("db.port", "0")), props.getProperty("db.name"), props.getProperty("db.user"),
                protector.reveal(props.getProperty("db.password", "")), props.getProperty("db.embeddedPath"),
                props.getProperty("db.extra")));
    }

    public void setDatabase(DatabaseSettings s) {
        props.setProperty("db.vendor", s.vendor().name());
        put("db.host", s.host());
        props.setProperty("db.port", String.valueOf(s.port()));
        put("db.name", s.database());
        put("db.user", s.username());
        props.setProperty("db.password", protector.protect(s.password()));
        put("db.embeddedPath", s.embeddedPath());
        put("db.extra", s.extraParameters());
    }

    public void clearDatabase() {
        props.stringPropertyNames().stream().filter(k -> k.startsWith("db.")).forEach(props::remove);
    }

    public String get(String key, String def) {
        return props.getProperty(key, def);
    }

    public void set(String key, String value) {
        put(key, value);
    }

    private void put(String key, String value) {
        if (value == null) {
            props.remove(key);
        } else {
            props.setProperty(key, value);
        }
    }

    public synchronized void save() {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            props.store(w, null);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write " + file, e);
        }
        AppPaths.restrict(file);
    }
}
