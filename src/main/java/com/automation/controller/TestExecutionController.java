package com.automation.controller;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Test Execution REST Controller
 * 
 * Exposes REST endpoints to trigger Karate test executions.
 * This is what makes the service "deployable" - other systems can
 * trigger tests by calling these APIs.
 * 
 * Why expose tests via REST API?
 * -------------------------------
 * 1. CI/CD Integration: Jenkins/GitLab can call API to run tests
 * 2. Scheduled Execution: CloudWatch Events can trigger via API
 * 3. Manual Trigger: Developers can run tests via Postman/curl
 * 4. Monitoring: Health checks validate test infrastructure
 * 5. On-Demand: Run specific test suites when needed
 * 
 * ECS/ALB Integration:
 * --------------------
 * - ALB routes requests to /test-automation/* to this service
 * - Health checks hit /test-automation/actuator/health
 * - Test execution via /test-automation/api/execute
 * 
 * Usage Examples:
 * ---------------
 * # Run all tests
 * POST http://localhost:8080/test-automation/api/execute
 * 
 * # Run S3 tests only
 * POST http://localhost:8080/test-automation/api/execute
 * {
 *   "tags": ["@s3"],
 *   "environment": "qa"
 * }
 * 
 * # Run specific feature
 * POST http://localhost:8080/test-automation/api/execute
 * {
 *   "feature": "features/s3/S3Operations.feature"
 * }
 */
@Slf4j
@RestController
@RequestMapping("/api")
@Tag(name = "Test Execution", description = "Endpoints for triggering and managing Karate test executions")
public class TestExecutionController {

    /**
     * Health check endpoint.
     * Used by ALB target group health checks.
     */
    @GetMapping("/health")
    @Tag(name = "Health & Monitoring")
    @Operation(
        summary = "Health Check",
        description = "Returns the health status of the test automation service. Used by ALB target groups."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "test-automation-service");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    /**
     * Executes Karate tests based on request parameters.
     * 
     * @param request Test execution request
     * @return Test execution results
     */
    @PostMapping("/execute")
    @Operation(
        summary = "Execute Karate Tests",
        description = """
            Triggers execution of Karate test suites based on the provided parameters.
            
            You can filter tests by:
            - Tags (e.g., @smoke, @s3, @integration)
            - Specific feature file
            - Environment (dev, qa, staging, prod)
            - Parallel execution threads
            
            The endpoint returns immediately with test results including:
            - Success/failure status
            - Number of scenarios executed
            - Execution duration
            - Path to detailed reports
            
            **Note:** Test execution can take several minutes depending on the test suite size.
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Test execution configuration",
            required = false,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TestExecutionRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Run all tests",
                        value = """
                            {
                              "environment": "dev",
                              "threads": 5
                            }
                            """,
                        description = "Execute all tests with default settings"
                    ),
                    @ExampleObject(
                        name = "Run smoke tests",
                        value = """
                            {
                              "tags": ["@smoke"],
                              "environment": "qa",
                              "threads": 3
                            }
                            """,
                        description = "Execute only smoke tests in QA environment"
                    ),
                    @ExampleObject(
                        name = "Run S3 tests",
                        value = """
                            {
                              "tags": ["@s3"],
                              "environment": "dev",
                              "threads": 1
                            }
                            """,
                        description = "Execute S3-related tests sequentially"
                    ),
                    @ExampleObject(
                        name = "Run specific feature",
                        value = """
                            {
                              "feature": "features/s3/S3Operations.feature",
                              "environment": "staging"
                            }
                            """,
                        description = "Execute a specific feature file"
                    ),
                    @ExampleObject(
                        name = "Parallel execution",
                        value = """
                            {
                              "environment": "qa",
                              "threads": 10
                            }
                            """,
                        description = "Execute all tests with 10 parallel threads"
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "All tests passed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TestExecutionResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "success": true,
                          "message": "All tests passed successfully",
                          "totalScenarios": 15,
                          "passedScenarios": 15,
                          "failedScenarios": 0,
                          "durationMs": 12500,
                          "reportPath": "build/karate-reports",
                          "timestamp": 1707456789000
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "206",
            description = "Test execution completed with some failures",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TestExecutionResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "success": false,
                          "message": "Test execution completed with failures",
                          "totalScenarios": 15,
                          "passedScenarios": 12,
                          "failedScenarios": 3,
                          "durationMs": 15000,
                          "reportPath": "build/karate-reports",
                          "timestamp": 1707456789000,
                          "errors": ["Failed scenarios: 3"]
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Test execution failed to start",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TestExecutionResponse.class)
            )
        )
    })
    public ResponseEntity<TestExecutionResponse> executeTests(
            @RequestBody(required = false) TestExecutionRequest request) {
        
        if (request == null) {
            request = new TestExecutionRequest();
        }
        
        log.info("Received test execution request: {}", request);
        
        try {
            // Build Karate runner based on request
            Runner.Builder<?> builder = Runner.path("classpath:features");
            
            // Apply environment
            if (request.getEnvironment() != null) {
                System.setProperty("karate.env", request.getEnvironment());
                builder.karateEnv(request.getEnvironment());
            }
            
            // Apply tags
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                builder.tags(request.getTags());
            }
            
            // Apply specific feature
            if (request.getFeature() != null) {
                builder = Runner.path("classpath:" + request.getFeature());
            }
            
            // Apply thread count
            if (request.getThreads() > 0) {
                builder.parallel(request.getThreads());
            } else {
                builder.parallel(5); // Default 5 threads
            }
            
            // Execute tests
            log.info("Starting Karate test execution...");
            long startTime = System.currentTimeMillis();
            Results results = builder.build().run();
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("Karate test execution completed in {}ms", duration);
            log.info("Results: {} scenarios, {} passed, {} failed",
                    results.getScenarioCount(),
                    results.getScenariosPassed(),
                    results.getScenariosFailed());
            
            // Build response
            TestExecutionResponse response = new TestExecutionResponse();
            response.setSuccess(results.getFailCount() == 0);
            response.setTotalScenarios(results.getScenarioCount());
            response.setPassedScenarios(results.getScenariosPassed());
            response.setFailedScenarios(results.getScenariosFailed());
            response.setDurationMs(duration);
            response.setReportPath(results.getReportDir());
            response.setTimestamp(System.currentTimeMillis());
            
            if (results.getFailCount() > 0) {
                response.setMessage("Test execution completed with failures");
                response.setErrors(extractErrors(results));
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
            } else {
                response.setMessage("All tests passed successfully");
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            log.error("Failed to execute Karate tests", e);
            
            TestExecutionResponse errorResponse = new TestExecutionResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Test execution failed: " + e.getMessage());
            errorResponse.setTimestamp(System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * Gets list of available test features.
     */
    @GetMapping("/features")
    @Tag(name = "Test Management")
    @Operation(
        summary = "List Available Test Features",
        description = "Returns a list of all available Karate feature files that can be executed."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved list of features",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        [
                          "features/s3/S3Operations.feature",
                          "features/dynamodb/DynamoDbOperations.feature",
                          "features/lambda/LambdaOperations.feature",
                          "features/sqs/SqsOperations.feature"
                        ]
                        """
                )
            )
        )
    })
    public ResponseEntity<List<String>> listFeatures() {
        // In a real implementation, scan classpath for .feature files
        List<String> features = Arrays.asList(
                "features/s3/S3Operations.feature",
                "features/dynamodb/DynamoDbOperations.feature",
                "features/lambda/LambdaOperations.feature",
                "features/sqs/SqsOperations.feature"
        );
        
        return ResponseEntity.ok(features);
    }

    /**
     * Gets information about the service.
     */
    @GetMapping("/info")
    @Tag(name = "Health & Monitoring")
    @Operation(
        summary = "Get Service Information",
        description = "Returns general information about the test automation service including version and available endpoints."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved service information",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "service": "test-automation-service",
                          "version": "1.0.0",
                          "description": "Karate-based test automation service",
                          "endpoints": [
                            "/api/execute - Execute tests",
                            "/api/features - List available features",
                            "/api/health - Health check"
                          ]
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "test-automation-service");
        info.put("version", "1.0.0");
        info.put("description", "Karate-based test automation service");
        info.put("endpoints", Arrays.asList(
                "/api/execute - Execute tests",
                "/api/features - List available features",
                "/api/health - Health check"
        ));
        
        return ResponseEntity.ok(info);
    }

    /**
     * Extracts error messages from test results.
     */
    private List<String> extractErrors(Results results) {
        List<String> errors = new ArrayList<>();
        // In real implementation, extract detailed error messages
        errors.add("Failed scenarios: " + results.getScenariosFailed());
        return errors;
    }

    /**
     * Test Execution Request DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Test execution request configuration")
    public static class TestExecutionRequest {
        
        @Schema(
            description = "Target environment for test execution",
            example = "qa",
            allowableValues = {"dev", "qa", "staging", "prod"},
            defaultValue = "dev"
        )
        private String environment = "dev"; // dev, qa, staging, prod
        
        @Schema(
            description = "List of tags to filter tests (e.g., @smoke, @s3, @integration)",
            example = "[\"@smoke\", \"@s3\"]",
            nullable = true
        )
        private List<String> tags; // e.g., ["@smoke", "@s3"]
        
        @Schema(
            description = "Specific feature file to execute (if not provided, runs all features)",
            example = "features/s3/S3Operations.feature",
            nullable = true
        )
        private String feature; // Specific feature file to run
        
        @Schema(
            description = "Number of parallel threads for test execution",
            example = "5",
            minimum = "1",
            maximum = "20",
            defaultValue = "5"
        )
        private int threads = 5; // Number of parallel threads
    }

    /**
     * Test Execution Response DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Test execution result")
    public static class TestExecutionResponse {
        
        @Schema(description = "Whether all tests passed", example = "true")
        private boolean success;
        
        @Schema(description = "Human-readable result message", example = "All tests passed successfully")
        private String message;
        
        @Schema(description = "Total number of test scenarios executed", example = "15")
        private int totalScenarios;
        
        @Schema(description = "Number of scenarios that passed", example = "15")
        private int passedScenarios;
        
        @Schema(description = "Number of scenarios that failed", example = "0")
        private int failedScenarios;
        
        @Schema(description = "Total execution time in milliseconds", example = "12500")
        private long durationMs;
        
        @Schema(description = "Path to detailed test reports", example = "build/karate-reports")
        private String reportPath;
        
        @Schema(description = "Execution timestamp (epoch milliseconds)", example = "1707456789000")
        private long timestamp;
        
        @Schema(description = "List of error messages (only present if there are failures)", nullable = true)
        private List<String> errors;
    }
}
