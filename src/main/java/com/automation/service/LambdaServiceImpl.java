package com.automation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;

/**
 * Lambda Service Implementation
 */
@Slf4j
@Service
public class LambdaServiceImpl {
    
    private final LambdaClient lambdaClient;
    
    public LambdaServiceImpl(LambdaClient lambdaClient) {
        this.lambdaClient = lambdaClient;
    }
    
    public String invokeFunction(String functionName, String payload) {
        log.info("Invoking Lambda function: {}", functionName);
        
        InvokeRequest request = InvokeRequest.builder()
                .functionName(functionName)
                .payload(SdkBytes.fromUtf8String(payload))
                .build();
        
        InvokeResponse response = lambdaClient.invoke(request);
        return response.payload().asUtf8String();
    }
}
