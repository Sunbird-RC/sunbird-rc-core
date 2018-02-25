@controller
Feature: Reading a record from the registry

  Scenario: Reading a record which doesn't exist
    Given base url are valid
    When Reading a non existent record
    Then Response for first valid record is an error
    And the error message says record does not exist