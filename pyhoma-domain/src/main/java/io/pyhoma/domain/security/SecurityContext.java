package io.pyhoma.domain.security;

import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.support.AccessDeniedException;
import java.util.function.Supplier;

public final class SecurityContext {

    private final ThreadLocal<UserSession> override = new ThreadLocal<>();
    private volatile UserSession session;

    public UserSession current() {
        UserSession s = override.get();
        if (s != null) {
            return s;
        }
        UserSession u = session;
        if (u == null) {
            throw new AccessDeniedException("SESSION", "LOGIN");
        }
        return u;
    }

    public boolean isAuthenticated() {
        return override.get() != null || session != null;
    }

    public void login(UserSession newSession) {
        this.session = newSession;
    }

    public void logout() {
        this.session = null;
    }

    public <R> R runAsSystem(Supplier<R> work) {
        UserSession previous = override.get();
        override.set(UserSession.system());
        try {
            return work.get();
        } finally {
            if (previous == null) {
                override.remove();
            } else {
                override.set(previous);
            }
        }
    }

    public void runAsSystem(Runnable work) {
        runAsSystem(() -> {
            work.run();
            return null;
        });
    }

    public boolean can(Module module, int right) {
        return isAuthenticated() && current().can(module, right);
    }

    public void require(Module module, int right) {
        if (!can(module, right)) {
            throw new AccessDeniedException(module.name(), Right.name(right));
        }
    }

    public Filter orgFilter(String field) {
        UserSession s = current();
        return s.seesAllOrgUnits() ? Filter.ALL : Filter.in(field, s.visibleOrgUnits());
    }

    public Filter orgFilter() {
        return orgFilter("orgUnitId");
    }

    public Long currentUserId() {
        return current().userId();
    }
}
