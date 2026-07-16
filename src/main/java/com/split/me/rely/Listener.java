package com.split.me.rely;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Listener {

    private static final Logger LOG = LoggerFactory.getLogger(Listener.class);

    void event(String payload) {
        LOG.info("Payload from listener is: {}", payload);
    }
}
