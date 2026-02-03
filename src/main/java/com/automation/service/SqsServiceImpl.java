package com.automation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SQS Service Implementation
 */
@Slf4j
@Service
public class SqsServiceImpl {
    
    private final SqsClient sqsClient;
    
    public SqsServiceImpl(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }
    
    public String sendMessage(String queueUrl, String message) {
        log.info("Sending message to SQS queue: {}", queueUrl);
        
        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message)
                .build();
        
        SendMessageResponse response = sqsClient.sendMessage(request);
        return response.messageId();
    }
    
    public List<String> receiveMessages(String queueUrl, int maxMessages) {
        log.info("Receiving up to {} messages from SQS queue: {}", maxMessages, queueUrl);
        
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(maxMessages)
                .waitTimeSeconds(20)
                .build();
        
        ReceiveMessageResponse response = sqsClient.receiveMessage(request);
        
        return response.messages().stream()
                .map(Message::body)
                .collect(Collectors.toList());
    }
    
    public void deleteMessage(String queueUrl, String receiptHandle) {
        DeleteMessageRequest request = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build();
        
        sqsClient.deleteMessage(request);
    }
}
