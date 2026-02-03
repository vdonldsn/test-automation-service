package com.automation.service;

import com.automation.config.AwsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * S3 Service Implementation
 * 
 * Provides comprehensive S3 operations for Karate tests using AWS SDK v2.
 * This implementation uses the official AWS SDK, which handles:
 * - Request signing (AWS Signature V4)
 * - Retry logic
 * - Error handling
 * - Connection pooling
 * 
 * Why use SDK instead of REST API directly?
 * ------------------------------------------
 * While the previous example showed REST API testing, in practice:
 * 1. SDK is more maintainable (AWS handles protocol changes)
 * 2. Better error messages and type safety
 * 3. Built-in retry and timeout handling
 * 4. Less code to maintain
 * 5. Focus on testing business logic, not AWS internals
 * 
 * Usage in Karate:
 * ----------------
 * * def s3 = commonUtility.S3Impl()
 * * def content = s3.readObject('my-bucket', 'test.txt')
 * * call s3.writeObject('my-bucket', 'output.txt', 'test content')
 * * def exists = s3.objectExists('my-bucket', 'test.txt')
 * * call s3.deleteObject('my-bucket', 'test.txt')
 * * def objects = s3.listObjects('my-bucket', 'prefix/')
 */
@Slf4j
@Service
public class S3ServiceImpl {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;
    private final String defaultBucket;

    @Autowired
    public S3ServiceImpl(S3Client s3Client, AwsProperties awsProperties) {
        this.s3Client = s3Client;
        this.awsProperties = awsProperties;
        this.defaultBucket = awsProperties.getS3().getBucketName();
        log.info("S3ServiceImpl initialized with default bucket: {}", defaultBucket);
    }

    /**
     * Reads object content from S3 as String.
     * 
     * @param bucketName S3 bucket name
     * @param key Object key
     * @return Object content as String
     */
    public String readObject(String bucketName, String key) {
        log.debug("Reading S3 object: s3://{}/{}", bucketName, key);
        
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            String content = new String(response.readAllBytes(), StandardCharsets.UTF_8);
            
            log.info("Successfully read S3 object: s3://{}/{} ({} bytes)", 
                    bucketName, key, content.length());
            return content;
            
        } catch (S3Exception e) {
            log.error("Failed to read S3 object: s3://{}/{} - {}", bucketName, key, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("S3 read failed: " + e.awsErrorDetails().errorMessage(), e);
        } catch (IOException e) {
            log.error("Failed to read S3 object stream: s3://{}/{}", bucketName, key, e);
            throw new RuntimeException("Failed to read S3 object stream", e);
        }
    }

    /**
     * Reads object content from default bucket.
     */
    public String readObject(String key) {
        return readObject(defaultBucket, key);
    }

    /**
     * Reads object as bytes (for binary files).
     */
    public byte[] readObjectBytes(String bucketName, String key) {
        log.debug("Reading S3 object as bytes: s3://{}/{}", bucketName, key);
        
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            byte[] content = response.readAllBytes();
            
            log.info("Successfully read S3 object as bytes: s3://{}/{} ({} bytes)", 
                    bucketName, key, content.length);
            return content;
            
        } catch (S3Exception | IOException e) {
            log.error("Failed to read S3 object as bytes: s3://{}/{}", bucketName, key, e);
            throw new RuntimeException("S3 read bytes failed", e);
        }
    }

    /**
     * Writes string content to S3.
     */
    public void writeObject(String bucketName, String key, String content) {
        writeObject(bucketName, key, content, null);
    }

    /**
     * Writes string content to S3 with metadata.
     */
    public void writeObject(String bucketName, String key, String content, Map<String, String> metadata) {
        log.debug("Writing S3 object: s3://{}/{} ({} bytes)", bucketName, key, content.length());
        
        try {
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key);
            
            if (metadata != null && !metadata.isEmpty()) {
                requestBuilder.metadata(metadata);
            }
            
            PutObjectRequest request = requestBuilder.build();
            RequestBody body = RequestBody.fromString(content);
            
            PutObjectResponse response = s3Client.putObject(request, body);
            
            log.info("Successfully wrote S3 object: s3://{}/{} (ETag: {})", 
                    bucketName, key, response.eTag());
            
        } catch (S3Exception e) {
            log.error("Failed to write S3 object: s3://{}/{} - {}", 
                    bucketName, key, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("S3 write failed: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Writes to default bucket.
     */
    public void writeObject(String key, String content) {
        writeObject(defaultBucket, key, content);
    }

    /**
     * Writes bytes to S3 (for binary files).
     */
    public void writeObjectBytes(String bucketName, String key, byte[] content) {
        log.debug("Writing S3 object bytes: s3://{}/{} ({} bytes)", bucketName, key, content.length);
        
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            RequestBody body = RequestBody.fromBytes(content);
            PutObjectResponse response = s3Client.putObject(request, body);
            
            log.info("Successfully wrote S3 object bytes: s3://{}/{} (ETag: {})", 
                    bucketName, key, response.eTag());
            
        } catch (S3Exception e) {
            log.error("Failed to write S3 object bytes: s3://{}/{}", bucketName, key, e);
            throw new RuntimeException("S3 write bytes failed", e);
        }
    }

    /**
     * Deletes object from S3.
     */
    public void deleteObject(String bucketName, String key) {
        log.debug("Deleting S3 object: s3://{}/{}", bucketName, key);
        
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            s3Client.deleteObject(request);
            log.info("Successfully deleted S3 object: s3://{}/{}", bucketName, key);
            
        } catch (S3Exception e) {
            log.error("Failed to delete S3 object: s3://{}/{} - {}", 
                    bucketName, key, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("S3 delete failed: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Deletes from default bucket.
     */
    public void deleteObject(String key) {
        deleteObject(defaultBucket, key);
    }

    /**
     * Checks if object exists in S3.
     */
    public boolean objectExists(String bucketName, String key) {
        log.debug("Checking if S3 object exists: s3://{}/{}", bucketName, key);
        
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            s3Client.headObject(request);
            log.debug("S3 object exists: s3://{}/{}", bucketName, key);
            return true;
            
        } catch (NoSuchKeyException e) {
            log.debug("S3 object does not exist: s3://{}/{}", bucketName, key);
            return false;
        } catch (S3Exception e) {
            log.error("Failed to check S3 object existence: s3://{}/{}", bucketName, key, e);
            throw new RuntimeException("S3 head object failed", e);
        }
    }

    /**
     * Checks existence in default bucket.
     */
    public boolean objectExists(String key) {
        return objectExists(defaultBucket, key);
    }

    /**
     * Lists objects in S3 with optional prefix.
     */
    public List<String> listObjects(String bucketName, String prefix) {
        log.debug("Listing S3 objects: s3://{}/{}", bucketName, prefix != null ? prefix : "*");
        
        try {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName);
            
            if (prefix != null && !prefix.isEmpty()) {
                requestBuilder.prefix(prefix);
            }
            
            ListObjectsV2Request request = requestBuilder.build();
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            
            List<String> keys = response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
            
            log.info("Found {} S3 objects in s3://{}/{}", keys.size(), bucketName, 
                    prefix != null ? prefix : "*");
            return keys;
            
        } catch (S3Exception e) {
            log.error("Failed to list S3 objects: s3://{}/{}", bucketName, prefix, e);
            throw new RuntimeException("S3 list failed", e);
        }
    }

    /**
     * Lists all objects in bucket.
     */
    public List<String> listObjects(String bucketName) {
        return listObjects(bucketName, null);
    }

    /**
     * Lists in default bucket with prefix.
     */
    public List<String> listObjects(String prefix) {
        return listObjects(defaultBucket, prefix);
    }

    /**
     * Gets object metadata.
     */
    public Map<String, String> getObjectMetadata(String bucketName, String key) {
        log.debug("Getting S3 object metadata: s3://{}/{}", bucketName, key);
        
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            HeadObjectResponse response = s3Client.headObject(request);
            
            Map<String, String> metadata = new HashMap<>(response.metadata());
            metadata.put("ContentLength", String.valueOf(response.contentLength()));
            metadata.put("ContentType", response.contentType());
            metadata.put("ETag", response.eTag());
            metadata.put("LastModified", response.lastModified().toString());
            
            log.info("Retrieved metadata for s3://{}/{}", bucketName, key);
            return metadata;
            
        } catch (S3Exception e) {
            log.error("Failed to get S3 object metadata: s3://{}/{}", bucketName, key, e);
            throw new RuntimeException("S3 get metadata failed", e);
        }
    }

    /**
     * Copies object within S3.
     */
    public void copyObject(String sourceBucket, String sourceKey, 
                          String destBucket, String destKey) {
        log.debug("Copying S3 object: s3://{}/{} -> s3://{}/{}", 
                sourceBucket, sourceKey, destBucket, destKey);
        
        try {
            CopyObjectRequest request = CopyObjectRequest.builder()
                    .sourceBucket(sourceBucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(destBucket)
                    .destinationKey(destKey)
                    .build();
            
            s3Client.copyObject(request);
            
            log.info("Successfully copied S3 object: s3://{}/{} -> s3://{}/{}", 
                    sourceBucket, sourceKey, destBucket, destKey);
            
        } catch (S3Exception e) {
            log.error("Failed to copy S3 object", e);
            throw new RuntimeException("S3 copy failed", e);
        }
    }

    /**
     * Deletes multiple objects at once.
     */
    public void deleteObjects(String bucketName, List<String> keys) {
        log.debug("Deleting {} S3 objects from bucket: {}", keys.size(), bucketName);
        
        try {
            List<ObjectIdentifier> objectIds = keys.stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .collect(Collectors.toList());
            
            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(objectIds).build())
                    .build();
            
            DeleteObjectsResponse response = s3Client.deleteObjects(request);
            
            log.info("Successfully deleted {} S3 objects from bucket: {}", 
                    response.deleted().size(), bucketName);
            
        } catch (S3Exception e) {
            log.error("Failed to delete multiple S3 objects", e);
            throw new RuntimeException("S3 batch delete failed", e);
        }
    }

    /**
     * Gets presigned URL for temporary access.
     * Useful for generating download links in tests.
     */
    public String getPresignedUrl(String bucketName, String key, int expirationMinutes) {
        // Note: Presigned URL generation requires additional SDK dependencies
        // This is a placeholder for the implementation
        log.info("Generating presigned URL for s3://{}/{} (expires in {} minutes)", 
                bucketName, key, expirationMinutes);
        return String.format("https://%s.s3.amazonaws.com/%s?presigned=true", bucketName, key);
    }
}
