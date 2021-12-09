@controller @read
Feature: Reading a record from the registry

  Scenario: Reading a record which doesn't exist
    Given a non existent record id
    And a valid auth token
    When retrieving the record from the registry 
    Then record retrieval should be unsuccessful
    And error message is Entity does not exist

  Scenario: Reading a record which does exist
    Given an existent record id
    And a valid auth token
    When retrieving the record from the registry
    Then record retrieval should be successful
    And the record should match

   Scenario: Getting an expected response
    Given a read response 
    When response matches expected format
    Then the response format should be successful

   Scenario: Reading a record with invalid token
    Given an existent record id
    And an invalid auth token
    When retrieving the record from the registry 
    Then record retrieval should be unsuccessful
    And error message is Auth token is invalid
    
  Scenario: Reading a record with missing token
    Given an existent record id
    And a missing auth token
    When retrieving the record from the registry 
    Then record retrieval should be unsuccessful
    And error message is Auth token is missing
   
   Scenario: Reading a record with audit info
    Given an existent record id
    When retrieving the record from the registry 
    Then record should never have any associated audit info