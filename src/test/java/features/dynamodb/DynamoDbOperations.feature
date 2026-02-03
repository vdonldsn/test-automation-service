Feature: DynamoDB Operations Testing
  Test DynamoDB operations using CommonUtility service implementation
  
  Background:
    * def config = commonUtility
    * def dynamo = config.DynamoDbImpl()
    * def logger = config.LoggerImpl()
    
    # Test configuration
    * def testTable = 'users'
    * def uniqueId = karate.config.generateUniqueId()

  Scenario: Put and get item from DynamoDB
    Given def testItem = 
      """
      {
        userId: '#(uniqueId)',
        name: 'Test User',
        email: 'test@example.com',
        age: 30,
        active: true
      }
      """
    
    # Put item into DynamoDB
    When call dynamo.putItem(testTable, testItem)
    * call logger.log('Put item into DynamoDB: ' + uniqueId, 'INFO')
    
    # Get item back
    Then def retrievedItem = dynamo.getItem(testTable, 'userId', uniqueId)
    And match retrievedItem != null
    And match retrievedItem.name == 'Test User'
    And match retrievedItem.email == 'test@example.com'
    And match retrievedItem.age == 30
    And match retrievedItem.active == true
    
    # Cleanup
    * call dynamo.deleteItem(testTable, 'userId', uniqueId)

  Scenario: Query items from DynamoDB
    Given def user1Id = uniqueId + '-1'
    And def user2Id = uniqueId + '-2'
    
    # Put multiple items
    And def item1 = { userId: '#(user1Id)', name: 'User One', email: 'user1@example.com' }
    And def item2 = { userId: '#(user2Id)', name: 'User Two', email: 'user2@example.com' }
    * call dynamo.putItem(testTable, item1)
    * call dynamo.putItem(testTable, item2)
    
    # Query by userId
    When def results = dynamo.query(testTable, 'userId', user1Id)
    Then match results == '#[1]'
    And match results[0].name == 'User One'
    
    # Cleanup
    * call dynamo.deleteItem(testTable, 'userId', user1Id)
    * call dynamo.deleteItem(testTable, 'userId', user2Id)

  Scenario: Update and verify item in DynamoDB
    Given def testItem = 
      """
      {
        userId: '#(uniqueId)',
        name: 'Original Name',
        version: 1
      }
      """
    
    # Put original item
    When call dynamo.putItem(testTable, testItem)
    
    # Update item (by putting with same key)
    And def updatedItem = 
      """
      {
        userId: '#(uniqueId)',
        name: 'Updated Name',
        version: 2
      }
      """
    And call dynamo.putItem(testTable, updatedItem)
    
    # Verify update
    Then def retrievedItem = dynamo.getItem(testTable, 'userId', uniqueId)
    And match retrievedItem.name == 'Updated Name'
    And match retrievedItem.version == 2
    
    # Cleanup
    * call dynamo.deleteItem(testTable, 'userId', uniqueId)

  Scenario: Integration - Use convenience method
    # Use CommonUtility convenience method
    Given def testItem = { userId: '#(uniqueId)', name: 'Convenience Test' }
    
    When call config.dynamoPut(testTable, testItem)
    Then def result = config.dynamoGet(testTable, 'userId', uniqueId)
    And match result.name == 'Convenience Test'
    
    # Cleanup
    * call dynamo.deleteItem(testTable, 'userId', uniqueId)
