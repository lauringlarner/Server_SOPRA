package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pusher.rest.Pusher;

@Service
public class PusherService {

    private final Pusher pusher;

    private final Logger log = LoggerFactory.getLogger(PusherService.class);

    public PusherService(
            @Value("${pusher.appId}") String appId,
            @Value("${pusher.key}") String key,
            @Value("${pusher.secret}") String secret,
            @Value("${pusher.cluster}") String cluster
        ) {

        this.pusher = new Pusher(appId, key, secret);
        this.pusher.setCluster(cluster);
        this.pusher.setEncrypted(true);
    }

    public void trigger(String channel, String event, Object payload) {
        pusher.trigger(channel, event, payload);
    }
}
