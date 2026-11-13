# Library Management — Project Rules (Constitution)

Specs live in `specs/`. Each module has `spec.md` (what/why), `plan.md` (how) and `tasks.md` (build order).
If code and spec disagree, fix the spec first, then the code. Work on one task at a time.

Current module: `specs/return-module/` (Module 2, Quản lí việc trả sách — owner: Nguyễn Hoàng Trung).

## Rules

These rules apply to every feature and change rarely. A task is not done if it breaks one.

1. **Stack:** Java 17, Spring Boot 3, Spring Web, Spring Data JPA, Spring Security, Bean Validation, jjwt (JWT), Lombok, SQL Server, Maven. Tests use JUnit 5 + H2 in SQL Server mode. Base package `com.library`.
2. **Layers:** Controller → Service → DAO. Controllers only map HTTP to service calls. All business rules live in services. Only DAOs talk to the database.
3. **DTOs at the edge:** Requests and responses use DTO records. JPA entities never appear in JSON.
4. **Money:** Use `BigDecimal`, in VND, rounded to whole dong with `RoundingMode.HALF_UP`. Never use `double` for money.
5. **Time:** Inject a `java.time.Clock` bean and get today's date as `LocalDate.now(clock)`. Tests use a fixed clock, which is how late returns are tested.
6. **Transactions:** Any service method that writes more than one row is `@Transactional`. A return either fully commits or fully rolls back.
7. **Errors:** One `@RestControllerAdvice` turns exceptions into the same JSON error shape (see plan). No stack traces in responses.
8. **Security:** Stateless JWT auth with a short-lived access token and a rotating refresh token. Every endpoint has an explicit role rule. Deny by default.
9. **Tests:** Every acceptance criterion in the spec has at least one automated test named after its ID (for example `AC_R3_lateBookCreatesFine`).
10. **Simplicity:** Build only what the spec asks for. New ideas go into Open questions in `tasks.md` first.

## How to work here

- Implement one task from `specs/<module>/tasks.md` at a time, then stop. Run `mvn test` before calling a task done.
- Tick the task in `tasks.md` only when its tests pass.
- If a rule needs to change, edit `spec.md` or `plan.md` first and say so.
