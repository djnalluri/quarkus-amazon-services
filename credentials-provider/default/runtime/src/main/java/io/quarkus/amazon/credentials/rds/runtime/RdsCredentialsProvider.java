package io.quarkus.amazon.credentials.rds.runtime;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import io.quarkus.credentials.CredentialsProvider;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

@ApplicationScoped
@Named("quarkus-amazon-rds")
public class RdsCredentialsProvider implements CredentialsProvider {

    @Inject
    RdsUtilities rds;

    @Inject
    RdsConfig config;

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {

        var entry = config.credentialsProvider.get(credentialsProviderName);
        if (entry == null) {
            return null;
        }

        var request = GenerateAuthenticationTokenRequest.builder()
                .username(entry.username)
                .hostname(entry.hostname)
                .port(entry.port)
                .build();

        String token = rds.generateAuthenticationToken(request);
        var expires = System.currentTimeMillis() + (1000 * 60 * 12); // Token lifespan is 15 minutes

        Map<String, String> credentials = new HashMap<>();
        credentials.put(CredentialsProvider.USER_PROPERTY_NAME, entry.username);
        credentials.put(CredentialsProvider.PASSWORD_PROPERTY_NAME, token);
        credentials.put(CredentialsProvider.EXPIRATION_TIMESTAMP_PROPERTY_NAME, Long.toString(expires));

        return credentials;
    }
}
