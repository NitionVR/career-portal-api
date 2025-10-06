# Job Post Permissions

This document describes the permission model for job post operations in the Etalente platform.

## Permission Matrix

| Operation | Hiring Manager | Recruiter | Candidate |
|-----------|----------------|-----------|-----------|
| Create Job Post | Yes | No | No |
| View Draft Job Post (own org) | Yes | Yes | No |
| View Public Job Post | Yes | Yes | Yes |
| Update Job Post (own org) | Yes | Yes | No |
| Delete Job Post (own only) | Yes | No | No |
| Publish Job Post (own org) | Yes | Yes | No |
| Close Job Post (own org) | Yes | Yes | No |
| Change Status (own org) | Yes | Yes | No |

## Permission Rules

### Create
- **Only Hiring Managers** can create new job posts
- Job posts are automatically associated with the creator's organization
- New job posts start in `DRAFT` status

### View
- **Public job posts** (status = `OPEN`) can be viewed by everyone, including unauthenticated users
- **Non-public job posts** can only be viewed by members of the same organization
- Candidates cannot view draft job posts from any organization

### Update
- **Hiring Managers** can update any job post in their organization
- **Recruiters** can update any job post in their organization
- Users cannot update job posts from other organizations

### Delete
- **Only Hiring Managers** can delete job posts
- Hiring managers can only delete their **own** job posts
- Recruiters cannot delete any job posts

### Status Changes (Publish/Close/Archive)
- **Hiring Managers** can change the status of any job post in their organization
- **Recruiters** can change the status of any job post in their organization
- Status changes are subject to state machine rules (see JOB-002)

## Organization Boundaries

All operations respect organization boundaries:
- Users can only access job posts within their organization (except public posts)
- Cross-organization access is prevented at both the service and repository layers
- Candidates don't belong to organizations and have read-only access to public posts

## Error Messages

The system provides clear error messages for permission violations:

- `"Only hiring managers can create job posts"` - When recruiter/candidate tries to create
- `"Only hiring managers can delete job posts"` - When recruiter/candidate tries to delete
- `"You can only delete your own job posts"` - When HM tries to delete another HM's post
- `"You can only update job posts in your organization"` - Cross-organization update attempt
- `"Job post not found in your organization"` - When accessing another org's post
- `"You don't have access to this job post"` - When candidate tries to view draft post

## Implementation Details

### Permission Service
The `JobPostPermissionService` centralizes all permission logic:
- `verifyCanCreate(User)` - Checks create permission
- `verifyCanUpdate(User, JobPost)` - Checks update permission
- `verifyCanDelete(User, JobPost)` - Checks delete permission
- `verifyCanChangeStatus(User, JobPost)` - Checks status change permission
- `canView(User, JobPost)` - Checks view permission

### Controller Security
The `JobPostController` uses Spring Security's `@PreAuthorize` annotations:
```java
@PreAuthorize("hasRole('HIRING_MANAGER')") // Only hiring managers
@PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')") // Both roles
// No annotation = public access
```

### Service Layer
The service layer provides an additional security layer with detailed business logic checks.

### Testing
Comprehensive tests cover:

- All permission combinations for all operations
- Cross-organization access prevention
- Error message validation
- Controller-level and service-level tests
- Integration tests with real database

## Summary

### Implemented Features:

- JobPostPermissionService - Centralized permission logic
- Updated JobPostService - Permission checks in business logic
- Updated JobPostController - @PreAuthorize annotations for recruiters
- New Status Management Endpoints - publish, close, updateStatus
- Comprehensive Tests - 40+ test cases covering all scenarios
- Documentation - Clear permission matrix and rules

### Acceptance Criteria Met:

- Recruiters can publish, advance stages, and close job posts within their organization
- Recruiters cannot create or delete job posts
- Role-based authorization (@PreAuthorize) applied to relevant methods

### Security Features:

- Organization boundary enforcement
- Clear error messages
- Multiple layers of security (controller, service, repository)
- Comprehensive test coverage
