# library-sdd

Library management backend (Spring Boot 3, SQL Server, JWT), built spec-first.

- Rules: `CLAUDE.md`
- Book return module: `specs/return-module/` — `spec.md` (what/why), `plan.md` (how), `tasks.md` (build order)

## Run

1. SQL Server running with a `LibraryManagement` database. Hibernate creates the tables.
2. Set environment variables (IntelliJ: Run > Edit Configurations > Environment variables):
   - `DB_PASSWORD` — your SQL Server password
   - `JWT_SECRET` — any random string of at least 32 characters
3. Run `LibraryApplication`.

On first start, demo data is created (turn off with `app.seed.enabled: false`):

| Login | Password | Role |
| --- | --- | --- |
| staff | staff123 | STAFF |
| reader1 | reader123 | USER, linked to reader RC0001 (7 copies on loan, 2 late) |
| reader2 | reader123 | USER, linked to reader RC0002 (no loans) |

Try the whole flow in Postman: import `postman/library-sdd.postman_collection.json` and run the collection (20 requests, 35 checks). Run it on fresh demo data.

## Tests

`mvn test` runs 42 tests on an in-memory H2 database (no SQL Server needed).
Every acceptance criterion in `specs/return-module/spec.md` has a test named after its ID, e.g. `AC_R5_...`.
