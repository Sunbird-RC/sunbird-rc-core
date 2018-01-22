#Author: jyotsna.raveendran@tarento.com

@tag
Feature: Data conversion between JSON-LD and RDF

  @tag1
  Scenario: Converting from JSON-LD to RDF
    Given JSON-LD data and base url are valid
    When JSON-LD data needs to be converted to RDF
    Then The response is success
    
  @tag2
  Scenario: Converting from RDF to JSON-LD
  When RDF data needs to be converted to JSON-LD
  Then The response contains id