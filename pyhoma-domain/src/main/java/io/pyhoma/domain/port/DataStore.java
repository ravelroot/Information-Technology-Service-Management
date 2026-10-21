package io.pyhoma.domain.port;

import io.pyhoma.domain.model.BaseEntity;
import java.util.function.Supplier;

public interface DataStore {

    <T extends BaseEntity> Repository<T> repository(Class<T> type);

    <R> R inTransaction(Supplier<R> work);

    default void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }

    String describe();
}
