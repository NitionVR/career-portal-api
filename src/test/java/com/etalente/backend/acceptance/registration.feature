# language: gherkin
Feature: User Registration

  Background:
    * url baseUrl
    * def randomEmail = 'testuser-' + java.util.UUID.randomUUID() + '@example.com'
    * def randomUsername = 'user-' + java.util.UUID.randomUUID()

  Scenario: Successfully register a new candidate
    # Step 1: Initiate registration
    Given path '/api/register'
    And request { email: '#(randomEmail)', role: 'CANDIDATE' }
    When method post
    Then status 200
    And match response.message == "Registration link sent to your email"

    # Step 2: Retrieve the registration token using the test endpoint
    Given path '/api/test/token'
    And param email = randomEmail
    When method get
    Then status 200
    And def registrationToken = response.token

    # Step 3: Validate the token
    Given path '/api/register/validate-token'
    And param token = registrationToken
    When method get
    Then status 200
    And match response.email == randomEmail
    And match response.role == 'CANDIDATE'
    And match response.valid == true

    # Step 4: Complete the candidate registration
    Given path '/api/register/candidate'
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

    # Step 5: Verify the token is a valid JWT
    * def jwtClaims = read('classpath:com/etalente/backend/acceptance/jwt-claims.js')
    * def payload = call jwtClaims response.token
    * match payload.username == randomUsername
    * match payload.role == 'CANDIDATE'
    * match payload.email == randomEmail
