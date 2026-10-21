package io.pyhoma.desktop.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class AppPaths {

    private AppPaths() {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static Path configDir() {
        String override = System.getProperty("pyhoma.home");
        if (override != null) {
            return ensure(Path.of(override));
        }
        if (isWindows() && System.getenv("APPDATA") != null) {
            return ensure(Path.of(System.getenv("APPDATA"), "Pyhoma"));
        }
        return ensure(Path.of(System.getProperty("user.home"), ".pyhoma"));
    }

    public static Path dataDir() {
        String override = System.getProperty("pyhoma.home");
        if (override != null) {
            return ensure(Path.of(override, "data"));
        }
        if (isWindows() && System.getenv("LOCALAPPDATA") != null) {
            return ensure(Path.of(System.getenv("LOCALAPPDATA"), "Pyhoma"));
        }
        return ensure(configDir().resolve("data"));
    }

    public static Path logDir() {
        return ensure(dataDir().resolve("logs"));
    }

    public static Path defaultEmbeddedDatabaseDir() {
        return dataDir().resolve("database");
    }

    private static Path ensure(Path p) {
        try {
            boolean created = !Files.exists(p);
            Files.createDirectories(p);
            if (created) {
                restrict(p);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create folder " + p, e);
        }
        return p;
    }

    public static void restrict(Path path) {
        try {
            if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(path, java.nio.file.attribute.PosixFilePermissions.fromString(
                        Files.isDirectory(path) ? "rwx------" : "rw-------"));
            }
        } catch (Exception ignored) {
            return;
        }
    }
}
