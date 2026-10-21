package io.pyhoma.infrastructure.persistence;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.port.DataStore;
import io.pyhoma.domain.port.Repository;
import io.pyhoma.domain.support.ConcurrentUpdateException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class JpaDataStore implements DataStore {

    @PersistenceContext
    private EntityManager entityManager;

    private final TransactionTemplate transactions;
    private final DataSource dataSource;
    private final Map<Class<?>, Repository<?>> repositories = new ConcurrentHashMap<>();
    private volatile String description;

    public JpaDataStore(PlatformTransactionManager transactionManager, DataSource dataSource) {
        this.transactions = new TransactionTemplate(transactionManager);
        this.dataSource = dataSource;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BaseEntity> Repository<T> repository(Class<T> type) {
        return (Repository<T>) repositories.computeIfAbsent(type, t -> new JpaRepositoryAdapter<>(type, entityManager));
    }

    @Override
    public <R> R inTransaction(Supplier<R> work) {
        try {
            return transactions.execute(status -> work.get());
        } catch (RuntimeException e) {
            if (isOptimisticLockFailure(e)) {
                throw new ConcurrentUpdateException("record", null);
            }
            throw e;
        }
    }

    static boolean isOptimisticLockFailure(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String name = t.getClass().getName();
            if (t instanceof ConcurrentUpdateException) {
                return false;
            }
            if (name.contains("OptimisticLock") || name.contains("StaleObjectState") || name.contains("StaleState")) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    @Override
    public String describe() {
        String d = description;
        if (d == null) {
            try (Connection c = dataSource.getConnection()) {
                DatabaseMetaData md = c.getMetaData();
                d = md.getDatabaseProductName() + " " + md.getDatabaseProductVersion();
            } catch (SQLException e) {
                d = "?";
            }
            description = d;
        }
        return d;
    }
}
