package com.automation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Swagger/OpenAPI Configuration
 * 
 * Provides interactive API documentation for the Test Automation Service.
 * 
 * Access Points:
 * - Swagger UI: http://localhost:8080/test-automation/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/test-automation/api-docs
 * 
 * Why Swagger is useful for Test Automation Service?
 * --------------------------------------------------
 * 1. Interactive Testing: Developers can trigger tests directly from browser
 * 2. API Discovery: Shows all available endpoints and their parameters
 * 3. Documentation: Auto-generated docs stay in sync with code
 * 4. Integration: Other teams can understand how to trigger tests
 * 5. Examples: Provides request/response examples for each endpoint
 * 
 * Real-World Usage:
 * -----------------
 * - QA team uses Swagger UI to manually trigger test suites
 * - DevOps uses API docs to integrate with CI/CD pipelines
 * - Developers use Try It Out to debug test execution
 * - Management uses to understand test automation capabilities
 * 
 * ECS/ALB Integration:
 * -------------------
 * - Access via ALB: https://alb.example.com/test-automation/swagger-ui.html
 * - Works with path-based routing
 * - No authentication required (add if needed)
 */
@Configuration
public class SwaggerConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * Configures OpenAPI documentation.
     * 
     * This defines the metadata shown in Swagger UI:
     * - Service name and description
     * - Version information
     * - Contact details
     * - Available servers (local, ECS)
     * - API tags for grouping endpoints
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Test Automation Service API")
                        .version("1.0.0")
                        .description("""
                                # Test Automation Service
                                
                                A Karate-based test automation microservice that executes automated tests via REST API.
                                
                                ## Features
                                - Execute Karate test suites on-demand
                                - Support for multiple environments (dev, qa, staging, prod)
                                - Tag-based test filtering
                                - Parallel test execution
                                - Real-time test results
                                
                                ## Common Use Cases
                                
                                ### 1. Run Smoke Tests
                                ```
                                POST /api/execute
                                {
                                  "tags": ["@smoke"],
                                  "environment": "qa"
                                }
                                ```
                                
                                ### 2. Run Specific Feature
                                ```
                                POST /api/execute
                                {
                                  "feature": "features/s3/S3Operations.feature",
                                  "environment": "dev"
                                }
                                ```
                                
                                ### 3. Run All Tests in Parallel
                                ```
                                POST /api/execute
                                {
                                  "environment": "staging",
                                  "threads": 10
                                }
                                ```
                                
                                ## Available Test Tags
                                - `@smoke` - Quick smoke tests
                                - `@s3` - S3 operations tests
                                - `@dynamodb` - DynamoDB operations tests
                                - `@lambda` - Lambda function tests
                                - `@sqs` - SQS messaging tests
                                - `@integration` - Full integration tests
                                
                                ## Authentication
                                Currently no authentication required. Add API key or OAuth if needed.
                                
                                ## Rate Limiting
                                Test execution is resource-intensive. Recommend limiting to:
                                - Max 5 concurrent executions
                                - Cool-down period of 1 minute between runs
                                
                                ## Support
                                - Email: test-automation-team@example.com
                                - Slack: #test-automation
                                - Wiki: https://wiki.example.com/test-automation
                                """)
                        .contact(new Contact()
                                .name("Test Automation Team")
                                .email("test-automation-team@example.com")
                                .url("https://github.com/your-org/test-automation-service"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(getServers())
                .tags(getTags());
    }

    /**
     * Defines available servers for API testing.
     * 
     * Swagger UI allows switching between servers.
     * Useful for testing against different environments.
     */
    private List<Server> getServers() {
        return Arrays.asList(
                new Server()
                        .url("http://localhost:8080" + contextPath)
                        .description("Local Development Server"),
                new Server()
                        .url("https://dev-alb.example.com" + contextPath)
                        .description("Development (ECS)"),
                new Server()
                        .url("https://qa-alb.example.com" + contextPath)
                        .description("QA Environment (ECS)"),
                new Server()
                        .url("https://staging-alb.example.com" + contextPath)
                        .description("Staging Environment (ECS)"),
                new Server()
                        .url("https://alb.example.com" + contextPath)
                        .description("Production (ECS)")
        );
    }

    /**
     * Defines API tags for grouping endpoints.
     * 
     * Tags organize endpoints in Swagger UI:
     * - Test Execution: Main test running endpoints
     * - Test Management: List features, get info
     * - Health & Monitoring: Health checks, metrics
     */
    private List<Tag> getTags() {
        return Arrays.asList(
                new Tag()
                        .name("Test Execution")
                        .description("Endpoints for triggering and managing test executions"),
                new Tag()
                        .name("Test Management")
                        .description("Endpoints for discovering and managing test resources"),
                new Tag()
                        .name("Health & Monitoring")
                        .description("Health checks and service information")
        );
    }
}
