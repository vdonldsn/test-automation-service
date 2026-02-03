# Swagger UI Guide - Test Automation Service

## Overview

The Test Automation Service includes **Swagger UI** for interactive API documentation and testing. This makes it easy to explore, test, and integrate with the test automation APIs without writing code.

## Accessing Swagger UI

### Local Development
```
http://localhost:8080/test-automation/swagger-ui.html
```

### ECS/Production
```
https://your-alb-url.com/test-automation/swagger-ui.html
```

### Alternative - OpenAPI Spec
```
http://localhost:8080/test-automation/api-docs
```

## What is Swagger UI?

Swagger UI is an interactive API documentation tool that:
- **Displays** all available endpoints
- **Shows** request/response schemas
- **Provides** interactive testing ("Try It Out")
- **Generates** example requests
- **Documents** parameters and responses

## Main Features

### 1. Server Selection

At the top of Swagger UI, you can select which server to test against:
- **Local Development** - `http://localhost:8080/test-automation`
- **Development (ECS)** - `https://dev-alb.example.com/test-automation`
- **QA Environment** - `https://qa-alb.example.com/test-automation`
- **Staging** - `https://staging-alb.example.com/test-automation`
- **Production** - `https://alb.example.com/test-automation`

**How to use:**
1. Click "Servers" dropdown at top
2. Select your target environment
3. All API calls will go to that server

### 2. API Groups (Tags)

Endpoints are organized into logical groups:

#### Test Execution
- `POST /api/execute` - Execute Karate tests
- Main endpoint for triggering test runs

#### Test Management
- `GET /api/features` - List available test features
- Discover what tests are available

#### Health & Monitoring
- `GET /api/health` - Service health check
- `GET /api/info` - Service information

### 3. Interactive Testing ("Try It Out")

Every endpoint has a "Try It Out" button that lets you:
1. Modify request parameters
2. Edit request body
3. Execute the request
4. See the actual response

## Common Use Cases

### Use Case 1: Run Smoke Tests via Swagger

**Steps:**
1. Open Swagger UI
2. Navigate to **Test Execution** section
3. Click `POST /api/execute`
4. Click **"Try it out"**
5. Select example: **"Run smoke tests"**
6. Click **"Execute"**
7. View response below

**Request Body:**
```json
{
  "tags": ["@smoke"],
  "environment": "qa",
  "threads": 3
}
```

**Expected Response:**
```json
{
  "success": true,
  "message": "All tests passed successfully",
  "totalScenarios": 5,
  "passedScenarios": 5,
  "failedScenarios": 0,
  "durationMs": 8500,
  "reportPath": "build/karate-reports",
  "timestamp": 1707456789000
}
```

### Use Case 2: Run S3 Tests

**Steps:**
1. Click `POST /api/execute`
2. Click **"Try it out"**
3. Select example: **"Run S3 tests"**
4. Modify if needed
5. Click **"Execute"**

**Request Body:**
```json
{
  "tags": ["@s3"],
  "environment": "dev",
  "threads": 1
}
```

### Use Case 3: Run Specific Feature File

**Steps:**
1. First, list available features: `GET /api/features`
2. Copy a feature path (e.g., `features/s3/S3Operations.feature`)
3. Go to `POST /api/execute`
4. Click **"Try it out"**
5. Use request body:

```json
{
  "feature": "features/s3/S3Operations.feature",
  "environment": "staging"
}
```

### Use Case 4: Parallel Test Execution

**Request Body:**
```json
{
  "environment": "qa",
  "threads": 10
}
```

This runs all tests with 10 parallel threads for faster execution.

### Use Case 5: Multiple Tag Filtering

**Request Body:**
```json
{
  "tags": ["@smoke", "@integration"],
  "environment": "qa",
  "threads": 5
}
```

Runs tests that have EITHER `@smoke` OR `@integration` tags.

## Request Parameters Explained

### TestExecutionRequest

| Field | Type | Description | Default | Example |
|-------|------|-------------|---------|---------|
| `environment` | String | Target environment | `dev` | `qa`, `staging`, `prod` |
| `tags` | Array | Test tags to filter | `null` | `["@smoke", "@s3"]` |
| `feature` | String | Specific feature file | `null` | `features/s3/S3Operations.feature` |
| `threads` | Integer | Parallel threads | `5` | `1` to `20` |

**Notes:**
- `tags` and `feature` are mutually exclusive (use one or the other)
- If neither is provided, all tests run
- Higher `threads` = faster execution but more resources

## Response Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | Success | All tests passed |
| 206 | Partial | Some tests failed |
| 500 | Error | Test execution failed to start |

## Response Fields Explained

### TestExecutionResponse

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `success` | Boolean | Whether all tests passed | `true` |
| `message` | String | Human-readable result | `"All tests passed successfully"` |
| `totalScenarios` | Integer | Total scenarios executed | `15` |
| `passedScenarios` | Integer | Scenarios that passed | `15` |
| `failedScenarios` | Integer | Scenarios that failed | `0` |
| `durationMs` | Long | Execution time in milliseconds | `12500` |
| `reportPath` | String | Path to detailed reports | `build/karate-reports` |
| `timestamp` | Long | Execution timestamp (epoch) | `1707456789000` |
| `errors` | Array | Error messages (if any) | `["Failed scenarios: 3"]` |

## Available Test Tags

Use these tags to filter test execution:

| Tag | Description | Example Usage |
|-----|-------------|---------------|
| `@smoke` | Quick validation tests | `{"tags": ["@smoke"]}` |
| `@s3` | S3 operations tests | `{"tags": ["@s3"]}` |
| `@dynamodb` | DynamoDB tests | `{"tags": ["@dynamodb"]}` |
| `@lambda` | Lambda function tests | `{"tags": ["@lambda"]}` |
| `@sqs` | SQS messaging tests | `{"tags": ["@sqs"]}` |
| `@sns` | SNS notification tests | `{"tags": ["@sns"]}` |
| `@integration` | Full integration tests | `{"tags": ["@integration"]}` |
| `@regression` | Regression test suite | `{"tags": ["@regression"]}` |

## Pro Tips

### 1. Use Examples
Swagger UI provides pre-configured examples for common scenarios. Click the dropdown next to "Example Value" to select one.

### 2. Copy as cURL
After executing a request:
1. Look for the "cURL" section in the response
2. Copy the command
3. Use it in scripts or CI/CD pipelines

### 3. Download OpenAPI Spec
1. Visit `http://localhost:8080/test-automation/api-docs`
2. Save the JSON
3. Use with code generators (OpenAPI Generator, Swagger Codegen)
4. Generate client libraries in any language

### 4. Schema Validation
Swagger UI validates your requests before sending:
- Required fields are marked with asterisk (*)
- Invalid JSON is highlighted
- Type mismatches are caught

### 5. Response Inspection
Click on response to expand and see:
- **Response body** - JSON result
- **Response headers** - HTTP headers
- **Response code** - HTTP status
- **Duration** - Request time

## Integration Examples

### From cURL
```bash
curl -X POST 'http://localhost:8080/test-automation/api/execute' \
  -H 'Content-Type: application/json' \
  -d '{
    "tags": ["@smoke"],
    "environment": "qa",
    "threads": 3
  }'
```

### From Python
```python
import requests

response = requests.post(
    'http://localhost:8080/test-automation/api/execute',
    json={
        'tags': ['@smoke'],
        'environment': 'qa',
        'threads': 3
    }
)

result = response.json()
print(f"Tests passed: {result['passedScenarios']}/{result['totalScenarios']}")
```

### From JavaScript
```javascript
fetch('http://localhost:8080/test-automation/api/execute', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    tags: ['@smoke'],
    environment: 'qa',
    threads: 3
  })
})
.then(response => response.json())
.then(data => {
  console.log('Test Results:', data);
  console.log(`Passed: ${data.passedScenarios}/${data.totalScenarios}`);
});
```

### From Postman
1. Create new request
2. Method: POST
3. URL: `http://localhost:8080/test-automation/api/execute`
4. Headers: `Content-Type: application/json`
5. Body (raw JSON):
```json
{
  "tags": ["@smoke"],
  "environment": "qa"
}
```
6. Click Send

## Troubleshooting

### Swagger UI Not Loading
- Check service is running: `curl http://localhost:8080/test-automation/actuator/health`
- Verify URL includes context path: `/test-automation/swagger-ui.html`
- Clear browser cache

### 404 Not Found
- Ensure using correct context path: `/test-automation`
- Check server logs for startup errors
- Verify Springdoc dependency is included

### Request Fails with 500
- Check application logs
- Verify AWS credentials are configured
- Ensure database is accessible
- Check feature files exist

### Cannot Select Server
- Servers are defined in `SwaggerConfiguration.java`
- Update with your actual ALB URLs
- Rebuild and redeploy

## Security Considerations

### Current Setup
- **No authentication** - Swagger UI is publicly accessible
- Suitable for development environments
- **NOT recommended for production**

### Adding Security (Recommended for Production)

#### Option 1: Basic Auth
Add to `application.yml`:
```yaml
spring:
  security:
    user:
      name: admin
      password: ${SWAGGER_PASSWORD}
```

#### Option 2: OAuth2
Integrate with your OAuth provider

#### Option 3: API Key
Implement custom API key validation

#### Option 4: Network Restriction
Restrict Swagger UI to internal networks only via security groups

## Additional Resources

- **OpenAPI Specification**: https://spec.openapis.org/oas/v3.0.0
- **Swagger UI Docs**: https://swagger.io/tools/swagger-ui/
- **Springdoc OpenAPI**: https://springdoc.org/
- **Service README**: [../README.md](../README.md)
- **Quick Start**: [../QUICKSTART.md](../QUICKSTART.md)

## Support

For Swagger UI issues:
- Check application logs: `logs/test-automation-service.log`
- Verify Springdoc version in `build.gradle`
- Contact: test-automation-team@example.com
