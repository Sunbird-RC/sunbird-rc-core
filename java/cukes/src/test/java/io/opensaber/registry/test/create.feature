@controller @create
Feature: Inserting a record into the registry

  Scenario: Issuing a valid record
    Given a valid record
    And a valid auth token
    When issuing the record into the registry
    Then record issuing should be successful
    And fetching the record from the registry should match the issued record

  Scenario: Issuing a duplicate record
    Given a record issued into the registry
    And a valid auth token
    When issuing the record into the registry again
    Then record issuing should be unsuccessful
    And error message is Cannot insert duplicate record

  Scenario: Inserting second valid record into the registry
    Given a record issued into the registry
    And a valid auth token
    When another record issued into the registry
    Then record issuing should be successful
    And fetching the record from the registry should match the issued record

   Scenario: Getting an expected response
    Given a response
    When response matches expected format
    Then the response format should be successful

  Scenario: Issuing a record with invalid auth token
    Given a valid record
    And an invalid auth token
    When issuing the record into the registry
    Then record issuing should be unsuccessful
    And error message is Auth token is invalid

  Scenario: Issuing a record with missing auth token
    Given a valid record
    And a missing auth token
    When issuing the record into the registry
    Then record issuing should be unsuccessful
    And error message is Auth token is missing

  Scenario: Issuing an invalid record
    Given an invalid record
    And a valid auth token
    When issuing the record into the registry
    Then record issuing should be unsuccessful
    And error message is Data validation failed!

  Scenario: Issuing an invalid request id for record
    Given an invalid request id for record
    And a valid auth token
    When issuing the record into the registry
    Then record issuing should be unsuccessful
    And error message is Entity id is wrongly provided in the input

  Scenario: Adding an entity for an existing record
    Given a record issued into the registry
    And a valid auth token
    When an entity for the record is issued into the registry
    Then record issuing should be successful
    And fetching the record from the registry should match the issued record

  Scenario: Adding a duplicate entity for an existing record
    Given a record issued into the registry
    And an entity for the record is issued into the registry
    And a valid auth token
    When the same entity for the record is issued into the registry
    Then record issuing should be unsuccessful
    And error message is Cannot insert duplicate record

  Scenario: Adding an entity for an non-existent record
    Given an id for a non-existent record
    And a valid auth token
    When an entity for the record is issued into the registry
    Then record issuing should be unsuccessful
    And error message is Entity does not exist