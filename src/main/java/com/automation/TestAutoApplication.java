package com.automation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Test Automation Service - Main Application Entry Point
 * 
 * This Spring Boot application serves as a test automation runner that:
 * 1. Runs as a microservice deployable to AWS ECS
 * 2. Exposes REST endpoints to trigger Karate test executions
 * 3. Integrates with multiple AWS services (S3, DynamoDB, Lambda, SNS/SQS)
 * 4. Connects to PostgreSQL database for test data/results storage
 * 5. Registers with shared Application Load Balancer (ALB)
 * 
 * Architecture:
 * - Spring Boot provides the web server and dependency injection
 * - Karate provides the test execution framework
 * - CommonUtility classes provide reusable service implementations
 * - karate-config.js bootstraps Karate with CommonUtility references
 * 
 * Deployment:
 * - Local: java -jar test-automation-service.jar
 * - ECS: Deployed as Docker container with ALB listener configuration
 * 
 * Why Spring Boot for Test Automation?
 * - Provides REST API to trigger tests programmatically
 * - Health checks for ECS/ALB integration
 * - Dependency injection for service clients
 * - Configuration management across environments
 * - Actuator for monitoring and metrics
 * - Can be triggered by CI/CD, schedulers, or manually
 * 
 * @author Test Automation Team
 */
@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan("com.automation.config")
public class TestAutoApplication {

    /**
     * Application entry point.
     * Starts the Spring Boot application which:
     * 1. Initializes Spring context
     * 2. Creates AWS service clients (S3, DynamoDB, Lambda, etc.)
     * 3. Establishes database connections
     * 4. Starts embedded Tomcat server
     * 5. Registers health check endpoints
     * 6. Waits for test execution requests via REST API
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Set default profile if not specified
        // This ensures correct configuration loading
        String activeProfile = System.getProperty("spring.profiles.active");
        if (activeProfile == null || activeProfile.isEmpty()) {
            System.setProperty("spring.profiles.active", "dev");
        }

        // Start Spring Boot application
        ConfigurableApplicationContext context = SpringApplication.run(TestAutoApplication.class, args);
        
        // Log application startup information
        logApplicationStartup(context.getEnvironment());
    }

    /**
     * Logs application startup information.
     * 
     * This provides essential debugging information when the service starts:
     * - Which profile is active (dev/qa/staging/prod)
     * - What port the service is listening on
     * - What the service URL is (important for ALB configuration)
     * - Local vs ECS deployment context
     * 
     * Why this matters:
     * - In ECS, you need to know the container port
     * - ALB needs to route to correct port and path
     * - Profile determines which AWS resources to connect to
     * - Helps troubleshoot startup issues quickly
     * 
     * @param env Spring Environment containing configuration
     */
    private static void logApplicationStartup(Environment env) {
        String protocol = "http";
        String serverPort = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path", "/");
        String hostAddress = "localhost";
        
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Unable to determine host address", e);
        }

        String ecsCluster = env.getProperty("ecs.cluster-name");
        String albName = env.getProperty("ecs.alb.name");
        String listenerPath = env.getProperty("server.listener.path-pattern");
        
        log.info("\n----------------------------------------------------------\n" +
                "Application '{}' is running!\n" +
                "Profile(s):     {}\n" +
                "Local Access:   {}://{}:{}{}\n" +
                "External URL:   {}://{}:{}{}\n" +
                "ECS Cluster:    {}\n" +
                "ALB:            {}\n" +
                "Listener Path:  {}\n" +
                "Health Check:   {}://{}:{}{}/actuator/health\n" +
                "----------------------------------------------------------",
                env.getProperty("spring.application.name"),
                env.getActiveProfiles().length > 0 ? 
                    String.join(", ", env.getActiveProfiles()) : "default",
                protocol,
                "localhost",
                serverPort,
                contextPath,
                protocol,
                hostAddress,
                serverPort,
                contextPath,
                ecsCluster != null ? ecsCluster : "N/A (running locally)",
                albName != null ? albName : "N/A (running locally)",
                listenerPath != null ? listenerPath : "N/A",
                protocol,
                hostAddress,
                serverPort,
                contextPath
        );

        // Log AWS service endpoints
        log.info("AWS Service Configuration:");
        log.info("  Region:    {}", env.getProperty("aws.region"));
        log.info("  S3 Bucket: {}", env.getProperty("aws.s3.bucket-name"));
        log.info("  DynamoDB:  {}", env.getProperty("aws.dynamodb.table-prefix"));
        log.info("  Lambda:    {}", env.getProperty("aws.lambda.function-prefix"));
        log.info("  SNS:       {}", env.getProperty("aws.sns.topic-prefix"));
        log.info("  SQS:       {}", env.getProperty("aws.sqs.queue-prefix"));

        // Log database configuration
        log.info("Database Configuration:");
        log.info("  URL: {}", env.getProperty("spring.datasource.url"));
        
        // Log Karate configuration
        log.info("Karate Configuration:");
        log.info("  Environment: {}", env.getProperty("karate.env"));
        log.info("  Threads:     {}", env.getProperty("karate.threads"));
        log.info("  Output Dir:  {}", env.getProperty("karate.output-dir"));
    }
}
