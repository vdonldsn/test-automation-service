package com.automation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;
import java.time.Duration;

/**
 * AWS SDK Client Configuration
 * 
 * Creates AWS SDK clients for all services.
 * Handles credential resolution and region configuration.
 * 
 * Credential Resolution Order:
 * 1. Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
 * 2. IAM role (when running in ECS)
 * 3. Configuration file credentials
 * 
 * This allows the same code to work:
 * - Locally (with env vars)
 * - In ECS (with IAM role)
 * - In CI/CD (with injected credentials)
 */
@Slf4j
@Configuration
public class AwsClientConfiguration {

    private final AwsProperties awsProperties;

    @Autowired
    public AwsClientConfiguration(AwsProperties awsProperties) {
        this.awsProperties = awsProperties;
        log.info("Initializing AWS clients for region: {}", awsProperties.getRegion());
    }

    /**
     * Creates AWS credentials provider.
     * 
     * Priority:
     * 1. Explicit credentials from config
     * 2. Environment variables
     * 3. ECS container credentials
     * 4. EC2 instance profile
     */
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        String accessKey = awsProperties.getCredentials().getAccessKey();
        String secretKey = awsProperties.getCredentials().getSecretKey();
        
        if (accessKey != null && !accessKey.isEmpty() && 
            secretKey != null && !secretKey.isEmpty()) {
            log.info("Using explicit AWS credentials from configuration");
            return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            );
        }
        
        log.info("Using default AWS credentials provider chain");
        return DefaultCredentialsProvider.create();
    }

    /**
     * S3 Client configuration.
     */
    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
        log.info("Creating S3 client for region: {}", awsProperties.getRegion());
        
        var builder = S3Client.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(credentialsProvider);
        
        // Override endpoint if specified (for localstack testing)
        if (awsProperties.getS3().getEndpoint() != null && 
            !awsProperties.getS3().getEndpoint().isEmpty()) {
            builder.endpointOverride(URI.create(awsProperties.getS3().getEndpoint()));
            log.info("Using custom S3 endpoint: {}", awsProperties.getS3().getEndpoint());
        }
        
        return builder.build();
    }

    /**
     * DynamoDB Client configuration.
     */
    @Bean
    public DynamoDbClient dynamoDbClient(AwsCredentialsProvider credentialsProvider) {
        log.info("Creating DynamoDB client for region: {}", awsProperties.getRegion());
        
        var builder = DynamoDbClient.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(credentialsProvider);
        
        if (awsProperties.getDynamodb().getEndpoint() != null && 
            !awsProperties.getDynamodb().getEndpoint().isEmpty()) {
            builder.endpointOverride(URI.create(awsProperties.getDynamodb().getEndpoint()));
            log.info("Using custom DynamoDB endpoint: {}", awsProperties.getDynamodb().getEndpoint());
        }
        
        return builder.build();
    }

    /**
     * Lambda Client configuration.
     */
    @Bean
    public LambdaClient lambdaClient(AwsCredentialsProvider credentialsProvider) {
        log.info("Creating Lambda client for region: {}", awsProperties.getRegion());
        
        var builder = LambdaClient.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(credentialsProvider);
        
        if (awsProperties.getLambda().getEndpoint() != null && 
            !awsProperties.getLambda().getEndpoint().isEmpty()) {
            builder.endpointOverride(URI.create(awsProperties.getLambda().getEndpoint()));
        }
        
        return builder.build();
    }

    /**
     * SNS Client configuration.
     */
    @Bean
    public SnsClient snsClient(AwsCredentialsProvider credentialsProvider) {
        log.info("Creating SNS client for region: {}", awsProperties.getRegion());
        
        var builder = SnsClient.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(credentialsProvider);
        
        if (awsProperties.getSns().getEndpoint() != null && 
            !awsProperties.getSns().getEndpoint().isEmpty()) {
            builder.endpointOverride(URI.create(awsProperties.getSns().getEndpoint()));
        }
        
        return builder.build();
    }

    /**
     * SQS Client configuration.
     */
    @Bean
    public SqsClient sqsClient(AwsCredentialsProvider credentialsProvider) {
        log.info("Creating SQS client for region: {}", awsProperties.getRegion());
        
        var builder = SqsClient.builder()
                .region(Region.of(awsProperties.getRegion()))
                .credentialsProvider(credentialsProvider);
        
        if (awsProperties.getSqs().getEndpoint() != null && 
            !awsProperties.getSqs().getEndpoint().isEmpty()) {
            builder.endpointOverride(URI.create(awsProperties.getSqs().getEndpoint()));
        }
        
        return builder.build();
    }
}
