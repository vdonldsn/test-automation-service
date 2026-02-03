package com.automation.karate;

import com.automation.common.CommonUtility;
import com.intuit.karate.junit5.Karate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Karate Test Runner
 * 
 * This class integrates Karate with Spring Boot for test execution.
 * 
 * Key Integration Points:
 * 1. @SpringBootTest - Starts full Spring context
 * 2. ApplicationContext injection - Provides Spring beans to Karate
 * 3. System property passing - Makes context available to karate-config.js
 * 
 * Why this approach?
 * ------------------
 * Karate tests need access to Spring-managed services (S3ServiceImpl, etc.).
 * By running as a Spring Boot test:
 * - All @Service beans are initialized
 * - Configuration from application.yml is loaded
 * - Database connections are established
 * - AWS clients are configured
 * 
 * Then we pass the ApplicationContext to Karate via system property,
 * and karate-config.js retrieves CommonUtility bean from it.
 * 
 * Usage:
 * ------
 * Run from IDE: Right-click -> Run 'KarateTestRunner'
 * Run from Gradle: ./gradlew test
 * Run specific feature: ./gradlew test -Dkarate.options="--tags @s3"
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class KarateTestRunner {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CommonUtility commonUtility;

    /**
     * Runs all Karate tests.
     * 
     * This method:
     * 1. Injects ApplicationContext into Karate via system property
     * 2. Configures Karate to run all .feature files
     * 3. Returns Karate runner for JUnit execution
     */
    @Karate.Test
    Karate testAll() {
        // Make Spring ApplicationContext available to karate-config.js
        System.setProperty("karate.applicationContext", "injected");
        
        // Create Karate runner
        return Karate.run()
                .relativeTo(getClass())
                .karateEnv(System.getProperty("karate.env", "dev"))
                .configDir("classpath:") // Look for karate-config.js in classpath
                .systemProperty("applicationContext", applicationContext)
                .systemProperty("commonUtility", commonUtility);
    }

    /**
     * Runs S3-specific tests.
     * Usage: ./gradlew test --tests KarateTestRunner.testS3
     */
    @Karate.Test
    Karate testS3() {
        System.setProperty("applicationContext", String.valueOf(applicationContext.hashCode()));
        
        return Karate.run("s3")
                .tags("@s3")
                .relativeTo(getClass())
                .karateEnv(System.getProperty("karate.env", "dev"))
                .systemProperty("applicationContext", applicationContext)
                .systemProperty("commonUtility", commonUtility);
    }

    /**
     * Runs DynamoDB-specific tests.
     */
    @Karate.Test
    Karate testDynamoDB() {
        System.setProperty("applicationContext", String.valueOf(applicationContext.hashCode()));
        
        return Karate.run("dynamodb")
                .tags("@dynamodb")
                .relativeTo(getClass())
                .karateEnv(System.getProperty("karate.env", "dev"))
                .systemProperty("applicationContext", applicationContext)
                .systemProperty("commonUtility", commonUtility);
    }

    /**
     * Runs smoke tests only.
     * Useful for quick validation.
     */
    @Karate.Test
    Karate testSmoke() {
        System.setProperty("applicationContext", String.valueOf(applicationContext.hashCode()));
        
        return Karate.run()
                .tags("@smoke")
                .relativeTo(getClass())
                .karateEnv(System.getProperty("karate.env", "dev"))
                .systemProperty("applicationContext", applicationContext)
                .systemProperty("commonUtility", commonUtility);
    }
}
