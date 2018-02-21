#Author: jyotsna.raveendran@tarento.com

@tag
Feature: Inserting a record into the registry

  @tag1
  Scenario: Inserting first valid record
    Given First input data and base url are valid
    When Inserting first valid record into the registry
    Then Response for first valid record is success
    
  @tag2
  Scenario: Inserting a duplicate record
    Given Valid duplicate data
    When Inserting a duplicate record into the registry
    Then Response for duplicate record is Cannot insert duplicate record
    
  @tag3
  Scenario: Inserting second valid record into the registry
    Given Second input data and base url are valid
    When Inserting second valid record into the registry
    Then Response for second valid record is success
    
  @tag4
  Scenario: Inserting invalid record
    Given Base url is valid but input data has invalid root label
    When Inserting record with no label/invalid label into the registry
    Then Response for invalid record is Failed to insert record
    