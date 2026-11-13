# Spec — Book Return Module

Staff can return some or all of a reader's borrowed copies. Late copies cost a flat 20% of the cover price each and must be paid on the spot, or they stay on loan. Backend only (JSON API). This file contains no technology; every rule here must be testable.

Related module: Module 1, borrowing (Trần Quang Minh). For this practice project the schema is defined independently.

## User stories

1. **US-1 (Staff):** I scan a reader's card and see their details, the books they still have, and the books they already returned, so I know what they can return.
2. **US-2 (Staff):** I scan the returned books one by one and preview the result (remaining books, fines) before committing, so mistakes can be fixed before anything is saved.
3. **US-3 (Staff):** I confirm the return and get an updated loan slip (if books remain) and a fine slip (if any book is late).
4. **US-4 (Staff):** When a reader cannot pay a fine, I confirm the return as unpaid, so the late books stay on loan and only the on-time books are returned.
5. **US-5 (User):** I see my own loans: how many days I have had each book and how many days are left or overdue.
6. **US-6 (User):** I check a book's availability: copies left, or out of copies.

## Business rules

1. **BR-1 Due date:** due date = borrow date + 30 days. Returned on or before the due date = on time. Returned after it = late.
2. **BR-2 Fine:** each late copy costs 20% of that book's cover price, flat, not per day. The cover price used is the book's current price at return time.
3. **BR-3 One scan, one copy:** a barcode identifies a title. Each scan returns one copy of that title.
4. **BR-4 Which loan closes:** a scan closes the reader's oldest unreturned copy of that title (earliest borrow date, then lowest slip number), even across different loan slips.
5. **BR-5 Invalid scans:** scanning a title the reader has no unreturned copy of, or more times than the copies they hold, rejects the whole request. Nothing changes.
6. **BR-6 Partial return:** a reader may return any number of their unreturned copies, from 1 to all.
7. **BR-7 Payment:** fines are paid at return. If the reader does not pay, every late copy in that return stays on loan, no fine is recorded, and the on-time copies are still returned.
8. **BR-8 Stock:** each returned copy adds 1 to the title's available copies.

## Outputs

- **Updated loan slip** (only when the reader still has unreturned copies): reader code, reader name, reader card barcode, then one line per title and borrow date with title code, title, author, title barcode, loan slip barcode, borrow date, due date, quantity. The last line is the total number of copies still borrowed.
- **Fine slip** (only when a late copy was returned and paid): reader code, reader name, reader card barcode, then one line per late copy with title code, title, author, title barcode, loan slip barcode, borrow date, due date, return date, fine. The last line is the total fine.

## Acceptance criteria — returns and user views

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC-L1 | A reader with 2 unreturned and 3 returned copies | Staff looks up their card barcode | Response shows reader info, 2 unreturned lines, 3 returned lines |
| AC-L2 | No reader has the card barcode | Staff looks it up | 404, code READER_NOT_FOUND |
| AC-R1 | A reader holds 3 copies, all borrowed 10 days ago | Staff returns all 3 | All 3 marked returned, available copies +1 each, no loan slip, no fine slip |
| AC-R2 | A reader holds 4 copies | Staff returns 1 on time | Loan slip lists the other 3, total = 3 |
| AC-R3 | A copy borrowed 31 days ago, cover price 100,000 | Staff returns it, fine paid | Copy returned, fine slip shows 20,000, total 20,000 |
| AC-R4 | A copy borrowed exactly 30 days ago | Staff returns it | Not late, no fine |
| AC-R5 | One late and one on-time copy | Staff returns both, fine not paid | On-time copy returned. Late copy stays on loan and appears on the loan slip. No fine recorded |
| AC-R6 | A reader does not hold title X | Staff scans X | 422, code BOOK_NOT_BORROWED, nothing saved |
| AC-R7 | A reader holds 1 copy of title X | Staff scans X twice | 422, code TOO_MANY_COPIES, nothing saved |
| AC-R8 | Two copies of X, borrowed 20 and 5 days ago | Staff scans X once | The copy borrowed 20 days ago is returned |
| AC-R9 | Any valid scan list | Staff calls preview | Same result shape as confirm, database unchanged |
| AC-R10 | An empty scan list | Staff calls preview or confirm | 400, code VALIDATION_ERROR |
| AC-U1 | A user's linked reader holds a copy borrowed 12 days ago | The user views their loans | Shows days borrowed 12, days left 18 |
| AC-U2 | A copy borrowed 33 days ago | The user views their loans | Shows overdue by 3 days |
| AC-U3 | Title X has 0 available copies | Anyone logged in checks X | Status OUT_OF_COPIES, available 0 |
| AC-U4 | A logged-in normal user | Calls any staff return endpoint | 403 |
| AC-U5 | No login | Calls any endpoint | 401 |

## Authentication

Users log in once and stay logged in without retyping their password, until they log out or are inactive for 7 days.

1. **US-7 (Anyone):** I log in with my username and password.
2. **US-8 (Anyone):** When my short session expires, the client renews it silently, so I am not logged out mid-task.
3. **US-9 (Anyone):** I log out, so my session cannot be renewed on this device.

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC-A1 | A valid username and password | User logs in | 200 with an access token (15 min) and a refresh token (7 days) |
| AC-A2 | A wrong password or unknown username | User logs in | 401 INVALID_CREDENTIALS, same message for both cases |
| AC-A3 | An access token older than 15 minutes | User calls any protected endpoint | 401 TOKEN_EXPIRED |
| AC-A4 | A valid refresh token | User refreshes | New access and refresh tokens. The old refresh token no longer works |
| AC-A5 | A refresh token that was already used once | User refreshes with it again | 401 REFRESH_TOKEN_REUSED, and all that user's refresh tokens are revoked |
| AC-A6 | A logged-in user | Logs out, then refreshes | 401 INVALID_TOKEN |

## Out of scope

- Borrowing (Module 1), renewals, reservations
- Lost or damaged books
- Per-day fines, partial fine payment, paying an old fine later as a separate action
- UI, real printing, email or SMS notifications
