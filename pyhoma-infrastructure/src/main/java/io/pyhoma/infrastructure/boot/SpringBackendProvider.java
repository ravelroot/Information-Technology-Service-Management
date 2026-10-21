package io.pyhoma.infrastructure.boot;

import io.pyhoma.PyhomaSpringApplication;
import io.pyhoma.domain.port.BackendProvider;
import io.pyhoma.domain.port.DataStore;
import io.pyhoma.domain.port.DatabaseSettings;
import io.pyhoma.infrastructure.persistence.JpaDataStore;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.TaskScheduler;

public class SpringBackendProvider implements BackendProvider {

    @Override
    public String testConnection(DatabaseSettings settings) throws Exception {
        Class.forName(settings.vendor().driverClass());
        DriverManager.setLoginTimeout(10);
        try (Connection c = DriverManager.getConnection(settings.jdbcUrl(),
                settings.username() == null ? "" : settings.username(),
                settings.password() == null ? "" : settings.password())) {
            DatabaseMetaData md = c.getMetaData();
            return md.getDatabaseProductName() + " " + md.getDatabaseProductVersion();
        }
    }

    @Override
    public Backend start(DatabaseSettings settings, BackendOptions options) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(PyhomaSpringApplication.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .registerShutdownHook(false)
                .properties(BackendProperties.of(settings, options))
                .run();
        return new SpringBackend(context);
    }

    static final class SpringBackend implements Backend {

        private final ConfigurableApplicationContext context;
        private final List<ScheduledFuture<?>> jobs = new ArrayList<>();

        SpringBackend(ConfigurableApplicationContext context) {
            this.context = context;
        }

        @Override
        public DataStore dataStore() {
            return context.getBean(JpaDataStore.class);
        }

        @Override
        public void scheduleJobs(Runnable everyMinute, Runnable hourly) {
            TaskScheduler scheduler = context.getBean(TaskScheduler.class);
            jobs.add(scheduler.scheduleWithFixedDelay(everyMinute, Duration.ofMinutes(1)));
            jobs.add(scheduler.scheduleWithFixedDelay(hourly, Duration.ofHours(1)));
        }

        @Override
        public void close() {
            jobs.forEach(j -> j.cancel(false));
            context.close();
        }
    }
}
