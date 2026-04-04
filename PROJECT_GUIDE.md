# TC Money Manager Project Guide

## What This Project Is

`tc-money-manager` is a Spring Boot backend for a personal finance application.

Core responsibilities:

- user registration and authentication
- email verification and password management
- user profile management
- category management
- transaction management
- default account balance tracking
- receipt/avatar file storage through S3

Tech stack at a glance:

- Java 21
- Spring Boot 4
- Spring MVC
- Spring Security with JWT
- Spring Data JPA + Hibernate
- Flyway
- MySQL
- AWS S3 SDK
- MapStruct
- Testcontainers + JUnit 5 + Mockito

---

## High-Level Architecture

The codebase is structured mostly by feature/module, not by layer-only packaging.

Main modules:

- `auth`
- `user`
- `category`
- `transaction`
- `account`
- `security`
- `common`
- `utils`

Typical request flow:

1. HTTP request hits a controller
2. Spring Security resolves the authenticated user from JWT
3. Controller delegates to a service
4. Service coordinates repositories, validators, mappers, and side effects
5. Repository persists/fetches data
6. Mapper converts entity to DTO
7. Exceptions are normalized by `GlobalExceptionHandler`

---

## Entry Point

Main application class:

- `src/main/java/org/tc/mtracker/MtrackerApplication.java`

It enables:

- Spring Boot auto-configuration
- configuration properties scanning via `@ConfigurationPropertiesScan`

That means typed config classes under `utils/config/properties` are discovered automatically.

---

## Package Map

### `auth`

Purpose:

- registration
- login
- refresh token flow
- email verification
- password reset/change

Main files:

- `auth/api/AuthenticationController.java`
- `auth/api/AuthenticationApi.java`
- `auth/api/UserIdentityController.java`
- `auth/service/RegistrationService.java`
- `auth/service/LoginService.java`
- `auth/service/RefreshTokenService.java`
- `auth/service/TokenRefreshService.java`
- `auth/service/EmailVerificationService.java`
- `auth/service/PasswordManagementService.java`
- `auth/mail/AuthEmailService.java`
- `auth/model/RefreshToken.java`
- `auth/repository/RefreshTokenRepository.java`

Mental model:

- `AuthenticationController` handles public auth endpoints under `/api/v1/auth`
- `UserIdentityController` handles authenticated user identity actions under `/api/v1/users`
- `RefreshTokenService` manages refresh token persistence and expiration
- `TokenRefreshService` is the orchestration layer for turning refresh token into new access token

### `user`

Purpose:

- user entity
- user profile fetch/update
- avatar handling
- current authenticated user lookup

Main files:

- `user/User.java`
- `user/UserController.java`
- `user/UserService.java`
- `user/UserRepository.java`
- `user/dto/*`

Mental model:

- `UserService` is the main place for “who is the current user” and “update profile” logic
- `UserController` only exposes `/api/v1/users/me`

### `category`

Purpose:

- custom and global categories
- filtering by type/name/status
- category ownership rules

Main files:

- `category/Category.java`
- `category/CategoryController.java`
- `category/CategoryService.java`
- `category/CategoryRepository.java`
- `category/CategoryMapper.java`
- `category/enums/CategoryStatus.java`

Mental model:

- categories can be global (`user_id IS NULL`) or user-owned
- access rules are enforced in repository/service methods
- delete means archive, not physical removal

### `transaction`

Purpose:

- create/update/delete transactions
- transaction filters
- balance recalculation
- receipt attachment support

Main files:

- `transaction/Transaction.java`
- `transaction/TransactionController.java`
- `transaction/TransactionService.java`
- `transaction/TransactionRepository.java`
- `transaction/ReceiptImage.java`
- `transaction/dto/*`

Mental model:

- this is the most business-heavy module
- transaction writes also update account balances
- category type must match transaction type
- archived categories cannot be used

### `account`

Purpose:

- default account provisioning
- default account balance retrieval

Main files:

- `account/Account.java`
- `account/AccountController.java`
- `account/AccountService.java`
- `account/DefaultAccountProvisioningService.java`
- `account/AccountRepository.java`

Mental model:

- every user gets a default account during registration
- account balance is derived incrementally through transaction writes

### `security`

Purpose:

- JWT parsing and validation
- request authentication
- security filter chain
- Spring Security `UserDetails`

Main files:

- `security/SecurityConfig.java`
- `security/JwtAuthenticationFilter.java`
- `security/JwtService.java`
- `security/CustomUserDetails.java`
- `security/CustomUserDetailsService.java`

Mental model:

- stateless JWT auth
- all endpoints are authenticated by default except explicit public routes
- JWT subject is user email

### `common`

Purpose:

- shared validators and enums

Main files:

- `common/enums/TransactionType.java`
- `common/image/*`
- `common/receipt/*`

Mental model:

- file validation rules live here
- common enums used across modules also live here

### `utils`

Purpose:

- S3 integration
- configuration
- exception handling

Main files:

- `utils/S3Service.java`
- `utils/config/S3Config.java`
- `utils/config/OpenAPIConfig.java`
- `utils/config/properties/*`
- `utils/exceptions/*`

Mental model:

- `S3Service` is the abstraction used by the domain services
- `GlobalExceptionHandler` converts exceptions into `ProblemDetail`

---

## Endpoint Map

### Public auth endpoints

Base path:

- `/api/v1/auth`

Main endpoints:

- `POST /sign-up`
- `POST /login`
- `POST /reset-token`
- `POST /getTokenToResetPassword`
- `POST /reset-password/confirm`
- `GET /verify`
- `POST /refresh`

Main implementation:

- `auth/api/AuthenticationController.java`
- contract annotations in `auth/api/AuthenticationApi.java`

### Authenticated user endpoints

Base path:

- `/api/v1/users`

Main endpoints:

- `GET /me`
- `PUT /me`
- `POST /me/update-email`
- `PUT /me/update-password`
- `GET /verify-email`

Main implementation:

- `user/UserController.java`
- `auth/api/UserIdentityController.java`

### Category endpoints

Base path:

- `/api/v1/categories`

Main endpoints:

- `GET /`
- `GET /{categoryId}`
- `POST /`
- `PUT /{categoryId}`
- `DELETE /{categoryId}`

### Transaction endpoints

Base path:

- `/api/v1/transactions`

Main endpoints:

- `GET /`
- `GET /{transactionId}`
- `POST /`
- `PUT /{transactionId}`
- `DELETE /{transactionId}`

### Account endpoints

Base path:

- `/api/v1/accounts`

Main endpoints:

- `GET /default`

---

## Security Rules

Main configuration file:

- `src/main/java/org/tc/mtracker/security/SecurityConfig.java`

Important behavior:

- CSRF disabled
- stateless session management
- JWT filter inserted before `UsernamePasswordAuthenticationFilter`
- public routes:
    - `/error`
    - `/api/v1/auth/**`
    - `/api/v1/users/verify-email`
    - swagger/api-docs endpoints
- everything else requires authentication

JWT flow:

1. `JwtAuthenticationFilter` reads `Authorization: Bearer <token>`
2. token subject is extracted as user email
3. `CustomUserDetailsService` loads the user
4. `JwtService` validates token
5. security context is populated

Important note:

- auth identity is currently email-based

---

## Core Business Rules

### Registration

Handled by:

- `auth/service/RegistrationService.java`

Rules:

- email must be unique
- password is encoded with BCrypt
- avatar upload is optional
- default account is provisioned immediately after user creation
- user starts as not activated
- verification email is sent after registration

### Login

Handled by:

- `auth/service/LoginService.java`

Rules:

- user must exist
- user must be activated
- password must match
- returns access token + refresh token

### Email Verification

Handled by:

- `auth/service/EmailVerificationService.java`

Rules:

- token must have purpose `email_verification`
- already-activated users are rejected
- successful verification activates user and returns access + refresh tokens

### Password Reset / Update

Handled by:

- `auth/service/PasswordManagementService.java`

Rules:

- reset token must have purpose `password_reset`
- password/confirmation must match
- update password requires current password match
- successful update triggers notification email

### Categories

Handled by:

- `category/service/CategoryService.java`

Rules:

- visible categories are either global or owned by current user
- create/update rejects duplicate `name + type` for same visibility scope
- delete archives category instead of removing it
- archived categories can be filtered separately

### Transactions

Handled by:

- `transaction/service/TransactionService.java`

Rules:

- if `accountId` is absent, use the user’s default account
- category must be accessible
- category must be active
- category type must match transaction type
- creating/updating/deleting transaction must update account balance
- receipts are optional
- receipts are stored in S3 by generated UUID key

### Accounts

Handled by:

- `account/service/AccountService.java`
- `account/service/DefaultAccountProvisioningService.java`

Rules:

- user should have a default account
- registration provisions default account with zero balance
- querying default account returns current stored balance

---

## Persistence Model

Main entities:

- `User`
- `Account`
- `Category`
- `Transaction`
- `ReceiptImage`
- `RefreshToken`

Important relationships:

- `User` -> many `Account`
- `User` -> one default `Account`
- `Category` -> optional `User` owner
- `Transaction` -> one `User`
- `Transaction` -> one `Account`
- `Transaction` -> one `Category`
- `Transaction` -> many `ReceiptImage`
- `RefreshToken` -> one `User`

Things worth remembering:

- categories can be global because `user_id` is nullable
- receipt image primary key is UUID
- transactions use soft-delete semantics in repository queries through `deletedAt`
- account balance is stored, not recomputed on every read

---

## Database Migrations

Flyway migrations:

- `V1__users_table_creation.sql`
- `V2__create_refresh_tokens_table.sql`
- `V3__create_categories_table.sql`
- `V4__create_transaction_table.sql`
- `V5__create_receipt_images_table.sql`
- `V6__create_accounts_and_link_transactions.sql`

Reading order matters.

If you need to understand the schema quickly:

1. users
2. refresh tokens
3. categories
4. transactions
5. receipt images
6. accounts + links

---

## Configuration Model

Main config files:

- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/test/resources/application-test.yml`

Current design:

- `application.yml` is the canonical config structure
- `application-dev.yml` contains only local-development overrides
- `application-test.yml` is dedicated to tests

Typed properties:

- `utils/config/properties/JwtProperties.java`
- `utils/config/properties/AwsProperties.java`
- `utils/config/properties/AppProperties.java`

Important property groups:

- `security.jwt.*`
- `aws.*`
- `app.*`

Important note:

- base config supports both `MT_*` and old `L_MT_*` env vars through fallback placeholders

---

## File Storage

Main files:

- `utils/S3Service.java`
- `utils/config/S3Config.java`

Current behavior:

- avatars and receipts are stored in S3
- persisted key is later converted to a presigned download URL
- services call `saveFile`, `generatePresignedUrl`, and `deleteFile`

Where used:

- registration avatar upload
- user avatar update
- transaction receipt upload/delete

---

## Mapping Layer

MapStruct mappers:

- `auth/mapper/RegistrationMapper.java`
- `category/CategoryMapper.java`
- `transaction/dto/TransactionMapper.java`
- `user/dto/UserMapper.java`

Why it matters:

- service logic stays cleaner
- DTO conversion is centralized
- update logic is easier to track, especially for user and transaction updates

Things to watch:

- generated mapper code lives under `target/generated-sources/annotations`
- if mapper compilation gets weird, `./mvnw clean test-compile` usually fixes stale generated output

---

## Exception Handling

Main file:

- `utils/exceptions/GlobalExceptionHandler.java`

What it does:

- catches domain and validation exceptions
- returns `ProblemDetail`
- maps domain exceptions to HTTP status codes

Examples:

- `UserAlreadyExistsException` -> `409`
- `BadCredentialsException` -> `401`
- `UserNotActivatedException` -> `403`
- `CategoryNotFoundException` -> `404`
- `MoneyFlowTypeMismatchException` -> `400`

This is the main place to update if API error behavior changes.

---

## Test Architecture

The test suite is layered.

### Unit

Path:

- `src/test/java/org/tc/mtracker/unit`

Purpose:

- verify isolated business logic
- no Spring context
- no DB

Examples:

- `unit/transaction/TransactionServiceTest.java`
- `unit/auth/LoginServiceTest.java`

### Integration Repository

Path:

- `src/test/java/org/tc/mtracker/integration/repository`

Purpose:

- verify repository queries against real MySQL

Examples:

- `integration/repository/CategoryRepositoryTest.java`
- `integration/repository/TransactionRepositoryTest.java`

### Integration API

Path:

- `src/test/java/org/tc/mtracker/integration/api`

Purpose:

- verify HTTP, security, validation, persistence side effects

Examples:

- `integration/api/AuthApiTest.java`
- `integration/api/TransactionApiTest.java`

### Test Support

Path:

- `src/test/java/org/tc/mtracker/support`

Important files:

- `support/base/BaseApiIntegrationTest.java`
- `support/base/BaseRepositoryIntegrationTest.java`
- `support/factory/DatabaseTestDataFactory.java`
- `support/factory/EntityTestFactory.java`
- `support/factory/JwtTestTokenFactory.java`
- `support/factory/MultipartTestResourceFactory.java`

Testcontainers setup:

- `src/test/java/org/tc/mtracker/TestcontainersConfiguration.java`

Current test commands:

```bash
./mvnw test
./mvnw test -Punit
./mvnw test -Pintegration
```

Important note:

- integration tests require Docker because they use Testcontainers

---

## Where To Look For Common Changes

If you need to change authentication:

- `security/*`
- `auth/service/*`
- `auth/api/*`

If you need to change user profile behavior:

- `user/UserService.java`
- `user/UserController.java`
- `auth/api/UserIdentityController.java`

If you need to change category visibility rules:

- `category/CategoryRepository.java`
- `category/CategoryService.java`

If you need to change balance behavior:

- `transaction/TransactionService.java`
- `account/AccountService.java`
- `account/DefaultAccountProvisioningService.java`

If you need to change file upload behavior:

- `common/image/*`
- `common/receipt/*`
- `utils/S3Service.java`

If you need to change API error responses:

- `utils/exceptions/GlobalExceptionHandler.java`

If you need to change config shape:

- `utils/config/properties/*`
- `application.yml`
- `application-dev.yml`
- `application-test.yml`

---

## Known Design Tradeoffs / Notes

- user identity is email-based, not id-based
- account balances are stored and adjusted incrementally, which is fast but requires careful transaction logic
- delete for categories is archive, not physical delete
- transactions are queried as active only through `deletedAt IS NULL`
- `application.yml` still supports old `L_MT_*` env names as fallback for compatibility
- e2e tests are intentionally not present right now

---

## Suggested Next Internal Improvements

If you return to this later, these are sensible next steps:

- add `.gitignore` entries for `.DS_Store`
- eventually remove legacy `L_MT_*` env fallbacks
- consider reducing Hibernate warning noise in tests
- add a small `.env.example` or local run cheat sheet
- add pagination if transactions/categories grow significantly

---

## Personal Quick Start

If you come back to this project after a break, read in this order:

1. `PROJECT_GUIDE.md`
2. `src/main/java/org/tc/mtracker/security/SecurityConfig.java`
3. `src/main/java/org/tc/mtracker/auth/service/RegistrationService.java`
4. `src/main/java/org/tc/mtracker/transaction/TransactionService.java`
5. `src/main/java/org/tc/mtracker/category/CategoryService.java`
6. `src/test/java/org/tc/mtracker/integration/api/AuthApiTest.java`
7. `src/test/java/org/tc/mtracker/integration/api/TransactionApiTest.java`

That path restores most of the project context quickly.
