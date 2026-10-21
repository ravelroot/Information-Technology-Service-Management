package io.pyhoma.domain.port;

public interface BackendProvider {

    String testConnection(DatabaseSettings settings) throws Exception;

    Backend start(DatabaseSettings settings, BackendOptions options) throws Exception;

    record BackendOptions(java.nio.file.Path dataFolder, java.nio.file.Path logFolder, boolean runBackgroundJobs) {
    }

    interface Backend extends AutoCloseable {

        DataStore dataStore();

        void scheduleJobs(Runnable everyMinute, Runnable hourly);

        @Override
        void close();
    }
}
