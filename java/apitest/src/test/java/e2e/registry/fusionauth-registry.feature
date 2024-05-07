Feature: Registry api tests
    Background:
        * string registryUrl = "http://localhost:8081"
        * string authUrl = "http://localhost:9011"
        * url registryUrl
        * def admin_token = ""
        * def client_secret = 'a52c5f4a-89fd-40b9-aea2-3f711f14c889'
        * def sleep = function(millis){ java.lang.Thread.sleep(millis) }
    @env=fusionauth
    Scenario: Create student with password schema and verify if password is set
      #    get admin token
        * url authUrl
        * path '/oauth2/token'
        * header Content-Type = 'application/x-www-form-urlencoded; charset=utf-8'
        * header Host = 'fusionauth:9011'
        * form field grant_type = 'password'
        * form field client_id = '85a03867-dccf-4882-adde-1a79aeec50df'
        * form field username = 'admin@sunbirdrc.dev'
        * form field password = 'admin@12345'
        * method post
        Then status 200
        And print response.access_token
        * def admin_token = 'Bearer ' + response.access_token
    # create student schema
        Given url registryUrl
        And path 'api/v1/Schema'
        And header Authorization = admin_token
        And request read('StudentWithPasswordSchemaRequest.json')
        When method post
        Then status 200
        And response.params.status == "SUCCESSFUL"
      # invite entity for student
        Given url registryUrl
        And path 'api/v1/StudentWithPassword/invite'
        * def studentRequest = read('StudentWithPasswordRequest.json')
        And request studentRequest
        When method post
        Then status 200
        * def studentOsid = response.result.StudentWithPassword.osid
      #  get student token
        * url authUrl
        * path '/oauth2/token'
        * header Content-Type = 'application/x-www-form-urlencoded; charset=utf-8'
        * header Host = 'fusionauth:9011'
        * form field grant_type = 'password'
        * form field client_id = '85a03867-dccf-4882-adde-1a79aeec50df'
        * form field username = studentRequest.contactDetails.mobile
        * form field password = studentRequest.userDetails.passkey
        * method post
        Then status 200
        And print response.access_token
        * def student_token = 'Bearer ' + response.access_token
        * sleep(3000)
      # get student info
        Given url registryUrl
        And path 'api/v1/StudentWithPassword/'
        And header Authorization = student_token
        When method get
        Then status 200
        And response.data[0].osid.length > 0
