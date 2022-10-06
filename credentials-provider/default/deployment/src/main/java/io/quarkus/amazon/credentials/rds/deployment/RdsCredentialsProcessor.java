package io.quarkus.amazon.credentials.rds.deployment;

import javax.inject.Singleton;

import io.quarkus.amazon.credentials.rds.runtime.RdsCredentialsRecorder;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import software.amazon.awssdk.services.rds.RdsUtilities;

public class RdsCredentialsProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem produceRdsUtilities(RdsCredentialsRecorder recorder) {
        return SyntheticBeanBuildItem.configure(RdsUtilities.class)
                .scope(Singleton.class)
                .runtimeValue(recorder.createRdsUtilities())
                .setRuntimeInit()
                .done();
    }
}
