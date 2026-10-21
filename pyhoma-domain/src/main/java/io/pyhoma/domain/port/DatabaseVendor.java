package io.pyhoma.domain.port;

public enum DatabaseVendor {
    H2("H2 (embedded)", "org.h2.Driver", 0, "h2"),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", 5432, "postgresql"),
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", 3306, "mysql"),
    MARIADB("MariaDB", "org.mariadb.jdbc.Driver", 3306, "mysql"),
    SQLSERVER("Microsoft SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver", 1433, "sqlserver"),
    ORACLE("Oracle Database", "oracle.jdbc.OracleDriver", 1521, "oracle");

    private final String label;
    private final String driverClass;
    private final int defaultPort;
    private final String migrationFolder;

    DatabaseVendor(String label, String driverClass, int defaultPort, String migrationFolder) {
        this.label = label;
        this.driverClass = driverClass;
        this.defaultPort = defaultPort;
        this.migrationFolder = migrationFolder;
    }

    public String label() {
        return label;
    }

    public String driverClass() {
        return driverClass;
    }

    public int defaultPort() {
        return defaultPort;
    }

    public String migrationFolder() {
        return migrationFolder;
    }

    public boolean isEmbedded() {
        return this == H2;
    }
}
