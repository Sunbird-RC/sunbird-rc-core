@controller
Feature: Inserting a record into the registry

  Scenario: Issuing a valid record
    Given a valid record
    When issuing the record into the registry
    Then record issuing should be successful

  Scenario: Issuing a duplicate record
    Given a record issued into the registry
    And valid duplicate data
    When Inserting the duplicate record into the registry
    Then record issuing is successful
    And response for duplicate record is Cannot insert duplicate record

  Scenario: Inserting second valid record into the registry
    Given Second input data and base url are valid
    When Inserting second valid record into the registry
    Then Response for second valid record is success

  Scenario: Inserting record with invalid type
    Given Base url is valid but input data has invalid type
    When Inserting record with invalid type into the registry
    Then Response for invalid record is Failed to insert due to invalid type
   