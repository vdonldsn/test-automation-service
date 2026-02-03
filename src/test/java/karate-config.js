function fn() {
  /**
   * Karate Configuration - karate-config.js
   * 
   * This JavaScript file is executed once before all Karate tests.
   * It sets up the testing environment by:
   * 1. Loading Spring application context
   * 2. Injecting CommonUtility for service access
   * 3. Setting environment-specific configuration
   * 4. Providing global variables and functions
   * 
   * Why this file is critical:
   * -------------------------
   * Karate runs in Nashorn/GraalVM JavaScript engine, but we want to use:
   * - Java service implementations (S3, DynamoDB, Lambda, etc.)
   * - Spring dependency injection
   * - Configuration from application.yml
   * 
   * This file bridges the gap between JavaScript (Karate) and Java (our services).
   * 
   * Usage in feature files:
   * ----------------------
   * Background:
   *   * def config = commonUtility
   *   * def s3 = config.S3Impl()
   *   * def content = s3.readObject('bucket', 'key')
   * 
   * Real-world benefit:
   * -------------------
   * - Write tests in Gherkin/Karate (easy to read)
   * - Execute using robust Java implementations (reliable)
   * - Share code across multiple test suites
   * - No code duplication between Java and JavaScript
   */

  // Get environment from system property or default to 'dev'
  var env = karate.env || 'dev';
  karate.log('Karate environment:', env);
  karate.log('Java version:', java.lang.System.getProperty('java.version'));

  // ==================== Spring Context Integration ====================
  
  /**
   * Load Spring Application Context
   * 
   * This is the magic that connects Karate to Spring Boot.
   * We get the running Spring context which contains all our @Service beans.
   * 
   * How it works:
   * 1. Karate tests run inside Spring Boot application
   * 2. Spring Boot initializes all @Service beans (S3ServiceImpl, etc.)
   * 3. We retrieve the application context
   * 4. We get CommonUtility bean which has references to all services
   * 5. We expose it to Karate as 'commonUtility'
   */
  var SpringContext = Java.type('org.springframework.context.ApplicationContext');
  var applicationContext = karate.get('applicationContext');
  
  if (!applicationContext) {
    karate.log('WARNING: Spring ApplicationContext not found. Running in standalone mode.');
    karate.log('Some service calls may not work without Spring context.');
  }

  // ==================== CommonUtility Integration ====================
  
  /**
   * Get CommonUtility bean from Spring context
   * 
   * This provides access to all service implementations:
   * - S3ServiceImpl
   * - DynamoDbServiceImpl
   * - LambdaServiceImpl
   * - SnsServiceImpl
   * - SqsServiceImpl
   * - DatabaseServiceImpl
   * - LoggerServiceImpl
   * - ConfigServiceImpl
   * - DataGeneratorServiceImpl
   * - MicroserviceClientImpl
   */
  var commonUtility = null;
  
  if (applicationContext) {
    try {
      commonUtility = applicationContext.getBean('commonUtility');
      karate.log('CommonUtility loaded successfully');
      karate.log('Available services:', Object.keys(commonUtility));
    } catch (e) {
      karate.log('ERROR: Failed to load CommonUtility:', e.message);
    }
  }

  // ==================== Environment-Specific Configuration ====================
  
  /**
   * Base configuration object
   * 
   * This object is returned and made available to all Karate features.
   * Features can access it via: * def config = karate.config
   * 
   * We populate it with:
   * - Environment name
   * - Service URLs
   * - AWS configuration
   * - Database settings
   * - CommonUtility reference
   * - Helper functions
   */
  var config = {
    env: env,
    
    // Make CommonUtility globally available
    commonUtility: commonUtility,
    
    // Base URLs for microservices
    baseUrl: karate.properties['base.url'] || 'http://localhost:8080',
    userServiceUrl: karate.properties['user.service.url'] || 'http://localhost:8081',
    orderServiceUrl: karate.properties['order.service.url'] || 'http://localhost:8082',
    paymentServiceUrl: karate.properties['payment.service.url'] || 'http://localhost:8083',
    
    // AWS Configuration
    aws: {
      region: karate.properties['aws.region'] || 'us-east-1',
      s3Bucket: karate.properties['s3.bucket'] || 'test-automation-bucket',
      dynamoTablePrefix: karate.properties['dynamodb.prefix'] || 'testautomation',
    },
    
    // Database Configuration
    database: {
      url: karate.properties['db.url'] || 'jdbc:postgresql://localhost:5432/testautomation',
      username: karate.properties['db.username'] || 'testuser',
    },
    
    // Test Configuration
    test: {
      timeout: 30000, // 30 seconds default timeout
      retryCount: 3,  // Number of retries for flaky tests
      parallelThreads: parseInt(karate.properties['karate.threads'] || '5'),
    },
    
    // Global test data directory
    testDataDir: 'src/test/resources/testdata',
    
    // Helper flag for debugging
    debug: env === 'dev',
  };

  // ==================== Environment-Specific Overrides ====================
  
  if (env === 'dev') {
    karate.log('Configuring for DEV environment');
    config.baseUrl = 'http://localhost:8080';
    config.debug = true;
    config.test.timeout = 60000; // Longer timeout for debugging
  } 
  else if (env === 'qa') {
    karate.log('Configuring for QA environment');
    config.baseUrl = karate.properties['qa.base.url'] || 'https://qa-api.example.com';
    config.aws.s3Bucket = 'test-automation-qa-bucket';
    config.aws.dynamoTablePrefix = 'qa-testautomation';
  } 
  else if (env === 'staging') {
    karate.log('Configuring for STAGING environment');
    config.baseUrl = karate.properties['staging.base.url'] || 'https://staging-api.example.com';
    config.aws.s3Bucket = 'test-automation-staging-bucket';
    config.aws.dynamoTablePrefix = 'staging-testautomation';
  } 
  else if (env === 'prod') {
    karate.log('Configuring for PROD environment');
    config.baseUrl = karate.properties['prod.base.url'] || 'https://api.example.com';
    config.aws.s3Bucket = 'test-automation-prod-bucket';
    config.aws.dynamoTablePrefix = 'prod-testautomation';
    config.debug = false;
    config.test.retryCount = 1; // No retries in prod
  }

  // ==================== Global Helper Functions ====================
  
  /**
   * Global helper function to generate unique IDs
   * Usage in Karate: * def uniqueId = generateUniqueId()
   */
  config.generateUniqueId = function() {
    var UUID = Java.type('java.util.UUID');
    return UUID.randomUUID().toString();
  };

  /**
   * Global helper function to generate timestamps
   * Usage in Karate: * def timestamp = getCurrentTimestamp()
   */
  config.getCurrentTimestamp = function() {
    var Instant = Java.type('java.time.Instant');
    return Instant.now().toString();
  };

  /**
   * Global helper function to wait/sleep
   * Usage in Karate: * call waitMs 1000
   */
  config.waitMs = function(milliseconds) {
    java.lang.Thread.sleep(milliseconds);
  };

  /**
   * Global helper to read file from classpath
   * Usage in Karate: * def data = readFile('testdata/sample.json')
   */
  config.readFile = function(path) {
    return karate.read('classpath:' + path);
  };

  /**
   * Global helper to format strings
   * Usage in Karate: * def message = format('Hello {0}, welcome to {1}', ['John', 'Karate'])
   */
  config.format = function(template, args) {
    for (var i = 0; i < args.length; i++) {
      template = template.replace('{' + i + '}', args[i]);
    }
    return template;
  };

  /**
   * Retry helper for flaky operations
   * Usage in Karate: * def result = retry(function() { return someOperation() }, 3)
   */
  config.retry = function(operation, maxAttempts, delayMs) {
    maxAttempts = maxAttempts || config.test.retryCount;
    delayMs = delayMs || 1000;
    
    for (var attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        karate.log('Attempt', attempt, 'of', maxAttempts);
        return operation();
      } catch (e) {
        karate.log('Attempt', attempt, 'failed:', e.message);
        if (attempt === maxAttempts) {
          throw e;
        }
        config.waitMs(delayMs);
      }
    }
  };

  // ==================== Logging & Debugging ====================
  
  /**
   * Enhanced logging function
   * Usage in Karate: * call logInfo 'Test message'
   */
  config.logInfo = function(message) {
    karate.log('[INFO]', config.getCurrentTimestamp(), '-', message);
    if (commonUtility) {
      commonUtility.LoggerImpl().log(message, 'INFO');
    }
  };

  config.logDebug = function(message) {
    if (config.debug) {
      karate.log('[DEBUG]', config.getCurrentTimestamp(), '-', message);
      if (commonUtility) {
        commonUtility.LoggerImpl().log(message, 'DEBUG');
      }
    }
  };

  config.logError = function(message) {
    karate.log('[ERROR]', config.getCurrentTimestamp(), '-', message);
    if (commonUtility) {
      commonUtility.LoggerImpl().log(message, 'ERROR');
    }
  };

  // ==================== Configuration Summary ====================
  
  karate.log('=====================================================');
  karate.log('Karate Configuration Summary:');
  karate.log('Environment:', config.env);
  karate.log('Base URL:', config.baseUrl);
  karate.log('AWS Region:', config.aws.region);
  karate.log('S3 Bucket:', config.aws.s3Bucket);
  karate.log('DynamoDB Prefix:', config.aws.dynamoTablePrefix);
  karate.log('Debug Mode:', config.debug);
  karate.log('Timeout:', config.test.timeout, 'ms');
  karate.log('Retry Count:', config.test.retryCount);
  karate.log('CommonUtility Available:', commonUtility !== null);
  karate.log('=====================================================');

  // Return configuration object
  // This becomes available in all Karate features
  return config;
}
