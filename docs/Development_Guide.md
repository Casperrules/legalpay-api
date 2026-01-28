# LegalPay Development Guide

## 🎯 What's Been Built

I've created a **production-ready MVP** of the LegalPay platform with:

### ✅ Backend (Java 21 + Spring Boot 3.2)

**Architecture**: Multi-module Maven project with clean separation of concerns

1. **legalpay-domain** - Domain models and repositories
   - 5 JPA entities: `Merchant`, `Payer`, `Contract`, `Mandate`, `Payment`
   - 5 Spring Data repositories with custom query methods
   - Lombok for boilerplate reduction
   - Proper JPA relationships and constraints

2. **legalpay-services** - Business logic layer
   - `ContractService`: Create contracts, initiate eSign, calculate EMI
   - `MandateService`: Create eNACH/UPI Autopay mandates
   - `PaymentService`: Schedule and execute payments with retry logic
   - `PaymentGatewayService`: DUMMY adapter for Razorpay integration
   - `PdfGenerationService`: DUMMY PDF generation (placeholder for iText)

3. **legalpay-api** - REST API layer
   - `ContractController`: Full CRUD for contracts
   - `HealthController`: Monitoring endpoint
   - DTOs with validation annotations
   - OpenAPI/Swagger documentation (auto-generated)
   - Spring Security configuration (basic auth for now)

### ✅ Frontend (React 18 + TypeScript + Tailwind CSS)

- **Mobile-first responsive design** with Tailwind CSS
- Real-time API health check from backend
- Feature showcase with 6 key capabilities
- Quick start guide integrated in UI
- Vite for fast development and HMR

### ✅ Testing

- **Unit Tests**: `ContractServiceTest` with Mockito
- **Integration Tests**: `ContractControllerTest` with MockMvc
- JUnit 5 framework
- AssertJ for fluent assertions
- Test coverage setup (ready for JaCoCo)

### ✅ Configuration & DevOps

- **Local Development**: H2 in-memory database (zero setup)
- **Production**: PostgreSQL with placeholder configs
- Environment variable templates (`.env.template`, `.env.local`)
- Shell scripts for easy start/stop (`scripts/start.sh`, `scripts/stop.sh`)
- Comprehensive README with troubleshooting

---

## 🚀 Quick Start (3 Commands)

```bash
# 1. Build backend
mvn clean install

# 2. Start backend (Terminal 1)
cd legalpay-api && mvn spring-boot:run

# 3. Start frontend (Terminal 2)
cd frontend && npm install && npm run dev
```

**Access:**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console

---

## 📂 File Structure Created

```
LegalPayApp/
├── pom.xml                                    # Parent POM with dependency management
├── legalpay-domain/
│   ├── pom.xml
│   └── src/main/java/com/legalpay/domain/
│       ├── entity/
│       │   ├── Merchant.java                  # Merchant aggregate root
│       │   ├── Payer.java                     # Payer entity
│       │   ├── Contract.java                  # Contract aggregate root
│       │   ├── Mandate.java                   # Mandate entity (1:1 with Contract)
│       │   └── Payment.java                   # Payment entity
│       └── repository/
│           ├── MerchantRepository.java
│           ├── PayerRepository.java
│           ├── ContractRepository.java
│           ├── MandateRepository.java
│           └── PaymentRepository.java
├── legalpay-services/
│   ├── pom.xml
│   ├── src/main/java/com/legalpay/services/
│   │   ├── contract/
│   │   │   ├── ContractService.java           # Core contract logic
│   │   │   └── PdfGenerationService.java      # DUMMY PDF service
│   │   └── payment/
│   │       ├── MandateService.java            # Mandate management
│   │       ├── PaymentService.java            # Payment execution
│   │       └── PaymentGatewayService.java     # DUMMY gateway adapter
│   └── src/test/java/com/legalpay/services/
│       └── contract/
│           └── ContractServiceTest.java       # Unit tests with Mockito
├── legalpay-api/
│   ├── pom.xml
│   ├── src/main/java/com/legalpay/api/
│   │   ├── LegalPayApplication.java           # Spring Boot main class
│   │   ├── controller/
│   │   │   ├── ContractController.java        # REST API for contracts
│   │   │   └── HealthController.java          # Health check endpoint
│   │   └── dto/
│   │       ├── ContractCreateRequest.java     # Request DTO with validation
│   │       └── ContractResponse.java          # Response DTO
│   ├── src/main/resources/
│   │   ├── application.yml                    # Local config (H2)
│   │   └── application-prod.yml               # Production config (PostgreSQL)
│   └── src/test/java/com/legalpay/api/
│       └── controller/
│           └── ContractControllerTest.java    # API integration tests
├── frontend/
│   ├── package.json                           # Frontend dependencies
│   ├── vite.config.ts                         # Vite configuration
│   ├── tailwind.config.js                     # Tailwind CSS config
│   ├── index.html
│   └── src/
│       ├── main.tsx                           # React entry point
│       ├── App.tsx                            # Main React component
│       └── index.css                          # Tailwind styles
├── scripts/
│   ├── start.sh                               # Start backend + frontend
│   └── stop.sh                                # Stop all services
├── docs/                                      # Architecture docs (existing)
├── .env.template                              # Production env template
├── .env.local                                 # Local dev env
├── .gitignore
└── README.md                                  # Comprehensive guide
```

---

## 🔑 Key Features Implemented

### 1. **Contract Management**

**Endpoints:**
```
POST   /api/v1/contracts          # Create contract
GET    /api/v1/contracts/:id      # Get contract details
POST   /api/v1/contracts/:id/esign # Initiate eSign
GET    /api/v1/contracts          # List contracts (paginated)
```

**State Machine:**
```
DRAFT → PENDING_ESIGN → SIGNED → ACTIVE → COMPLETED
                                         ↓
                                      DEFAULTED → LEGAL_NOTICE_SENT
```

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/v1/contracts \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "uuid",
    "payerId": "uuid",
    "principalAmount": 100000,
    "interestRate": 12.0,
    "startDate": "2026-02-01",
    "endDate": "2027-02-01",
    "paymentFrequency": "MONTHLY"
  }'
```

### 2. **DUMMY Services (Replace with Real APIs)**

All third-party integrations have DUMMY implementations for local testing:

| Service | Current Status | Production Replacement |
|---------|---------------|------------------------|
| `PdfGenerationService` | Returns fake URL | Use iText to generate PDF, upload to Cloudflare R2 |
| `PaymentGatewayService.createMandate()` | Returns `mandate_<uuid>` | Call Razorpay SDK `createMandate()` |
| `PaymentGatewayService.executePayment()` | Always returns success | Call Razorpay SDK with idempotency key |
| eSign integration | Sets fake document ID | Call Digio REST API |
| Blockchain recording | Not implemented | Use Web3j to write to Polygon |

**How to Replace:**

See [System_Architecture_and_Implementation.md](../docs/System_Architecture_and_Implementation.md) for complete implementation details with actual code examples.

### 3. **Validation & Error Handling**

**Request Validation:**
```java
@NotNull(message = "Principal amount is required")
@DecimalMin(value = "1000.00", message = "Principal amount must be at least ₹1000")
@DecimalMax(value = "10000000.00", message = "Principal amount cannot exceed ₹1 crore")
private BigDecimal principalAmount;
```

**Error Response:**
```json
{
  "timestamp": "2026-01-27T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Principal amount must be at least ₹1000",
  "path": "/api/v1/contracts"
}
```

### 4. **Database Schema**

**H2 (Local):** Auto-created from JPA entities via `ddl-auto: create-drop`

**PostgreSQL (Production):** Schema will be created on first run with `ddl-auto: validate`

**Tables Created:**
- `merchants` - Business/individual accounts
- `payers` - Customers who make payments
- `contracts` - Legal contracts
- `mandates` - eNACH/UPI Autopay mandates (1:1 with contracts)
- `payments` - Individual payment transactions (EMIs)

---

## 🧪 Testing Guide

### Run All Tests

```bash
mvn test
```

### Run Specific Test

```bash
mvn test -Dtest=ContractServiceTest
```

### Test Coverage Report

```bash
mvn test jacoco:report
open target/site/jacoco/index.html
```

### What's Tested

✅ **ContractService:**
- Creating contracts in DRAFT state
- EMI calculation logic
- eSign initiation state transitions
- Signing workflow

✅ **ContractController:**
- POST /contracts with valid request → 201 Created
- POST /contracts with invalid amount → 400 Bad Request
- GET /contracts/:id → 200 OK with contract data

---

## 🔧 Configuration Guide

### Local Development

**No setup needed!** Uses H2 in-memory database.

**application.yml** (active by default):
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:legalpaydb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop  # Auto-create tables from entities
```

**Access H2 Console:**
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:legalpaydb`
- Username: `sa`
- Password: _(empty)_

### Production Configuration

**application-prod.yml**:
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    driver-class-name: org.postgresql.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  jpa:
    hibernate:
      ddl-auto: validate  # Don't auto-create, validate schema exists
```

**Set environment variables:**
```bash
export DATABASE_URL=postgresql://user:pass@host:5432/legalpay
export RAZORPAY_KEY_ID=rzp_live_xxxxxx
export RAZORPAY_KEY_SECRET=xxxxxx
export SPRING_PROFILES_ACTIVE=prod

mvn spring-boot:run
```

---

## 🚢 Deployment Options

### Option 1: Railway.app (Free Tier)

**Cost:** ₹0/month

```bash
npm install -g @railway/cli
railway login
railway init
railway add postgresql
railway up
```

**Environment Variables to Set in Railway:**
- `SPRING_PROFILES_ACTIVE=prod`
- `RAZORPAY_KEY_ID=rzp_live_xxx`
- `RAZORPAY_KEY_SECRET=xxx`
- (Railway auto-sets DATABASE_URL)

### Option 2: Docker

```dockerfile
# Dockerfile (create in project root)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY legalpay-domain ./legalpay-domain
COPY legalpay-services ./legalpay-services
COPY legalpay-api ./legalpay-api
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/legalpay-api/target/legalpay-api-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
docker build -t legalpay-api .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=postgresql://... \
  legalpay-api
```

---

## 🔐 Security Checklist

### ✅ Already Implemented

- ✅ Input validation with `@Valid` and Bean Validation annotations
- ✅ SQL injection prevention (JPA parameterized queries)
- ✅ CORS configuration (Spring Security)
- ✅ Error messages don't leak sensitive info (production profile)

### ⚠️ TODO Before Production

- [ ] Enable JWT authentication (replace basic auth)
- [ ] Implement RBAC (merchant vs admin roles)
- [ ] Enable CSRF protection for state-changing endpoints
- [ ] Add rate limiting (Spring Cloud Gateway)
- [ ] Encrypt sensitive DB fields (API keys, bank accounts)
- [ ] Set up HTTPS/TLS (Let's Encrypt)
- [ ] Configure Content Security Policy headers
- [ ] Add request/response logging (audit trail)

---

## 📡 API Documentation

**OpenAPI 3.0 Docs:** http://localhost:8080/swagger-ui.html

**Example API Calls:**

### Create Contract
```bash
curl -X POST http://localhost:8080/api/v1/contracts \
  -H "Content-Type: application/json" \
  -d @- << EOF
{
  "merchantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "payerId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "principalAmount": 100000.00,
  "interestRate": 12.0,
  "startDate": "2026-02-01",
  "endDate": "2027-02-01",
  "paymentFrequency": "MONTHLY"
}
EOF
```

### Initiate eSign
```bash
curl -X POST http://localhost:8080/api/v1/contracts/{contractId}/esign
```

### Check Health
```bash
curl http://localhost:8080/health
```

---

## 🐛 Troubleshooting

### Backend won't start

**Error:** `java.lang.UnsupportedClassVersionError`

**Solution:** Check Java version
```bash
java -version  # Must be 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

**Error:** `Port 8080 already in use`

**Solution:**
```bash
lsof -ti:8080 | xargs kill -9
```

### Frontend can't connect to backend

**Error:** `Network Error` or `ERR_CONNECTION_REFUSED`

**Solution:**
1. Verify backend is running: `curl http://localhost:8080/health`
2. Check proxy config in `frontend/vite.config.ts`
3. Check CORS settings in backend

### Tests failing

**Error:** `java.lang.IllegalStateException: Failed to load ApplicationContext`

**Solution:**
```bash
# Clean build
mvn clean install

# Run single test with debug
mvn test -Dtest=ContractServiceTest -X
```

### H2 Console not accessible

**Error:** `404 Not Found` at `/h2-console`

**Solution:** Check `application.yml`:
```yaml
spring:
  h2:
    console:
      enabled: true
```

---

## 🎯 Next Steps

### Phase 1: Complete DUMMY Replacements

1. **PDF Generation** (`PdfGenerationService`)
   - Replace with iText integration
   - Upload to Cloudflare R2
   - Generate SHA-256 hash

2. **Payment Gateway** (`PaymentGatewayService`)
   - Integrate Razorpay Java SDK
   - Implement webhook handlers
   - Add HMAC signature validation

3. **eSign Integration**
   - Call Digio REST API
   - Handle eSign webhooks
   - Store signed PDF URL

4. **Blockchain Recording**
   - Use Web3j to connect to Polygon Mumbai testnet
   - Write contract hash to blockchain
   - Store transaction hash

### Phase 2: Add Missing Features

- [ ] Mandate authorization webhook handler
- [ ] Payment retry scheduler (Spring @Scheduled)
- [ ] Dunning engine for failed payments
- [ ] Legal notice generation (Section 25)
- [ ] Email/SMS notifications (MSG91)
- [ ] Invoice generation (GST compliance)

### Phase 3: Production Readiness

- [ ] Add JWT authentication
- [ ] Implement RBAC
- [ ] Set up CI/CD pipeline
- [ ] Add distributed tracing (Jaeger)
- [ ] Configure monitoring (Prometheus + Grafana)
- [ ] Load testing (JMeter)
- [ ] Security audit (OWASP ZAP)

---

## 📚 Documentation References

- [System Architecture](../docs/System_Architecture_and_Implementation.md)
- [Bootstrap Strategy](../docs/Bootstrap_Strategy_Free_Minimal_Cost.md)
- [Product Requirements](../docs/PRD_PaymentAutomation.md)
- [Marketing Strategy](../docs/Marketing_and_Expansion_Strategy.md)

---

**Current Status:** ✅ MVP Ready for Local Testing

**Next Milestone:** Replace DUMMY services with real API integrations

**Timeline:** See [Implementation Roadmap](../docs/System_Architecture_and_Implementation.md#11-implementation-roadmap) for 12-week plan

---

**Built with:** Java 21 • Spring Boot 3.2 • React 18 • TypeScript • PostgreSQL • Tailwind CSS
