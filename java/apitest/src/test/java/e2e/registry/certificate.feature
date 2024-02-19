Feature: Get certificate pdf
  Background:
    * def certUrl = "http://localhost:8078"
    * def templateBody = {"certificate": "{\"name\":\"Test Name\", \"dob\":\"2002-12-22\"}","templateUrl": "http://registry:8081/api/v1/templates/Student.html"}

  @envnot=fusionauth
  Scenario:
    And header Accept = 'application/pdf'
    Given url certUrl
    Given path "api/v1/certificate"
    And request templateBody
    When method post
    Then status 200
    #* print response

  @envnot=fusionauth
  Scenario:
    And header Accept = 'application/pdf'
    Given url certUrl
    Given path "api/v1/certificate"
    And request {"certificate":"","templateUrl": "http://registry:8081/api/v1/templates/Student.html"}
    When method post
    Then status 400
    * print response