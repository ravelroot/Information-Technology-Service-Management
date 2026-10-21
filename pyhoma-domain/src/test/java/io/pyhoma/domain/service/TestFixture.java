package io.pyhoma.domain.service;

import io.pyhoma.domain.memory.InMemoryDataStore;
import io.pyhoma.domain.model.core.Profile;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.support.MutableClock;
import java.time.LocalDateTime;

final class TestFixture {

    static final String ADMIN_PASSWORD = "Granite-Ocean-91";

    final MutableClock clock = new MutableClock(LocalDateTime.of(2026, 3, 2, 9, 0));
    final Services services = new Services(new InMemoryDataStore(), clock);

    static TestFixture installed(boolean demo) {
        TestFixture f = new TestFixture();
        f.services.installation().install(new InstallationService.InstallRequest("Acme", "admin", "Admin User",
                "admin@example.com", ADMIN_PASSWORD, "pt-BR", demo));
        f.services.auth().login("admin", ADMIN_PASSWORD);
        return f;
    }

    Profile profile(String name) {
        return services.profiles().all().stream().filter(p -> p.getName().equals(name)).findFirst().orElseThrow();
    }

    User user(String login, String profileName) {
        User u = new User();
        u.setLogin(login);
        u.setFullName(login.toUpperCase());
        u.setEmail(login + "@example.com");
        User saved = services.users().create(u, "Blue-River-2026");
        services.users().addAssignment(saved.getId(), profile(profileName).getId(), services.orgUnits().root().getId(), true);
        return saved;
    }

    void loginAs(String login) {
        services.auth().logout();
        services.auth().login(login, "Blue-River-2026");
    }

    void loginAdmin() {
        services.auth().logout();
        services.auth().login("admin", ADMIN_PASSWORD);
    }
}
