Feature: Get certificate pdf
  Background:
    * def certUrl = "http://localhost:8078"
    * def templateBody = {"certificate": "{\"name\":\"Test Name\", \"dob\":\"2002-12-22\"}","templateUrl": "http://registry:8081/api/v1/templates/Student.html"}
  Scenario:
    Given url certUrl
    Given path "api/v1/certificatePDF"
    And request templateBody
    When method post
    Then status 200
    #* print response

  Scenario:
    Given url certUrl
    Given path "api/v1/certificatePDF"
    And request {"certificate":"","templateUrl": "http://registry:8081/api/v1/templates/Student.html"}
    When method post
    Then status 400
    * print response