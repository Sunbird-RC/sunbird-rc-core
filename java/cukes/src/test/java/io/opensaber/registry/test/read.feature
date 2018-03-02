@controller
Feature: Reading a record from the registry

  Scenario: Reading a record which doesn't exist
    Given a non existent record id
    When retrieving the record from the registry 
    Then record retrieval should be unsuccessful
    And error message is Entity does not exist

  Scenario: Reading a record which does exist
    Given an existent record id
    When retrieving the record from the registry
    Then record retrieval should be successful
    And the record should match
