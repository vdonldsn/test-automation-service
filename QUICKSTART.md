# Quick Start Guide

## 1. Local Development Setup (5 minutes)

### Prerequisites
- Java 17+
- Docker Desktop
- AWS CLI configured

### Quick Setup Script

```bash
# Clone repository
git clone <repo-url>
cd test-automation-service

# Set environment variables
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=your-key
export AWS_SECRET_ACCESS_KEY=your-secret
export S3_BUCKET_NAME=test-automation-dev-bucket
export DB_URL=jdbc:postgresql://localhost:5432/testautomation
export DB_USERNAME=testuser
export DB_PASSWORD=testpass

# Start PostgreSQL with Docker
docker run -d \
  --name postgres \
  -e POSTGRES_DB=testautomation \
  -e POSTGRES_USER=testuser \
  -e POSTGRES_PASSWORD=testpass \
  -p 5432:5432 \
  postgres:15

# Build and run
./gradlew bootRun
```

Application starts at: `http://localhost:8080/test-automation`

**Access Swagger UI:** `http://localhost:8080/test-automation/swagger-ui.html`

## 2. Run Your First Test

### Option A: Via Swagger UI (Recommended for Beginners)

1. Open browser: `http://localhost:8080/test-automation/swagger-ui.html`
2. Navigate to **Test Execution** → `POST /api/execute`
3. Click **"Try it out"**
4. Select example: **"Run smoke tests"**
5. Click **"Execute"**
6. View results in response section

### Option B: Via REST API (curl)

```bash
# Trigger S3 tests
curl -X POST http://localhost:8080/test-automation/api/execute \
  -H "Content-Type: application/json" \
  -d '{
    "tags": ["@s3"],
    "environment": "dev"
  }'
```

### Option C: Via Gradle

```bash
# Run all tests
./gradlew test

# Run S3 tests only
./gradlew test --tests KarateTestRunner.testS3

# Run with specific environment
./gradlew test -Dkarate.env=qa
```

### Option D: Via IDE

1. Open project in IntelliJ IDEA
2. Navigate to `KarateTestRunner.java`
3. Right-click on `testS3()` method
4. Select "Run 'testS3()'"

## 3. Create Your First Test

Create `src/test/java/features/example/MyFirstTest.feature`:

```gherkin
Feature: My First Test

  Background:
    * def config = commonUtility
    * def s3 = config.S3Impl()
    * def logger = config.LoggerImpl()

  Scenario: Upload and verify file
    # Log test start
    * call logger.log('Starting my first test', 'INFO')
    
    # Upload file to S3
    Given def bucket = karate.config.aws.s3Bucket
    And def key = 'my-test-file.txt'
    And def content = 'Hello Karate!'
    
    When call s3.writeObject(bucket, key, content)
    Then def result = s3.readObject(bucket, key)
    And match result == content
    
    # Cleanup
    * call s3.deleteObject(bucket, key)
    * call logger.log('Test completed successfully', 'INFO')
```

Run it:
```bash
./gradlew test --tests "KarateTestRunner.testAll"
```

## 4. Use CommonUtility Services

### S3 Operations

```gherkin
# Using typed service
* def s3 = commonUtility.S3Impl()
* call s3.writeObject('bucket', 'key', 'content')
* def result = s3.readObject('bucket', 'key')

# Using convenience method
* call commonUtility.s3Write('bucket', 'key', 'content')
* def result = commonUtility.s3Read('bucket', 'key')
```

### DynamoDB Operations

```gherkin
# Using typed service
* def dynamo = commonUtility.DynamoDbImpl()
* def item = { userId: '123', name: 'John' }
* call dynamo.putItem('users', item)
* def result = dynamo.getItem('users', 'userId', '123')

# Using convenience method
* call commonUtility.dynamoPut('users', item)
* def result = commonUtility.dynamoGet('users', 'userId', '123')
```

### Lambda Invocation

```gherkin
* def lambda = commonUtility.LambdaImpl()
* def payload = { action: 'test', data: 'value' }
* def result = lambda.invokeFunction('my-function', payload)
```

### Database Operations

```gherkin
* def db = commonUtility.DatabaseImpl()
* def sql = 'SELECT * FROM users WHERE id = ?'
* def results = db.executeQuery(sql, 123)
```

## 5. Deploy to ECS (Production)

### Build and Push to ECR

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com

# Build and tag
docker build -t test-automation-service:1.0.0 .
docker tag test-automation-service:1.0.0 \
  ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/test-automation-service:1.0.0

# Push
docker push ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/test-automation-service:1.0.0
```

### Deploy to ECS

```bash
# Update task definition with new image
aws ecs register-task-definition \
  --cli-input-json file://ecs-task-definition.json

# Update service
aws ecs update-service \
  --cluster test-automation-cluster \
  --service test-automation-service \
  --task-definition test-automation-task \
  --force-new-deployment
```

### Verify Deployment

```bash
# Check service status
aws ecs describe-services \
  --cluster test-automation-cluster \
  --services test-automation-service

# Test health endpoint
curl https://your-alb-url.com/test-automation/actuator/health

# Trigger test via ALB
curl -X POST https://your-alb-url.com/test-automation/api/execute \
  -H "Content-Type: application/json" \
  -d '{"tags": ["@smoke"]}'
```

## 6. Common Tasks

### Add New AWS Service

1. **Add SDK dependency** in `build.gradle`:
```gradle
implementation 'software.amazon.awssdk:kinesis'
```

2. **Create service implementation**:
```java
@Service
public class KinesisServiceImpl {
    private final KinesisClient kinesisClient;
    
    public void putRecord(String streamName, String data) {
        // Implementation
    }
}
```

3. **Add to CommonUtility**:
```java
@Autowired
private KinesisServiceImpl kinesisService;

public KinesisServiceImpl KinesisImpl() {
    return kinesisService;
}
```

4. **Use in Karate**:
```gherkin
* def kinesis = commonUtility.KinesisImpl()
* call kinesis.putRecord('my-stream', 'test-data')
```

### Add New Test Feature

1. Create `src/test/java/features/<service>/NewTest.feature`
2. Add test method to `KarateTestRunner.java`:
```java
@Karate.Test
Karate testNewFeature() {
    return Karate.run("newfeature")
        .relativeTo(getClass())
        .systemProperty("applicationContext", applicationContext);
}
```
3. Run: `./gradlew test --tests KarateTestRunner.testNewFeature`

### Environment-Specific Configuration

Set Spring profile:
```bash
# Dev
./gradlew bootRun --args='--spring.profiles.active=dev'

# QA
./gradlew bootRun --args='--spring.profiles.active=qa'

# Prod (in ECS via environment variable)
SPRING_PROFILES_ACTIVE=prod
```

### View Test Reports

After running tests:
```bash
# Open HTML report
open build/karate-reports/karate-summary.html

# View JSON report
cat build/karate-reports/karate-summary.json
```

## 7. Troubleshooting

### Tests Can't Find CommonUtility
```bash
# Check Spring context is loaded
# Look for this log line:
"CommonUtility loaded successfully"

# If missing, ensure test extends KarateTestRunner
```

### AWS SDK Errors
```bash
# Check credentials
aws sts get-caller-identity

# Check IAM permissions
aws iam get-user

# Enable debug logging
export AWS_SDK_LOGGING=DEBUG
```

### Database Connection Failed
```bash
# Test PostgreSQL connection
psql -h localhost -U testuser -d testautomation

# Check if Docker container is running
docker ps | grep postgres

# View logs
docker logs postgres
```

### Port Already in Use
```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port
export SERVER_PORT=8081
```

## 8. Next Steps

- [ ] Read [README.md](README.md) for full documentation
- [ ] Review sample features in `src/test/java/features/`
- [ ] Explore `CommonUtility.java` for available services
- [ ] Check `application.yml` for configuration options
- [ ] Set up CI/CD pipeline
- [ ] Configure monitoring and alerting

## Resources

- Karate Documentation: https://github.com/karatelabs/karate
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- AWS SDK Documentation: https://docs.aws.amazon.com/sdk-for-java/
- Project README: [README.md](README.md)
