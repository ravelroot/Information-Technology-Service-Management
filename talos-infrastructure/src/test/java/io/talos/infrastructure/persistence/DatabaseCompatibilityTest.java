package io.talos.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.talos.domain.model.itil.ItilType;
import io.talos.domain.model.itil.Ticket;
import io.talos.domain.port.BackendProvider;
import io.talos.domain.port.DatabaseSettings;
import io.talos.domain.port.DatabaseVendor;
import io.talos.domain.service.ItilSearch;
import io.talos.domain.service.ReportService;
import io.talos.domain.service.Services;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@EnabledIfSystemProperty(named = "talos.databases", matches = ".+")
class DatabaseCompatibilityTest {

    private static final String SECRET = "Talos!Test2026";

    @TempDir
    Path logs;

    static Stream<String> databases() {
        return Arrays.stream(System.getProperty("talos.databases", "").split(",")).map(String::trim).filter(s -> !s.isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("databases")
    void fullScenario(String name) throws Exception {
        DatabaseVendor vendor = DatabaseVendor.valueOf(name.toUpperCase());
        try (GenericContainer<?> db = container(vendor)) {
            db.start();
            DatabaseSettings settings = settings(vendor, db);
            waitUntilReachable(settings);
            if (vendor == DatabaseVendor.SQLSERVER) {
                try (Connection c = DriverManager.getConnection(settings.jdbcUrl().replace(";databaseName=talos", ""), "sa", SECRET);
                     Statement st = c.createStatement()) {
                    st.execute("CREATE DATABASE talos COLLATE Latin1_General_100_CI_AS_SC_UTF8");
                }
            }
            try (BackendProvider.Backend backend = BackendTestSupport.start(settings, logs)) {
                Services s = BackendTestSupport.installDemo(backend);
                assertTrue(s.tickets().count(ItilSearch.create().view(ItilSearch.View.ALL)) >= 150);
                Ticket t = s.tickets().draft();
                t.setTitle("Acentuação çãõ — " + name);
                t.setContent("x".repeat(6000));
                t = s.tickets().create(t);
                Ticket reloaded = s.tickets().get(t.getId());
                assertEquals(6000, reloaded.getContent().length(), "long text round trip");
                assertEquals(t.getTitle(), reloaded.getTitle(), "unicode round trip");
                s.itilActivity().addFollowup(ItilType.TICKET, t.getId(), "ok", true);
                for (ReportService.ReportKind kind : ReportService.ReportKind.values()) {
                    s.reports().run(kind, null, null);
                }
                s.jobs().runFrequent();
                s.jobs().runHourly();
            }
        }
    }

    @SuppressWarnings("resource")
    private static GenericContainer<?> container(DatabaseVendor vendor) {
        return switch (vendor) {
            case POSTGRESQL -> new GenericContainer<>("postgres:17-alpine")
                    .withEnv("POSTGRES_USER", "talos").withEnv("POSTGRES_PASSWORD", SECRET).withEnv("POSTGRES_DB", "talos")
                    .withExposedPorts(5432)
                    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));
            case MYSQL -> new GenericContainer<>("mysql:8.4")
                    .withEnv("MYSQL_ROOT_PASSWORD", SECRET).withEnv("MYSQL_DATABASE", "talos")
                    .withEnv("MYSQL_USER", "talos").withEnv("MYSQL_PASSWORD", SECRET)
                    .withExposedPorts(3306)
                    .waitingFor(Wait.forLogMessage(".*ready for connections.*port: 3306.*", 1));
            case MARIADB -> new GenericContainer<>("mariadb:11.4")
                    .withEnv("MARIADB_ROOT_PASSWORD", SECRET).withEnv("MARIADB_DATABASE", "talos")
                    .withEnv("MARIADB_USER", "talos").withEnv("MARIADB_PASSWORD", SECRET)
                    .withExposedPorts(3306)
                    .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2));
            case SQLSERVER -> new GenericContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .withEnv("ACCEPT_EULA", "Y").withEnv("MSSQL_SA_PASSWORD", SECRET)
                    .withExposedPorts(1433)
                    .waitingFor(Wait.forLogMessage(".*SQL Server is now ready for client connections.*", 1));
            case ORACLE -> new GenericContainer<>("gvenzl/oracle-free:23-slim-faststart")
                    .withEnv("ORACLE_PASSWORD", SECRET).withEnv("APP_USER", "talos").withEnv("APP_USER_PASSWORD", SECRET)
                    .withExposedPorts(1521)
                    .waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE!.*", 1).withStartupTimeout(Duration.ofMinutes(5)));
            case H2 -> throw new IllegalArgumentException("H2 is covered by JpaBackendTest");
        };
    }

    private static DatabaseSettings settings(DatabaseVendor vendor, GenericContainer<?> db) {
        String host = db.getHost();
        int port = db.getFirstMappedPort();
        return switch (vendor) {
            case SQLSERVER -> new DatabaseSettings(vendor, host, port, "talos", "sa", SECRET, null, "trustServerCertificate=true");
            case ORACLE -> new DatabaseSettings(vendor, host, port, "FREEPDB1", "talos", SECRET, null, null);
            default -> new DatabaseSettings(vendor, host, port, "talos", "talos", SECRET, null, null);
        };
    }

    private static void waitUntilReachable(DatabaseSettings s) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 120_000;
        String url = s.vendor() == DatabaseVendor.SQLSERVER ? s.jdbcUrl().replace(";databaseName=talos", "") : s.jdbcUrl();
        while (true) {
            try (Connection c = DriverManager.getConnection(url, s.username(), s.password())) {
                if (c.isValid(5)) {
                    return;
                }
            } catch (Exception e) {
                if (System.currentTimeMillis() > deadline) {
                    throw new IllegalStateException("Database not reachable: " + s.safeDescription(), e);
                }
                Thread.sleep(2000);
            }
        }
    }
}
