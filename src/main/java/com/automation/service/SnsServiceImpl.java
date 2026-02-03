package com.automation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

/**
 * SNS Service Implementation
 */
@Slf4j
@Service
public class SnsServiceImpl {
    
    private final SnsClient snsClient;
    
    public SnsServiceImpl(SnsClient snsClient) {
        this.snsClient = snsClient;
    }
    
    public String publishMessage(String topicArn, String message) {
        log.info("Publishing message to SNS topic: {}", topicArn);
        
        PublishRequest request = PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .build();
        
        PublishResponse response = snsClient.publish(request);
        return response.messageId();
    }
}
