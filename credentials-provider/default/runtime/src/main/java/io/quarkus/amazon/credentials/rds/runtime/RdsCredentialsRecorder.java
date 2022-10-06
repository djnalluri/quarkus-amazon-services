package io.quarkus.amazon.credentials.rds.runtime;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.services.rds.RdsUtilities;

@Recorder
public class RdsCredentialsRecorder {

    final RdsConfig config;

    public RdsCredentialsRecorder(RdsConfig config) {
        this.config = config;
    }

    public RuntimeValue<RdsUtilities> createRdsUtilities() {
        // AWS recommends creating from an RdsClient, however, internally, it only
        // makes use of the client's credentials provider and region configuration.
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
        return new RuntimeValue<>(builder.build());
    }
}
