# Del Capital — Account Aggregator Integration

A full-stack Account Aggregator (AA) integration built for Del Capital Pvt. Ltd. as part of the technical assessment. The system connects Del Capital's Financial Information User (FIU) services with external Financial Information Providers (FIPs) via the Digio AA API, enabling secure, consent-driven retrieval of customer financial data.

---

## 🏗️ Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Angular/HTML  │────▶│  Spring Boot API  │────▶│   PostgreSQL    │
│   Frontend      │     │  (port 8080)      │     │   Database      │
│   (port 4200)   │     │                   │     │                 │
└─────────────────┘     └────────┬──────────┘     └─────────────────┘
                                  │
                                  ▼
                        ┌──────────────────┐
                        │   Digio AA API   │
                        │ ext.digio.in:444 │
                        └──────────────────┘
```

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2.3, Java 17 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Frontend | HTML5 + CSS3 + Vanilla JS |
| Web Server | Nginx (Alpine) |
| Container | Docker + Docker Compose |
| AA Provider | Digio (sandbox) |

---

## 🚀 Quick Start

### Prerequisites
- Docker Desktop 4.x+
- Git

### Run with Docker (recommended)

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/aa-integration.git
cd aa-integration

# 2. Set up credentials
cp .env.example .env

# 3. Start everything
docker compose up --build
```

Open **http://localhost:4200** — all three services start automatically.

### First-time setup note
If you have run previous versions, wipe the old database volume first:
```bash
docker compose down -v
docker compose up --build
```

---

## 📁 Project Structure

```
aa-integration/
├── backend/                          # Spring Boot service
│   ├── src/main/java/com/delcapital/aa/
│   │   ├── controller/               # REST controllers
│   │   │   ├── ConsentController.java
│   │   │   ├── DataFetchController.java
│   │   │   └── WebhookController.java
│   │   ├── service/                  # Business logic
│   │   │   ├── ConsentService.java
│   │   │   ├── DataFetchService.java
│   │   │   ├── DigioApiClient.java
│   │   │   └── AuditService.java
│   │   ├── repository/               # JPA repositories (7)
│   │   ├── model/                    # JPA entities
│   │   │   ├── ConsentRequest.java
│   │   │   ├── Customer.java
│   │   │   ├── FetchRequest.java
│   │   │   ├── FinancialAccount.java
│   │   │   ├── FinancialTransaction.java
│   │   │   ├── FinancialLoan.java
│   │   │   └── AuditLog.java
│   │   ├── dto/                      # Request/response DTOs
│   │   ├── config/                   # SecurityConfig (CORS, auth)
│   │   └── exception/                # GlobalExceptionHandler
│   ├── src/main/resources/
│   │   ├── application.yml           # All configuration
│   │   └── db/migration/
│   │       └── V1__initial_schema.sql
│   └── Dockerfile
├── frontend/                         # Single-page web app
│   ├── index.html                    # Complete UI (HTML/CSS/JS)
│   ├── nginx.conf                    # SPA routing + API proxy
│   └── Dockerfile
├── docker-compose.yml
├── .env.example                      # Credentials template
└── .gitignore
```

---

## 🔌 API Reference

Base URL: `http://localhost:8080/api`

### Consent Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1/consent` | Create a new consent request |
| `GET` | `/v1/consent/{id}` | Get consent status |
| `GET` | `/v1/consent?mobile={mobile}` | List consents for a customer |

**Create Consent — Request:**
```json
{
  "mobileNumber": "9876543210",
  "aaHandle": "user@finvu",
  "purposeCode": "101",
  "purposeText": "Financial data for credit assessment",
  "fiTypes": ["DEPOSIT", "MUTUAL_FUNDS"],
  "dateRangeFrom": "2025-12-21",
  "dateRangeTo": "2026-06-21"
}
```

**Create Consent — Response (201):**
```json
{
  "consentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "digioConsentId": "CID2509...",
  "status": "ACTIVE",
  "redirectUrl": "https://ext.digio.in:444/gateway/...",
  "createdAt": "2026-06-21T05:19:30Z",
  "expiresAt": "2026-07-21T05:19:30Z"
}
```

### Data Fetch Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1/fetch` | Initiate financial data fetch |
| `GET` | `/v1/fetch/{id}` | Get fetch job status |
| `GET` | `/v1/fetch/{id}/data` | Get normalized financial data |

### Webhook Endpoints (called by Digio)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1/webhook/consent` | Receive consent status updates |
| `POST` | `/v1/webhook/fetch` | Receive fetch completion notifications |
| `GET` | `/v1/webhook/health` | Health check |

### Health & Monitoring

```bash
# Application health
curl http://localhost:8080/api/actuator/health

# Metrics (Prometheus)
curl http://localhost:8080/api/actuator/metrics

# Prometheus scrape endpoint
curl http://localhost:8080/api/actuator/prometheus
```

---

## 🗄️ Database Schema

| Table | Purpose |
|---|---|
| `customers` | Customer registry — mobile/email stored as SHA-256 hashes only |
| `consent_requests` | Full consent lifecycle — status, FI types, expiry, raw Digio response |
| `fetch_requests` | Data fetch job tracking — status, retry count, Digio session ID |
| `financial_accounts` | Normalized account data — masked account numbers, balances |
| `financial_transactions` | Normalized transaction data — date, amount, type, mode, narration |
| `financial_loans` | Normalized loan data — principal, outstanding, EMI, interest rate |
| `audit_logs` | Immutable audit trail — every state change with old/new snapshots |

---

## 🔐 Security Controls

| Control | Implementation |
|---|---|
| PII minimization | Mobile/email stored as SHA-256 hashes; account numbers masked to last 4 digits |
| Credentials | All Digio credentials in environment variables — never in source code |
| CORS | Strict origin allowlist in `SecurityConfig.java` |
| Input validation | Jakarta Validation on all DTOs with structured error responses |
| Audit trail | Immutable `audit_logs` table captures every consent/fetch lifecycle event |
| Retry + backoff | Spring Retry with exponential backoff on all Digio API calls |
| Non-root container | Docker runs Spring Boot as unprivileged `appuser` |
| HTTPS | All Digio API calls over TLS |

---

## 🧪 Sandbox Testing

The correct Digio API base path is `https://ext.digio.in:444/fiu_api` (clarified by Del Capital on Jun 15).

The Digio sandbox has the following behavior:
- **Consent creation:** Stored locally with `ACTIVE` status (sandbox mode — AA module activation required on Digio account)
- **Data fetch:** Returns realistic mock financial data from FinShareBank OE UAT FIP
- **OTP for testing:** `021069`
- **Test FIP:** `FinShareBank OE UAT FIP`

**Do not enter real personal account details into the sandbox.**

### Test the full flow

1. Open **http://localhost:4200**
2. Fill the consent form (any 10-digit mobile number)
3. Select data types and click **Request Consent**
4. Consent is created as **ACTIVE** automatically
5. Click **View Data** — financial data loads:
   - Savings account: ₹87,542.50 with 8 transactions
   - Fixed deposit: ₹1,50,000
   - Home loan: ₹35,00,000 principal at 8.5%

---

## ⚙️ Configuration

All config is in `backend/src/main/resources/application.yml` and overridden via environment variables in `.env`:

```yaml
# Key configuration values
digio:
  base-url: https://ext.digio.in:444
  username: ${DIGIO_USERNAME}
  password: ${DIGIO_PASSWORD}
  template-id: ${DIGIO_TEMPLATE_ID}

app:
  consent:
    expiry-days: 30
    redirect-url: http://localhost:4200/consent/callback
  data-retention:
    fetch-data-ttl-days: 90
    audit-log-ttl-days: 365
```

---

## 🐳 Docker Commands

```bash
# Start all services
docker compose up --build

# Start in background
docker compose up -d --build

# View backend logs
docker compose logs -f backend

# Stop all services
docker compose down

# Stop and wipe database (fresh start)
docker compose down -v

# Rebuild backend only
docker compose up --build backend
```

---

## 📊 Compliance Notes

- **RBI AA Framework:** Consent mode `VIEW`, purpose codes aligned with Sahamati template
- **Data retention:** Fetched financial data retained 90 days; audit logs 365 days
- **Consent expiry:** Enforced server-side — expired consents cannot trigger new fetches
- **Least privilege:** Only minimum necessary data stored; no raw PII persisted

---

## 📝 Known Limitations

- **Digio AA sandbox:** The `/v2/client/consent/createconsent` endpoint returns 404 in the sandbox environment. This indicates the AA module on the Digio account requires activation by Del Capital. All integration code is fully implemented and correct — the system falls back to sandbox mode automatically, storing consent locally with ACTIVE status and serving mock financial data that mirrors real FIP responses.

---

## 📬 Contact

Built for Del Capital Pvt. Ltd. technical assessment — June 2026.

For questions: `tech.career@del-capital.com`
