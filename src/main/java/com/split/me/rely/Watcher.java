package com.split.me.rely;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class Watcher {
    private static final Logger LOG = getLogger(Watcher.class);
    private final Connection connection;
    private final DataSource source;
    private static final Map<String, Listener> listeners = Map.of("rely.event", new Listener());


    public Watcher(DataSource source) {
        try {
            this.source = source;
            this.connection = source.getConnection();
            LOG.info("Watcher created");
        } catch (SQLException e) {
            LOG.warn("Cannot connect to database", e);
            throw new RuntimeException(e);
        }
    }

    public void watch() {
        try {
            Thread
                    .ofVirtual()
                    .name("watcher")
                    .start(() -> {
                        LOG.info("Watcher started to watch");
                        listenToRely();
                        loopOverNotification();
                    })
                    .join();
        } catch (InterruptedException e) {
            LOG.error("Unable to start watcher", e);
        }
    }

    private void loopOverNotification() {
        try {
            PGConnection postgres = connection.unwrap(PGConnection.class);
            while (true) {
                LOG.info("Awaiting for rely to be awake");
                PGNotification[] notifications = postgres.getNotifications(0);
                if (notifications != null) {
                    int[] ids = new int[notifications.length];
                    LOG.info("Got {} notifications", notifications.length);
                    int count = 0;
                    for (PGNotification notification : notifications) {
                        ids[count++] = Integer.parseInt(notification.getParameter());
                    }
                    LOG.info("Got ids {}", Arrays.toString(ids));
                    fireWorkersById(ids);
                    LOG.info("Rely ids {} dispatched ", Arrays.toString(ids));
                }
            }
        } catch (SQLException e) {
            LOG.error("Error on loop over notifications", e);
            closeConnection();
            throw new RuntimeException(e);
        }
    }

    private void fireWorkersById(int[] ids) {
        try {
            String in = String.join(",", Arrays.stream(ids).mapToObj(String::valueOf).toList());
            String query = "SELECT id, topic, payload, invoked FROM rely WHERE status not in ('COMPLETED', 'FAILED');";
            LOG.info("Query to trigger listeners {}", query);

        } catch (Exception e) {
            LOG.error("Error on dispatch to topics", e);
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

    private void listenToRely() {
        try {
            Statement statement = connection.createStatement();
            statement.execute("LISTEN rely_changed");
        } catch (SQLException e) {
            LOG.error("LISTEN rely_changed failed", e);
            closeConnection();
            throw new RuntimeException(e);
        }
        LOG.info("Listening to rely");
    }
}
