# Tasks — Book Return Module

11 tasks in build order. Each is small enough to finish and test in one sitting, and ends with its tests passing. Tick a task only when its tests pass, then commit.

- [x] **T1 Setup:** Spring Boot project, SQL Server connection, `ClockConfig` (a `Clock` bean), and an empty `SecurityConfig`. *Check:* the app starts.
- [x] **T2 Entities and DAOs:** the 7 entities from the plan, DAO interfaces and EntityManager implementations, including a `LoanItemDao` method that loads a reader's `BORROWED` items in BR-4 order. *Check:* a DAO test loads items in the right order.
- [x] **T3 Seed data:** a `data.sql` or `CommandLineRunner` with 1 staff, 1 user linked to a reader, 5 books, and loans at 5, 10, 20, 30, 31 and 33 days old. *Check:* rows appear on startup.
- [x] **T4 Errors:** `ErrorCode` enum, `ApiException`, `GlobalExceptionHandler` with the error JSON shape. *Check:* a test endpoint returns the shape.
- [x] **T5 Reader lookup:** `GET /api/staff/readers/{cardBarcode}/loans`. *Tests:* AC-L1, AC-L2.
- [x] **T6 Return core:** `ReturnService.process()` steps 1 to 5 and step 7, with no saving yet. Unit tests use a fixed `Clock`. *Tests:* AC-R4, AC-R6, AC-R7, AC-R8.
- [x] **T7 Commit:** step 6 (save items, transaction, stock), plus the fine slip and unpaid path. *Tests:* AC-R1, AC-R2, AC-R3, AC-R5.
- [x] **T8 Return endpoints:** preview and confirm controllers with request validation. *Tests:* AC-R9, AC-R10.
- [x] **T9 User endpoints:** `/api/me/loans`, book search and availability. *Tests:* AC-U1, AC-U2, AC-U3.
- [x] **T10 Security (JWT):** JwtService, JWT filter, login, refresh with rotation and reuse detection, logout, then the role rules from the plan. *Tests:* AC-A1 to AC-A6, AC-U4, AC-U5.
- [x] **T11 Verify:** run the full suite, then walk through every acceptance criterion in `spec.md` and tick it off. Try the main flow in Postman.

## Open questions

Each has a working answer the code uses until someone changes it here.

- [ ] **Unpaid fine scope:** should an unpaid fine keep only the late copies on loan (current choice, BR-7), or cancel the whole return?
- [ ] **Cover price:** should the fine use the price at borrow time instead of the current price? That would need a `cover_price_at_loan` column on `loan_item`.
- [ ] **Loan slip lines:** the requirement says one line per title. This spec uses one line per title + slip + borrow date with a quantity, so copies borrowed on different dates are not merged.

## Decision log

| Decision | Choice | Decided by |
| --- | --- | --- |
| Deliverable | Backend only, JSON responses | Trung |
| Fine | Flat 20% of cover price per late copy | Trung |
| Due date | Borrow date + exactly 30 days, due date itself is on time | Trung |
| Barcode | Identifies a title, one scan = one copy | Trung |
| Multiple slips | Oldest unreturned copy closes first | Trung |
| Invalid scan | Reject the whole request, save nothing | Trung |
| Payment | Paid at return; unpaid late copies stay on loan | Trung |
| Lost / damaged books | Out of scope | Trung |
| Printing | JSON only for now; HTML print view later | Trung |
| Schema | Defined here, not coordinated with Module 1 | Trung |
| Roles | STAFF does returns; USER sees own loans and book availability | Trung |
| Auth | JWT access token + refresh token | Trung |
| Data access | DAO layer between services and the database | Trung |
| Token lifetimes | Access 15 min, refresh 7 days | Claude |
| Refresh handling | Rotate on every use; reuse revokes all the user's tokens | Claude |
| DAO implementation | Interface + impl using JPA EntityManager and JPQL | Claude |
| Preview endpoint | Same logic as confirm with `commit = false` | Claude |
| One row per copy in `loan_item` | Makes partial returns simple | Claude |
| Injected `Clock` | Makes late-return tests deterministic | Claude |
| Fresh project | New `library-sdd` project; old `library-simple` deleted | Trung |
| Test database | H2 in-memory, SQL Server mode, so tests run without SQL Server | Claude |
| Logout vs reuse | A token revoked by logout gives INVALID_TOKEN; only a rotated token reused gives REFRESH_TOKEN_REUSED | Claude |
| Return with nothing returned | All scanned copies late and unpaid: no transaction row, returnId null | Claude |
| Extra error codes | NOT_FOUND, METHOD_NOT_ALLOWED, INTERNAL_ERROR added for completeness | Claude |
