@controller @delete
Feature: Deleting a record in registry

  Background:
    Given a valid entity record
    And the record is created in the registry

  Scenario: Deleting a non-existent record
    Given a non-existent record id
    When deleting the record in the registry
    Then deleting the record should be unsuccessful
    And delete api error message is Entity does not exist

  Scenario: Deleting an existing record with parent record status is active
    Given a existing record id while parent record status is active
    When deleting the record in the registry
    Then deleting the record should be unsuccessful
    And delete api error message is Delete operation not supports
   