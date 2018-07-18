@opensaberclient
Feature: Invoke registry apis using client library

  Scenario: Creating a valid entity in the registry
    Given a valid json input for a new entity
    When creating the entity in the registry
    Then response from the api should be successful

  Scenario: Creating an entity with no field mapping
    Given a json input with missing field mapping
    When creating the entity in the registry
    Then creation of new entity should be unsuccessful

  Scenario: Updating a valid entity in the registry
    Given a valid json input for a new entity
    And an existing entity in the registry
    When updating the entity in the registry
    Then response from the api should be successful

  Scenario: Deleting a valid entity in the registry
    Given a valid json input for a new entity
    When creating the entity in the registry
    And delete the entity in the registry
    Then response from the api should be successful
