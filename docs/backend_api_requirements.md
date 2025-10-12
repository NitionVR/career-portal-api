# Backend API Requirements for Job Post Filtering & Searching

This document outlines the required changes for the backend API to support server-side searching and filtering of job posts.

## Objective

To move the filtering and searching logic from the client-side to the server-side for improved performance and scalability. The frontend will pass user selections as query parameters, and the backend will be responsible for returning the filtered and paginated results.

## Endpoint to Modify

`GET /api/job-posts`

## Current Functionality

The endpoint currently only accepts `pageable` parameters for pagination and sorting (e.g., `page`, `size`, `sort`).

## Required Enhancements

The endpoint must be updated to accept the following **optional** query parameters:

| Parameter          | Type           | Description                                                                                                | Example Usage                                  |
| ------------------ | -------------- | ---------------------------------------------------------------------------------------------------------- | ---------------------------------------------- |
| `search`           | `string`       | A general search term to be matched against job `title` and `description`.                               | `?search=React%20Developer`                    |
| `skillSearch`      | `string`       | A search term to be matched against the `name` of the skills within a job's `skills` array.              | `?skillSearch=typescript`                      |
| `experienceLevels` | `string`       | A comma-separated list of experience levels to filter by. Should match if the job has any of the specified levels. | `?experienceLevels=entry,mid`                  |
| `jobTypes`         | `string`       | A comma-separated list of job types (contracts). Should match if the job has any of the specified types.   | `?jobTypes=full-time,contract`                 |
| `workTypes`        | `string`       | A comma-separated list of work types. Should match if the job has any of the specified types.            | `?workTypes=remote,hybrid`                     |

### Combined Example

A request to get the first page of senior-level, full-time React developer jobs would look like this:

```
GET /api/job-posts?page=0&size=10&search=React%20Developer&experienceLevels=senior&jobTypes=full-time
```

## Expected Response

The endpoint should continue to return a `PageJobPostResponse` object. The `content` array within this object must only contain `JobPostResponse` objects that match **all** of the provided filter criteria. The pagination fields (`totalPages`, `totalElements`, etc.) should also be updated to reflect the filtered result set, not the entire dataset.
