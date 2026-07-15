package com.split.me.rely;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Arrays;
import java.util.Map;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final Map<String, Listener> listners = Map.of("rely.event", new Listener());

    static void main() throws Exception {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setServerNames(new String[]{"localhost"});
        source.setPortNumbers(new int[]{5432});
        source.setDatabaseName("rely");
        source.setUser("mateus");
        source.setPassword("mateus");
        new Watcher(source).watch();
    }

    private static void work(String topic, String payload, Integer id) {
        Thread
                .ofVirtual()
                .uncaughtExceptionHandler((_, e) -> {

                })
                .start(() -> {
                    listners.get(topic).event(payload);
                });
    }

    private static void changes(Connection connection, int[] ids) {
        try {
            String in = String.join(",", Arrays.stream(ids).mapToObj(String::valueOf).toList());
            String query = "SELECT * FROM rely WHERE id in (" + in + ") and status in ('PENDING');";
            LOG.info("Query to trigger listeners {}", query);
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(query);
            while (result.next()) {
                Integer id = result.getInt("id");
                String topic = result.getString("topic");
                String payload = new String(result.getBytes("payload"), StandardCharsets.UTF_8);
                LOG.info("Will dispatch to topic {}", topic);
                work(topic, payload, id);
                // create another thread and send the id
                // send payload to all the workers registered
                // worker will

            }
        } catch (Exception e) {
            LOG.error("Error on dispatch to topics", e);
            throw new RuntimeException(e);
        }
    }

    private static void awake(Connection connection) throws RuntimeException {
        try {
            Statement statement = connection.createStatement();
            statement.execute("LISTEN rely_changed");
            LOG.info("Listening to rely");
            PGConnection postgres = connection.unwrap(PGConnection.class);

            while (true) {
                LOG.info("Awaiting for message to be awake");
                PGNotification[] notifications = postgres.getNotifications(0);
                if (notifications != null) {
                    int[] ids = new int[notifications.length];
                    int count = 0;
                    for (PGNotification notification : notifications) {
                        ids[count++] = Integer.parseInt(notification.getParameter());
                    }
                    changes(connection, ids);
                    LOG.info("Rely ids {} to be dispatched ", Arrays.toString(ids));
                }
            }
        } catch (Exception e) {
            LOG.error("Error on acquiring connection", e);
            throw new RuntimeException(e);
        }
    }
}
