package ch.uzh.ifi.hase.soprafs26.config;

import org.springframework.stereotype.Component;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;

@Component
public class SecretManagerService {

    public String getSecret(String projectId, String secretId) {
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {

            SecretVersionName secretVersionName =
                    SecretVersionName.of(projectId, secretId, "latest");

            AccessSecretVersionResponse response =
                    client.accessSecretVersion(secretVersionName);

            return response.getPayload().getData().toStringUtf8();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load secret: " + secretId, e);
        }
    }
}