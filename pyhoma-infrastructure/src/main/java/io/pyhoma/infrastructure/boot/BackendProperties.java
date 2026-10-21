package io.pyhoma.infrastructure.boot;

import io.pyhoma.domain.port.BackendProvider.BackendOptions;
import io.pyhoma.domain.port.DatabaseSettings;
import java.util.LinkedHashMap;
import java.util.Map;

final class BackendProperties {

    private BackendProperties() {
    }

    static Map<String, Object> of(DatabaseSettings db, BackendOptions options) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("spring.main.web-application-type", "none");
        p.put("spring.main.banner-mode", "off");
        p.put("spring.main.lazy-initialization", "false");
        p.put("spring.datasource.url", db.jdbcUrl());
        p.put("spring.datasource.username", db.username() == null ? "" : db.username());
        p.put("spring.datasource.password", db.password() == null ? "" : db.password());
        p.put("spring.datasource.driver-class-name", db.vendor().driverClass());
        p.put("spring.datasource.hikari.pool-name", "pyhoma");
        p.put("spring.datasource.hikari.maximum-pool-size", "5");
        p.put("spring.datasource.hikari.minimum-idle", "1");
        p.put("spring.datasource.hikari.connection-timeout", "15000");
        p.put("spring.flyway.enabled", "true");
        p.put("spring.flyway.locations", "classpath:db/migration/" + db.vendor().migrationFolder());
        p.put("spring.flyway.table", "pyh_schema_history");
        p.put("spring.flyway.baseline-on-migrate", "false");
        p.put("spring.flyway.validate-migration-naming", "true");
        p.put("spring.flyway.clean-disabled", "true");
        p.put("spring.flyway.validate-on-migrate", "true");
        p.put("spring.flyway.out-of-order", "false");
        p.put("spring.jpa.open-in-view", "false");
        p.put("spring.jpa.hibernate.ddl-auto", "none");
        p.put("spring.jpa.show-sql", "false");
        p.put("spring.jpa.properties.hibernate.format_sql", "false");
        p.put("spring.jpa.properties.hibernate.generate_statistics", "false");
        p.put("logging.level.org.hibernate.SQL", "OFF");
        p.put("logging.level.org.hibernate.orm.jdbc.bind", "OFF");
        p.put("spring.jpa.properties.hibernate.order_updates", "true");
        if (options != null && options.logFolder() != null) {
            p.put("logging.file.name", options.logFolder().resolve("pyhoma.log").toString());
            p.put("logging.logback.rollingpolicy.max-file-size", "10MB");
            p.put("logging.logback.rollingpolicy.max-history", "10");
        }
        p.put("logging.level.root", "WARN");
        p.put("logging.level.io.pyhoma", "INFO");
        p.put("logging.level.org.flywaydb", "INFO");
        return p;
    }
}
