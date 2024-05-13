
Feature: Registry api tests
  Background:
    * string registryUrl = "http://localhost:8081"
    * string authUrl = "http://localhost:8080"
    * string metricsUrl = "http://localhost:8070"
    * string notificationsUrl = "http://localhost:8765"
    * url registryUrl
    * def admin_token = ""
    * def client_secret = '**********'
    * def sleep = function(millis){ java.lang.Thread.sleep(millis) }
  @envnot=fusionauth
  Scenario: health check
    Given path 'health'
    When method get
    Then status 200
  @envnot=fusionauth
  Scenario: Create birth certificate schema and issue credentials
#    get admin token
    * url authUrl
    * path 'auth/realms/sunbird-rc/protocol/openid-connect/token'
    * header Content-Type = 'application/x-www-form-urlencoded; charset=utf-8'
    * header Host = 'keycloak:8080'
    * form field grant_type = 'client_credentials'
    * form field client_id = 'admin-api'
    * form field client_secret = client_secret
    * method post
    Then status 200
    And print response.access_token
    * def admin_token = 'Bearer ' + response.access_token
# create birth certificate schema
    Given url registryUrl
    And path 'api/v1/Schema'
    And header Authorization = admin_token
    And request read('BirthCertificateSchemaRequest.json')
    When method post
    Then status 200
    And response.params.status == "SUCCESSFUL"
    * sleep(10000)
# create entity for birth certificate
    Given url registryUrl
    And path 'api/v1/BirthCertificate'
    And request read('BirthCertificateRequest.json')
    When method post
    Then status 200
    And def birthCertificateOsid = response.result.BirthCertificate.osid
  # get entity by id
    Given url registryUrl
    And path 'api/v1/BirthCertificate/' + birthCertificateOsid
    And header Authorization = admin_token
    When method get
    Then status 200
    And response._osSignedData.length > 0
  # modify entity
    Given url registryUrl
    And path 'api/v1/BirthCertificate/' + birthCertificateOsid
    And header Authorization = admin_token
    * def requestBody = read('BirthCertificateRequest.json')
    * requestBody.name = "test"
    And request requestBody
    When method put
    Then status 200
    And response.params.status == "SUCCESSFUL"
  # get entity by id
    Given url registryUrl
    And path 'api/v1/BirthCertificate/' + birthCertificateOsid
    And header Authorization = admin_token
    When method get
    Then status 200
    And response.name == "test"
    And response._osSignedData.contains("test")
  # get certificate for entity
    Given url registryUrl
    And path 'api/v1/BirthCertificate/' + birthCertificateOsid
    And header Authorization = admin_token
    And header Accept = "text/html"
    And header template-key = "html"
    When method get
    Then status 200
    And response.length > 0
  # get VC for entity
    Given url registryUrl
    And path 'api/v1/BirthCertificate/' + birthCertificateOsid
    And header Authorization = admin_token
    And header Accept = "application/vc+ld+json"
    When method get
    Then status 200
    And response.credentialSubject.name == "test"
  # delete entity by id
    Given url registryUrl
    And path 'api/v1/BirthCertificate/' + birthCertificateOsid
    And header Authorization = admin_token
    When method delete
    Then status 200
    And response.name == "test"
    And response.params.status == "SUCCESSFUL"
  # get entity by id
    Given url registryUrl
    And path 'api/v1/BirthCertificate/' + birthCertificateOsid
    And header Authorization = admin_token
    When method get
    Then status 404
    And response.params.status == "UNSUCCESSFUL"
    And response.params.errmsg == "entity status is inactive"

  @envnot=fusionauth
  Scenario: Create student schema and verify crud apis
  #    get admin token
    * url authUrl
    * path 'auth/realms/sunbird-rc/protocol/openid-connect/token'
    * header Content-Type = 'application/x-www-form-urlencoded; charset=utf-8'
    * header Host = 'keycloak:8080'
    * form field grant_type = 'client_credentials'
    * form field client_id = 'admin-api'
    * form field client_secret = client_secret
    * method post
    Then status 200
    And print response.access_token
    * def admin_token = 'Bearer ' + response.access_token
# create student schema
    Given url registryUrl
    And path 'api/v1/Schema'
    And header Authorization = admin_token
    And request read('StudentSchemaRequest.json')
    When method post
    Then status 200
    And response.params.status == "SUCCESSFUL"
    * sleep(10000)
  # invite entity for student
    Given url registryUrl
    And path 'api/v1/Student/invite'
    * def studentRequest = read('StudentRequest.json')
    And request studentRequest
    When method post
    Then status 200
  #  get student token
    * url authUrl
    * path 'auth/realms/sunbird-rc/protocol/openid-connect/token'
    * header Content-Type = 'application/x-www-form-urlencoded; charset=utf-8'
    * header Host = 'keycloak:8080'
    * form field grant_type = 'password'
    * form field client_id = 'registry-frontend'
    * form field username = studentRequest.contact
    * form field password = 'abcd@123'
    * method post
    Then status 200
    And print response.access_token
    * def student_token = 'Bearer ' + response.access_token
    * sleep(3000)
  # get student info
    Given url registryUrl
    And path 'api/v1/Student'
    And header Authorization = student_token
    When method get
    Then status 200
    And response.data[0].osid.length > 0
    * def studentOsid = response.data[0].osid
  # update student info
    Given url registryUrl
    And path 'api/v1/Student/' + studentOsid
    And header Authorization = student_token
    * set studentRequest.name = "xyz"
    And request studentRequest
    When method put
    Then status 200
    And response.params.status == "SUCCESSFUL"
  # verify student info after update
    Given url registryUrl
    And path 'api/v1/Student'
    And header Authorization = student_token
    When method get
    Then status 200
    And response.data[0].name == "xyz"
  # get student
    Given url registryUrl
    And path 'api/v1/Student/search'
    And request {"filters":{}}
    When method post
    Then status 200
    * print response
    And response.length == 1
    And match response.data[0].contact == '#notpresent'
    And match response.data[0].favoriteSubject == '#notpresent'
  # delete student info
    Given url registryUrl
    And path 'api/v1/Student/' + studentOsid
    And header Authorization = student_token
    When method delete
    Then status 200
    And response.params.status == "SUCCESSFUL"
  # verify student info after delete
    Given url registryUrl
    And path 'api/v1/Student'
    And header Authorization = student_token
    When method get
    Then status 200
    And response.totalCount == 0

  @env=async
  Scenario: Create a teacher schema and create teacher entity asynchronously
  #    get admin token
    * url authUrl
    * path 'auth/realms/sunbird-rc/protocol/openid-connect/token'
    * header Content-Type = 'application/x-www-form-urlencoded; charset=utf-8'
    * header Host = 'keycloak:8080'
    * form field grant_type = 'client_credentials'
    * form field client_id = 'admin-api'
    * form field client_secret = client_secret
    * method post
    Then status 200
    And print response.access_token
    * def admin_token = 'Bearer ' + response.access_token
# create teacher schema
    Given url registryUrl
    And path 'api/v1/Schema'
    And header Authorization = admin_token
    And request read('TeacherSchemaRequest.json')
    When method post
    Then status 200
    And response.params.status == "SUCCESSFUL"
    * sleep(10000)
  # create entity for teacher
    Given url registryUrl
    And path 'api/v1/Teacher?mode=async'
    * def teacherRequest = read('TeacherRequest.json')
    And request teacherRequest
    When method post
    Then status 200
    And response.result.Teacher.transactionId.length > 0
    * sleep(7000)
  # create entity for teacher
    Given url registryUrl
    And path 'api/v1/Teacher?mode=async'
    * def teacherRequest = read('TeacherRequest2.json')
    And request teacherRequest
    When method post
    Then status 200
    And response.result.Teacher.transactionId.length > 0
    * sleep(15000)
  # get teacher info
    Given url registryUrl
    And path 'api/v1/Teacher/search'
    And request {"filters":{ "name":  { "eq":  "abc" }}}
    When method post
    Then status 200
    * print response
    And response.totalCount == 1
    And response.data[0].contact == '#notpresent'
  # get teacher info
    Given url registryUrl
    And path 'api/v1/Teacher/search'
    And request {"filters":{ "name":  { "endsWith":  "abc" }}}
    When method post
    Then status 200
    * print response
    And response.totalCount == 1
    And response.data[0].contact == '#notpresent'

  @envnot=fusionauth
  Scenario: Create Board and invite institutes
    #    get admin token
    * url authUrl
    * path 'auth/realms/sunbird-rc/protocol/openid-connect/token'
    * header Content-Type = 'application/x-www-form-urlencoded; charset=utf-8'
    * header Host = 'keycloak:8080'
    * form field grant_type = 'client_credentials'
    * form field client_id = 'admin-api'
    * form field client_secret = client_secret
    * method post
    * def sample = read('inviteFlow.json')
    Then status 200
    And print response.access_token
    * def admin_token = 'Bearer ' + response.access_token
  # create board schema
    Given url registryUrl
    And path 'api/v1/Schema'
    And header Authorization = admin_token
    And request sample.boardSchema
    When method post
    Then status 200
    And response.params.status == "SUCCESSFUL"
    * sleep(10000)
  # create institute schema
    Given url registryUrl
    And path 'api/v1/Schema'
    And header Authorization = admin_token
    And request sample.instituteSchema
    When method post
    Then status 200
    And response.params.status == "SUCCESSFUL"
    * sleep(10000)
   # invite institute without token should fail
    Given url registryUrl
    And path 'api/v1/Institute/invite'
    And request sample.instituteRequest
    When method post
    Then status 401
  # invite board
    Given url registryUrl
    And path 'api/v1/Board/invite'
    And request sample.boardInviteRequest
    When method post
    Then status 200
    * def boardOsid = response.result.Board.osid
  #  get board token
    * url authUrl
    * path 'auth/realms/sunbird-rc/protocol/openid-connect/token'
    * header Content-Type = 'application/x-www-form-urlencoded; charset=utf-8'
    * header Host = 'keycloak:8080'
    * form field grant_type = 'password'
    * form field client_id = 'registry-frontend'
    * form field username = sample.boardInviteRequest.mobile
    * form field password = 'abcd@123'
    * method post
    Then status 200
    And print response.access_token
    * def board_token = 'Bearer ' + response.access_token
    * sleep(3000)
  # get board info
    Given url registryUrl
    And path 'api/v1/Board'
    And header Authorization = board_token
    When method get
    Then status 200
    And response.data[0].osid.length > 0

  # invite institute with token
    Given url registryUrl
    And path 'api/v1/Institute/invite'
    * def requestBody = sample.instituteRequest
    * requestBody.references = 'did:Board:' + boardOsid
    And request requestBody
    And header Authorization = board_token
    When method post
    Then status 200
    #  get institute token
    * url authUrl
    * path 'auth/realms/sunbird-rc/protocol/openid-connect/token'
    * header Content-Type = 'application/x-www-form-urlencoded; charset=utf-8'
    * header Host = 'keycloak:8080'
    * form field grant_type = 'password'
    * form field client_id = 'registry-frontend'
    * form field username = sample.instituteRequest.mobile
    * form field password = 'abcd@123'
    * method post
    Then status 200
    And print response.access_token
    * def institute_token = 'Bearer ' + response.access_token
    * sleep(3000)
  # get institute info
    Given url registryUrl
    And path 'api/v1/Institute'
    And header Authorization = institute_token
    When method get
    Then status 200
    And response.data[0].osid.length > 0
    * def instituteOsid = response.data[0].osid
    * def address = response.data[0].address

  # update property of institute
    Given url registryUrl
    And path 'api/v1/Institute/'+instituteOsid+'/address/'+address[0].osid
    And address[0].phoneNo = ["444"]
    And request address[0]
    And header Authorization = institute_token
    When method put
    Then status 200
    * print response
  # check if array updated institute info
    Given url registryUrl
    And path 'api/v1/Institute'
    And header Authorization = institute_token
    When method get
    Then status 200
    And assert response.data[0].address[0].phoneNo.length == 1
    And assert response.data[0].address[0].phoneNo[0] == "444"

  @envnot=fusionauth
  Scenario: write a api test, to test the schema not found error
  # get admin token
    * url authUrl
    * path 'auth/realms/sunbird-rc/protocol/openid-connect/token'
    * header Content-Type = 'application/x-www-form-urlencoded; charset=utf-8'
    * header Host = 'keycloak:8080'
    * form field grant_type = 'client_credentials'
    * form field client_id = 'admin-api'
    * form field client_secret = client_secret
    * method post
    Then status 200
    And print response.access_token
    * def admin_token = 'Bearer ' + response.access_token
   # invite schema which is unavailable/not found
    Given url registryUrl
    And path 'api/v1/Teacher1/invite'
    And header Authorization = admin_token
    And request read('TeacherRequest.json')
    When method post
    Then status 404
    And print response
    And response.params.status == "UNSUCCESSFUL"
    And response.params.errmsg == "Schema 'Teacher1' not found"
  # delete unavailable schema with id
    Given url registryUrl
    And path '/api/v1/Teacher1/123'
    And header Authorization = admin_token
    And request read('TeacherRequest.json')
    When method delete
    Then status 404
    Then response.params.status =="UNSUCCESSFUL"
    And response.params.errmsg == "Schema 'Teacher1' not found"
  # search unavailable schema
    Given url registryUrl
    And path '/api/v1/Teacher1/search'
    And header Authorization = admin_token
    And request { "filters": { } }
    When method post
    Then status 404
    Then response.params.status =="UNSUCCESSFUL"
    And response.params.errmsg == "Schema 'Teacher1' not found"
  # update unavailable schema with id
    Given url registryUrl
    And path '/api/v1/Teacher1/{entityId}'
    And header Authorization = admin_token
    And request read('TeacherRequest.json')
    When method put
    Then status 404
    Then response.params.status =="UNSUCCESSFUL"
    And response.params.errmsg == "Schema 'Teacher1' not found"
  # post unavailable schema with name
    Given url registryUrl
    And path '/api/v1/{entityName}'
    And header Authorization = admin_token
    And request read('TeacherRequest.json')
    When method post
    Then status 404
    Then response.params.status =="UNSUCCESSFUL"
    And response.params.errmsg == "Schema 'Teacher1' not found"
  # update unavailable schema
    Given url registryUrl
    And path '/api/v1/Teacher1/123/contact/456'
    And header Authorization = admin_token
    And request read('TeacherRequest.json')
    When method put
    Then status 404
    Then response.params.status =="UNSUCCESSFUL"
    And response.params.errmsg == "Schema 'Teacher1' not found"
  # update unavailable schema
    Given url registryUrl
    And path '/api/v1/Teacher1/123/name/'
    And header Authorization = admin_token
    And request read('TeacherRequest.json')
    When method post
    Then status 404
    Then response.params.status =="UNSUCCESSFUL"
    And response.params.errmsg == "Schema 'Teacher1' not found"
  # get unavailable schema
    Given url registryUrl
    And path '/partner/api/v1/Teacher1'
    And header Authorization = admin_token
    When method get
    Then status 404
    Then response.params.status =="UNSUCCESSFUL"
    And response.params.errmsg == "Schema 'Teacher1' not found"
  # get unavailable schema with id and
    Given url registryUrl
    And path '/api/v1/Teacher1/123'
    And header Authorization = admin_token
    When method get
    And request read('TeacherRequest.json')
    Then status 404
    Then response.params.status =="UNSUCCESSFUL"
    And response.params.errmsg == "Schema 'Teacher1' not found"
  # get unavailable schema with id
    Given url registryUrl
    And path '/api/v1/Teacher1/{entityId}'
    And header Authorization = admin_token
    When method get
    Then status 404
    Then response.params.status =="UNSUCCESSFUL"
    And response.params.errmsg == "Schema 'Teacher1' not found"
  # get unavailable schema with name
    Given url registryUrl
    And path '/api/v1/Teacher1'
    And header Authorization = admin_token
    When method get
    Then status 404
    Then response.params.status =="UNSUCCESSFUL"
    And response.params.errmsg == "Schema 'Teacher1' not found"
  # patch unavailable schema with name
    Given url registryUrl
    And path '/api/v1/{entityName}/{entityId}'
    And header Authorization = admin_token
    And request read('TeacherRequest.json')
    When method patch
    Then status 404
    Then response.params.status =="UNSUCCESSFUL"
    And response.params.errmsg == "Schema 'Teacher1' not found"
 # patch unavailable schema with sign
    Given url registryUrl
    And path '/api/v1/Teacher1/sign'
    And header Authorization = admin_token
    And request read('TeacherRequest.json')
    When method get
    Then status 404
    Then response.params.status =="UNSUCCESSFUL"
    And response.params.errmsg == "Schema 'Teacher1' not found"
 # get unavailable schema with attestations
    Given url registryUrl
    And path '/api/v1/Teacher1/123/attestation/teacherAttest/456'
    And header Accept = "application/json"
    And header Content-Type = "application/json"
    And header Authorization = admin_token
    When method get
    Then status 404
    And response.params.status =="UNSUCCESSFUL"
    And response.params.errmsg == "Schema 'Teacher1' not found"

  @envnot=fusionauth
  Scenario: Create student with password schema and verify if password is set
  #    get admin token
    * url authUrl
    * path 'auth/realms/sunbird-rc/protocol/openid-connect/token'
    * header Content-Type = 'application/x-www-form-urlencoded; charset=utf-8'
    * header Host = 'keycloak:8080'
    * form field grant_type = 'client_credentials'
    * form field client_id = 'admin-api'
    * form field client_secret = client_secret
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
    * sleep(10000)
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
    * path 'auth/realms/sunbird-rc/protocol/openid-connect/token'
    * header Content-Type = 'application/x-www-form-urlencoded; charset=utf-8'
    * header Host = 'keycloak:8080'
    * form field grant_type = 'password'
    * form field client_id = 'registry-frontend'
    * form field username = studentRequest.contactDetails.mobile
    * form field password = studentRequest.userDetails.passkey
    * method post
    Then status 200
    And print response.access_token
    * def student_token = 'Bearer ' + response.access_token
    * sleep(3000)
  # get student info
    Given url registryUrl
    And path 'api/v1/StudentWithPassword/' + studentOsid
    And header Authorization = student_token
    When method get
    Then status 200
    And response.osid.length > 0
  # get student info with view template
    Given url registryUrl
    And path 'api/v1/StudentWithPassword/' + studentOsid
    And header Authorization = student_token
    And header viewTemplateId = 'student_view_template.json'
    When method get
    Then status 200
    * match response.contactDetails == { mobile: '#notpresent', email: '#present', osid: '#present' }

  @envnot=fusionauth
  Scenario: Create birth certificate schema, issue credentials then revoke the credential and check for CRUD APIS
#    get admin token
    * url authUrl
    * path 'auth/realms/sunbird-rc/protocol/openid-connect/token'
    * header Content-Type = 'application/x-www-form-urlencoded; charset=utf-8'
    * header Host = 'keycloak:8080'
    * form field grant_type = 'client_credentials'
    * form field client_id = 'admin-api'
    * form field client_secret = client_secret
    * method post
    Then status 200
    And print response.access_token
    * def admin_token = 'Bearer ' + response.access_token
# create birth certificate schema
    Given url registryUrl
    And path 'api/v1/Schema'
    And header Authorization = admin_token
    And request read('BirthCertificateSchemaRequestForRevokeFlow.json')
    When method post
    Then status 200
    And response.params.status == "SUCCESSFUL"
    * sleep(10000)
# create entity for birth certificate
    Given url registryUrl
    And path 'api/v1/BirthCertificate1'
    And request read('BirthCertificateRequest.json')
    When method post
    Then status 200
    And def birthCertificateOsid = response.result.BirthCertificate1.osid
# get entity by id
    Given url registryUrl
    And path 'api/v1/BirthCertificate1/' + birthCertificateOsid
    And header Authorization = admin_token
    When method get
    Then status 200
    And response._osSignedData.length > 0
# modify entity
    Given url registryUrl
    And path 'api/v1/BirthCertificate1/' + birthCertificateOsid
    And header Authorization = admin_token
    * def requestBody = read('BirthCertificateRequest.json')
    * requestBody.name = "test"
    And request requestBody
    When method put
    Then status 200
    And response.params.status == "SUCCESSFUL"
# get entity by id
    Given url registryUrl
    And path 'api/v1/BirthCertificate1/' + birthCertificateOsid
    And header Authorization = admin_token
    When method get
    Then status 200
    And response.name == "test"
    And response._osSignedData.contains("test")
# get certificate for entity
    Given url registryUrl
    And path 'api/v1/BirthCertificate1/' + birthCertificateOsid
    And header Authorization = admin_token
    And header Accept = "text/html"
    And header template-key = "html"
    When method get
    Then status 200
    And response.length > 0
# get VC for entity
    Given url registryUrl
    And path 'api/v1/BirthCertificate1/' + birthCertificateOsid
    And header Authorization = admin_token
    And header Accept = "application/vc+ld+json"
    When method get
    Then status 200
    And response.credentialSubject.name == "test"
# revoke entity by id
    Given url registryUrl
    And path 'api/v1/BirthCertificate1/' + birthCertificateOsid + '/revoke'
    And header Authorization = admin_token
    When method post
    Then status 200
    And response.params.status == "SUCCESSFUL"
# get entity by id and check whether signed data got removed and still we are able to fetch the data
    Given url registryUrl
    And path 'api/v1/BirthCertificate1/' + birthCertificateOsid
    And header Authorization = admin_token
    When method get
    Then status 200
    And response._osSignedData.length = 0
# Try to revoke the same entity again it should inform that the VC is already revoked
    Given url registryUrl
    And path 'api/v1/BirthCertificate1/' + birthCertificateOsid + '/revoke'
    And header Authorization = admin_token
    When method post
    Then status 500
    And response.params.status == "UNSUCCESSFUL"
    And response.params.errmsg == "Credential is already revoked"
# Now try deleting the entity by id
    Given url registryUrl
    And path 'api/v1/BirthCertificate1/' + birthCertificateOsid
    And header Authorization = admin_token
    When method delete
    Then status 200
    And response.name == "test"
    And response.params.status == "SUCCESSFUL"
# get entity by id and check for its status
    Given url registryUrl
    And path 'api/v1/BirthCertificate1/' + birthCertificateOsid
    And header Authorization = admin_token
    When method get
    Then status 404
    And response.params.status == "UNSUCCESSFUL"
    And response.params.errmsg == "entity status is inactive"


  @env=async
  Scenario: Check if events are published
  # should get metrics
    * sleep(11000)
    Given url metricsUrl
    And path '/v1/metrics'
    When method get
    Then status 200
    And assert response.birthcertificate.READ == "7"
    And assert response.birthcertificate.UPDATE == "1"
    And assert response.birthcertificate.ADD == "1"
    And assert response.birthcertificate.DELETE == "1"

  @env=async
  Scenario: Check if notifications are sent
    Given url notificationsUrl
    And path '/notification-service/v1/notification'
    When method get
    Then status 200
    * def studentRequest = read('StudentRequest.json')
    * def notificationStudent = studentRequest.contact
    And print response[notificationStudent]
    And assert response[notificationStudent] != null

  @envnot=fusionauth
  Scenario: Test unique constraints with nested and composite fields
# create entity
    Given url registryUrl
    And path 'api/v1/TeacherUnique/invite'
    * def teacherRequest = read('TeacherUniqueRequest.json')
    And request teacherRequest
    When method post
    Then status 200
    # create entity with same identity details
    * sleep(3000)
    Given url registryUrl
    And path 'api/v1/TeacherUnique/invite'
    And request read('TeacherUniqueRequest.json')
    When method post
    Then status 500
    * match response.params.errmsg contains "java.lang.RuntimeException: org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint \"public_V_personal_details_email_sqlgIdx\"\n  Detail: Key (email)=(test2@rc.com) already exists."
    # create entity with different email, violates composite unique index
    Given url registryUrl
    And path 'api/v1/TeacherUnique/invite'
    * teacherRequest.personal_details.email = "xyz@rc.dev"
    And request teacherRequest
    When method post
    Then status 500
    * match response.params.errmsg contains "java.lang.RuntimeException: org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint \"public_V_identity_details_id_value_sqlgIdx\"\n  Detail: Key (id, value)=(id, 1) already exists."
    # create entity with different data
    Given url registryUrl
    And path 'api/v1/TeacherUnique/invite'
    * teacherRequest.personal_details.email = "xyz1@rc.dev"
    * teacherRequest.identity_details.id = "xyz"
    And request teacherRequest
    When method post
    Then status 200

