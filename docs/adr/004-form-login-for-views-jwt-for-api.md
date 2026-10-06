# ADR-004: Form login for views and JWT for the API

- **Status:** Accepted
- **Date:** 2026-10-05
- **Related:** [#29](https://github.com/angelgonzalezg/event-iq/issues/29), [ADR-006](006-functional-modules-owned-end-to-end.md)

## Context

The application has two kinds of clients:

- **Browsers** navigating the Thymeleaf views. A browser sends its cookies with every request, but it does not add an `Authorization` header when it follows a link or submits a form.
- **API clients** calling `/api/**`, such as the Postman collection each module maintains. They set headers explicitly and have no use for a login page or a session.

One administrator account is enough, and the security story has to protect the endpoints of both modules without each module writing its own rules.

## Decision

The security story (#29) implements this design: the `security` package declares two `SecurityFilterChain` beans, and no other package defines security rules.

- **API (`/api/**`):** registered first (`@Order(1)` and `securityMatcher("/api/**")`) and stateless. `POST /api/auth/login` is public and returns a JWT; every other endpoint requires `Authorization: Bearer <token>`. Tokens are issued with a `JwtEncoder` and validated by Spring Security's OAuth2 Resource Server, both with an HS256 key from `JWT_SECRET` (at least 32 random characters). CSRF protection is off, because no cookie authenticates these requests.
- **Views (every other path):** form login at `/login` with a session, and CSRF protection on; Thymeleaf adds the token to forms. `/login`, `/css/**`, `/img/**` and `/error` are public.

The administrator is the only user, kept in an `InMemoryUserDetailsManager` with its password from `ADMIN_PASSWORD`. Because the API chain matches every `/api/**` path, new endpoints in either module are protected without extra configuration.

Until that story is merged, a temporary `DevSecurityConfig` permits every request so the CRUD can be tested from Postman; the story deletes it.

## Alternatives considered

- **JWT for everything:** the views would need JavaScript to attach the token to every request, or a JWT stored in a cookie with its own CSRF handling. More complexity for server-rendered pages, with no benefit.
- **The jjwt library:** an external dependency whose version is managed by hand. OAuth2 Resource Server is part of Spring Security, Spring Boot manages its version, and it includes the Nimbus classes that encode and decode tokens.

## Consequences

- Each kind of client uses its standard mechanism, and no extra library is needed.
- There are two chains to understand and test, and their order matters: the view chain matches every request, so if it came first, API calls would be redirected to the login page instead of receiving a 401.
- With a symmetric key, anyone who has `JWT_SECRET` can issue valid tokens. That fits an application that issues and validates its own tokens; if another service ever had to validate them, an asymmetric key pair would be the next step.
- A single in-memory user means no user management and no roles. Adding them later means a `UserDetailsService` backed by the database.
- The static resources and the error page must stay public: otherwise the login page loads without styles, and error pages redirect to the login.
