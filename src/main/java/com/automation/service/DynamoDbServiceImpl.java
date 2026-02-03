package com.automation.service;

import com.automation.config.AwsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DynamoDB Service Implementation
 * 
 * Provides DynamoDB operations for Karate tests.
 * Handles CRUD operations, queries, and scans.
 */
@Slf4j
@Service
public class DynamoDbServiceImpl {

    private final DynamoDbClient dynamoDbClient;
    private final AwsProperties awsProperties;
    private final ObjectMapper objectMapper;
    private final String tablePrefix;

    @Autowired
    public DynamoDbServiceImpl(DynamoDbClient dynamoDbClient, 
                               AwsProperties awsProperties,
                               ObjectMapper objectMapper) {
        this.dynamoDbClient = dynamoDbClient;
        this.awsProperties = awsProperties;
        this.objectMapper = objectMapper;
        this.tablePrefix = awsProperties.getDynamodb().getTablePrefix();
        log.info("DynamoDbServiceImpl initialized with table prefix: {}", tablePrefix);
    }

    /**
     * Gets full table name with prefix.
     */
    private String getTableName(String tableName) {
        return tablePrefix + "-" + tableName;
    }

    /**
     * Gets item from DynamoDB table.
     */
    public Map<String, Object> getItem(String tableName, String keyName, String keyValue) {
        String fullTableName = getTableName(tableName);
        log.debug("Getting item from DynamoDB: table={}, key={}:{}", 
                fullTableName, keyName, keyValue);
        
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(keyName, AttributeValue.builder().s(keyValue).build());
            
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(fullTableName)
                    .key(key)
                    .build();
            
            GetItemResponse response = dynamoDbClient.getItem(request);
            
            if (!response.hasItem()) {
                log.warn("Item not found in DynamoDB: table={}, key={}:{}", 
                        fullTableName, keyName, keyValue);
                return null;
            }
            
            Map<String, Object> item = convertToMap(response.item());
            log.info("Retrieved item from DynamoDB: table={}, key={}:{}", 
                    fullTableName, keyName, keyValue);
            return item;
            
        } catch (DynamoDbException e) {
            log.error("Failed to get item from DynamoDB", e);
            throw new RuntimeException("DynamoDB get failed", e);
        }
    }

    /**
     * Puts item into DynamoDB table.
     */
    public void putItem(String tableName, Map<String, Object> item) {
        String fullTableName = getTableName(tableName);
        log.debug("Putting item into DynamoDB: table={}", fullTableName);
        
        try {
            Map<String, AttributeValue> dynamoItem = convertToAttributeMap(item);
            
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(fullTableName)
                    .item(dynamoItem)
                    .build();
            
            dynamoDbClient.putItem(request);
            log.info("Put item into DynamoDB: table={}", fullTableName);
            
        } catch (DynamoDbException e) {
            log.error("Failed to put item into DynamoDB", e);
            throw new RuntimeException("DynamoDB put failed", e);
        }
    }

    /**
     * Deletes item from DynamoDB table.
     */
    public void deleteItem(String tableName, String keyName, String keyValue) {
        String fullTableName = getTableName(tableName);
        log.debug("Deleting item from DynamoDB: table={}, key={}:{}", 
                fullTableName, keyName, keyValue);
        
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(keyName, AttributeValue.builder().s(keyValue).build());
            
            DeleteItemRequest request = DeleteItemRequest.builder()
                    .tableName(fullTableName)
                    .key(key)
                    .build();
            
            dynamoDbClient.deleteItem(request);
            log.info("Deleted item from DynamoDB: table={}, key={}:{}", 
                    fullTableName, keyName, keyValue);
            
        } catch (DynamoDbException e) {
            log.error("Failed to delete item from DynamoDB", e);
            throw new RuntimeException("DynamoDB delete failed", e);
        }
    }

    /**
     * Queries DynamoDB table.
     */
    public List<Map<String, Object>> query(String tableName, String keyName, String keyValue) {
        String fullTableName = getTableName(tableName);
        log.debug("Querying DynamoDB: table={}, key={}:{}", fullTableName, keyName, keyValue);
        
        try {
            QueryRequest request = QueryRequest.builder()
                    .tableName(fullTableName)
                    .keyConditionExpression("#k = :v")
                    .expressionAttributeNames(Collections.singletonMap("#k", keyName))
                    .expressionAttributeValues(Collections.singletonMap(":v", 
                            AttributeValue.builder().s(keyValue).build()))
                    .build();
            
            QueryResponse response = dynamoDbClient.query(request);
            
            List<Map<String, Object>> items = response.items().stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
            
            log.info("Query returned {} items from DynamoDB: table={}", 
                    items.size(), fullTableName);
            return items;
            
        } catch (DynamoDbException e) {
            log.error("Failed to query DynamoDB", e);
            throw new RuntimeException("DynamoDB query failed", e);
        }
    }

    /**
     * Scans DynamoDB table.
     */
    public List<Map<String, Object>> scan(String tableName) {
        String fullTableName = getTableName(tableName);
        log.debug("Scanning DynamoDB: table={}", fullTableName);
        
        try {
            ScanRequest request = ScanRequest.builder()
                    .tableName(fullTableName)
                    .build();
            
            ScanResponse response = dynamoDbClient.scan(request);
            
            List<Map<String, Object>> items = response.items().stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());
            
            log.info("Scan returned {} items from DynamoDB: table={}", 
                    items.size(), fullTableName);
            return items;
            
        } catch (DynamoDbException e) {
            log.error("Failed to scan DynamoDB", e);
            throw new RuntimeException("DynamoDB scan failed", e);
        }
    }

    /**
     * Converts DynamoDB AttributeValue map to regular map.
     */
    private Map<String, Object> convertToMap(Map<String, AttributeValue> item) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
            result.put(entry.getKey(), convertAttributeValue(entry.getValue()));
        }
        return result;
    }

    /**
     * Converts AttributeValue to Java object.
     */
    private Object convertAttributeValue(AttributeValue value) {
        if (value.s() != null) return value.s();
        if (value.n() != null) return Double.parseDouble(value.n());
        if (value.bool() != null) return value.bool();
        if (value.hasL()) {
            return value.l().stream()
                    .map(this::convertAttributeValue)
                    .collect(Collectors.toList());
        }
        if (value.hasM()) {
            return convertToMap(value.m());
        }
        return null;
    }

    /**
     * Converts regular map to DynamoDB AttributeValue map.
     */
    private Map<String, AttributeValue> convertToAttributeMap(Map<String, Object> item) {
        Map<String, AttributeValue> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : item.entrySet()) {
            result.put(entry.getKey(), convertToAttributeValue(entry.getValue()));
        }
        return result;
    }

    /**
     * Converts Java object to AttributeValue.
     */
    private AttributeValue convertToAttributeValue(Object value) {
        if (value == null) return AttributeValue.builder().nul(true).build();
        if (value instanceof String) return AttributeValue.builder().s((String) value).build();
        if (value instanceof Number) return AttributeValue.builder().n(value.toString()).build();
        if (value instanceof Boolean) return AttributeValue.builder().bool((Boolean) value).build();
        if (value instanceof List) {
            List<AttributeValue> list = ((List<?>) value).stream()
                    .map(this::convertToAttributeValue)
                    .collect(Collectors.toList());
            return AttributeValue.builder().l(list).build();
        }
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, AttributeValue> map = convertToAttributeMap((Map<String, Object>) value);
            return AttributeValue.builder().m(map).build();
        }
        return AttributeValue.builder().s(value.toString()).build();
    }
}
