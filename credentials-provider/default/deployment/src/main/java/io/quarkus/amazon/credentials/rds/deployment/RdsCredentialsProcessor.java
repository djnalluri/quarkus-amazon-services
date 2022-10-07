package io.quarkus.amazon.credentials.rds.deployment;

import io.quarkus.amazon.credentials.rds.runtime.RdsCredentialsProvider;
import io.quarkus.amazon.credentials.rds.runtime.RdsUtilitiesProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;

public class RdsCredentialsProcessor {

    @BuildStep
    AdditionalBeanBuildItem produceBeans() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(RdsCredentialsProvider.class)
                .addBeanClass(RdsUtilitiesProducer.class)
                .build();
    }
}
