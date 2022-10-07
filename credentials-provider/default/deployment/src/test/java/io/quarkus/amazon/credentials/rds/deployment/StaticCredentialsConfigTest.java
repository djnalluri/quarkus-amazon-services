package io.quarkus.amazon.credentials.rds.deployment;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.amazon.credentials.rds.runtime.RdsCredentialsProvider;
import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

public class StaticCredentialsConfigTest {

    private static final long TOKEN_LIFETIME = 15 * 60 * 1000;

    @Inject
    RdsUtilities rds;

    @Inject
    RdsCredentialsProvider provider;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("static-credentials-config.properties", "application.properties"));

    @Test
    public void testRdsUtilities() {
        Assertions.assertNotNull(rds);
        var request = GenerateAuthenticationTokenRequest.builder()
                .username("test-user")
                .hostname("test-database")
                .port(9999)
                .build();
        var token = rds.generateAuthenticationToken(request);
        Assertions.assertNotNull(token);
    }

    @Test
    public void testCredentialsProvider() {
        var missing = provider.getCredentials("missing");
        Assertions.assertNull(missing);

        var found = provider.getCredentials("test");
        Assertions.assertNotNull(found);
        Assertions.assertEquals(found.get(CredentialsProvider.USER_PROPERTY_NAME), "test-user");

        var password = found.get(CredentialsProvider.PASSWORD_PROPERTY_NAME);
        Assertions.assertNotNull(password);
        Assertions.assertNotEquals(password.indexOf("test-hostname"), -1);
        Assertions.assertNotEquals(password.indexOf("9999"), -1);

        var expirationString = found.get(CredentialsProvider.EXPIRATION_TIMESTAMP_PROPERTY_NAME);
        var expiration = Long.parseLong(expirationString);
        var validFor = expiration - System.currentTimeMillis();
        Assertions.assertTrue(validFor > 0 && validFor <= TOKEN_LIFETIME);
    }
}
