# language: gherkin
Feature: User Registration

  Background:
    * url 'http://localhost:' + karate.properties['local.server.port']
    * def randomEmail = 'testuser-' + java.util.UUID.randomUUID() + '@example.com'
    * def randomUsername = 'user-' + java.util.UUID.randomUUID()

  Scenario: Successfully register a new candidate
    # Step 1: Initiate registration
    Given path '/api/auth/register'
    And request { email: '#(randomEmail)', role: 'CANDIDATE' }
    When method post
    Then status 200
    And match response.message == "Registration link sent to your email"

    # Step 2: Retrieve the registration token using the test endpoint
    Given path '/test/latest-registration-token'
    And param email = randomEmail
    When method get
    Then status 200
    And def registrationToken = response.token

    # Step 3: Validate the token (optional, but good practice)
    Given path '/api/auth/validate-registration-token'
    And param token = registrationToken
    When method get
    Then status 200
    And match response.email == randomEmail
    And match response.role == 'CANDIDATE'
    And match response.valid == true

    # Step 4: Complete the candidate registration
    Given path '/api/auth/register/candidate'
    And param token = registrationToken
    And request
    """
    {
      "username": "#(randomUsername)",
      "firstName": "Test",
      "lastName": "User",
      "contactNumber": "+1234567890"
    }
    """
    When method post
    Then status 200
    And match response.token != null

    # Step 5: Verify the token is a valid JWT (optional)
    * def claims = karate.read('classpath:com/etalente/backend/acceptance/jwt-claims.js')
    * def payload = claims(response.token)
    * match payload.username == randomUsername
    * match payload.role == 'CANDIDATE'
    * match payload.sub == randomEmail
