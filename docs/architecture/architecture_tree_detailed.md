```
.
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── etalente
│   │   │           └── backend
│   │   │               ├── EtalenteBackendApplication.java
│   │   │               ├── config
│   │   │               │   ├── CorsProperties.java
│   │   │               │   ├── JobPostSeeder.java
│   │   │               │   ├── JpaConfig.java
│   │   │               │   ├── MailConfig.java
│   │   │               │   ├── S3Config.java
│   │   │               │   └── UploadProperties.java
│   │   │               ├── controller
│   │   │               │   ├── AuthenticationController.java
│   │   │               │   ├── InvitationController.java
│   │   │               │   ├── JobApplicationController.java
│   │   │               │   ├── JobPostController.java
│   │   │               │   ├── NotificationController.java
│   │   │               │   ├── OrganizationController.java
│   │   │               │   ├── ProfileController.java
│   │   │               │   ├── RegistrationController.java
│   │   │               │   └── WorkflowController.java
│   │   │               ├── dto
│   │   │               │   ├── AcceptInvitationRequest.java
│   │   │               │   ├── ApplicationDetailsDto.java
│   │   │               │   ├── ApplicationSummaryDto.java
│   │   │               │   ├── ApplicationTransitionRequest.java
│   │   │               │   ├── CandidateProfileDto.java
│   │   │               │   ├── CandidateRegistrationDto.java
│   │   │               │   ├── CandidateRegistrationRequest.java
│   │   │               │   ├── CompleteProfileRequest.java
│   │   │               │   ├── EmployerApplicationSummaryDto.java
│   │   │               │   ├── HiringManagerRegistrationDto.java
│   │   │               │   ├── HiringManagerRegistrationRequest.java
│   │   │               │   ├── JobPostDto.java
│   │   │               │   ├── JobPostRequest.java
│   │   │               │   ├── JobPostResponse.java
│   │   │               │   ├── LocationDto.java
│   │   │               │   ├── LoginRequest.java
│   │   │               │   ├── NotificationRequest.java
│   │   │               │   ├── NotificationResponse.java
│   │   │               │   ├── OrganizationDto.java
│   │   │               │   ├── RecruiterInvitationDto.java
│   │   │               │   ├── RecruiterInvitationRequest.java
│   │   │               │   ├── RegistrationRequest.java
│   │   │               │   ├── RegistrationResponse.java
│   │   │               │   ├── SessionResponse.java
│   │   │               │   ├── SkillDto.java
│   │   │               │   ├── StateAuditResponse.java
│   │   │               │   ├── StateTransitionRequest.java
│   │   │               │   ├── UpdateAvatarRequest.java
│   │   │               │   ├── UpdateCompanyLogoRequest.java
│   │   │               │   ├── UploadUrlRequest.java
│   │   │               │   ├── UploadUrlResponse.java
│   │   │               │   ├── UserDto.java
│   │   │               │   ├── VerifyTokenResponse.java
│   │   │               │   ├── WorkflowTriggerRequest.java
│   │   │               │   └── WorkflowTriggerResponse.java
│   │   │               ├── exception
│   │   │               │   ├── BadRequestException.java
│   │   │               │   ├── GlobalExceptionHandler.java
│   │   │               │   ├── ResourceNotFoundException.java
│   │   │               │   └── UnauthorizedException.java
│   │   │               ├── integration
│   │   │               │   └── novu
│   │   │               │       ├── NovuClientConfig.java
│   │   │               │       ├── NovuSubscriberService.java
│   │   │               │       ├── NovuWorkflowCreator.java
│   │   │               │       └── NovuWorkflowService.java
│   │   │               ├── model
│   │   │               │   ├── InvitationStatus.java
│   │   │               │   ├── JobApplication.java
│   │   │               │   ├── JobApplicationAudit.java
│   │   │               │   ├── JobApplicationStateTransition.java
│   │   │               │   ├── JobApplicationStatus.java
│   │   │               │   ├── JobPost.java
│   │   │               │   ├── JobPostStateAudit.java
│   │   │               │   ├── JobPostStatus.java
│   │   │               │   ├── Notification.java
│   │   │               │   ├── NotificationDeliveryLog.java
│   │   │               │   ├── NotificationPreference.java
│   │   │               │   ├── NotificationStatus.java
│   │   │               │   ├── OneTimeToken.java
│   │   │               │   ├── Organization.java
│   │   │               │   ├── RecruiterInvitation.java
│   │   │               │   ├── RegistrationToken.java
│   │   │               │   ├── Role.java
│   │   │               │   ├── StateTransition.java
│   │   │               │   └── User.java
│   │   │               ├── repository
│   │   │               │   ├── JobApplicationAuditRepository.java
│   │   │               │   ├── JobApplicationRepository.java
│   │   │               │   ├── JobApplicationSpecification.java
│   │   │               │   ├── JobPostRepository.java
│   │   │               │   ├── JobPostSpecification.java
│   │   │               │   ├── JobPostStateAuditRepository.java
│   │   │               │   ├── NotificationDeliveryLogRepository.java
│   │   │               │   ├── NotificationPreferenceRepository.java
│   │   │               │   ├── NotificationRepository.java
│   │   │               │   ├── OneTimeTokenRepository.java
│   │   │               │   ├── OrganizationRepository.java
│   │   │               │   ├── RecruiterInvitationRepository.java
│   │   │               │   ├── RegistrationTokenRepository.java
│   │   │               │   └── UserRepository.java
│   │   │               ├── security
│   │   │               │   ├── JwtAuthenticationFilter.java
│   │   │               │   ├── JwtService.java
│   │   │               │   ├── OrganizationContext.java
│   │   │               │   ├── SecurityConfig.java
│   │   │               │   ├── TestSecurityConfig.java
│   │   │               │   └── UserDetailsServiceImpl.java
│   │   │               └── service
│   │   │                   ├── AuthenticationService.java
│   │   │                   ├── EmailSender.java
│   │   │                   ├── EmailService.java
│   │   │                   ├── InvitationService.java
│   │   │                   ├── JobApplicationService.java
│   │   │                   ├── JobPostPermissionService.java
│   │   │                   ├── JobPostService.java
│   │   │                   ├── JobPostStateMachine.java
│   │   │                   ├── NotificationPreferenceService.java
│   │   │                   ├── NotificationService.java
│   │   │                   ├── NovuNotificationService.java
│   │   │                   ├── OrganizationService.java
│   │   │                   ├── ProfileService.java
│   │   │                   ├── RegistrationService.java
│   │   │                   ├── S3Service.java
│   │   │                   ├── TokenStore.java
│   │   │                   └── impl
│   │   │                       ├── AuthenticationServiceImpl.java
│   │   │                       ├── InvitationServiceImpl.java
│   │   │                       ├── JobApplicationServiceImpl.java
│   │   │                       ├── JobPostServiceImpl.java
│   │   │                       ├── JobPostStateMachineImpl.java
│   │   │                       ├── NovuNotificationServiceImpl.java
│   │   │                       ├── ProfileServiceImpl.java
│   │   │                       ├── RegistrationServiceImpl.java
│   │   │                       ├── SesEmailSender.java
│   │   │                       └── SmtpEmailSender.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── db
│   │       │   └── migration
│   │       └── templates
│   └── test
│       └── java
│           └── com
│               └── etalente
│                   └── backend
│                       ├── BaseIntegrationTest.java
│                       ├── EtalenteBackendApplicationTests.java
│                       ├── TestHelper.java
│                       ├── acceptance
│                       │   └── AcceptanceTest.java
│                       ├── config
│                       │   ├── TestAwsConfig.java
│                       │   ├── TestEmailConfig.java
│                       │   └── TestTokenStore.java
│                       ├── controller
│                       │   ├── AuthenticationControllerTest.java
│                       │   ├── AuthenticationFlowControllerTest.java
│                       │   ├── InvitationControllerTest.java
│                       │   ├── JobApplicationControllerTest.java
│                       │   ├── JobApplicationTransitionControllerTest.java
│                       │   ├── JobPostControllerTest.java
│                       │   ├── JobPostPermissionControllerTest.java
│                       │   ├── JobPostStateMachineControllerTest.java
│                       │   ├── OrganizationControllerTest.java
│                       │   ├── ProfileControllerTest.java
│                       │   ├── RegistrationControllerTest.java
│                       │   └── TestHelperController.java
│                       ├── security
│                       │   ├── JwtServiceTest.java
│                       │   └── OrganizationContextTest.java
│                       └── service
│                           ├── JobApplicationServiceTest.java
│                           ├── JobPostFilteringTest.java
│                           ├── JobPostMultiTenancyTest.java
│                           ├── JobPostPermissionErrorHandlingTest.java
│                           ├── JobPostPermissionTest.java
│                           ├── JobPostStateMachineEdgeCasesTest.java
│                           ├── JobPostStateMachineTest.java
│                           └── impl
│                               ├── AuthenticationServiceImplTest.java
│                               ├── InvitationServiceImplTest.java
│                               ├── JobPostServiceImplTest.java
│                               ├── ProfileServiceImplTest.java
│                               └── RegistrationServiceImplTest.java
├── aws
│   └── cloudformation
│       ├── application
│       │   └── ecs-service.yaml
│       └── shared
│           ├── alb.yaml
│           ├── ecr-repositories.yaml
│           ├── ecs-cluster.yaml
│           ├── networking.yaml
│           ├── rds.yaml
│           ├── s3.yaml
│           └── security-groups.yaml
├── docs
├── build.gradle
└── README.md
```