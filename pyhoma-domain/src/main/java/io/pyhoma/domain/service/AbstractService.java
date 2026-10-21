package io.pyhoma.domain.service;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.OrgScoped;
import io.pyhoma.domain.model.core.AuditAction;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.port.Repository;
import io.pyhoma.domain.security.SecurityContext;
import io.pyhoma.domain.support.NotFoundException;
import io.pyhoma.domain.support.Fields;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

public abstract class AbstractService {

    protected final Services services;

    protected AbstractService(Services services) {
        this.services = services;
    }

    protected <T extends BaseEntity> Repository<T> repo(Class<T> type) {
        return services.store().repository(type);
    }

    protected <R> R tx(Supplier<R> work) {
        return services.store().inTransaction(work);
    }

    protected void txRun(Runnable work) {
        services.store().inTransaction(work);
    }

    protected SecurityContext security() {
        return services.security();
    }

    protected void require(Module module, int right) {
        services.security().require(module, right);
    }

    protected boolean can(Module module, int right) {
        return services.security().can(module, right);
    }

    protected Long currentUserId() {
        return services.security().currentUserId();
    }

    protected LocalDateTime now() {
        return LocalDateTime.now(services.clock()).truncatedTo(ChronoUnit.SECONDS);
    }

    protected LocalDate today() {
        return LocalDate.now(services.clock());
    }

    protected void checkVisible(OrgScoped item) {
        if (!services.security().current().canSeeOrgUnit(item.getOrgUnitId())) {
            throw new NotFoundException(item.getClass().getSimpleName(), ((BaseEntity) item).getId());
        }
    }

    protected <T extends BaseEntity> T insert(T entity) {
        LocalDateTime now = now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        T saved = repo(entityClass(entity)).save(entity);
        services.audit().record(saved, AuditAction.CREATE, null, null, null);
        return saved;
    }

    protected <T extends BaseEntity> T update(T entity) {
        T before = repo(entityClass(entity)).getById(entity.getId());
        entity.setUpdatedAt(now());
        entity.setCreatedAt(before.getCreatedAt());
        T saved = repo(entityClass(entity)).save(entity);
        services.audit().recordDiff(before, saved);
        return saved;
    }

    protected <T extends BaseEntity> T persist(T entity) {
        return entity.isNew() ? insert(entity) : update(entity);
    }

    protected <T extends BaseEntity> void purge(T entity) {
        repo(entityClass(entity)).delete(entity);
        services.audit().record(entity, AuditAction.PURGE, null, null, null);
    }

    protected <T extends BaseEntity> T saveQuietly(T entity) {
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now());
        }
        entity.setUpdatedAt(now());
        return repo(entityClass(entity)).save(entity);
    }

    protected <T extends BaseEntity> T copy(T entity) {
        return Fields.copy(entity);
    }

    @SuppressWarnings("unchecked")
    protected static <T extends BaseEntity> Class<T> entityClass(T entity) {
        return (Class<T>) entity.getClass();
    }
}
