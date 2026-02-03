# Test Automation Service - Karate-Based Microservice

A Spring Boot microservice that runs Karate tests and exposes them via REST API. Designed for deployment to AWS ECS behind a shared Application Load Balancer.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    Application Load Balancer (ALB)              │
│                    Listener: Port 80/443                         │
└────────────┬────────────────────────────────────────────────────┘
             │
             ├─→ /user-service/*      → User Service (ECS)
             ├─→ /order-service/*     → Order Service (ECS)
             ├─→ /payment-service/*   → Payment Service (ECS)
             └─→ /test-automation/*   → Test Automation Service (ECS) ← THIS SERVICE
                     │
                     ├─→ /actuator/health  (Health Check)
                     ├─→ /api/execute      (Trigger Tests)
                     └─→ /api/features     (List Tests)

┌─────────────────────────────────────────────────────────────────┐
│              Test Automation Service Container                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Spring Boot Application (TestAutoApplication.java)      │  │
│  │  • REST API (TestExecutionController)                    │  │
│  │  • CommonUtility (Service Orchestration)                 │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                          │
│  ┌────────────────────┴─────────────────────────────────────┐  │
│  │              Karate Test Engine                          │  │
│  │  • karate-config.js (Bootstrap)                          │  │
│  │  • Feature Files (.feature)                              │  │
│  │  • Java Services (via CommonUtility)                     │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                          │
│  ┌────────────────────┴─────────────────────────────────────┐  │
│  │           Service Implementations                        │  │
│  │  • S3ServiceImpl                                         │  │
│  │  • DynamoDbServiceImpl                                   │  │
│  │  • LambdaServiceImpl                                     │  │
│  │  • SnsServiceImpl / SqsServiceImpl                       │  │
│  │  • DatabaseServiceImpl (PostgreSQL)                      │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
             │         │         │         │
             ↓         ↓         ↓         ↓
          AWS S3   DynamoDB   Lambda   PostgreSQL
```

## Key Components

### 1. **TestAutoApplication.java**
- Main Spring Boot application entry point
- Starts embedded Tomcat server on port 8080
- Initializes all service beans
- Logs startup information for debugging

### 2. **CommonUtility.java**
- Central access point for all service implementations
- Injected into Karate via karate-config.js
- Provides both typed services and convenience methods
- Used in Karate features: `* def config = commonUtility`

### 3. **karate-config.js**
- Bootstrap configuration for Karate tests
- Loads Spring context and injects CommonUtility
- Provides environment-specific configuration
- Creates global helper functions

### 4. **Service Implementations**
- **S3ServiceImpl**: S3 operations (read, write, delete, list)
- **DynamoDbServiceImpl**: DynamoDB CRUD operations
- **LambdaServiceImpl**: Lambda function invocation
- **SnsServiceImpl/SqsServiceImpl**: Messaging operations
- **DatabaseServiceImpl**: PostgreSQL operations

### 5. **TestExecutionController**
- REST API to trigger Karate tests
- POST /api/execute - Run tests programmatically
- GET /api/features - List available tests
- GET /api/health - Health check for ALB

## Prerequisites

- Java 17+
- Gradle 8.5+
- Docker (for containerization)
- AWS Account (for ECS deployment)
- PostgreSQL database
- AWS Services: S3, DynamoDB, Lambda, SNS, SQS

## Local Development Setup

### 1. Clone and Build

```bash
git clone <repository-url>
cd test-automation-service
./gradlew clean build
```

### 2. Configure Application

Edit `src/main/resources/application.yml` or set environment variables:

```bash
# AWS Configuration
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
export S3_BUCKET_NAME=test-automation-bucket

# Database Configuration
export DB_URL=jdbc:postgresql://localhost:5432/testautomation
export DB_USERNAME=testuser
export DB_PASSWORD=testpass

# Service Configuration
export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=dev
```

### 3. Run Application

```bash
# Run Spring Boot application
./gradlew bootRun

# Application starts on http://localhost:8080/test-automation
```

### 4. Run Tests

```bash
# Run all Karate tests
./gradlew test

# Run with specific environment
./gradlew test -Dkarate.env=qa

# Run specific test suite
./gradlew test --tests KarateTestRunner.testS3
```

### 5. Access Swagger UI

Once the application is running, access interactive API documentation:

```
http://localhost:8080/test-automation/swagger-ui.html
```

**Swagger UI Features:**
- Interactive API testing (Try It Out button)
- Auto-generated documentation
- Request/response examples
- Multiple environment support (switch servers)
- OpenAPI 3.0 specification

**Quick Test via Swagger:**
1. Open Swagger UI in browser
2. Navigate to "Test Execution" → POST /api/execute
3. Click "Try it out"
4. Modify request body (or use example)
5. Click "Execute"
6. View response with test results

## Usage Examples

### From Karate Feature Files

```gherkin
Feature: S3 Operations

  Background:
    * def config = commonUtility
    * def s3 = config.S3Impl()
    
  Scenario: Upload and download file
    * def content = 'Test content'
    * call s3.writeObject('my-bucket', 'test.txt', content)
    * def result = s3.readObject('my-bucket', 'test.txt')
    * match result == content
```

### Via REST API

```bash
# Trigger test execution
curl -X POST http://localhost:8080/test-automation/api/execute \
  -H "Content-Type: application/json" \
  -d '{
    "environment": "qa",
    "tags": ["@smoke", "@s3"],
    "threads": 3
  }'

# Response:
{
  "success": true,
  "message": "All tests passed successfully",
  "totalScenarios": 10,
  "passedScenarios": 10,
  "failedScenarios": 0,
  "durationMs": 15000,
  "reportPath": "build/karate-reports"
}
```

## ECS Deployment

### 1. Build Docker Image

```bash
# Build image
docker build -t test-automation-service:latest .

# Tag for ECR
docker tag test-automation-service:latest \
  ACCOUNT_ID.dkr.ecr.REGION.amazonaws.com/test-automation-service:latest

# Push to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  ACCOUNT_ID.dkr.ecr.REGION.amazonaws.com

docker push ACCOUNT_ID.dkr.ecr.REGION.amazonaws.com/test-automation-service:latest
```

### 2. Create ECS Task Definition

```bash
# Update ecs-task-definition.json with your ACCOUNT_ID and REGION

# Register task definition
aws ecs register-task-definition \
  --cli-input-json file://ecs-task-definition.json
```

### 3. Create ECS Service

```bash
# Create service in ECS cluster
aws ecs create-service \
  --cluster test-automation-cluster \
  --service-name test-automation-service \
  --task-definition test-automation-task \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={
    subnets=[subnet-xxx,subnet-yyy],
    securityGroups=[sg-xxx],
    assignPublicIp=ENABLED
  }" \
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:...,
    containerName=test-automation-service,
    containerPort=8080"
```

### 4. Configure ALB Listener Rule

```bash
# Create listener rule for path-based routing
aws elbv2 create-rule \
  --listener-arn arn:aws:elasticloadbalancing:REGION:ACCOUNT_ID:listener/... \
  --priority 100 \
  --conditions Field=path-pattern,Values='/test-automation/*' \
  --actions Type=forward,TargetGroupArn=arn:aws:elasticloadbalancing:...
```

## ALB Configuration

The service registers with a shared ALB using these settings:

**application.yml:**
```yaml
server:
  listener:
    path-pattern: /test-automation/*  # ALB routing path
    health-check-path: /test-automation/actuator/health
    priority: 100  # Listener rule priority
```

**ALB Target Group:**
- Protocol: HTTP
- Port: 8080
- Health check path: /test-automation/actuator/health
- Health check interval: 30s
- Healthy threshold: 2
- Unhealthy threshold: 3

## Environment Configuration

### Dev Environment
```yaml
spring:
  profiles:
    active: dev
    
aws:
  s3:
    bucket-name: test-automation-dev-bucket
  dynamodb:
    table-prefix: dev-testautomation

karate:
  env: dev
  threads: 1
```

### QA Environment
```yaml
spring:
  profiles:
    active: qa
    
aws:
  s3:
    bucket-name: test-automation-qa-bucket
  dynamodb:
    table-prefix: qa-testautomation

karate:
  env: qa
  threads: 3
```

### Production Environment
```yaml
spring:
  profiles:
    active: prod
    
aws:
  s3:
    bucket-name: test-automation-prod-bucket
  dynamodb:
    table-prefix: prod-testautomation

karate:
  env: prod
  threads: 10
```

## Monitoring and Logging

### Swagger/OpenAPI Documentation
Interactive API documentation available at:
- **Swagger UI**: `http://localhost:8080/test-automation/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/test-automation/api-docs`

In ECS/Production:
- **Swagger UI**: `https://alb.example.com/test-automation/swagger-ui.html`

**Features:**
- Try out APIs directly from browser
- See request/response examples
- Switch between environments (dev/qa/staging/prod)
- Export OpenAPI spec for client generation

### CloudWatch Logs
Logs are sent to CloudWatch Logs group: `/ecs/test-automation-service`

View logs:
```bash
aws logs tail /ecs/test-automation-service --follow
```

### Metrics
Service exposes Prometheus metrics at: `/test-automation/actuator/prometheus`

Key metrics:
- `http_server_requests_seconds_count` - Request count
- `http_server_requests_seconds_sum` - Total request time
- `jvm_memory_used_bytes` - Memory usage

### Health Checks
- Liveness: `/test-automation/actuator/health/liveness`
- Readiness: `/test-automation/actuator/health/readiness`
- Overall: `/test-automation/actuator/health`

## CI/CD Integration

### Jenkins Pipeline
```groovy
pipeline {
    agent any
    
    environment {
        AWS_REGION = 'us-east-1'
        ECR_REPO = 'test-automation-service'
        ECS_CLUSTER = 'test-automation-cluster'
        ECS_SERVICE = 'test-automation-service'
    }
    
    stages {
        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
        }
        
        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
        
        stage('Docker Build') {
            steps {
                sh 'docker build -t ${ECR_REPO}:${BUILD_NUMBER} .'
            }
        }
        
        stage('Push to ECR') {
            steps {
                sh '''
                    aws ecr get-login-password | docker login --username AWS --password-stdin ${ECR_URI}
                    docker push ${ECR_REPO}:${BUILD_NUMBER}
                '''
            }
        }
        
        stage('Deploy to ECS') {
            steps {
                sh '''
                    aws ecs update-service \
                      --cluster ${ECS_CLUSTER} \
                      --service ${ECS_SERVICE} \
                      --force-new-deployment
                '''
            }
        }
    }
}
```

## Troubleshooting

### Issue: Tests fail to find CommonUtility
**Cause:** Spring context not properly initialized
**Solution:** Ensure tests extend KarateTestRunner which starts Spring Boot

### Issue: AWS credentials not found
**Cause:** IAM role not attached to ECS task
**Solution:** Verify taskRoleArn in ECS task definition

### Issue: Database connection failed
**Cause:** Security group blocking PostgreSQL port
**Solution:** Add ECS security group to RDS inbound rules on port 5432

### Issue: ALB health checks failing
**Cause:** Wrong health check path or service not responding
**Solution:** Verify /test-automation/actuator/health returns 200

## Project Structure

```
test-automation-service/
├── build.gradle                        # Gradle build configuration
├── settings.gradle
├── Dockerfile                          # Container image definition
├── ecs-task-definition.json           # ECS task configuration
├── src/
│   ├── main/
│   │   ├── java/com/automation/
│   │   │   ├── TestAutoApplication.java       # Main application class
│   │   │   ├── common/
│   │   │   │   └── CommonUtility.java         # Service orchestrator
│   │   │   ├── config/
│   │   │   │   ├── AwsProperties.java         # AWS configuration
│   │   │   │   └── AwsClientConfiguration.java # AWS client beans
│   │   │   ├── controller/
│   │   │   │   └── TestExecutionController.java # REST API
│   │   │   └── service/
│   │   │       ├── S3ServiceImpl.java
│   │   │       ├── DynamoDbServiceImpl.java
│   │   │       ├── LambdaServiceImpl.java
│   │   │       ├── SnsServiceImpl.java
│   │   │       ├── SqsServiceImpl.java
│   │   │       └── UtilityServices.java
│   │   └── resources/
│   │       └── application.yml                 # Application configuration
│   └── test/
│       ├── java/
│       │   ├── karate-config.js                # Karate bootstrap
│       │   ├── com/automation/karate/
│       │   │   └── KarateTestRunner.java       # Test runner
│       │   └── features/
│       │       ├── s3/
│       │       │   └── S3Operations.feature    # S3 tests
│       │       └── dynamodb/
│       │           └── DynamoDbOperations.feature # DynamoDB tests
│       └── resources/
│           └── testdata/                       # Test data files
```

## License

[Your License]

## Support

For issues or questions:
- Create issue in repository
- Contact: test-automation-team@example.com
