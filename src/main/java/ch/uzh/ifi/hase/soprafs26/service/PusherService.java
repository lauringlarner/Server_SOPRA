package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.pusher.rest.Pusher;

@Service
public class PusherService {

    private final Logger log = LoggerFactory.getLogger(PusherService.class);

    private final Pusher pusher;

    public PusherService(
            @Value("${PUSHER_APP_ID}") String appId,
            @Value("${PUSHER_KEY}") String key,
            @Value("${PUSHER_SECRET}") String secret,
            @Value("${PUSHER_CLUSTER}") String cluster
        ) {

        this.pusher = new Pusher(appId, key, secret);
        this.pusher.setCluster(cluster);
        this.pusher.setEncrypted(true);
    }

    @Async
    public void trigger(String channel, String event, Object payload) {
        try {
            pusher.trigger(channel, event, payload);
        } catch (RuntimeException exception) {
            log.warn("Pusher trigger failed for channel {} and event {}", channel, event, exception);
        }
    }
}
