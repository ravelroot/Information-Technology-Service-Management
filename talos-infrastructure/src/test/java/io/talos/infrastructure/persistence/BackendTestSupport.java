package io.talos.infrastructure.persistence;

import io.talos.domain.port.BackendProvider;
import io.talos.domain.port.DatabaseSettings;
import io.talos.domain.service.InstallationService;
import io.talos.domain.service.Services;
import io.talos.infrastructure.boot.SpringBackendProvider;
import java.nio.file.Path;

final class BackendTestSupport {

    static final String PASSWORD = "Granite-Ocean-91";

    private BackendTestSupport() {
    }

    static BackendProvider.Backend start(DatabaseSettings settings, Path logs) throws Exception {
        return new SpringBackendProvider().start(settings, new BackendProvider.BackendOptions(logs, logs, false));
    }

    static Services installDemo(BackendProvider.Backend backend) {
        Services services = new Services(backend.dataStore());
        if (!services.installation().isInstalled()) {
            services.installation().install(new InstallationService.InstallRequest("Test Org", "admin", "Administrator",
                    "admin@example.com", PASSWORD, "pt-BR", true));
        }
        services.auth().login("admin", PASSWORD);
        return services;
    }
}
