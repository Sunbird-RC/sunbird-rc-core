@controller @search
Feature: Searching a record in registry

  Background:
    Given a record to add
    And adding record in the registry

  Scenario: Searching with non-existent data
    Given a search filter with no match in the registry
    When searching record in the registry
    Then search api message is successful
    And result is empty

  Scenario: Searching an existing record by single property
    Given a search filter with single property
    When searching record in the registry
    Then search api message is successful
    And response must have atleast one record

  Scenario: Searching an existing record by multiple properties
    Given a search filter with multiple properties
    When searching record in the registry
    Then search api message is successful
    And response must have atleast one record
    
  Scenario: Searching an existing record by multiple values for a property
    Given a search filter with multiple values for a property
    When searching record in the registry
    Then search api message is successful
    And response must have atleast one record
    
  Scenario: Searching an existing record without entity type
    Given a search filter without entity type
    When searching record in the registry
    Then search api message is unsuccessful
    And message is Entity type is not provided in the input