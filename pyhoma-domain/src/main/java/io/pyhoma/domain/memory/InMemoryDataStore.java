package io.pyhoma.domain.memory;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.port.DataStore;
import io.pyhoma.domain.port.Repository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class InMemoryDataStore implements DataStore {

    private final Map<Class<?>, InMemoryRepository<?>> repositories = new ConcurrentHashMap<>();
    private final ReentrantLock txLock = new ReentrantLock();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BaseEntity> Repository<T> repository(Class<T> type) {
        return (Repository<T>) repositories.computeIfAbsent(type, t -> new InMemoryRepository<>(type, txLock));
    }

    @Override
    public <R> R inTransaction(Supplier<R> work) {
        txLock.lock();
        boolean outer = txLock.getHoldCount() == 1;
        Map<Class<?>, Object> snapshot = outer ? snapshot() : null;
        try {
            return work.get();
        } catch (RuntimeException | Error e) {
            if (outer) {
                restore(snapshot);
            }
            throw e;
        } finally {
            txLock.unlock();
        }
    }

    private Map<Class<?>, Object> snapshot() {
        Map<Class<?>, Object> s = new java.util.HashMap<>();
        repositories.forEach((k, v) -> s.put(k, v.snapshot()));
        return s;
    }

    private void restore(Map<Class<?>, Object> snapshot) {
        repositories.forEach((k, v) -> v.restore(snapshot.get(k)));
    }

    @Override
    public String describe() {
        return "In-memory";
    }
}
