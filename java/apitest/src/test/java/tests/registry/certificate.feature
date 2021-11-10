Feature: Get certificate pdf
  Background:
    * def certUrl = "http://localhost:8078"
    * def templateBody = {"data": "{\"name\": \"Kesavan\", \"dob\": \"09-Oct-2021\"}","templateUrl": "http://localhost:8081/api/v1/templates/Student.html"}
  Scenario:
    Given url certUrl
    Given path "api/v1/certificatePDF"
    And request templateBody
    When method post
    Then status 200
    * print response