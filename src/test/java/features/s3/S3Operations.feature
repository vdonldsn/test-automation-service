Feature: S3 Operations Testing
  Test S3 operations using CommonUtility service implementation
  
  Background:
    # Load configuration and CommonUtility
    * def config = commonUtility
    * def s3 = config.S3Impl()
    * def logger = config.LoggerImpl()
    
    # Test configuration
    * def testBucket = karate.config.aws.s3Bucket
    * def uniqueId = karate.config.generateUniqueId()
    * def testKeyPrefix = 'karate-test/' + uniqueId + '/'
    
    # Log test start
    * call logger.log('Starting S3 test suite', 'INFO')

  Scenario: Upload and read text file from S3
    Given def testKey = testKeyPrefix + 'test.txt'
    And def testContent = 'Hello from Karate S3 test!'
    
    # Upload file to S3
    When call s3.writeObject(testBucket, testKey, testContent)
    * call logger.log('Uploaded file to S3: ' + testKey, 'INFO')
    
    # Verify file exists
    Then def exists = s3.objectExists(testBucket, testKey)
    And match exists == true
    
    # Read file back
    And def readContent = s3.readObject(testBucket, testKey)
    And match readContent == testContent
    * call logger.log('File content matches!', 'INFO')
    
    # Cleanup
    * call s3.deleteObject(testBucket, testKey)

  Scenario: Upload file with metadata
    Given def testKey = testKeyPrefix + 'metadata-test.txt'
    And def testContent = 'File with metadata'
    And def metadata = { author: 'Karate Test', version: '1.0', purpose: 'testing' }
    
    # Upload with metadata
    When call s3.writeObject(testBucket, testKey, testContent, metadata)
    
    # Get metadata
    Then def retrievedMetadata = s3.getObjectMetadata(testBucket, testKey)
    And match retrievedMetadata.author == 'Karate Test'
    And match retrievedMetadata.version == '1.0'
    And match retrievedMetadata.purpose == 'testing'
    
    # Cleanup
    * call s3.deleteObject(testBucket, testKey)

  Scenario: List objects with prefix
    Given def testFiles = ['file1.txt', 'file2.txt', 'file3.txt']
    
    # Upload multiple files
    * def uploadFile = function(fileName) { karate.call('s3.writeObject', testBucket, testKeyPrefix + fileName, 'content-' + fileName) }
    * karate.forEach(testFiles, uploadFile)
    
    # List objects with prefix
    When def listedObjects = s3.listObjects(testBucket, testKeyPrefix)
    Then match listedObjects == '#[3]'
    And match listedObjects contains testKeyPrefix + 'file1.txt'
    And match listedObjects contains testKeyPrefix + 'file2.txt'
    And match listedObjects contains testKeyPrefix + 'file3.txt'
    
    # Cleanup
    * call s3.deleteObjects(testBucket, listedObjects)

  Scenario: Upload and download binary file
    Given def testKey = testKeyPrefix + 'binary-test.bin'
    And def binaryData = Java.type('java.nio.charset.StandardCharsets').UTF_8.encode('Binary content').array()
    
    # Upload binary data
    When call s3.writeObjectBytes(testBucket, testKey, binaryData)
    
    # Download binary data
    Then def downloadedData = s3.readObjectBytes(testBucket, testKey)
    And match downloadedData == binaryData
    
    # Cleanup
    * call s3.deleteObject(testBucket, testKey)

  Scenario: Copy object within S3
    Given def sourceKey = testKeyPrefix + 'source.txt'
    And def destKey = testKeyPrefix + 'destination.txt'
    And def content = 'Content to copy'
    
    # Upload source file
    When call s3.writeObject(testBucket, sourceKey, content)
    
    # Copy file
    And call s3.copyObject(testBucket, sourceKey, testBucket, destKey)
    
    # Verify destination exists
    Then def destExists = s3.objectExists(testBucket, destKey)
    And match destExists == true
    
    # Verify content matches
    And def destContent = s3.readObject(testBucket, destKey)
    And match destContent == content
    
    # Cleanup
    * call s3.deleteObject(testBucket, sourceKey)
    * call s3.deleteObject(testBucket, destKey)

  Scenario: Verify object does not exist after deletion
    Given def testKey = testKeyPrefix + 'delete-test.txt'
    And def content = 'File to delete'
    
    # Upload file
    When call s3.writeObject(testBucket, testKey, content)
    And def existsBefore = s3.objectExists(testBucket, testKey)
    And match existsBefore == true
    
    # Delete file
    When call s3.deleteObject(testBucket, testKey)
    
    # Verify deleted
    Then def existsAfter = s3.objectExists(testBucket, testKey)
    And match existsAfter == false

  Scenario Outline: Upload files of various sizes
    Given def testKey = testKeyPrefix + 'size-test-<size>.txt'
    And def content = karate.repeat(<size>, 'x')
    
    When call s3.writeObject(testBucket, testKey, content)
    Then def metadata = s3.getObjectMetadata(testBucket, testKey)
    And match metadata.ContentLength == '<size>'
    
    # Cleanup
    * call s3.deleteObject(testBucket, testKey)
    
    Examples:
      | size  |
      | 100   |
      | 1024  |
      | 10240 |

  Scenario: Integration with other services - Upload and log
    Given def testKey = testKeyPrefix + 'integration-test.txt'
    And def content = 'Integration test content'
    
    # Log before upload
    * call logger.log('About to upload file: ' + testKey, 'INFO')
    
    # Upload using CommonUtility convenience method
    When call config.s3Write(testBucket, testKey, content)
    
    # Log after upload
    * call logger.log('Upload completed successfully', 'INFO')
    
    # Read using convenience method
    Then def readContent = config.s3Read(testBucket, testKey)
    And match readContent == content
    
    # Log result
    * call logger.log('Content verification successful', 'INFO')
    
    # Cleanup
    * call s3.deleteObject(testBucket, testKey)
