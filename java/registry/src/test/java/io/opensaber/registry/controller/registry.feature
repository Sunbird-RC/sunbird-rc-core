#Author: jyotsna.raveendran@tarento.com

@tag
Feature: Inserting a record into the registry

  @tag1
  Scenario: Inserting a valid record
    Given Input data and base url are valid
    When Inserting a record into the registry
    Then Response for valid record is success
    
  @tag2
  Scenario: Inserting a duplicate record
    Given Valid duplicate data
    When Inserting a duplicate record into the registry
    Then Response for duplicate record is failure
    
  @tag3
  Scenario: Inserting invalid record
    Given Input data is invalid
    When Inserting invalid record into the registry
    Then Response for invalid record is failure
    