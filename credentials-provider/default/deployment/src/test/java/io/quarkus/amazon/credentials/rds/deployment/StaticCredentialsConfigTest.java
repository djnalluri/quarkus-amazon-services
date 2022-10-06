package io.quarkus.amazon.credentials.rds.deployment;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

public class StaticCredentialsConfigTest {

    @Inject
    RdsUtilities rds;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("static-credentials-config.properties", "application.properties"));

    @Test
    public void test() {
        Assertions.assertNotNull(rds);
        var request = GenerateAuthenticationTokenRequest.builder()
                .username("test-user")
                .hostname("test-database")
                .port(9999)
                .build();
        var token = rds.generateAuthenticationToken(request);
        Assertions.assertNotNull(token);
    }
}
