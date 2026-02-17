## AI Tool Usage Declaration

ChatGPT was used as a supplementary development aid during this code
challenge.\
It supported architectural validation, design discussions, and
debugging guidance.\
All core implementation, business logic, API design, and final decisions
were written and reviewed manually.

------------------------------------------------------------------------

## Key Prompts / Questions Asked

- How to structure interfaces vs implementations and keep SOLID
- How to handle pagination responses and validation
- How to map errors consistently in a Spring Boot API
- How to avoid slow reads by removing refresh calls from read endpoints
- How to write targeted tests for parsing and persistence edge cases
- How to apply the application context path globally and update URLs
- How to fix Mockito inline mock maker issues on JDK 21
- How to split services by responsibility and remove unused dependencies

------------------------------------------------------------------------

## Relevant AI Guidance

- DB-only reads with scheduled refresh
- DTO vs raw Page response tradeoffs
- Consistent error handling strategy
- Unit test ideas for parser, persister, and exception handler coverage

Key decisions:
- Remove lazy-loading from read endpoints to keep latency predictable (accepted).
- Remove Resilience4j from read paths after switching to DB-only reads (accepted).
- Return list-only output for pagination in this challenge (modified).

------------------------------------------------------------------------

## Decision Ownership

- All AI suggestions were reviewed and manually applied.
- No code was blindly copied without understanding.

------------------------------------------------------------------------
