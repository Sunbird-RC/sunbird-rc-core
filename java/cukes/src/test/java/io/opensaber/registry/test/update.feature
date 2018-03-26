@controller @update
Feature: Updating a record in registry

  Scenario: Updating an existing record
   	Given a record issued into the registry
    And a valid auth token
    And an invalid record for updating
    When updating the record in registry
    Then record issuing should be unsuccessful
    And error message is Data validation failed!