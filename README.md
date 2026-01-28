# LegalPay Platform

<p align="center">
  <strong>Contract-to-Cash Automation for India</strong>
</p>

<p align="center">
  Java 21 • Spring Boot 3.2 • React 18 • TypeScript • PostgreSQL
</p>

---

## 🚀 Quick Start

### Prerequisites

- **Java 21** (LTS)
- **Maven 3.9+**
- **Node.js 20** (LTS)
- **PostgreSQL 15** (for production) or H2 (auto-configured for local)

### Local Development (5 Minutes)

```bash
# 1. Clone repository
cd /Volumes/Mac_backup\ 1/LegalPayApp

# 2. Build backend (one-time)
mvn clean install

# 3. Start backend (Terminal 1)
cd legalpay-api
mvn spring-boot:run

# Backend will start at: http://localhost:8080
# API Docs: http://localhost:8080/swagger-ui.html
# H2 Console: http://localhost:8080/h2-console

# 4. Start frontend (Terminal 2)
cd frontend
npm install
npm run dev

# Frontend will start at: http://localhost:3000
```

**That's it!** The app is running with H2 in-memory database and dummy services.

---

## 📁 Project Structure

```
LegalPayApp/
├── pom.xml                          # Parent Maven POM
├── legalpay-domain/                 # Domain layer (entities, repositories)
│   ├── src/main/java/com/legalpay/domain/
│   │   ├── entity/                  # JPA entities
│   │   │   ├── Merchant.java
│   │   │   ├── Payer.java
│   │   │   ├── Contract.java
│   │   │   ├── Mandate.java
│   │   │   └── Payment.java
│   │   └── repository/              # Spring Data repositories
│   └── pom.xml
├── legalpay-services/               # Business logic layer
│   ├── src/main/java/com/legalpay/services/
│   │   ├── contract/
│   │   │   ├── ContractService.java
│   │   │   └── PdfGenerationService.java (DUMMY)
│   │   └── payment/
│   │       ├── MandateService.java
│   │       ├── PaymentService.java
│   │       └── PaymentGatewayService.java (DUMMY)
│   └── pom.xml
├── legalpay-api/                    # REST API layer
│   ├── src/main/java/com/legalpay/api/
│   │   ├── LegalPayApplication.java # Spring Boot main class
│   │   ├── controller/
│   │   │   ├── ContractController.java
│   │   │   └── HealthController.java
│   │   └── dto/                     # Request/Response DTOs
│   ├── src/main/resources/
│   │   ├── application.yml          # Local config (H2)
│   │   └── application-prod.yml     # Production config (PostgreSQL)
│   └── pom.xml
├── frontend/                        # React TypeScript UI
│   ├── src/
│   │   ├── App.tsx                  # Main component
│   │   └── main.tsx
│   ├── package.json
│   └── vite.config.ts
├── docs/                            # Architecture documentation
│   ├── System_Architecture_and_Implementation.md
│   ├── Bootstrap_Strategy_Free_Minimal_Cost.md
│   ├── PRD_PaymentAutomation.md
│   └── Marketing_and_Expansion_Strategy.md
├── .env.template                    # Environment variables template
├── .env.local                       # Local development env
└── README.md
```

---

## 🏗️ Architecture

**Design Pattern**: Domain-Driven Design (DDD) with Layered Architecture

```
┌─────────────────────────────────────┐
│         React Frontend (Port 3000)   │
└────────────┬────────────────────────┘
             │ HTTP/REST
┌────────────▼────────────────────────┐
│    API Layer (Controllers + DTOs)   │  ← legalpay-api
├─────────────────────────────────────┤
│  Service Layer (Business Logic)     │  ← legalpay-services
├─────────────────────────────────────┤
│  Domain Layer (Entities + Repos)    │  ← legalpay-domain
├─────────────────────────────────────┤
│  PostgreSQL / H2 (Database)         │
└─────────────────────────────────────┘
```

**Key Patterns Used:**

- ✅ **Repository Pattern**: Data access abstraction
- ✅ **Service Layer**: Business logic encapsulation
- ✅ **DTO Pattern**: API request/response separation
- ✅ **Builder Pattern**: Complex object construction (Lombok)
- ✅ **Adapter Pattern**: Third-party API abstraction (PaymentGatewayService)
- ✅ **Strategy Pattern**: Different payment frequencies, retry policies

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=ContractServiceTest

# Run only unit tests
mvn test -Dgroups=unit

# Run only integration tests
mvn test -Dgroups=integration
```

**Test Coverage:**

- ✅ Unit Tests: `ContractServiceTest`, `PaymentServiceTest`
- ✅ Integration Tests: `ContractControllerTest`
- 🎯 Target: 80%+ coverage

---

## 🔧 Configuration

### Local Development

Uses H2 in-memory database (auto-configured). No setup needed.

**Database Console**: http://localhost:8080/h2-console

- URL: `jdbc:h2:mem:legalpaydb`
- Username: `sa`
- Password: _(empty)_

### Production Deployment

1. **Copy environment template:**

```bash
cp .env.template .env
```

2. **Fill in actual values** (see `.env.template` for all variables):

```bash
# Database (Supabase/PostgreSQL)
DATABASE_URL=postgresql://user:pass@host:5432/legalpay

# Payment Gateway
RAZORPAY_KEY_ID=rzp_live_xxxxxx
RAZORPAY_KEY_SECRET=xxxxxx

# eSign Provider
DIGIO_API_KEY=xxxxxx

# Blockchain
POLYGON_RPC_URL=https://polygon-rpc.com
POLYGON_PRIVATE_KEY=0xabcdef...

# Cloud Storage
R2_ENDPOINT=https://xxx.r2.cloudflarestorage.com
R2_ACCESS_KEY_ID=xxxxxx
R2_SECRET_ACCESS_KEY=xxxxxx
```

3. **Run with production profile:**

```bash
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run
```

---

## 📡 API Endpoints

### Health Check

```
GET /health
```

**Response:**

```json
{
  "status": "UP",
  "timestamp": "2026-01-27T10:00:00Z",
  "service": "legalpay-api",
  "version": "1.0.0-SNAPSHOT"
}
```

### Contracts

```
POST   /api/v1/contracts         # Create contract
GET    /api/v1/contracts/:id     # Get contract
POST   /api/v1/contracts/:id/esign  # Initiate eSign
GET    /api/v1/contracts         # List contracts (paginated)
```

**Example: Create Contract**

```bash
curl -X POST http://localhost:8080/api/v1/contracts \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "uuid-here",
    "payerId": "uuid-here",
    "principalAmount": 100000,
    "interestRate": 12.0,
    "startDate": "2026-02-01",
    "endDate": "2027-02-01",
    "paymentFrequency": "MONTHLY"
  }'
```

**OpenAPI Documentation**: http://localhost:8080/swagger-ui.html

---

## 🚢 Deployment

### Quick Start: Railway + Vercel (Recommended)

**Complete step-by-step guide:** [docs/RAILWAY_VERCEL_QUICK_DEPLOY.md](docs/RAILWAY_VERCEL_QUICK_DEPLOY.md)

**Environment variables guide:** [docs/ENV_VARIABLES_GUIDE.md](docs/ENV_VARIABLES_GUIDE.md)

**Time:** 2-3 hours | **Cost:** $5-20/month

1. **Push to GitHub**
2. **Deploy Backend on Railway** (with PostgreSQL)
3. **Deploy Frontend on Vercel**
4. **Configure services:**
   - Razorpay (payments)
   - Resend (emails)
   - Polygon (blockchain - optional)

### Full Deployment Options

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for:

- Railway + Vercel (Easiest)
- AWS EC2 + S3 (Enterprise)
- DigitalOcean (Budget)
- Docker + Kubernetes (Advanced)

### Local Testing Only

```bash
# Backend
cd legalpay-api
mvn spring-boot:run
# Runs on http://localhost:8080

# Frontend
cd frontend
npm run dev
# Runs on http://localhost:3000
```

---

## 🛡️ Security

- ✅ **Spring Security**: JWT authentication (production)
- ✅ **HTTPS**: TLS 1.3 (Let's Encrypt in production)
- ✅ **SQL Injection**: Prevented by JPA parameterized queries
- ✅ **CSRF**: Protection enabled for state-changing endpoints
- ✅ **Sensitive Data**: API keys stored as encrypted byte arrays
- ✅ **Password Hashing**: BCrypt (for merchant/payer passwords)
- ✅ **Rate Limiting**: Spring Cloud Gateway (production)

---

## 📊 Monitoring

**Prometheus Metrics**: http://localhost:8080/actuator/prometheus

```bash
# Health check
curl http://localhost:8080/actuator/health

# Application metrics
curl http://localhost:8080/actuator/metrics
```

**Grafana Dashboard** (Production):

- JVM metrics (heap, threads, GC)
- API latency (p50, p95, p99)
- Database connection pool
- Payment success rate

---

## 🤝 Contributing

1. Create feature branch: `git checkout -b feature/contract-pdf-generation`
2. Write tests (80%+ coverage required)
3. Follow code style: `mvn spotless:apply`
4. Commit: `git commit -m "feat: add contract PDF generation"`
5. Push: `git push origin feature/contract-pdf-generation`
6. Open Pull Request

---

## 📝 License

Proprietary - LegalPay Platform © 2026

---

## 🆘 Troubleshooting

### Backend won't start

```bash
# Check Java version
java -version  # Must be 21

# Clean build
mvn clean install -DskipTests

# Check port 8080
lsof -i :8080  # Kill any process using port 8080
```

### Frontend won't connect to backend

```bash
# Verify backend is running
curl http://localhost:8080/health

# Check proxy configuration in vite.config.ts
# Should proxy /api requests to http://localhost:8080
```

### Tests failing

```bash
# Run specific test with debug
mvn test -Dtest=ContractServiceTest -X

# Skip tests for quick build
mvn clean install -DskipTests
```

---

## 📞 Support

**Deployment Guides:**

- 🚀 [Quick Deploy (Railway + Vercel)](./docs/RAILWAY_VERCEL_QUICK_DEPLOY.md)
- ⚙️ [Environment Variables Guide](./docs/ENV_VARIABLES_GUIDE.md)
- 📖 [Complete Deployment Guide](./docs/DEPLOYMENT.md)

**Integration Guides:**

- 💳 [Payment Integration (Razorpay)](./docs/Payment_Integration_Implementation_Guide.md)
- 📧 [Email Integration (Resend)](./docs/Email_Integration_Guide.md)
- ⛓️ [Blockchain Integration (Polygon)](./docs/Blockchain_Integration_Guide.md)

**Architecture & Strategy:**

- 🏗️ [System Architecture](./docs/System_Architecture_and_Implementation.md)
- 📋 [Product Requirements](./docs/PRD_PaymentAutomation.md)
- 💰 [Bootstrap Strategy](./docs/Bootstrap_Strategy_Free_Minimal_Cost.md)

**API Documentation:** http://localhost:8080/swagger-ui.html (when running locally)

---

**Built with ❤️ for the Indian fintech ecosystem**
