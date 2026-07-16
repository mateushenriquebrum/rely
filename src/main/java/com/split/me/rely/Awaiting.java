package com.split.me.rely;

import javax.sql.DataSource;
import java.sql.Connection;

import static com.split.me.rely.Panic.tryConvertToStatus;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Its await the notification from database, also recover in case of failure.
 */
public class Awaiting {
    private final Connection connection;

    public Awaiting(DataSource source) {
        this.connection = Panic.tryGetConnection(source);
        recovery();
    }

    /**
     * Whenever this object is created it need to verify if there is messages pending
     */
    private void recovery() {
        var fetch = fetch();
        if (fetch.length != 0) {
            dispatch(fetch);
        } else {
            watch();
        }
    }

    /**
     * Keep watching for verifications from database and dispatch it according
     */
    private void watch() {

    }

    /**
     * Create dispatch process and await the result.
     * @param statuses to be resolved ideally to Status.Success
     */
    private void dispatch(Status[] statuses) {
        String query = "SELECT id, status, topic, payload, invoked FROM rely WHERE status not in ('COMPLETED', 'FAILED');";
        Panic.PanicResult result = Panic.tryQuery(connection, query);
        Status[] collected = new Status[statuses.length];
        int count = 0;
        while (result.tryNext()) {
            String status = result.tryGetString("id");
            String topic = result.tryGetString("topic");
            String payload = new String(result.tryGetBytes("payload"), UTF_8);
            int id = result.tyrGetInt("id");
            int invoked = result.tyrGetInt("id");

            collected[count++] = tryConvertToStatus(status, topic, payload, id, invoked);
            //LOG.info("Will dispatch to topic {}", topic);
            //new Worker(listeners, source).work(topic, payload, id);
        }}

    private Status[] fetch() {

    }
}
