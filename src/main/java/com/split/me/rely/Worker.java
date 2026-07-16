package com.split.me.rely;

import org.slf4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;

public class Worker {
    private static final Logger LOG = getLogger(Worker.class);
    private final Connection connection;
    private final Map<String, Listener> listeners;

    public Worker(Map<String, Listener> listeners, DataSource source) {
        this.listeners = listeners;
        try {
            this.connection = source.getConnection();
            LOG.info("Watcher created");
        } catch (SQLException e) {
            LOG.warn("Cannot connect to database", e);
            throw new RuntimeException(e);
        }
    }
    public void work(String topic, String payload, Integer id) {
        Thread
            .ofVirtual()
            .name("worker-"+ randomUUID().getMostSignificantBits())
            .uncaughtExceptionHandler((t, e) -> {
                LOG.error("Listener for topic {} failed to complete with payload {}", topic, payload);
                markAsFailed(id);
                closeConnection();
            })
            .start(() -> {
                LOG.info("Worker started to work on {} {} {}", topic, payload, id );

                listeners.get(topic).event(payload);
                markAsCompleted(id);
                LOG.info("Listener finished successfully");
                closeConnection();
            });
    }

    private void markAsCompleted(Integer id) {
        try {
            connection.createStatement().execute("UPDATE rely SET status = 'COMPLETED' where id = "+id+";");
        } catch (SQLException e) {
            LOG.error("Failed to update to completed status for id {}", id, e);
            throw new RuntimeException(e);
        }
    }

    private void markAsFailed(Integer id) {
        try {
            connection.createStatement().execute("UPDATE rely SET status = 'FAILED' where id = "+id+";");
        } catch (SQLException e) {
            LOG.error("Failed to update to failed status for id {}", id, e);
            throw new RuntimeException(e);
        }
    }

    private void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            LOG.error("Error to close datasource after exception, nothing to do :(", e);
        }
    }
}
