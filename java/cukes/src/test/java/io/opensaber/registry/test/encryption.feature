@controller
Feature: Inserting a record into the registry

  Scenario: Getting an expected response after encryption followed by decryption
    Given a valid record
    And a valid auth token
    When issuing the record into the registry
    Then record retrieval should be successful
    And fetching the record from the registry should match the issued record