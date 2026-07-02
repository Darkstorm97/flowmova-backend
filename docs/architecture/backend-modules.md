# Backend Modules

FlowMova backend follows a modular monolith approach. The application is deployed as one Spring Boot service, while the code is organized by business capabilities with explicit package boundaries.

## Module Layout

Each business module uses the same internal package structure:

- `api`: REST controllers, request/response DTOs, and API-specific mapping.
- `application`: use-case orchestration and transaction boundaries.
- `domain`: business rules, domain entities, value objects, and domain contracts.
- `infrastructure`: persistence, technical adapters, external integrations, and framework-specific implementations.

## Modules

### `auth`

Handles authentication use cases from FS-002:

- account registration entry points related to authentication
- login
- JWT creation and validation
- password reset
- authentication security adapters

### `user`

Handles user account and profile use cases from FS-002:

- user account data
- profile consultation and update
- password change
- user status lifecycle

### `companyaccess`

Handles company membership and role use cases from FS-002:

- user-company associations
- roles `ADMIN` and `EMPLOYEE`
- membership status lifecycle
- company-scoped authorization checks
- protection of the last administrator

### `shared`

Contains cross-module support that is not owned by a single business module:

- API error model
- validation support
- audit support
- persistence conventions
- common domain primitives
- shared configuration

## Dependency Direction

Modules should prefer this internal dependency direction:

```text
api -> application -> domain
infrastructure -> application/domain
```

Business modules should not access another module's internal implementation directly. Cross-module collaboration should happen through explicit application or domain contracts when needed.
