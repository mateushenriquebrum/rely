package com.split.me.rely;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Panic {
    public static Connection tryGetConnection(DataSource source) {
        int count = 0;
        while (count < 3) {
            try {
                return source.getConnection();
            } catch (Exception e) {
                count++;
                if (count == 3) {
                    throw new RuntimeException(e);
                }
                try {
                    Thread.sleep(10L * count);
                } catch (InterruptedException e1) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    public static Status tryConvertToStatus(String status, String topic, String payload, int id, int invoked) {
        return switch (status) {
            case "PENDING" -> new Status.Pending(topic, payload, id);
            case "RETRY" -> new Status.Retry(topic, payload, id, invoked);
            case "UNREACHABLE" -> new Status.Unreachable(topic, payload, id, invoked);
            case "SUCCESS" -> new Status.Success(topic, payload, id);
            case "FAILURE" -> new Status.Failure(topic, payload, id);
            default ->  throw new IllegalStateException("Unexpected value: " + status);
        };
    }

    public static PanicResult tryQuery(Connection connection, String query) {
        try {
            Statement statement = connection.createStatement();
            return new PanicResult(statement.executeQuery(query));
        } catch (Exception e) {
            throw  new RuntimeException(e);
        }
    }

    public static class PanicResult {
        private final ResultSet unsafe;

        public PanicResult(ResultSet unsafe) {
            this.unsafe = unsafe;
        }

        public int tyrGetInt(String column) {
            try {
                return unsafe.getInt(column);
            } catch (Exception e) {
                throw  new RuntimeException(e);
            }
        }

        public String tryGetString(String column) {
            try {
                return unsafe.getString(column);
            } catch (Exception e) {
                throw  new RuntimeException(e);
            }
        }

        public byte[] tryGetBytes(String column) {
            try {
                return unsafe.getBytes(column);
            } catch (Exception e) {
                throw  new RuntimeException(e);
            }
        }

        public boolean tryNext() {
            try {
                return unsafe.next();
            } catch (Exception e) {
                throw  new RuntimeException(e);
            }
        }
    }
}
