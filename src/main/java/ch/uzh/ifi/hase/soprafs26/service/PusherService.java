package ch.uzh.ifi.hase.soprafs26.service;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.pusher.rest.Pusher;

import ch.uzh.ifi.hase.soprafs26.config.SecretManagerService;

@Profile("!test")
@Service
public class PusherService {

    private final Pusher pusher;

    private final Logger log = LoggerFactory.getLogger(GameService.class);

    public PusherService(org.springframework.core.env.Environment env,
                          SecretManagerService secrets) {

        // look for env variables locally
        String appId = env.getProperty("pusher.appId");
        String key = env.getProperty("pusher.key");
        String secret = env.getProperty("pusher.secret");
        String cluster = env.getProperty("pusher.cluster");

     
        // if env variables missing locally -> use secret manager
        if (isMissing(appId, key, secret, cluster)) {

            String projectId = com.google.cloud.ServiceOptions.getDefaultProjectId();
            if (projectId == null || projectId.isBlank()) {
                log.debug("GOOGLE_CLOUD_PROJECT resulted in faulty projectId {}", projectId);

                throw new IllegalStateException(
                    "GOOGLE_CLOUD_PROJECT is not set. required for secret manager"); 
            }  

            appId = Objects.requireNonNull(secrets.getSecret(projectId, "pusher-app-id"));
            key = Objects.requireNonNull(secrets.getSecret(projectId, "pusher-key"));
            secret = Objects.requireNonNull(secrets.getSecret(projectId, "pusher-secret"));
            cluster = Objects.requireNonNull(secrets.getSecret(projectId, "pusher-cluster"));
        }

        this.pusher = new Pusher(appId, key, secret);
        this.pusher.setCluster(cluster);
        this.pusher.setEncrypted(true);
    }

    private boolean isMissing(String... values) {
        for (String v : values) {
            if (v == null || v.isBlank()) return true;
        }
        return false;
    }

    public void trigger(String channel, String event, Object payload) {
        pusher.trigger(channel, event, payload);
    }
}
