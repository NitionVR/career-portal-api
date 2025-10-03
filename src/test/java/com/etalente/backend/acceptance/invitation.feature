# language: gherkin
Feature: Recruiter Invitation

  Background:
    * url baseUrl
    * def faker = Java.type('com.github.javafaker.Faker')
    * def Random = Java.type('java.util.Random')
    * def random = new Random()
    * def hiringManagerEmail = 'hm' + random.nextInt(100000) + '@test.com'
    * def recruiterEmail = 'recruiter' + random.nextInt(100000) + '@test.com'

  Scenario: A hiring manager invites a recruiter who successfully registers
    # 1. Register a new Hiring Manager
    Given path '/api/auth/register'
    And request { email: '#(hiringManagerEmail)', role: 'HIRING_MANAGER' }
    When method POST
    Then status 200

    # 2. Get the registration token for the Hiring Manager (using the test endpoint)
    Given path '/api/test/token'
    And param email = hiringManagerEmail
    When method GET
    Then status 200
    * def hmRegToken = response.token

    # 3. Complete Hiring Manager registration
    Given path '/api/auth/register/hiring-manager'
    And param token = hmRegToken
    And request
    """
    {
      "username": "hm_user_#(random.nextInt(100000))",
      "companyName": "Space Corp",
      "industry": "Aerospace",
      "contactPerson": "Elon Tusk",
      "contactNumber": "+1234567890"
    }
    """
    When method POST
    Then status 200
    And match response.token == '#string'
    * def hmJwt = response.token

    # 4. Hiring Manager invites a Recruiter
    Given path '/api/invitations/recruiter'
    And header Authorization = 'Bearer ' + hmJwt
    And request { email: '#(recruiterEmail)', personalMessage: 'Join my team!' }
    When method POST
    Then status 201
    And match response.email == recruiterEmail
    And match response.status == 'PENDING'

    # 5. Get the invitation token for the Recruiter
    Given path '/api/test/token'
    And param email = recruiterEmail
    When method GET
    Then status 200
    * def recruiterInvToken = response.token
    * print 'Recruiter Invitation Token:', recruiterInvToken

    # 6. Validate the invitation token
    Given path '/api/invitations/validate/' + recruiterInvToken
    When method GET
    Then status 200
    And match response.valid == true
    And match response.email == recruiterEmail
    And match response.organization == "Space Corp"

    # 7. Recruiter accepts the invitation and completes their profile
    Given path '/api/invitations/accept/' + recruiterInvToken
    And request
    """
    {
      "username": "recruiter_user_#(random.nextInt(100000))",
      "firstName": "Jane",
      "lastName": "Recruiter",
      "contactNumber": "+1987654321"
    }
    """
    When method POST
    Then status 200
    And match response.token == '#string'
    And match response.message == 'Invitation accepted successfully'
    * def recruiterToken = response.token
    * print 'Recruiter JWT Token:', recruiterToken

    # 8. Verify the new recruiter's token is valid and contains the correct role
    * def jwtClaims = read('classpath:com/etalente/backend/acceptance/jwt-claims.js')
    * def claims = call jwtClaims recruiterToken
    And match claims.role == 'RECRUITER'
