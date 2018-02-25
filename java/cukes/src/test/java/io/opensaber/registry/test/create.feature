@controller
Feature: Inserting a record into the registry

  Scenario: Issuing a valid record
    Given a valid record
    When issuing the record into the registry
    Then record issuing should be successful

  Scenario: Issuing a duplicate record
    Given a record issued into the registry
    When issuing the record into the registry again
    Then record issuing should be unsuccessful
    And error message is Cannot insert duplicate record

  Scenario: Inserting second valid record into the registry
    Given a record issued into the registry
    When another record issued into the registry
    Then record issuing should be successful

  Scenario: Inserting record with invalid type
    Given an invalid record
    When issuing the record into the registry
    Then record issuing should be unsuccessful
    And error message is Failed to insert due to invalid type
   