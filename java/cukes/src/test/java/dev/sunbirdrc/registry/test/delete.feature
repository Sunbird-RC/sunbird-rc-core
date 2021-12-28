@controller @delete
Feature: Deleting a record in registry

  Background:
    Given a valid create entity record
    And the record is inserted in the registry

  Scenario: Deleting a record after record is created
    Given delete the record in the registry
    Then deleted record should be successful

  Scenario: Deleting a non-existent record
    Given delete a non-existent record id
    When delete the record in the registry
    Then deleted record should be unsuccessful
    And delete api error message is Entity does not exist

  Scenario: Deleting a record with connected records are active
    Given delete record with connected nodes are active
    When delete the record in the registry
    Then deleted record should be unsuccessful