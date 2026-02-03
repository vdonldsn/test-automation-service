package com.automation.common;

import com.automation.service.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * CommonUtility - Central access point for all service implementations in Karate tests.
 * 
 * This class serves as a bridge between Karate feature files and Java service implementations.
 * It's injected into karate-config.js and made available to all Karate tests.
 * 
 * Why this pattern?
 * ----------------
 * Karate is JavaScript-based, but we want to leverage Java implementations for:
 * 1. AWS SDK interactions (S3, DynamoDB, Lambda, SNS/SQS)
 * 2. Database operations (PostgreSQL via JPA)
 * 3. Complex business logic
 * 4. Reusable utility functions
 * 
 * Usage in Karate feature files:
 * ------------------------------
 * Feature: S3 Operations Test
 *   Background:
 *     * def config = commonUtility
 *     * def s3Service = config.S3Impl()
 *     * def s3Read = s3Service.s3Read()
 *   
 *   Scenario: Read file from S3
 *     * def content = s3Read.readObject('my-bucket', 'test.txt')
 *     * match content != null
 * 
 * Design Benefits:
 * ----------------
 * 1. Single Responsibility: Each service handles one AWS resource
 * 2. Testability: Services can be mocked for unit testing
 * 3. Reusability: Same logic used across multiple Karate features
 * 4. Type Safety: Java compilation catches errors early
 * 5. Spring Integration: Automatic dependency injection and configuration
 * 
 * Real-World Application:
 * -----------------------
 * In enterprise testing, you often need to:
 * - Set up test data in S3 before API tests
 * - Validate DynamoDB records after mutations
 * - Invoke Lambda functions to prepare state
 * - Verify SQS messages were sent
 * - Query PostgreSQL for expected results
 * 
 * This utility makes all of that accessible from simple Karate scripts.
 * 
 * @author Test Automation Team
 */
@Slf4j
@Component("commonUtility")
@Getter
public class CommonUtility {

    // AWS Service Implementations
    private final S3ServiceImpl s3Service;
    private final DynamoDbServiceImpl dynamoDbService;
    private final LambdaServiceImpl lambdaService;
    private final SnsServiceImpl snsService;
    private final SqsServiceImpl sqsService;
    
    // Database Service
    private final DatabaseServiceImpl databaseService;
    
    // Utility Services
    private final LoggerServiceImpl loggerService;
    private final ConfigServiceImpl configService;
    private final DataGeneratorServiceImpl dataGeneratorService;
    
    // Microservice Clients
    private final MicroserviceClientImpl microserviceClient;

    /**
     * Constructor with dependency injection.
     * 
     * Spring automatically injects all service implementations.
     * These services are singletons, created once and reused across all tests.
     * 
     * Why constructor injection?
     * - Immutable fields (final)
     * - Clear dependencies
     * - Easy to test
     * - No reflection magic
     */
    @Autowired
    public CommonUtility(
            S3ServiceImpl s3Service,
            DynamoDbServiceImpl dynamoDbService,
            LambdaServiceImpl lambdaService,
            SnsServiceImpl snsService,
            SqsServiceImpl sqsService,
            DatabaseServiceImpl databaseService,
            LoggerServiceImpl loggerService,
            ConfigServiceImpl configService,
            DataGeneratorServiceImpl dataGeneratorService,
            MicroserviceClientImpl microserviceClient) {
        
        this.s3Service = s3Service;
        this.dynamoDbService = dynamoDbService;
        this.lambdaService = lambdaService;
        this.snsService = snsService;
        this.sqsService = sqsService;
        this.databaseService = databaseService;
        this.loggerService = loggerService;
        this.configService = configService;
        this.dataGeneratorService = dataGeneratorService;
        this.microserviceClient = microserviceClient;
        
        log.info("CommonUtility initialized with all service implementations");
    }

    // ==================== Service Accessor Methods ====================
    // These methods return service implementations for use in Karate
    // Named with 'Impl' suffix to match your requested pattern

    /**
     * Returns S3 service implementation.
     * Usage in Karate: * def s3 = commonUtility.S3Impl()
     */
    public S3ServiceImpl S3Impl() {
        return s3Service;
    }

    /**
     * Returns DynamoDB service implementation.
     * Usage in Karate: * def dynamo = commonUtility.DynamoDbImpl()
     */
    public DynamoDbServiceImpl DynamoDbImpl() {
        return dynamoDbService;
    }

    /**
     * Returns Lambda service implementation.
     * Usage in Karate: * def lambda = commonUtility.LambdaImpl()
     */
    public LambdaServiceImpl LambdaImpl() {
        return lambdaService;
    }

    /**
     * Returns SNS service implementation.
     * Usage in Karate: * def sns = commonUtility.SnsImpl()
     */
    public SnsServiceImpl SnsImpl() {
        return snsService;
    }

    /**
     * Returns SQS service implementation.
     * Usage in Karate: * def sqs = commonUtility.SqsImpl()
     */
    public SqsServiceImpl SqsImpl() {
        return sqsService;
    }

    /**
     * Returns Database service implementation.
     * Usage in Karate: * def db = commonUtility.DatabaseImpl()
     */
    public DatabaseServiceImpl DatabaseImpl() {
        return databaseService;
    }

    /**
     * Returns Logger service implementation.
     * Usage in Karate: * def logger = commonUtility.LoggerImpl()
     */
    public LoggerServiceImpl LoggerImpl() {
        return loggerService;
    }

    /**
     * Returns Config service implementation.
     * Usage in Karate: * def config = commonUtility.ConfigImpl()
     */
    public ConfigServiceImpl ConfigImpl() {
        return configService;
    }

    /**
     * Returns Data Generator service implementation.
     * Usage in Karate: * def generator = commonUtility.DataGeneratorImpl()
     */
    public DataGeneratorServiceImpl DataGeneratorImpl() {
        return dataGeneratorService;
    }

    /**
     * Returns Microservice Client implementation.
     * Usage in Karate: * def microservice = commonUtility.MicroserviceImpl()
     */
    public MicroserviceClientImpl MicroserviceImpl() {
        return microserviceClient;
    }

    // ==================== Convenience Methods ====================
    // These provide quick access to common operations without needing
    // to call Impl() methods first

    /**
     * Quick access to S3 read operation.
     * Usage in Karate: * def content = commonUtility.s3Read('bucket', 'key')
     */
    public String s3Read(String bucketName, String key) {
        return s3Service.readObject(bucketName, key);
    }

    /**
     * Quick access to S3 write operation.
     * Usage in Karate: * call commonUtility.s3Write('bucket', 'key', 'content')
     */
    public void s3Write(String bucketName, String key, String content) {
        s3Service.writeObject(bucketName, key, content);
    }

    /**
     * Quick access to DynamoDB get operation.
     * Usage in Karate: * def item = commonUtility.dynamoGet('table', 'id', '123')
     */
    public Object dynamoGet(String tableName, String keyName, String keyValue) {
        return dynamoDbService.getItem(tableName, keyName, keyValue);
    }

    /**
     * Quick access to DynamoDB put operation.
     * Usage in Karate: * call commonUtility.dynamoPut('table', item)
     */
    public void dynamoPut(String tableName, Object item) {
        dynamoDbService.putItem(tableName, item);
    }

    /**
     * Quick access to Lambda invoke operation.
     * Usage in Karate: * def result = commonUtility.lambdaInvoke('function', payload)
     */
    public String lambdaInvoke(String functionName, String payload) {
        return lambdaService.invokeFunction(functionName, payload);
    }

    /**
     * Quick access to SQS send operation.
     * Usage in Karate: * call commonUtility.sqsSend('queue', 'message')
     */
    public void sqsSend(String queueName, String message) {
        sqsService.sendMessage(queueName, message);
    }

    /**
     * Quick access to SNS publish operation.
     * Usage in Karate: * call commonUtility.snsPublish('topic', 'message')
     */
    public void snsPublish(String topicName, String message) {
        snsService.publishMessage(topicName, message);
    }

    /**
     * Quick access to database query operation.
     * Usage in Karate: * def results = commonUtility.dbQuery('SELECT * FROM users WHERE id = ?', [123])
     */
    public Object dbQuery(String sql, Object... params) {
        return databaseService.executeQuery(sql, params);
    }

    /**
     * Quick access to log operation.
     * Usage in Karate: * call commonUtility.log('Test message', 'INFO')
     */
    public void log(String message, String level) {
        loggerService.log(message, level);
    }

    /**
     * Quick access to test data generation.
     * Usage in Karate: * def testData = commonUtility.generateTestData('user', 10)
     */
    public Object generateTestData(String type, int count) {
        return dataGeneratorService.generateData(type, count);
    }

    /**
     * Quick access to microservice API calls.
     * Usage in Karate: * def response = commonUtility.callMicroservice('user-service', '/users/123', 'GET')
     */
    public Object callMicroservice(String serviceName, String endpoint, String method) {
        return microserviceClient.call(serviceName, endpoint, method, null);
    }

    /**
     * Get configuration value.
     * Usage in Karate: * def awsRegion = commonUtility.getConfig('aws.region')
     */
    public String getConfig(String key) {
        return configService.getProperty(key);
    }
}
