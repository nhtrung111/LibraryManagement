# Plan — Book Return Module

The design uses 7 tables and 9 endpoints. A single `ReturnService.process()` method serves both preview (`commit = false`, fine treated as paid) and confirm, so the two can never disagree (AC-R9).

## Data model

Each row in `loan_item` is one physical copy. That makes partial returns (BR-6) and "oldest copy first" (BR-4) simple queries.

| Entity / table | Key fields | Notes |
| --- | --- | --- |
| `AppUser` / `app_user` | id, username (unique), password_hash, role, reader_id (nullable FK) | role is `STAFF` or `USER`. A USER account links to one reader |
| `Reader` / `reader` | id, code (unique), name, birth_date, address, phone, card_barcode (unique) | The library card |
| `Book` / `book` | id, code (unique), title, author, publish_year, cover_price DECIMAL(12,0), total_copies, available_copies, barcode (unique), description | One row per title |
| `LoanSlip` / `loan_slip` | id, slip_barcode (unique), reader_id, borrow_date, due_date | Created by Module 1. due_date = borrow_date + 30 |
| `LoanItem` / `loan_item` | id, loan_slip_id, book_id, status, return_date, fine_amount, return_transaction_id, version | One row per copy. status is `BORROWED` or `RETURNED`. `@Version` prevents double returns |
| `ReturnTransaction` / `return_transaction` | id, reader_id, staff_user_id, return_date, total_fine, fine_paid | One row per confirmed return, so a fine slip can be re-printed later |
| `RefreshToken` / `refresh_token` | id, user_id (FK), token_hash (unique), expires_at, revoked_at (nullable), replaced_by_id (nullable), created_at | See Security |

## Endpoints

| Method | Path | Role | Story / AC |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | Public | AC-A1, AC-A2 |
| POST | `/api/auth/refresh` | Public, needs refresh token | AC-A4, AC-A5 |
| POST | `/api/auth/logout` | Public, needs refresh token | AC-A6 |
| GET | `/api/staff/readers/{cardBarcode}/loans` | STAFF | US-1, AC-L1, AC-L2 |
| POST | `/api/staff/returns/preview` | STAFF | US-2, AC-R9 |
| POST | `/api/staff/returns` | STAFF | US-3, US-4, AC-R1 to AC-R8 |
| GET | `/api/me/loans` | USER | US-5, AC-U1, AC-U2 |
| GET | `/api/books?q={text}` | USER, STAFF | US-6 |
| GET | `/api/books/{barcode}/availability` | USER, STAFF | US-6, AC-U3 |

## JSON contracts

Confirm request. Preview takes the same body without `finePaid`, and treats the fine as paid.

```json
{
  "readerCardBarcode": "RC0001",
  "bookBarcodes": ["B0001", "B0001", "B0042"],
  "finePaid": true
}
```

Return result, shared by preview and confirm. `returnId` is null in a preview. `loanSlip` is null when nothing remains. `fineSlip` is null when nothing late was paid for.

```json
{
  "returnId": 15,
  "returnDate": "2026-11-13",
  "reader": { "code": "R001", "name": "Nguyen Van A", "cardBarcode": "RC0001" },
  "returnedItems": [
    { "bookCode": "S001", "title": "Clean Code", "author": "Robert C. Martin",
      "bookBarcode": "B0001", "slipBarcode": "LS0007",
      "borrowDate": "2026-10-01", "dueDate": "2026-10-31", "late": true, "fine": 20000 }
  ],
  "keptOnLoanItems": [],
  "loanSlip": {
    "reader": { "code": "R001", "name": "Nguyen Van A", "cardBarcode": "RC0001" },
    "lines": [
      { "bookCode": "S042", "title": "Effective Java", "author": "Joshua Bloch",
        "bookBarcode": "B0042", "slipBarcode": "LS0009",
        "borrowDate": "2026-11-01", "dueDate": "2026-12-01", "quantity": 1 }
    ],
    "totalCopies": 1
  },
  "fineSlip": {
    "reader": { "code": "R001", "name": "Nguyen Van A", "cardBarcode": "RC0001" },
    "lines": [
      { "bookCode": "S001", "title": "Clean Code", "author": "Robert C. Martin",
        "bookBarcode": "B0001", "slipBarcode": "LS0007",
        "borrowDate": "2026-10-01", "dueDate": "2026-10-31",
        "returnDate": "2026-11-13", "fine": 20000 }
    ],
    "totalFine": 20000
  }
}
```

My loans (`/api/me/loans`), one entry per unreturned copy:

```json
[
  { "title": "Effective Java", "bookBarcode": "B0042", "borrowDate": "2026-11-01",
    "dueDate": "2026-12-01", "daysBorrowed": 12, "daysLeft": 18, "overdueDays": 0, "status": "ON_TIME" }
]
```

Book availability:

```json
{ "code": "S042", "title": "Effective Java", "author": "Joshua Bloch", "barcode": "B0042",
  "totalCopies": 3, "availableCopies": 0, "status": "OUT_OF_COPIES" }
```

Login and refresh both return:

```json
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "refreshToken": "q8V3...", "tokenType": "Bearer", "expiresIn": 900 }
```

## Return algorithm

`ReturnService.process(request, boolean commit)`, `@Transactional`:

1. Find the reader by card barcode, or throw READER_NOT_FOUND.
2. Load the reader's `BORROWED` items ordered by borrow date, then slip id, then item id. Group them into one queue per book barcode.
3. For each scanned barcode: if no book has it, throw BOOK_NOT_FOUND. If the reader started with no copy of it, throw BOOK_NOT_BORROWED. If its queue is now empty, throw TOO_MANY_COPIES. Otherwise take the head of the queue (BR-4, BR-5).
4. Set `today = LocalDate.now(clock)`. An item is late if `today.isAfter(dueDate)`. Fine = cover_price × 0.20, rounded HALF_UP to whole dong (BR-1, BR-2).
5. If `finePaid` is false, move late items to `keptOnLoanItems` and do not return them (BR-7).
6. If `commit`: create a `ReturnTransaction`. For each returned item, set status `RETURNED`, return_date, fine_amount, and the transaction, and add 1 to the book's available copies (BR-8).
7. Build `loanSlip` from all items still `BORROWED` (including kept ones), grouped by book + slip + borrow date. Build `fineSlip` from returned late items.

The service throws before any write, so a rejected scan saves nothing. The transaction also rolls back on any error.

## Errors

Every error uses one shape:

```json
{ "status": 422, "code": "BOOK_NOT_BORROWED", "message": "Reader RC0001 has no unreturned copy of B0099",
  "details": { "barcode": "B0099" }, "timestamp": "2026-11-13T09:30:00+07:00" }
```

| Code | HTTP | When |
| --- | --- | --- |
| VALIDATION_ERROR | 400 | Missing fields, empty barcode list |
| UNAUTHORIZED | 401 | Not logged in |
| INVALID_CREDENTIALS | 401 | Wrong username or password |
| TOKEN_EXPIRED | 401 | Access token past its expiry |
| INVALID_TOKEN | 401 | Bad, unknown, expired or logged-out refresh token |
| REFRESH_TOKEN_REUSED | 401 | A rotated refresh token was used again |
| FORBIDDEN | 403 | Wrong role |
| NOT_FOUND | 404 | Unknown URL (only reachable when logged in with the right role) |
| METHOD_NOT_ALLOWED | 405 | Wrong HTTP method for a known URL |
| READER_NOT_FOUND | 404 | Unknown card barcode |
| BOOK_NOT_FOUND | 404 | Unknown book barcode |
| READER_NOT_LINKED | 404 | USER account has no linked reader |
| CONCURRENT_UPDATE | 409 | `@Version` conflict, e.g. two staff returning the same copy |
| BOOK_NOT_BORROWED | 422 | Reader holds no copy of that title |
| TOO_MANY_COPIES | 422 | More scans than copies held |
| INTERNAL_ERROR | 500 | Anything unexpected. Logged on the server; the response hides the details |

## Security

Auth is stateless JWT. A short-lived access token proves who you are on every request. A long-lived refresh token is used only to get new tokens, and it can be revoked.

| | Access token | Refresh token |
| --- | --- | --- |
| Format | JWT signed HS256 with jjwt. Claims: `sub` (user id), `username`, `role`, `iat`, `exp` | Random 256-bit string, opaque |
| Lifetime | 15 minutes | 7 days |
| Stored on server | No, verified by signature only | Yes, only its SHA-256 hash, in `refresh_token` |
| Sent as | `Authorization: Bearer <token>` header | JSON body of `/api/auth/refresh` and `/api/auth/logout` only |
| Revocable | No, it just expires | Yes |

**Refresh with rotation** (`RefreshTokenService.rotate()`, `@Transactional`):

1. Hash the incoming token and find its row. If there is none, or it has expired, throw INVALID_TOKEN.
2. If the row was revoked *by rotation* (`replaced_by_id` is set), someone is reusing an old token. Revoke every refresh token of that user and throw REFRESH_TOKEN_REUSED. This write is kept even though an error is thrown (`noRollbackFor`).
   If it was revoked by logout (no `replaced_by_id`), throw INVALID_TOKEN (AC-A6).
3. Otherwise revoke it, create a new refresh token, set `replaced_by_id`, and return a new access + refresh pair.

**Filter chain:** `JwtAuthenticationFilter` (a `OncePerRequestFilter`) reads the Bearer header, verifies the signature and expiry, and puts the user id and role into the `SecurityContext` without a database call. Sessions are `STATELESS`. CSRF is disabled because tokens are not sent in cookies. A custom `AuthenticationEntryPoint` and `AccessDeniedHandler` return the standard error JSON for 401 and 403.

**Secrets and passwords:** passwords are hashed with BCrypt. The JWT signing key comes from the `JWT_SECRET` environment variable (at least 32 bytes) and is never committed.

**Role rules:** `/api/auth/**` is public. `/api/staff/**` requires STAFF. `/api/me/**` requires USER. `/api/books/**` allows USER or STAFF. Everything else is denied. If the current code uses ADMIN, treat ADMIN as STAFF. `/api/me/loans` reads the reader from the token's user id, never from a request parameter. Because the role lives in the access token, a role change takes effect within 15 minutes.

## Data access (DAO)

Services never touch the database directly. They call DAO interfaces, one per entity: `AppUserDao`, `ReaderDao`, `BookDao`, `LoanSlipDao`, `LoanItemDao`, `ReturnTransactionDao`, `RefreshTokenDao`. Each implementation (`LoanItemDaoImpl`, …) uses JPA `EntityManager` with JPQL. Services depend on the interface, so unit tests can mock it.

```java
public interface LoanItemDao {
    List<LoanItem> findBorrowedByReaderOrdered(Long readerId); // BR-4 order
    List<LoanItem> findReturnedByReader(Long readerId);
    LoanItem save(LoanItem item);
}
```

## Package layout

```
controller   AuthController, StaffReturnController, MeController, BookController
service      AuthService, ReturnService, LoanQueryService, BookService, LoanViews (fine rule + DTO mapping)
dao          interfaces
dao.impl     EntityManager implementations
entity
dto
security     JwtService, JwtAuthenticationFilter, RefreshTokenService, entry point, denied handler
exception    ApiException, ErrorCode, ErrorResponses, GlobalExceptionHandler
security     AuthUser, JsonSecurityHandlers (401/403 JSON)
config       SecurityConfig, ClockConfig, DataSeeder (demo data, app.seed.enabled)
```
