package com.automation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AWS Configuration Properties
 * 
 * Maps application.yml AWS configuration to Java objects.
 * Spring Boot automatically populates these from application.yml.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {
    
    private String region;
    private Credentials credentials = new Credentials();
    private S3 s3 = new S3();
    private Dynamodb dynamodb = new Dynamodb();
    private Lambda lambda = new Lambda();
    private Sns sns = new Sns();
    private Sqs sqs = new Sqs();
    private Cloudwatch cloudwatch = new Cloudwatch();
    
    @Data
    public static class Credentials {
        private String accessKey;
        private String secretKey;
    }
    
    @Data
    public static class S3 {
        private String endpoint;
        private String bucketName;
        private int connectionTimeout;
        private int requestTimeout;
    }
    
    @Data
    public static class Dynamodb {
        private String endpoint;
        private String tablePrefix;
        private int connectionTimeout;
        private int requestTimeout;
    }
    
    @Data
    public static class Lambda {
        private String endpoint;
        private String functionPrefix;
        private int invocationTimeout;
    }
    
    @Data
    public static class Sns {
        private String endpoint;
        private String topicPrefix;
        private int connectionTimeout;
    }
    
    @Data
    public static class Sqs {
        private String endpoint;
        private String queuePrefix;
        private int connectionTimeout;
        private int waitTimeSeconds;
        private int maxMessages;
    }
    
    @Data
    public static class Cloudwatch {
        private String namespace;
        private boolean enabled;
    }
}
