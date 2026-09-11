# Enviro365 Withdrawal Notice System

A full-stack application for Enviro365 Investments. Investors sign in, view their portfolio, submit withdrawal notices
that are checked against the business rules, follow each notice through an approval workflow, review their history and
download CSV statements. Investors can only see their own data. Enviro365 staff get a staff portal with a dashboard of
statistics and charts, a client list, each client's portfolio and every withdrawal notice, where they approve, reject
and pay notices.

| Layer    | Technology |
|----------|------------|
| Backend  | Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security), H2 in-memory database, springdoc-openapi, Apache Commons CSV |
| Frontend | React 19 with TypeScript, Vite 8, Tailwind CSS v4 and shadcn/ui (Radix), React Router 8, TanStack Query 5, Recharts through shadcn/ui charts, Sonner notifications |
| Quality  | JUnit 5, Mockito, AssertJ, MockMvc with spring-security-test, Vitest, Spotless (palantir-java-format), ESLint (typescript-eslint with type checking, React Hooks rules), Prettier |

Base package: `com.enviro.assessment.junior.smsibi`

## Contents

1. [Screenshots](#screenshots)
2. [Getting started](#getting-started)
3. [Security](#security)
4. [API documentation](#api-documentation)
5. [Business rules and assumptions](#business-rules-and-assumptions)
6. [Withdrawal notice workflow](#withdrawal-notice-workflow)
7. [Architecture and design decisions](#architecture-and-design-decisions)
8. [Development standards](#development-standards)
9. [Testing](#testing)
10. [Requirements checklist](#requirements-checklist)
11. [AI usage disclosure](#ai-usage-disclosure)

## Screenshots

| | |
|---|---|
| Sign-in page. Every page requires a session.<br>![Sign-in](docs/screenshots/01-login.png) | Failed sign-in. A wrong password and an unknown user get the same message.<br>![Failed sign-in](docs/screenshots/02-login-failed.png) |
| Investor overview with amounts on hold and the status of each notice<br>![Dashboard](docs/screenshots/03-dashboard.png) | Form validation for an amount above the available balance<br>![Validation](docs/screenshots/04-withdraw-validation.png) |
| Preview of what stays available after the notice<br>![Preview](docs/screenshots/05-withdraw-preview.png) | Notice submitted as pending<br>![Success](docs/screenshots/06-withdraw-success.png) |
| History with statuses, filtered by product, with CSV download<br>![History](docs/screenshots/07-history-filtered.png) | Retirement rule for an investor aged 40<br>![Retirement restriction](docs/screenshots/08-retirement-restriction.png) |
| Staff dashboard with the notices waiting for staff, statistics and charts<br>![Staff dashboard](docs/screenshots/09-admin-dashboard.png) | Staff client list with open notices and amounts paid out<br>![Clients](docs/screenshots/10-admin-clients.png) |
| A client's portfolio, seen by staff<br>![Client portfolio](docs/screenshots/11-admin-client.png) | Staff approving a pending notice<br>![Approve](docs/screenshots/12-approve-dialog.png) |
| Staff rejecting a notice with a reason for the investor<br>![Reject](docs/screenshots/13-reject-dialog.png) | Staff marking an approved notice as paid<br>![Pay](docs/screenshots/14-pay-dialog.png) |
| Every notice and its status, seen by staff<br>![Withdrawal notices](docs/screenshots/15-admin-notices.png) | The investor sees why a notice was rejected<br>![Rejected notice](docs/screenshots/16-investor-rejected.png) |
| Mobile layout<br>![Mobile](docs/screenshots/17-mobile.png) | Swagger UI (dev profile)<br>![Swagger](docs/screenshots/18-swagger.png) |

## Getting started

### Prerequisites

- JDK 21 or newer. Spring Boot 4 needs at least Java 17. Maven does not need to be installed, because the Maven wrapper (`mvnw`) downloads it.
- Node.js 20.19 or newer (tested with Node 24) and npm.

### 1. Run the backend (port 8080)

```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

The backend starts in the `dev` profile by default. This profile creates an in-memory database with the
[demo accounts](#demo-accounts) and turns on the H2 console and Swagger UI. The data is created again on every start.
To run without the developer tools, use `./mvnw spring-boot:run -Dspring-boot.run.profiles=prod`. The `prod` profile
also sends the session cookie over HTTPS only.

| URL (dev profile) | What it is |
|-----|------|
| http://localhost:8080/swagger-ui.html | Interactive API documentation. Sign in through the app first, and Swagger then reuses the session and CSRF cookies. |
| http://localhost:8080/v3/api-docs | OpenAPI 3 specification (JSON) |
| http://localhost:8080/h2-console | H2 database console. Staff sign-in required. JDBC URL `jdbc:h2:mem:enviro`, user `sa`, no password. |

### 2. Run the frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 and sign in. During development Vite forwards every `/api` request to
http://localhost:8080, so the browser sees one origin and the session and CSRF cookies work without any CORS setup.

### Demo accounts

These are created by [`DataSeeder`](backend/src/main/java/com/enviro/assessment/junior/smsibi/config/DataSeeder.java)
in the `dev` profile only. Every account uses the password `Enviro365!`, which is for demo data only. In development
builds the sign-in page also lists these accounts.

| Username | Role | Age | What it shows |
|----------|------|-----|---------------|
| `thabo.mokoena@example.com` | Investor | 70 | Retirement withdrawals are allowed. Has paid notices, an approved notice waiting for payment and a cancelled notice. |
| `sipho.ndlovu@example.com` | Investor | 65 | Edge case: exactly 65 is not eligible, because the rule is strictly older than 65. Has a rejected notice with the reason. |
| `lerato.dlamini@example.com` | Investor | 40 | Retirement blocked, savings allowed. Has a pending notice. |
| `admin@enviro365.example` | Staff (`ADMIN`) | n/a | Staff portal with the dashboard, client list, client portfolios and all withdrawal notices. Approves, rejects and pays notices, but cannot submit withdrawals. |

Dates of birth are calculated relative to today, so the ages stay the same whenever the app runs. Five wrong passwords
lock a username for 15 minutes. The lock is kept in memory, so restarting the backend clears it.

### Other commands

| Where | Command | Purpose |
|-------|---------|---------|
| `backend/` | `./mvnw test` | Run all 111 backend tests (unit, web layer and integration) |
| `backend/` | `./mvnw spotless:apply` | Format all Java code (palantir-java-format) |
| `backend/` | `./mvnw verify` | Run the tests and check the formatting. Fails if any file is not formatted. |
| `backend/` | `./mvnw package` | Build an executable jar in `target/` |
| `frontend/` | `npm test` | Frontend unit tests (Vitest) |
| `frontend/` | `npm run lint` | ESLint with type-checked TypeScript rules and React Hooks rules |
| `frontend/` | `npm run format` or `npm run format:check` | Format the code with Prettier, or only check the formatting |
| `frontend/` | `npm run build` | Type-check and build for production into `dist/` |

## Security

### Authentication with a server-side session

```
Browser (React)                                   Spring Boot
  │ GET  /api/auth/csrf  ───────────────────────►  sets XSRF-TOKEN cookie (readable by JS)
  │ POST /api/auth/login  username, password
  │      + header X-XSRF-TOKEN  ────────────────►  BCrypt check, new session
  │                           ◄───────────────── 204 + JSESSIONID cookie (HttpOnly, SameSite=Strict)
  │ GET  /api/auth/me  ─────────────────────────►  { username, displayName, role, investorId }
  │ every API call sends JSESSIONID automatically; every POST also sends X-XSRF-TOKEN
  │ POST /api/auth/logout ──────────────────────►  session destroyed, cookies cleared, 204
```

Why a session cookie and not a JWT? The session id is kept in an HttpOnly cookie that JavaScript cannot read, so even
an XSS bug could not steal it, and nothing sensitive is stored in `localStorage`. The server can end a session
immediately on sign-out or after 30 minutes of inactivity, which is hard to do with a JWT. It also uses Spring
Security's built-in and well-tested form login instead of custom token code. The trade-off is that the server keeps
session state. That is fine for one server, and several servers would share sessions through Spring Session and Redis.

The configuration is in
[`SecurityConfig`](backend/src/main/java/com/enviro/assessment/junior/smsibi/security/SecurityConfig.java).

### CSRF protection

Because the browser sends cookies automatically, a malicious website could try to submit a withdrawal on behalf of a
signed-in investor, or approve a payment on behalf of a signed-in staff member. Spring Security's `csrf.spa()` puts a
random token in a readable `XSRF-TOKEN` cookie, and every POST must copy that value into an `X-XSRF-TOKEN` header.
Another site can make the browser send our cookies but cannot read them, so it cannot produce the header. The
`SameSite=Strict` session cookie is a second, independent defence. The token is replaced at sign-in and sign-out, and
the React client ([`api/client.ts`](frontend/src/api/client.ts)) fetches a new one whenever it needs to.

### Authorisation with roles and ownership checks

| Endpoint | Anonymous | Investor | Staff (`ADMIN`) |
|----------|-----------|----------|-----------------|
| `POST /api/auth/login`, `GET /api/auth/csrf` | Allowed | Allowed | Allowed |
| `GET /api/investors`, `GET /api/dashboard` | 401 | 403 | Allowed |
| `GET /api/investors/{id}/portfolio` | 401 | Own id only, otherwise 403 | Any investor |
| `POST /api/withdrawals` | 401 | Own products only, otherwise 403 | 403, staff do not submit withdrawals |
| `POST /api/withdrawals/{id}/approve`, `/reject`, `/pay` | 401 | 403 | Allowed |
| `POST /api/withdrawals/{id}/cancel` | 401 | Own notices only, otherwise 403 | 403, staff reject instead |
| `GET /api/withdrawals`, `/export`, `/{id}` | 401 | Always limited to their own data | Any investor |
| Anything else | Denied | Denied | Denied |

The URL rules decide which kinds of request each role may make. Ownership checks in the services
([`AccessGuard`](backend/src/main/java/com/enviro/assessment/junior/smsibi/security/AccessGuard.java)) decide which
records a user may see or change. Without them an investor could read someone else's portfolio by changing the id in
the URL. This is known as an insecure direct object reference and is one of the most common API flaws. The services
also check the role again for every workflow step. The route guard and the buttons in React only improve the
experience. The server is the security boundary.

### Passwords and brute-force protection

- Passwords are stored as BCrypt hashes, which are salted and deliberately slow, through Spring's `DelegatingPasswordEncoder`. They are never stored in plain text.
- A failed sign-in always returns the same message, "Invalid username or password.", so an attacker cannot find out which email addresses have accounts.
- Five failures lock a username for 15 minutes and return HTTP 429 with a `Retry-After` header. While a username is locked, even the correct password is refused. Unknown usernames are counted in the same way, so the lock cannot be used to discover accounts either ([`LoginAttemptService`](backend/src/main/java/com/enviro/assessment/junior/smsibi/security/LoginAttemptService.java)).

### Other hardening

- The session cookie is `HttpOnly` and `SameSite=Strict`, expires after 30 minutes of inactivity, is never put in URLs, and is marked `Secure` (HTTPS only) in the `prod` profile.
- Every response carries security headers: `X-Content-Type-Options: nosniff`, `X-Frame-Options`, `Cache-Control: no-store` and `Referrer-Policy: same-origin`.
- Access is denied by default. Any URL that is not explicitly allowed is refused.
- 401, 403 and 429 responses use the same Problem Details JSON as every other error, and nothing internal such as stack traces or SQL is ever returned.
- Sign-ins (successful and failed) and every step of a withdrawal notice (submitted, approved, rejected, paid, cancelled) are written to an audit log with the user who took the step. Values supplied by users are cleaned before logging so nobody can forge log lines, rejection reasons are not logged, and passwords are never logged.
- CORS allows only listed origins, which is required when cookies are involved.
- The developer tools (H2 console, Swagger and demo data) exist only in the `dev` profile, and the H2 console also requires a staff sign-in.

A production system would also add HTTPS termination, multi-factor authentication, password reset and account
management, a Content-Security-Policy, rate limiting per IP address, and a persistent database with versioned
migrations.

## API documentation

All endpoints are under `/api` and need a session, except the sign-in endpoints. The full interactive documentation is
in Swagger UI at http://localhost:8080/swagger-ui.html (dev profile).

### Authentication endpoints

These are handled by Spring Security filters. See [Security](#security) for how they work.

| Method | Endpoint | Body and headers | Response |
|--------|----------|------------------|----------|
| `GET`  | `/api/auth/csrf` | none | `204` and an `XSRF-TOKEN` cookie |
| `POST` | `/api/auth/login` | form fields `username` and `password`, header `X-XSRF-TOKEN` | `204` and a session cookie, `401`, or `429` |
| `GET`  | `/api/auth/me` | none | `200` with the current user, or `401` |
| `POST` | `/api/auth/logout` | header `X-XSRF-TOKEN` | `204` |

### Portfolio and withdrawal endpoints

| Method | Endpoint | Description | Success |
|--------|----------|-------------|---------|
| `GET`  | `/api/dashboard` | Staff dashboard statistics: clients, assets under management (total and by product type), notices waiting for approval and for payment, the amount on hold, the total and average paid out, activity in the last 30 days and payments per month for the last six months. Staff only. | `200` |
| `GET`  | `/api/investors` | Staff client list: every investor with their product count, total balance, notice count, open notices, total paid out and last notice. Staff only. | `200` |
| `GET`  | `/api/investors/{investorId}/portfolio` | Investor details and products, including the amount on hold, the available balance and the withdrawal limit | `200` |
| `POST` | `/api/withdrawals` | Submit a withdrawal notice (investors, own products only). It starts as `PENDING` with the amount on hold. | `201` with a `Location` header |
| `POST` | `/api/withdrawals/{id}/approve` | Approve a pending notice (staff) | `200` |
| `POST` | `/api/withdrawals/{id}/reject` | Reject a pending notice with body `{ "reason": "..." }`. The investor sees the reason. (staff) | `200` |
| `POST` | `/api/withdrawals/{id}/pay` | Mark an approved notice as paid, which deducts the amount from the balance (staff) | `200` |
| `POST` | `/api/withdrawals/{id}/cancel` | Cancel your own pending notice (investors) | `200` |
| `GET`  | `/api/withdrawals/{id}` | A single withdrawal notice | `200` |
| `GET`  | `/api/withdrawals` | Withdrawal notices, newest first | `200` |
| `GET`  | `/api/withdrawals/export` | Withdrawal notices as a CSV file download | `200` (`text/csv`) |

### Filters

`GET /api/withdrawals` and `GET /api/withdrawals/export` accept these optional filters, which can be combined. For
investors the results are always limited to their own data.

| Parameter | Example | Meaning |
|-----------|---------|---------|
| `investorId` | `1` | Only this investor's notices. Staff can pass any id, an investor only their own. |
| `productId` | `2` | Only notices for this product |
| `status` | `PENDING` | Only notices with this status. Repeat it for several, e.g. `status=PENDING&status=APPROVED`. |
| `from` | `2026-08-01` | Submitted on or after this date |
| `to` | `2026-08-31` | Submitted on or before this date (the whole day is included) |

### Trying it with curl

```bash
B=http://localhost:8080
curl -s -c jar -b jar $B/api/auth/csrf                                   # 1. get a CSRF cookie
TOKEN=$(awk '$6=="XSRF-TOKEN"{print $7}' jar)
curl -s -c jar -b jar -H "X-XSRF-TOKEN: $TOKEN" \
     --data-urlencode "username=thabo.mokoena@example.com" \
     --data-urlencode 'password=Enviro365!' $B/api/auth/login           # 2. sign in (204)
curl -s -b jar $B/api/auth/me                                            # 3. who am I?
curl -s -b jar $B/api/investors/1/portfolio                              # 4. own portfolio (200)
curl -s -b jar $B/api/investors/2/portfolio                              #    someone else's (403)
```

Signing in replaces the CSRF token, so call `/api/auth/csrf` again and read the cookie again before the next POST.

### Examples

These responses were captured from the running application with the demo data.

#### Get a portfolio

`GET /api/investors/1/portfolio`, signed in as Thabo. His retirement annuity has R 15,000.00 on hold for an approved
notice, so R 810,000.00 is available and 90% of that is the most he can request.

```json
{
  "investor": {
    "id": 1, "firstName": "Thabo", "lastName": "Mokoena", "fullName": "Thabo Mokoena",
    "email": "thabo.mokoena@example.com", "phone": "+27 82 555 0101",
    "dateOfBirth": "1956-05-11", "age": 70
  },
  "products": [
    { "id": 1, "name": "Retirement Annuity", "type": "RETIREMENT", "balance": 825000.00,
      "heldAmount": 15000.00, "availableBalance": 810000.00, "maxWithdrawalAmount": 729000.00,
      "withdrawalAllowed": true, "restrictionReason": null },
    { "id": 2, "name": "Tax-Free Savings Account", "type": "SAVINGS", "balance": 110000.00,
      "heldAmount": 0.00, "availableBalance": 110000.00, "maxWithdrawalAmount": 99000.00,
      "withdrawalAllowed": true, "restrictionReason": null }
  ],
  "totalBalance": 935000.00
}
```

#### Client list

`GET /api/investors`, signed in as staff. The totals come from two `GROUP BY` queries instead of one query per client.
`withdrawalCount` counts notices in every status, and `totalWithdrawn` counts only paid notices.

```json
[
  { "id": 3, "fullName": "Lerato Dlamini", "email": "lerato.dlamini@example.com", "age": 40,
    "productCount": 2, "totalBalance": 350500.00, "withdrawalCount": 2, "openNoticeCount": 1,
    "totalWithdrawn": 5000.00, "lastWithdrawalAt": "2026-09-09T13:50:30" }
]
```

#### Staff dashboard

`GET /api/dashboard`, signed in as staff. Withdrawn amounts are payments. Months without payments are included with
zero values.

```json
{
  "clientCount": 3, "productCount": 6, "assetsUnderManagement": 1878000.00, "retirementEligibleClients": 1,
  "noticeCount": 8, "awaitingApproval": 1, "awaitingPayment": 1, "amountOnHold": 17000.00,
  "paidCount": 4, "totalWithdrawn": 47500.00, "averageWithdrawal": 11875.00,
  "noticesLast30Days": 5, "withdrawnLast30Days": 17500.00,
  "assetsByProductType": [
    { "type": "RETIREMENT", "productCount": 3, "balance": 1675000.00 },
    { "type": "SAVINGS", "productCount": 3, "balance": 203000.00 }
  ],
  "withdrawalsByMonth": [
    { "month": "2026-04", "noticeCount": 0, "amount": 0.00 },
    { "month": "2026-05", "noticeCount": 0, "amount": 0.00 },
    { "month": "2026-06", "noticeCount": 0, "amount": 0.00 },
    { "month": "2026-07", "noticeCount": 2, "amount": 30000.00 },
    { "month": "2026-08", "noticeCount": 2, "amount": 17500.00 },
    { "month": "2026-09", "noticeCount": 0, "amount": 0.00 }
  ]
}
```

#### Submit a withdrawal notice

```http
POST /api/withdrawals
Content-Type: application/json
X-XSRF-TOKEN: <value of the XSRF-TOKEN cookie>

{ "productId": 2, "amount": 1000.00 }
```
```http
HTTP/1.1 201 Created
Location: http://localhost:8080/api/withdrawals/9
```
```json
{
  "id": 9, "investorId": 1, "investorName": "Thabo Mokoena",
  "productId": 2, "productName": "Tax-Free Savings Account", "productType": "SAVINGS",
  "amount": 1000.00, "status": "PENDING", "balanceBefore": null, "balanceAfter": null,
  "createdAt": "2026-09-11T13:51:16", "reviewedBy": null, "reviewedAt": null, "rejectionReason": null,
  "paidBy": null, "paidAt": null, "cancelledAt": null
}
```

The balance is not touched yet. The R 1,000.00 is on hold until the notice is paid, rejected or cancelled.

#### Approve and pay a notice

`POST /api/withdrawals/9/approve` and then `POST /api/withdrawals/9/pay`, signed in as staff. Both need the
`X-XSRF-TOKEN` header and no body. The response to the payment:

```json
{
  "id": 9, "investorId": 1, "investorName": "Thabo Mokoena",
  "productId": 2, "productName": "Tax-Free Savings Account", "productType": "SAVINGS",
  "amount": 1000.00, "status": "PAID", "balanceBefore": 110000.00, "balanceAfter": 109000.00,
  "createdAt": "2026-09-11T13:51:16", "reviewedBy": "admin@enviro365.example", "reviewedAt": "2026-09-11T13:51:16",
  "rejectionReason": null, "paidBy": "admin@enviro365.example", "paidAt": "2026-09-11T13:51:16", "cancelledAt": null
}
```

Paying the notice before it was approved was refused:

```json
{
  "title": "Notice status conflict",
  "status": 409,
  "detail": "Withdrawal notice #9 is pending, so it cannot be paid. Only approved notices can be paid.",
  "instance": "/api/withdrawals/9/pay",
  "code": "INVALID_STATUS_TRANSITION",
  "currentStatus": "PENDING"
}
```

#### Download a CSV statement

`GET /api/withdrawals/export?productId=2`, signed in as Thabo. Every notice is listed with its status. The payment
columns are only filled in for paid notices.

```http
HTTP/1.1 200 OK
Content-Type: text/csv;charset=UTF-8
Content-Disposition: attachment; filename="withdrawal-statement-2026-09-11.csv"

Notice ID,Submitted,Investor,Product,Product Type,Amount,Status,Paid,Balance Before,Balance After
9,2026-09-11 13:51:16,Thabo Mokoena,Tax-Free Savings Account,SAVINGS,1000.00,PAID,2026-09-11 13:51:16,110000.00,109000.00
4,2026-08-22 13:50:30,Thabo Mokoena,Tax-Free Savings Account,SAVINGS,4000.00,CANCELLED,,,
3,2026-08-12 13:50:30,Thabo Mokoena,Tax-Free Savings Account,SAVINGS,10000.00,PAID,2026-08-13 13:50:30,120000.00,110000.00
```

### Errors

Every error, including security errors, uses the standard RFC 9457 Problem Details JSON format
(`application/problem+json`).

| Status | When | Extra fields |
|--------|------|--------------|
| `400 Bad Request` | Invalid input: a missing or non-positive amount, more than 2 decimals, malformed JSON, a bad date or status, `from` after `to`, or a missing rejection reason | `errors`, with a message for each field |
| `401 Unauthorized` | Not signed in, the session expired, or a wrong username or password | |
| `403 Forbidden` | Signed in but not allowed: another investor's data, staff submitting or cancelling a notice, an investor approving, rejecting or paying, or a missing or invalid CSRF token | |
| `404 Not Found` | Unknown investor, product or withdrawal notice | |
| `409 Conflict` | A workflow step that the notice's status does not allow, such as paying a pending notice or cancelling an approved one. Also returned when two requests change the same product or notice at the same time (optimistic locking). | `code`: `INVALID_STATUS_TRANSITION` and `currentStatus`, for workflow steps |
| `422 Unprocessable Content` | Valid input that breaks a business rule | `code`: `RETIREMENT_AGE_RESTRICTION`, `INSUFFICIENT_BALANCE` or `EXCEEDS_WITHDRAWAL_LIMIT` |
| `429 Too Many Requests` | The username is locked after 5 failed sign-ins | `Retry-After` header |
| `500 Internal Server Error` | Anything unexpected. The details are logged and a generic message is returned. | |

```json
{
  "title": "Withdrawal not allowed",
  "status": 422,
  "detail": "Withdrawals may not exceed 90% of the available balance. The maximum you can withdraw is R 98,100.00.",
  "instance": "/api/withdrawals",
  "code": "EXCEEDS_WITHDRAWAL_LIMIT"
}
```

## Business rules and assumptions

All the rules are in one class,
[`WithdrawalPolicy`](backend/src/main/java/com/enviro/assessment/junior/smsibi/service/WithdrawalPolicy.java). They
are checked in this order when a notice is submitted, and the first rule that is broken is reported.

1. The amount must be positive, with at most 2 decimal places. Bean Validation checks this first, and the policy checks it again as a safety net.
2. Retirement products need the investor to be older than 65. The brief says "age > 65", which is read as strictly greater, so a 65-year-old is not eligible. Age is calculated from the date of birth on the day of the request. It is never stored, so it cannot go out of date.
3. The amount must not be more than the available balance, which is the balance minus the amounts on hold for the product's open notices. This is reported as `INSUFFICIENT_BALANCE`.
4. The amount must not be more than 90% of the available balance. This is reported as `EXCEEDS_WITHDRAWAL_LIMIT`. The limit is rounded down to the cent, so rounding can never allow a withdrawal above 90%.

Assumptions:

- The 90% rule on its own would also catch an amount above the balance. Both rules are still checked, because the brief lists them separately and they need different messages.
- The 90% limit applies to every product type, for each notice, based on the available balance when the notice is submitted.
- Open notices hold their amount, so the rules use the balance minus what is already on hold. This gives the same result as if every open notice had already been paid. With a balance of R 100,000 and R 90,000 on hold, at most R 9,000 can be requested, which is exactly what the 90% rule allows once the first notice is paid. Without this, several open notices could together promise more money than the product holds.
- The rules are not checked again when a notice is approved or paid. The money has been on hold since the notice was submitted, and age only goes up, so a notice that passed the rules still passes them.
- Only the investor who owns a product can submit a notice for it. Staff review and pay notices but never submit them, and investors cannot approve or pay their own notices.
- Money is handled as `BigDecimal` with 2 decimal places, and all amounts are in Rand (ZAR).
- Everything runs in one time zone. Timestamps are in the server's local time, and date filters cover whole local days.

## Withdrawal notice workflow

A withdrawal notice is not processed on the spot. It is submitted, reviewed by staff and then paid.

```mermaid
stateDiagram-v2
    [*] --> PENDING: investor submits
    PENDING --> APPROVED: staff approve
    PENDING --> REJECTED: staff reject with a reason
    PENDING --> CANCELLED: investor cancels
    APPROVED --> PAID: staff mark as paid
```

| Status | How a notice gets here | What happens to the money |
|--------|------------------------|---------------------------|
| `PENDING` | The investor submits a notice that passes the rules | The amount is put on hold on the product. The balance does not change. |
| `APPROVED` | Staff approve a pending notice | The amount stays on hold. |
| `PAID` | Staff mark an approved notice as paid | The amount is deducted from the balance and released from the hold. The balance before and after are stored on the notice, so statements stay accurate. |
| `REJECTED` | Staff reject a pending notice, with a reason the investor can see | The hold is released. |
| `CANCELLED` | The investor cancels a pending notice | The hold is released. |

`PAID`, `REJECTED` and `CANCELLED` are final. Any other step, such as paying a pending notice or cancelling one that has
already been approved, is refused with `409 Conflict` and the notice's current status. A notice can never skip a step
or be paid twice.

How it is built:

- The allowed moves are listed in one place, [`NoticeStatus.canMoveTo`](backend/src/main/java/com/enviro/assessment/junior/smsibi/entity/NoticeStatus.java). The methods on [`WithdrawalNotice`](backend/src/main/java/com/enviro/assessment/junior/smsibi/entity/WithdrawalNotice.java) (`submit`, `approve`, `reject`, `cancel` and `markPaid`) are the only way to change a notice, and each one checks the move first.
- The methods that change a product's held amount and balance are package-private, so only `WithdrawalNotice` can call them. Money only moves through the workflow.
- Each step has its own endpoint (`POST /api/withdrawals/{id}/approve`) rather than a generic status update. Every step has its own permission and side effects, and a URL per step lets `SecurityConfig` restrict each one to the right role. `WithdrawalService` checks the role and ownership again.
- `Product` and `WithdrawalNotice` both use optimistic locking (`@Version`). If two notices on the same product are submitted at the same moment, or two staff members act on the same notice, the second request gets a 409 and can try again instead of silently overwriting the first.
- The history table, the CSV export and the client list include notices in every status. Figures called "withdrawn" or "paid out" count only paid notices. The CSV payment columns are empty until a notice is paid.
- The interface only offers the buttons that can succeed for the user's role and the notice's status (`availableActions` in [`utils/notices.ts`](frontend/src/utils/notices.ts)), and every step asks for confirmation first. Rejecting needs a reason. The server still checks every request.

## Architecture and design decisions

```
Assessment/
├── backend/                         Spring Boot API
│   └── src/main/java/com/enviro/assessment/junior/smsibi/
│       ├── controller/              REST endpoints (thin, they only translate HTTP to service calls)
│       ├── service/                 Use cases and the notice workflow, WithdrawalPolicy (business rules), dashboard, CSV export
│       ├── security/                SecurityConfig, the signed-in user, sign-in lock, ownership checks
│       ├── repository/              Spring Data JPA repositories, filter specification, aggregate queries
│       ├── entity/                  JPA entities: Investor, Product, WithdrawalNotice and its NoticeStatus, UserAccount
│       ├── dto/                     Request and response records (the public API contract)
│       ├── mapper/                  Entity to DTO mapping
│       ├── exception/               Custom exceptions and GlobalExceptionHandler
│       └── config/                  Clock, OpenAPI information, demo data seeder
├── frontend/                        React and TypeScript UI
│   └── src/
│       ├── api/                     client.ts (fetch with session cookie and CSRF) and queries.ts (TanStack Query)
│       ├── auth/                    AuthProvider, useAuth and the RequireAuth route guard
│       ├── pages/                   login, investor overview, staff dashboard, clients, client page, withdraw, history
│       ├── components/              app shell, portfolio, dashboard and withdrawal components (status badges, workflow buttons and dialogs)
│       ├── components/ui/           shadcn/ui components (generated by the shadcn tool, kept in this repo)
│       ├── hooks/, utils/           shared hooks, formatting, validation, notice workflow helpers and their tests
│       └── types.ts                 TypeScript versions of the backend DTOs
└── docs/screenshots/
```

A request travels from a React page through TanStack Query and `api/client.ts`, which adds the session cookie and CSRF
header. In development it passes through the Vite proxy to the Spring Security filters, then to a controller, a service
(with `AccessGuard` and `WithdrawalPolicy`), a repository and finally the H2 database. Errors come back through
`GlobalExceptionHandler` as Problem Details, and the interface shows them next to the relevant field or as a
notification.

| Decision | Why |
|----------|-----|
| Layered architecture with thin controllers | Each layer has one job. Business logic can be tested without HTTP, and the web layer without a database. |
| Session cookie and CSRF instead of JWT | The session id cannot be read by JavaScript, sessions end immediately on sign-out or timeout, and it uses Spring's built-in login. See [Security](#security). |
| Ownership checks in the services (`AccessGuard`) | Role rules alone would let an investor read another investor's data by changing an id. |
| Security errors handled by `GlobalExceptionHandler` | 401, 403 and 429 look exactly like every other error, so the UI handles them in the same way. |
| DTO layer (Java records) | The JSON contract stays independent of the database schema, and internal fields such as password hashes and versions never leak. |
| All rules in `WithdrawalPolicy` | There is one place to read, test and change the rules. The portfolio endpoint also uses it to tell the UI what is allowed. |
| A workflow with money on hold instead of immediate deduction | Staff review every notice before money leaves a product, and holding the amount stops several open notices from promising the same money. See [Withdrawal notice workflow](#withdrawal-notice-workflow). |
| The workflow in the entities (`NoticeStatus`, `WithdrawalNotice`) | The allowed moves and their effect on the money live next to the data they change, so no service can skip a step or move money another way. |
| `BigDecimal` for money | `double` cannot represent cents exactly. The frontend compares amounts in whole cents for the same reason. |
| An injected `Clock` | Logic that depends on dates, such as the age rule and the sign-in lock, can be tested with a fixed or controllable clock. |
| `@Transactional` and `@Version` | A notice and the money it holds change together or not at all, and two requests at the same time cannot overwrite each other's changes to a product or a notice. |
| JPA Specification and `@EntityGraph` | Any combination of optional filters works, without extra queries per row (the N+1 problem). |
| shadcn/ui and Tailwind | Accessible Radix components are copied into the repo, so every line can be read and changed. The `login-03` and `sidebar-07` templates were adapted for the sign-in page and the app layout. |
| TanStack Query for server data | Caching, loading and error states, and automatic reloading after a notice changes, without hand-written `useEffect` fetching. |
| Figures kept current across users (`useLiveRefresh`) | Other people change the figures too: staff pay a notice, an investor submits one. Every page fetches fresh data when it opens and when the user comes back to the tab, and refreshes every 30 seconds while the user is active. It stops after five minutes without input, so an unattended screen does not keep the session alive past the 30-minute idle sign-out. |
| Staff pages with their own URLs (`/clients/3`, `/history?investor=3&status=open`) | Every client and filter has a page that can be bookmarked or linked to, for example from the dashboard, and a refresh keeps the selection. The server still enforces access. |
| Aggregate queries for the client list and dashboard (`GROUP BY` with interface projections) | Totals for all clients come from two queries rather than one per client, and the counts and amounts for every status come from one query, so the pages stay fast as the data grows. |
| Dashboard statistics calculated on the server (`GET /api/dashboard`) | The browser receives a handful of numbers instead of every notice. Months without payments are filled in with zeros, so the chart has no gaps. The count of clients eligible for retirement withdrawals uses the cut-off date from `WithdrawalPolicy`, so the age rule stays in one place. |
| Profiles (`dev` by default, and `prod`) | Demo data and developer tools cannot be switched on by accident in production. |

## Development standards

- Formatting is automated. Java is formatted by [Spotless](https://github.com/diffplug/spotless) with palantir-java-format (`./mvnw spotless:apply`), and `./mvnw verify` fails if any file is not formatted. TypeScript, CSS and JSON are formatted by Prettier, with the Tailwind plugin sorting class names.
- Linting uses an ESLint flat config with the type-checked typescript-eslint rules (for example no floating promises and no unsafe `any`), the React Hooks rules and react-refresh. The React Hooks rules found a real problem in the generated `use-mobile` hook, which called `setState` inside an effect, and it was rewritten with `useSyncExternalStore`. The generated shadcn/ui components are linted with the standard rules only, because the shadcn tool replaces them when they are updated.
- TypeScript uses strict settings (unused code is an error, `erasableSyntaxOnly`), typed environment variables, and TypeScript versions of the DTOs in `types.ts`.
- The code uses constructor injection, immutable DTO records, Problem Details for every error and one responsibility per class. Frontend files use kebab-case names, as shadcn does, and the `@/` path alias.
- The tests follow the testing pyramid: many fast unit tests, focused web layer tests and a few full-stack integration tests.
- Settings for each environment live in profiles, not in code. The only password in the repository is the demo password in `application-dev.properties`.
- Comments explain why the code is written the way it is, next to the code they describe, for reviewers and for the follow-up interview.

## Testing

### Backend (111 tests, `./mvnw test`)

| Test class | Type | What it covers |
|------------|------|----------------|
| `WithdrawalPolicyTest` (15) | Unit | Every business rule and boundary: age 65 and 66, the day before a 66th birthday, exactly 90% and one cent over, more than the balance, zero and negative amounts, rounding the limit down, money on hold lowering the available balance and the limit, and the retirement cut-off date used by the dashboard |
| `WithdrawalNoticeTest` (11) | Unit | The workflow on the entities: every allowed and refused move, holds placed and released, payment deducting the balance with the before and after snapshots, no double payment, and holds never above the available balance |
| `WithdrawalServiceTest` (15) | Unit (Mockito) | Submitting holds the amount without changing the balance, rounding to cents, money on hold counting against the next notice, unknown products and notices, nothing saved when a rule fails, approve then pay, reject with a trimmed reason, cancel, 409 for paying a pending notice, and who may take each step |
| `DashboardServiceTest` (3) | Unit (Mockito, fixed clock) | Totals per status, the amount on hold, the total and average paid out, the last 30 days, the six-month payment chart with empty months filled in, no notices at all (no division by zero), investors refused |
| `InvestorServiceTest` (2) | Unit (Mockito) | The client list combines each client's product and notice totals, including open notices, clients whose notices are not paid yet and clients without notices; investors cannot list clients |
| `CsvExportServiceTest` (6) | Unit | Header and rows with the status, empty payment columns until a notice is paid, an empty export, quoting, the protection against spreadsheet formulas, the file name |
| `LoginAttemptServiceTest` (6) | Unit (controllable clock) | Locking after repeated failures, unlocking after the lock expires, resetting on success, usernames in any letter case, unknown usernames treated the same way |
| `AccessGuardTest` (5) | Unit | Investors limited to their own portfolio and history; staff not limited |
| `WithdrawalControllerTest` (20) | Web layer with the real `SecurityConfig` | 201 with `Location`, 400 field errors, malformed JSON, 422 with `code`, 404, filter binding including repeated statuses, the workflow endpoints, a missing rejection reason (400), 409 with `code` and `currentStatus`, 401 without a session, 403 without a CSRF token, 403 for staff submitting or cancelling and for investors approving, rejecting or paying |
| `WithdrawalApiIntegrationTest` (19) | Full stack (`@SpringBootTest` with H2) | Runs as the real seeded users: roles, ownership of portfolios and notices, the staff dashboard and client totals, the age 65 rejection, the 90% rejection, money on hold refused for new notices, a notice that appears in the history and the CSV, the whole workflow from pending to paid with the balance checked at each step, cancelling, and rejecting with a reason the investor can see |
| `AuthIntegrationTest` (9) | Full stack | Real sign-in and session, the generic failure message, usernames in any letter case, CSRF required, the lock after 5 failures (429), sign-out, security headers |

### Frontend (47 tests, `npm test`)

- Notice workflow helpers: which buttons each role gets for each status, open statuses, paid totals added up in cents, oldest-first ordering and the rejection reason check.
- Client-side validation: the 90% boundary, money on hold, badly formatted amounts, the message for a restricted product, converting to cents without floating-point errors, and the date range check.
- The query string builder, including repeated status parameters.
- When the screens refresh themselves: a user counts as active until five minutes pass without any input.
- Dashboard helpers: month labels, short amounts, percentages and the ranking of top clients, with money handled in cents.
- The page to return to after sign-in, which only accepts paths inside the app so it cannot be used as an open redirect.

## Requirements checklist

### Backend (Spring Boot)

- [x] Retrieve an investor portfolio with details and products: `GET /api/investors/{id}/portfolio`
- [x] Create withdrawal notices with balance calculations: `POST /api/withdrawals`, with the balance deducted when staff pay the notice (`POST /api/withdrawals/{id}/pay`)
- [x] Export CSV statements with filtering: `GET /api/withdrawals/export?investorId&productId&status&from&to`

### Frontend (React)

- [x] Portfolio dashboard
- [x] Withdrawal form, with a live preview of the available balance
- [x] Withdrawal history table, with statuses, filters and totals
- [x] CSV download button
- [x] Connected to the backend APIs

### Business rules

- [x] Retirement withdrawals only if age > 65
- [x] A withdrawal must not exceed the balance
- [x] A withdrawal must not exceed 90% of the balance
- [x] Proper error handling and user feedback

### Advanced requirements (at least three are required, all five are implemented)

- [x] Global exception handling
- [x] DTO layer
- [x] Input validation
- [x] Unit tests (111 backend and 47 frontend)
- [x] UI validation

### Beyond the brief

- [x] Sign-in with Spring Security (session cookie, CSRF, BCrypt, sign-in lock), with role and ownership based access control
- [x] A withdrawal notice workflow (pending, approved, paid, with rejection and cancellation), with the amount held on the product until it is paid
- [x] Staff portal with a dashboard of statistics, charts and the notices waiting for staff, a client list with totals, a page for each client, and every withdrawal notice with filters, workflow buttons and CSV export
- [x] Enforced formatting and linting (Spotless, ESLint, Prettier)

### Other

- [x] Package `com.enviro.assessment.junior.smsibi`
- [x] H2 database
- [x] REST conventions
- [x] README with setup, API documentation, AI usage and screenshots

## AI usage disclosure

The assessment brief allows AI tools as long as their use is disclosed, and this project was built with the help of AI.

Tool and model. The project was built with Claude Code, Anthropic's AI coding assistant, used from Visual Studio Code
and the terminal. The model was Claude Opus 5 with the 1 million token context window (model ID `claude-opus-5[1m]`).

How it was used. I gave Claude Code the assessment brief and my instructions, and it wrote most of the code, the tests
and this README from them. This includes the Spring Boot backend (entities, services, business rules, the notice
workflow, security, error handling and tests), the React frontend, the development setup (installing JDK 21 for Spring
Boot 4), the screenshots and the git commits. Every commit made with it ends with the line "Co-Authored-By: Claude
Opus 5 (1M context)".

Other tools. Claude Code looked up current library documentation through Context7 instead of relying on memory, for
Spring Security 7, shadcn/ui, typescript-eslint, Spotless and React Router 8. The shadcn/ui command line tool generated
the components in `frontend/src/components/ui/` and the starting point for the sign-in page and the sidebar layout,
which were then adapted.

My decisions. I chose the technology stack and the package name, session cookies with CSRF protection rather than JWT,
the two roles (investors, and staff who review and pay notices), the staff dashboard and client pages, a workflow for
withdrawal notices (pending, approved, paid) instead of processing them immediately, shadcn/ui with Tailwind for the
interface, and linting and formatting as the development standards to enforce.

How the work was checked. All 111 backend tests and 47 frontend tests pass, and ESLint, Prettier and Spotless report no
problems. A script took notices through every workflow step and checked that each amount on the investor overview,
the staff dashboard and the client list changed by exactly the expected amount, and a browser test checked that the
screens show those changes without reloading, including changes made by another user. The API, the security rules (sign-in, CSRF, ownership and sign-out) and the notice workflow were also tested
by hand with curl, and the interface was tested in a browser with both investor and staff accounts.

My understanding of the code. I reviewed the generated code, and I can explain how every part works and why it was
built this way. The reasoning is written in comments next to the code and in the design decisions table above.
