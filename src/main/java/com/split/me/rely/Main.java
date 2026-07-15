package com.split.me.rely;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;

public class Main {
    static void main() throws Exception {
        Connection connection  = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/rely",
                "mateus",
                "mateus");
        Statement statement = connection.createStatement();
        statement.execute("LISTEN rely_changed");
        PGConnection postgres = connection.unwrap(PGConnection.class);

        while (true) {
            statement.execute("SELECT 1");
            PGNotification[] notifications = postgres.getNotifications();
            if (notifications != null) {
                System.out.println(Arrays.toString(notifications));
            }
            System.out.println("No notifications");
            Thread.sleep(3000);
        }

    }
}
