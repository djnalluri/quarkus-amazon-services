package io.quarkus.amazon.credentials.rds.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import software.amazon.awssdk.services.rds.RdsUtilities;

/**
 * This class is necessary to work around an issue with credential providers
 * being or relying on synthetic beans. Some downstream consumers like
 * reactive-mysql-client produce synthetic beans while consuming credential providers
 * leading to a dependency loop if a credential provider requires synthetic beans.
 */
@Singleton
public class RdsUtilitiesProducer {

    @Produces
    @Singleton
    public RdsUtilities produceUtilities(RdsConfig config) {
        // AWS recommends creating from an RdsClient, however, internally, it only
        // makes use of the client's credentials provider and region configuration.
        // RdsUtilities "does not make network calls" except for some credential types.
        var builder = RdsUtilities.builder();
        if (config.aws != null) {
            var aws = config.aws;
            if (aws.credentials != null && aws.credentials.type != null) {
                builder.credentialsProvider(
                        aws.credentials.type.create(aws.credentials, "quarkus.rds"));
            }
            if (aws.region != null && aws.region.isPresent()) {
                builder.region(aws.region.get());
            }
        }
        return builder.build();
    }
}
