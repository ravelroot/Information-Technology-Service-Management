package io.pyhoma.desktop.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public final class Async {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "pyhoma-ui-" + COUNTER.incrementAndGet());
        t.setDaemon(true);
        return t;
    });
    private static final SimpleIntegerProperty RUNNING = new SimpleIntegerProperty();

    private Async() {
    }

    public static ReadOnlyIntegerProperty running() {
        return RUNNING;
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess) {
        run(work, onSuccess, Dialogs::error);
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        change(1);
        POOL.submit(() -> {
            try {
                T result = work.call();
                Platform.runLater(() -> {
                    change(-1);
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                });
            } catch (Throwable e) {
                Platform.runLater(() -> {
                    change(-1);
                    if (onError != null) {
                        onError.accept(e);
                    }
                });
            }
        });
    }

    public static void exec(Runnable work, Runnable onSuccess) {
        run(() -> {
            work.run();
            return Boolean.TRUE;
        }, ok -> {
            if (onSuccess != null) {
                onSuccess.run();
            }
        });
    }

    private static void change(int delta) {
        if (Platform.isFxApplicationThread()) {
            RUNNING.set(RUNNING.get() + delta);
        } else {
            Platform.runLater(() -> RUNNING.set(RUNNING.get() + delta));
        }
    }
}
