package io.pyhoma.domain.port;

import java.nio.file.Path;
import java.util.Objects;

public record DatabaseSettings(
        DatabaseVendor vendor,
        String host,
        int port,
        String database,
        String username,
        String password,
        String embeddedPath,
        String extraParameters) {

    public DatabaseSettings {
        Objects.requireNonNull(vendor, "vendor");
    }

    public static DatabaseSettings embedded(Path folder) {
        return new DatabaseSettings(DatabaseVendor.H2, null, 0, "pyhoma", "sa", "",
                folder.toAbsolutePath().toString(), null);
    }

    public String jdbcUrl() {
        if (extraParameters != null && extraParameters.startsWith("jdbc:")) {
            return extraParameters;
        }
        String extra = extraParameters == null || extraParameters.isBlank() ? "" : extraParameters.trim();
        int p = port > 0 ? port : vendor.defaultPort();
        return switch (vendor) {
            case H2 -> "jdbc:h2:file:" + Path.of(embeddedPath, database == null ? "pyhoma" : database)
                    .toAbsolutePath().toString().replace('\\', '/') + prefixed(";", extra);
            case POSTGRESQL -> "jdbc:postgresql://" + host + ":" + p + "/" + database + prefixed("?", extra);
            case MYSQL -> "jdbc:mysql://" + host + ":" + p + "/" + database
                    + "?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC" + prefixed("&", extra);
            case MARIADB -> "jdbc:mariadb://" + host + ":" + p + "/" + database + prefixed("?", extra);
            case SQLSERVER -> "jdbc:sqlserver://" + host + ":" + p + ";databaseName=" + database
                    + ";encrypt=true;trustServerCertificate=false" + prefixed(";", extra);
            case ORACLE -> "jdbc:oracle:thin:@//" + host + ":" + p + "/" + database + prefixed("?", extra);
        };
    }

    private static String prefixed(String separator, String extra) {
        return extra.isEmpty() ? "" : (extra.startsWith(separator) ? extra : separator + extra);
    }

    public DatabaseSettings withPassword(String newPassword) {
        return new DatabaseSettings(vendor, host, port, database, username, newPassword, embeddedPath, extraParameters);
    }

    public String safeDescription() {
        return vendor.isEmbedded()
                ? vendor.label() + " - " + embeddedPath
                : vendor.label() + " - " + username + "@" + host + ":" + (port > 0 ? port : vendor.defaultPort()) + "/" + database;
    }

    @Override
    public String toString() {
        return "DatabaseSettings[" + safeDescription() + "]";
    }
}
