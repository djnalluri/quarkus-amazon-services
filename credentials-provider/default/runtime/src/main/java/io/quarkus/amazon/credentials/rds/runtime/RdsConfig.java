package io.quarkus.amazon.credentials.rds.runtime;

import java.util.Map;

import io.quarkus.amazon.common.runtime.AwsConfig;
import io.quarkus.runtime.annotations.*;

@ConfigRoot(name = "rds", phase = ConfigPhase.RUN_TIME)
public class RdsConfig {

    /**
     * AWS services configurations
     */
    @ConfigItem
    @ConfigDocSection
    public AwsConfig aws;

    /**
     * A set of named configurations for generating RDS credentials
     */
    @ConfigDocMapKey("name")
    @ConfigItem
    public Map<String, RdsCredentialsProviderConfig> credentialsProvider;

    @ConfigGroup
    public static class RdsCredentialsProviderConfig {

        /**
         * The username to log in as.
         */
        @ConfigItem
        public String username;

        /**
         * The hostname of the database to connect to.
         */
        @ConfigItem
        public String hostname;

        /**
         * The port number the database is listening on.
         */
        @ConfigItem
        public Integer port;
    }

}
