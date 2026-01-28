# LegalPay: System Architecture & Implementation Plan

**Version:** 1.0  
**Date:** 26 January 2026  
**Status:** Technical Design Document  
**Author:** Senior Product Engineer

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [High-Level Architecture (HLD)](#2-high-level-architecture-hld)
3. [Low-Level Design (LLD)](#3-low-level-design-lld)
4. [API Wrapper Layer Design](#4-api-wrapper-layer-design)
5. [Data Models & Database Schema](#5-data-models--database-schema)
6. [State Machines & Workflows](#6-state-machines--workflows)
7. [Security & Compliance Architecture](#7-security--compliance-architecture)
8. [Error Handling & Resilience](#8-error-handling--resilience)
9. [Observability & Monitoring](#9-observability--monitoring)
10. [Deployment & Infrastructure](#10-deployment--infrastructure)
11. [Implementation Roadmap](#11-implementation-roadmap)
12. [Edge Cases & Mitigation](#12-edge-cases--mitigation)

---

## 1. Executive Summary

LegalPay is a contract-to-cash automation platform that bridges digital contracts with banking execution. This document defines the complete system architecture, implementation patterns, and technical specifications needed to build a production-ready, legally compliant platform.

**🚀 Bootstrap-First Approach:** See [Bootstrap_Strategy_Free_Minimal_Cost.md](Bootstrap_Strategy_Free_Minimal_Cost.md) for ₹0-₹5000/month launch plan using free tiers.

**Core Technical Pillars:**

- **API-First Design:** All third-party integrations (eSign, eStamp, Payment Gateways, Blockchain) are wrapped behind internal service abstractions
- **Event-Driven Architecture:** Async processing for payment retries, notifications, and legal escalations
- **Audit-First Logging:** Every state transition, API call, and user action is logged with cryptographic proofs
- **Idempotency by Design:** All payment operations use idempotency keys; all webhooks are replay-safe
- **Multi-Tenant:** Merchant isolation at data and API key level
- **Cost-Optimized:** Free tier options for all services (Railway, Supabase, Upstash, Cloudflare R2)

### 1.1 Technology Stack Strategy

**Primary Stack: Java 21 + Spring Boot** ✅

**Why Full Java Backend:**

1. **Proven Fintech Track Record**:
   - HDFC Bank, ICICI Bank, SBI core banking systems
   - Razorpay/Cashfree backend (Java/Kotlin)
   - PhonePe, Paytm microservices
   - NSE/BSE trading platforms

2. **Enterprise-Grade Security**:
   - Spring Security (PCI DSS v4.0 compliant patterns)
   - Bouncy Castle (FIPS 140-2 certified crypto)
   - Built-in protection: CSRF, XSS, SQL injection
   - Extensive security audits by banking industry

3. **Performance & Scalability**:
   - Virtual Threads (Java 21): 10x better concurrency than Node.js
   - JVM JIT compiler: Optimizes hot paths at runtime
   - Proven to handle millions of TPS (banking systems)
   - Better memory management than garbage-collected JS

4. **Type Safety & Maintainability**:
   - Compile-time type checking (catch bugs before production)
   - Better refactoring support in IntelliJ IDEA
   - Stronger contracts between services
   - Less runtime errors compared to TypeScript

5. **Ecosystem for Fintech**:
   - Spring Cloud: Service discovery, circuit breakers, config
   - Spring Data JPA: Type-safe database access
   - Micrometer: Production-grade metrics
   - Extensive payment gateway SDKs (Razorpay Java SDK, Cashfree SDK)

6. **Talent Pool in India**:
   - Largest developer community (Java > JavaScript for backend)
   - Fintech experience: Most Indian payment companies use Java
   - Lower training cost: Enterprise Java patterns well-documented

**What About Node.js Payment SDKs?**

- Razorpay has official Java SDK: `com.razorpay:razorpay-java:1.4.3`
- Cashfree has Java SDK: `com.cashfree:cashfree-pg-sdk-java:2.0.0`
- Leegality/Digio: REST APIs (easy to integrate with RestTemplate/WebClient)
- Web3j: `org.web3j:core:4.9.8` for Polygon blockchain

**Stack Summary:**

```
Language:            Java 21 (LTS)
Framework:           Spring Boot 3.2 + Spring Cloud
Build Tool:          Maven / Gradle
Database Access:     Spring Data JPA + Hibernate

🆓 FREE TIER OPTIONS (Bootstrap):
Database:            Supabase (500MB free) → PostgreSQL 15
Cache:               Upstash (10K commands/day free) → Redis 7
Message Queue:       Railway RabbitMQ (included in $5/mo credit)
Storage:             Cloudflare R2 (10GB free, zero egress fees)
Hosting:             Railway.app ($5/mo credit) or Render (free tier)
Monitoring:          Grafana Cloud (10K metrics/mo free)

💰 PAID TIER (Scale):
Database:            AWS RDS PostgreSQL / Supabase Pro (₹800/mo)
Cache:               AWS ElastiCache / Upstash Pay-as-go
Message Queue:       AWS SQS (1M requests/mo free) / RabbitMQ cluster
Storage:             AWS S3 / Cloudflare R2 (paid tier)
Hosting:             AWS EKS / GCP GKE with gVisor

Blockchain:          Web3j → Polygon Mumbai (testnet, ₹0) → Mainnet (₹0.50/tx)
API Gateway:         Spring Cloud Gateway
Security:            Spring Security 6 + OAuth2
Observability:       Micrometer + Prometheus + Grafana + Jaeger
Testing:             JUnit 5 + Mockito + TestContainers
```

**Future Considerations:**

- **Kotlin**: Migrate gradually for more concise code (Spring supports Kotlin natively)
- **GraalVM**: Compile to native binaries for faster startup (useful for serverless)
- **Rust**: Only if cryptographic operations require memory-safe guarantees (unlikely)

---

## 2. High-Level Architecture (HLD)

### 2.1 System Context Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         LegalPay Platform                        │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  Web Portal  │  │  Mobile App  │  │  Public API  │          │
│  │ (Merchant)   │  │  (Payer)     │  │  (Partners)  │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                  │                  │                   │
│         └──────────────────┴──────────────────┘                   │
│                            │                                      │
│                   ┌────────▼────────┐                            │
│                   │  API Gateway    │ ◄── Rate Limiting          │
│                   │  (Kong/Nginx)   │ ◄── Auth (JWT)             │
│                   └────────┬────────┘                            │
│                            │                                      │
│         ┌──────────────────┼──────────────────┐                  │
│         │                  │                  │                  │
│    ┌────▼─────┐   ┌───────▼──────┐   ┌──────▼──────┐           │
│    │ Contract │   │   Mandate    │   │   Payment   │           │
│    │ Service  │   │   Service    │   │   Service   │           │
│    └────┬─────┘   └───────┬──────┘   └──────┬──────┘           │
│         │                  │                  │                  │
│    ┌────▼─────┐   ┌───────▼──────┐   ┌──────▼──────┐           │
│    │ eSign    │   │  Payment     │   │   Dunning   │           │
│    │ Wrapper  │   │  Gateway     │   │   Engine    │           │
│    │          │   │  Wrapper     │   │             │           │
│    └────┬─────┘   └───────┬──────┘   └──────┬──────┘           │
│         │                  │                  │                  │
│         │         ┌────────▼────────┐         │                  │
│         │         │  Notification   │         │                  │
│         │         │   Service       │         │                  │
│         │         └────────┬────────┘         │                  │
│         │                  │                  │                  │
│    ┌────▼──────────────────▼──────────────────▼─────┐           │
│    │         Event Bus (RabbitMQ/SQS/Kafka)         │           │
│    └────────────────────────┬───────────────────────┘           │
│                             │                                    │
│    ┌────────────────────────▼───────────────────────┐           │
│    │  Background Workers (Payment Retry, Legal)     │           │
│    └────────────────────────┬───────────────────────┘           │
│                             │                                    │
│    ┌────────────────────────▼───────────────────────┐           │
│    │  Evidence Layer (Blockchain + Off-Chain)       │           │
│    └────────────────────────────────────────────────┘           │
└───────────────────────────────────────────────────────────────────┘
           │                  │                  │
    ┌──────▼──────┐  ┌───────▼──────┐  ┌───────▼──────┐
    │ PostgreSQL  │  │   S3/GCS     │  │   Redis      │
    │ (Primary)   │  │ (Documents)  │  │ (Cache/Lock) │
    └─────────────┘  └──────────────┘  └──────────────┘
```

### 2.2 External Integrations

```
┌──────────────────────────────────────────────────────┐
│              Third-Party Services                     │
├──────────────────────────────────────────────────────┤
│                                                       │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐     │
│  │  Leegality │  │   Digio    │  │  Razorpay  │     │
│  │  /eSign    │  │  /eStamp   │  │  /Cashfree │     │
│  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘     │
│        │               │               │             │
│        └───────────────┴───────────────┘             │
│                        │                             │
│               ┌────────▼────────┐                    │
│               │  API Wrappers   │                    │
│               │  (Retry, Cache) │                    │
│               └────────┬────────┘                    │
│                        │                             │
│  ┌─────────────────────┼─────────────────────┐      │
│  │                     │                     │       │
│  │  ┌────────────┐  ┌──▼──────────┐  ┌──────▼────┐ │
│  │  │  Twilio    │  │   Polygon   │  │ TRAI DLT  │ │
│  │  │  /Gupshup  │  │  (Blockchain)│  │ (SMS Reg) │ │
│  │  └────────────┘  └─────────────┘  └───────────┘ │
│  │                                                   │
│  └───────────────────────────────────────────────────┘
│
└──────────────────────────────────────────────────────┘
```

### 2.3 Core Services Architecture

**Microservices Pattern with Domain-Driven Design (DDD)**

| Service                     | Responsibility                | Technology                     | Scale Pattern                | Key Libraries                          |
| --------------------------- | ----------------------------- | ------------------------------ | ---------------------------- | -------------------------------------- |
| **API Gateway**             | Auth, Rate Limiting, Routing  | Spring Cloud Gateway           | Horizontal (Stateless)       | Spring Security, Redis (rate limiter)  |
| **Contract Service**        | PDF gen, hashing, storage     | Spring Boot 3.2                | Horizontal                   | iText, AWS SDK, Spring Data JPA        |
| **eSign Wrapper**           | Abstract Leegality/Digio APIs | Spring Boot 3.2                | Horizontal + Circuit Breaker | Resilience4j, RestTemplate             |
| **Mandate Service**         | eNACH/UPI Autopay creation    | Spring Boot 3.2                | Horizontal                   | Razorpay SDK, Cashfree SDK             |
| **Payment Gateway Wrapper** | Abstract Razorpay/Cashfree    | Spring Boot 3.2                | Horizontal + Idempotency     | Razorpay Java SDK, Redis (idempotency) |
| **Payment Service**         | Orchestrate debit execution   | Spring Boot 3.2                | Horizontal                   | Spring State Machine                   |
| **Dunning Engine**          | Retry logic + state machine   | Spring Boot 3.2 + Spring Batch | Background workers           | Quartz Scheduler, Spring AMQP          |
| **Notification Service**    | Email/SMS/WhatsApp dispatcher | Spring Boot 3.2                | Horizontal + Queue           | Twilio SDK, Gupshup SDK                |
| **Crypto Service**          | Sign/verify, hash computation | Spring Boot 3.2                | Vertical + Cache             | Bouncy Castle, Spring Cache            |
| **Legal Service**           | Generate Section 25 notices   | Spring Boot 3.2                | Background workers           | iText, Thymeleaf                       |
| **Evidence Service**        | Blockchain + off-chain proofs | Spring Boot 3.2                | Vertical (Gas optimization)  | Web3j, AWS S3 SDK                      |
| **Audit Logger**            | Write-only event log          | Spring Boot 3.2                | Write-heavy optimized        | Spring Data JPA (batch inserts)        |

**Technology Rationale:**

- All services use **Java 21 + Spring Boot 3.2** for consistency
- **Virtual Threads**: Enable high concurrency without blocking (10K+ concurrent requests/JVM)
- **Spring Cloud**: Service discovery (Eureka), config (Spring Cloud Config), tracing (Sleuth)
- **Spring Security**: JWT authentication, RBAC, CSRF protection
- **Resilience4j**: Circuit breaker, retry, rate limiter, bulkhead patterns

---

## 3. Low-Level Design (LLD) with Design Patterns

### 3.0 Core Design Principles & Patterns

**SOLID Principles:**

- **S**ingle Responsibility: Each service has one reason to change
- **O**pen/Closed: Extensible via interfaces (new payment gateways without modifying core)
- **L**iskov Substitution: All adapters implement same interface contract
- **I**nterface Segregation: Thin, focused interfaces (IPaymentGateway, IESignProvider)
- **D**ependency Inversion: Depend on abstractions (interfaces), not concrete implementations

**Design Patterns Used:**

| Pattern                     | Usage                                            | Location                           |
| --------------------------- | ------------------------------------------------ | ---------------------------------- |
| **Factory**                 | Create payment gateway instances based on config | `PaymentGatewayFactory.java`       |
| **Strategy**                | Different retry policies (exponential, linear)   | `RetryStrategy.java`               |
| **Adapter**                 | Wrap Razorpay, Cashfree, Leegality APIs          | `RazorpayAdapter.java`             |
| **Repository**              | Abstract data access layer                       | `ContractRepository.java`          |
| **Builder**                 | Complex object construction (Contract, Payment)  | Lombok `@Builder`                  |
| **Observer**                | Event-driven notifications                       | Spring `@EventListener`            |
| **Chain of Responsibility** | Validation pipeline, middleware                  | Spring Filter chain                |
| **Template Method**         | Common payment workflow                          | `AbstractPaymentService.java`      |
| **State**                   | Contract lifecycle (DRAFT → SIGNED → ACTIVE)     | Spring State Machine               |
| **Decorator**               | Add logging, metrics to services                 | `@Aspect` (AOP)                    |
| **Facade**                  | Simplify complex subsystems                      | `PaymentOrchestrationService.java` |
| **Proxy**                   | Circuit breaker, caching                         | Resilience4j proxies               |

**Domain-Driven Design (DDD):**

- **Aggregates**: Contract (root), Mandate, Payment
- **Value Objects**: Money, ContractTerms, Address (immutable)
- **Entities**: Merchant, Payer (have identity)
- **Domain Events**: ContractSigned, PaymentExecuted, MandateCreated
- **Repositories**: Clean separation of domain and persistence

---

### 3.1 Contract Service

**Responsibilities:**

- Generate PDF contracts from templates
- Hash contracts (SHA-256) before and after eSign
- Store signed contracts in S3 with encryption
- Trigger eSign workflow via eSign Wrapper

**Design Patterns Applied:**

- **Builder Pattern**: Contract object construction
- **Template Method**: Common PDF generation workflow
- **Strategy Pattern**: Different template engines (Thymeleaf, FreeMarker)
- **Repository Pattern**: Data access abstraction

**API Endpoints:**

```java
// ContractController.java
@RestController
@RequestMapping("/api/v1/contracts")
@Validated
public class ContractController {

  @Autowired
  private ContractService contractService;

  @PostMapping
  public ResponseEntity<ContractResponse> createContract(
      @Valid @RequestBody CreateContractRequest request) {

    ContractResponse response = contractService.createContract(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/{contractId}/esign")
  public ResponseEntity<ESignResponse> initiateESign(
      @PathVariable UUID contractId,
      @Valid @RequestBody ESignRequest request) {

    ESignResponse response = contractService.initiateESign(contractId, request);
    return ResponseEntity.ok(response);
  }
}

// Request/Response DTOs
@Data
@Validated
public class CreateContractRequest {
  @NotNull private UUID merchantId;
  @NotNull private UUID payerId;
  @Positive private BigDecimal amount;
  @NotNull private String currency = "INR";
  @Future private LocalDateTime dueDate;
  @NotNull private ContractTerms terms;
  @NotBlank private String templateId;
}

@Data
public class ContractResponse {
  private UUID contractId;
  private String preSignHash;
  private ContractStatus status;
  private String pdfUrl; // Pre-signed S3 URL, 5min expiry
}

// Webhook endpoint
@PostMapping("/webhooks/esign/{provider}")
public ResponseEntity<Void> handleESignWebhook(
    @PathVariable String provider,
    @RequestHeader("X-Signature") String signature,
    @RequestBody String payload) {

  webhookService.processESignCallback(provider, signature, payload);
  return ResponseEntity.ok().build();
}
```

Action: - Validate HMAC - Download signed PDF - Compute postSignHash - Update contract status - Trigger mandate creation - Persist evidence to blockchain

````

**Database Schema (Contracts Table):**

```sql
CREATE TABLE contracts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  merchant_id UUID NOT NULL REFERENCES merchants(id),
  payer_id UUID NOT NULL REFERENCES payers(id),

  -- Contract Details
  amount DECIMAL(15,2) NOT NULL CHECK (amount > 0),
  currency VARCHAR(3) DEFAULT 'INR',
  due_date TIMESTAMP NOT NULL,
  terms JSONB NOT NULL,
  template_id VARCHAR(50) NOT NULL,

  -- Document Storage
  pre_sign_pdf_s3_key VARCHAR(255) NOT NULL,
  post_sign_pdf_s3_key VARCHAR(255),
  pre_sign_hash VARCHAR(64) NOT NULL, -- SHA-256
  post_sign_hash VARCHAR(64),

  -- eSign Tracking
  esign_provider VARCHAR(20), -- 'leegality' | 'digio'
  esign_request_id VARCHAR(100),
  esign_status VARCHAR(20), -- 'PENDING' | 'INITIATED' | 'SUCCESS' | 'FAILED'
  esign_completed_at TIMESTAMP,

  -- Stamp Duty
  stamp_provider VARCHAR(20),
  stamp_certificate_number VARCHAR(100),
  stamp_state VARCHAR(50),
  stamp_amount DECIMAL(10,2),

  -- State
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  -- DRAFT -> ESIGN_INITIATED -> SIGNED -> MANDATE_CREATED -> ACTIVE

  -- Audit
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  created_by_ip INET,

  -- Indexes
  INDEX idx_merchant_id (merchant_id),
  INDEX idx_payer_id (payer_id),
  INDEX idx_status (status),
  INDEX idx_due_date (due_date)
);
````

**Edge Cases & Handling:**

| Edge Case                               | Mitigation                                                                |
| --------------------------------------- | ------------------------------------------------------------------------- |
| eSign provider timeout (>30s)           | Queue callback processing; retry webhook delivery 3x with exp backoff     |
| PDF generation fails mid-process        | Java service returns error; rollback transaction; retry once              |
| Payer closes eSign page without signing | Set 48h expiry on eSign link; send reminder at T+24h                      |
| Duplicate webhook delivery              | Use `esign_request_id` as idempotency key; check status before processing |
| Hash mismatch after signing (tampering) | Reject contract; log security event; notify merchant                      |

**PDF Generation Service (Embedded in Contract Service):**

```java
// ContractService.java
@Service
@Slf4j
public class ContractService {

  @Autowired private PDFGenerationService pdfService;
  @Autowired private S3Service s3Service;
  @Autowired private HashingService hashingService;
  @Autowired private ContractRepository contractRepository;

  @Transactional
  public ContractResponse createContract(CreateContractRequest request) {
    // Generate PDF from template
    byte[] pdfBytes = pdfService.renderFromTemplate(
      request.getTemplateId(),
      request.getTerms()
    );

    // Compute pre-sign hash
    String preSignHash = hashingService.sha256(pdfBytes);

    // Upload to S3
    String s3Key = s3Service.uploadPDF(
      "contracts/" + UUID.randomUUID() + "/pre-sign.pdf",
      pdfBytes
    );

    // Save contract entity
    Contract contract = Contract.builder()
      .merchantId(request.getMerchantId())
      .payerId(request.getPayerId())
      .amount(request.getAmount())
      .dueDate(request.getDueDate())
      .preSignPdfS3Key(s3Key)
      .preSignHash(preSignHash)
      .status(ContractStatus.DRAFT)
      .build();

    contractRepository.save(contract);

    // Generate pre-signed URL
    String pdfUrl = s3Service.generatePresignedUrl(s3Key, 300); // 5 min expiry

    return ContractResponse.builder()
      .contractId(contract.getId())
      .preSignHash(preSignHash)
      .status(ContractStatus.DRAFT)
      .pdfUrl(pdfUrl)
      .build();
  }
}

// PDFGenerationService.java
@Service
@Slf4j
public class PDFGenerationService {

  @Autowired private TemplateRepository templateRepository;
  @Autowired private TemplateEngine templateEngine;

  public byte[] renderFromTemplate(String templateId, ContractTerms terms) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    try (PdfWriter writer = new PdfWriter(baos);
         PdfDocument pdf = new PdfDocument(writer);
         Document document = new Document(pdf, PageSize.A4)) {

      // Load HTML template
      String htmlTemplate = templateRepository.findById(templateId)
        .orElseThrow(() -> new TemplateNotFoundException(templateId));

      // Merge template with data using Thymeleaf
      Context context = new Context();
      context.setVariable("merchantName", terms.getMerchantName());
      context.setVariable("payerName", terms.getPayerName());
      context.setVariable("amount", terms.getAmount());
      context.setVariable("dueDate", terms.getDueDate());
      context.setVariable("clauses", terms.getClauses());

      String htmlContent = templateEngine.process(htmlTemplate, context);

      // Convert HTML to PDF using iText html2pdf
      ConverterProperties props = new ConverterProperties();
      HtmlConverter.convertToPdf(htmlContent, pdf, props);

      document.close();

      log.info("PDF generated successfully for template: {}", templateId);
      return baos.toByteArray();

    } catch (IOException e) {
      throw new PDFGenerationException("Failed to generate PDF", e);
    }
  }
}
```

**Performance:**

```
Java iText: ~300-500ms per PDF, 200MB RAM per instance
Handles 100+ PDFs/sec sustained on 4-core machine
```

---

### 3.2 Payment Gateway Wrapper Service

**Purpose:** Abstract Razorpay, Cashfree, and future payment gateways behind a unified interface.

**Interface Design:**

````java
// IPaymentGateway.java
public interface IPaymentGateway {

  // Mandate Management
  MandateResponse createMandate(CreateMandateRequest request);
  MandateDetails getMandate(String mandateId);
  CancelResponse cancelMandate(String mandateId);

  // Payment Execution
  PaymentResponse executePayment(ExecutePaymentRequest request);
  PaymentDetails getPayment(String paymentId);

  // Webhook Validation
  boolean validateWebhook(String signature, String payload);
### 3.2 Payment Gateway Wrapper Service

**Purpose:** Abstract Razorpay, Cashfree, and future payment gateways behind a unified interface.

**Design Patterns Applied:**
- **Adapter Pattern**: Wrap third-party APIs with consistent interface
- **Factory Pattern**: Select gateway implementation based on configuration
- **Strategy Pattern**: Different payment methods (eNACH, UPI Autopay)
- **Decorator Pattern**: Add circuit breaker, retry, logging via AOP
- **Proxy Pattern**: Resilience4j circuit breaker wraps actual calls

**Interface Design (Adapter Pattern):**

```java
// IPaymentGateway.java (Interface - Dependency Inversion)
public interface IPaymentGateway {

  // Mandate Management
  MandateResponse createMandate(CreateMandateRequest request);
  MandateDetails getMandate(String mandateId);
  CancelResponse cancelMandate(String mandateId);

  // Payment Execution
  PaymentResponse executePayment(ExecutePaymentRequest request);
  PaymentDetails getPayment(String paymentId);

  // Webhook Validation
  boolean validateWebhook(String signature, String payload);
}

// DTOs (Value Objects - Immutable)
@Value
@Builder
public class CreateMandateRequest {
  UUID merchantId;
  String payerEmail;
  String payerPhone;
  BigDecimal amount;
  Currency currency;
  MandateType type; // ENACH, UPI_AUTOPAY
  RecurringFrequency frequency;
  LocalDateTime startDate;
  LocalDateTime endDate;
  BigDecimal maxAmount;
  String description;
  String callbackUrl;
  Map<String, String> metadata;
}

@Value
@Builder
public class MandateResponse {
  String mandateId;
  String umrn; // Unique Mandate Reference Number
  MandateStatus status;
  String authUrl;
  Instant expiresAt;
}

@Value
@Builder
public class PaymentResponse {
  String paymentId;
  PaymentStatus status;
  String mandateId;
  BigDecimal amount;
  String failureCode;
  String failureReason;
  String transactionId;
  Instant settledAt;
}
````

**Factory Pattern Implementation:**

```java
// PaymentGatewayFactory.java (Factory Pattern)
@Component
public class PaymentGatewayFactory {

  private final Map<String, IPaymentGateway> gateways = new ConcurrentHashMap<>();

  @Autowired
  public PaymentGatewayFactory(
      RazorpayAdapter razorpayAdapter,
      CashfreeAdapter cashfreeAdapter) {
    gateways.put("razorpay", razorpayAdapter);
    gateways.put("cashfree", cashfreeAdapter);
  }

  public IPaymentGateway getGateway(String provider) {
    IPaymentGateway gateway = gateways.get(provider.toLowerCase());
    if (gateway == null) {
      throw new UnsupportedGatewayException("Gateway not supported: " + provider);
    }
    return gateway;
  }

  public IPaymentGateway getPrimaryGateway() {
    return gateways.get("razorpay"); // Default
  }

  public IPaymentGateway getFallbackGateway() {
    return gateways.get("cashfree");
  }
}
```

**Adapter Pattern Implementation (Razorpay):**

```java
// RazorpayAdapter.java (Adapter Pattern + Decorator with Circuit Breaker)
@Service
@Slf4j
public class RazorpayAdapter implements IPaymentGateway {

  private final RazorpayClient client;
  private final CircuitBreaker circuitBreaker;
  private final RedisTemplate<String, Object> redisTemplate;
  private final AuditLogger auditLogger;

  @Autowired
  public RazorpayAdapter(
      @Value("${razorpay.key.id}") String keyId,
      @Value("${razorpay.key.secret}") String keySecret,
      RedisTemplate<String, Object> redisTemplate,
      AuditLogger auditLogger) throws RazorpayException {

    this.client = new RazorpayClient(keyId, keySecret);

    // Circuit breaker configuration using Resilience4j
    this.circuitBreaker = CircuitBreaker.of("razorpay", CircuitBreakerConfig.custom()
      .failureRateThreshold(50)
      .waitDurationInOpenState(Duration.ofMinutes(1))
      .slidingWindowSize(10)
      .build());

    this.redisTemplate = redisTemplate;
  }

  @Override
  @Retryable(
    value = {RazorpayException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
  )
  public MandateResponse createMandate(CreateMandateRequest request) {
    return circuitBreaker.executeSupplier(() -> {
      try {
        JSONObject mandateRequest = new JSONObject();
        mandateRequest.put("type", request.getType() == MandateType.UPI_AUTOPAY ? "upi" : "emandate");
        mandateRequest.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
        mandateRequest.put("currency", request.getCurrency());
        mandateRequest.put("customer_notify", 1);
        mandateRequest.put("email", request.getPayerEmail());
        mandateRequest.put("contact", request.getPayerPhone());
        mandateRequest.put("recurring_frequency", request.getFrequency().toString().toLowerCase());
        mandateRequest.put("start_at", request.getStartDate().getEpochSecond());

        JSONObject notes = new JSONObject();
        request.getMetadata().forEach(notes::put);
        mandateRequest.put("notes", notes);

        Subscription subscription = client.subscriptions.create(mandateRequest);

        // Audit logging
        auditLogger.log(AuditEvent.builder()
          .eventType("MANDATE_CREATED")
          .service("razorpay-adapter")
          .entityType("MANDATE")
          .entityId(UUID.fromString(subscription.get("id")))
          .payload(new ObjectMapper().writeValueAsString(request))
          .build());

        return MandateResponse.builder()
          .mandateId(subscription.get("id"))
          .umrn(subscription.get("token").get("notes").get("umrn"))
          .status(mapStatus(subscription.get("status")))
          .authUrl(subscription.get("short_url"))
          .build();

      } catch (RazorpayException | JsonProcessingException e) {
        log.error("Failed to create mandate: {}", e.getMessage());
        throw new PaymentGatewayException("Razorpay mandate creation failed", e);
      }
    });
  }

  @Override
  public PaymentResponse executePayment(ExecutePaymentRequest request) {
    return circuitBreaker.executeSupplier(() -> {
      // Check idempotency cache (Redis)
      String cacheKey = "idempotency:" + request.getIdempotencyKey();
      PaymentResponse cached = (PaymentResponse) redisTemplate.opsForValue().get(cacheKey);
      if (cached != null) {
        log.info("Returning cached payment response for key: {}", request.getIdempotencyKey());
        return cached;
      }

      try {
        JSONObject paymentRequest = new JSONObject();
        paymentRequest.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
        paymentRequest.put("currency", request.getCurrency());
        paymentRequest.put("recurring", "1");
        paymentRequest.put("subscription_id", request.getMandateId());
        paymentRequest.put("description", request.getDescription());

        Payment payment = client.payments.create(paymentRequest);

        PaymentResponse response = PaymentResponse.builder()
          .paymentId(payment.get("id"))
          .status(mapPaymentStatus(payment.get("status")))
          .mandateId(request.getMandateId())
          .amount(request.getAmount())
          .failureCode(payment.get("error_code"))
          .failureReason(payment.get("error_description"))
          .transactionId(payment.get("acquirer_data") != null ?
            payment.get("acquirer_data").get("bank_transaction_id") : null)
          .build();

        // Store in Redis (24h TTL)
        redisTemplate.opsForValue().set(cacheKey, response, Duration.ofHours(24));

        // Audit logging
        auditLogger.log(AuditEvent.builder()
          .eventType("PAYMENT_EXECUTED")
          .service("razorpay-adapter")
          .entityType("PAYMENT")
          .entityId(UUID.fromString(payment.get("id")))
          .build());

        return response;

      } catch (RazorpayException e) {
        log.error("Payment execution failed: {}", e.getMessage());
        throw new PaymentGatewayException("Razorpay payment failed", e);
      }
    });
  }

  @Override
  public boolean validateWebhook(String signature, String payload) {
    try {
      String webhookSecret = env.getProperty("razorpay.webhook.secret");
      String expectedSignature = HmacUtils.hmacSha256Hex(webhookSecret, payload);
      return MessageDigest.isEqual(
        signature.getBytes(StandardCharsets.UTF_8),
        expectedSignature.getBytes(StandardCharsets.UTF_8)
      );
    } catch (Exception e) {
      log.error("Webhook validation failed: {}", e.getMessage());
      return false;
    }
  }

  private MandateStatus mapStatus(String razorpayStatus) {
    return switch (razorpayStatus) {
      case "created" -> MandateStatus.CREATED;
      case "authenticated" -> MandateStatus.ACTIVE;
      case "cancelled" -> MandateStatus.CANCELLED;
      default -> MandateStatus.CREATED;
    };
  }
}

  validateWebhook(signature: string, payload: string): boolean {
    const secret = process.env.RAZORPAY_WEBHOOK_SECRET!;
    const expectedSignature = crypto
      .createHmac("sha256", secret)
      .update(payload)
      .digest("hex");

    return crypto.timingSafeEqual(
      Buffer.from(signature),
      Buffer.from(expectedSignature),
    );
  }

  private mapStatus(status: string): MandateResponse["status"] {
    const mapping: Record<string, MandateResponse["status"]> = {
      created: "CREATED",
      authenticated: "ACTIVE",
      active: "ACTIVE",
      paused: "PAUSED",
      cancelled: "CANCELLED",
    };
    return mapping[status] || "CREATED";
  }

  private async logAPICall(method: string, request: any, response: any) {
    // Redact sensitive data
    const sanitizedRequest = { ...request };
    delete sanitizedRequest.payerEmail;
    delete sanitizedRequest.payerPhone;

    await AuditLogger.log({
      service: "PaymentGateway",
      provider: "Razorpay",
      method,
      request: sanitizedRequest,
      response: { id: response.id, status: response.status },
      timestamp: new Date().toISOString(),
    });
  }

  private async checkIdempotencyCache(
    key: string,
  ): Promise<PaymentResponse | null> {
    const redis = RedisClient.getInstance();
    const cached = await redis.get(`idempotency:${key}`);
    return cached ? JSON.parse(cached) : null;
  }

  private async storeIdempotencyCache(key: string, value: PaymentResponse) {
    const redis = RedisClient.getInstance();
    await redis.setex(`idempotency:${key}`, 86400, JSON.stringify(value));
  }
}
```

**Gateway Factory Pattern:**

```typescript
// src/services/payment-gateway/PaymentGatewayFactory.ts
export class PaymentGatewayFactory {
  static create(provider: "razorpay" | "cashfree"): IPaymentGateway {
    switch (provider) {
      case "razorpay":
        return new RazorpayAdapter({
          keyId: process.env.RAZORPAY_KEY_ID!,
          keySecret: process.env.RAZORPAY_KEY_SECRET!,
        });
      case "cashfree":
        return new CashfreeAdapter({
          appId: process.env.CASHFREE_APP_ID!,
          secretKey: process.env.CASHFREE_SECRET_KEY!,
        });
      default:
        throw new Error(`Unsupported payment gateway: ${provider}`);
    }
  }

  // Per-merchant gateway selection
  static async createForMerchant(merchantId: string): Promise<IPaymentGateway> {
    const merchant = await MerchantRepository.findById(merchantId);
    const provider = merchant.paymentGateway || "razorpay";
    return this.create(provider);
  }
}
```

---

### 3.3 Dunning Engine (Smart Retry Logic)

**State Machine:**

```
PAYMENT_PENDING
    |
    v
PAYMENT_INITIATED
    |
    +---> SUCCESS --> COMPLETED
    |
    +---> FAILED
            |
            v
      [Check Return Code]
            |
            +---> R03 (Insufficient Funds)
            |       |
            |       v
            |   SCHEDULED_RETRY (T+10 days)
            |       |
            |       v
            |   RETRY_INITIATED
            |       |
            |       +---> SUCCESS --> COMPLETED
            |       |
            |       +---> FAILED --> LEGAL_DEFAULT
            |
            +---> R04 (Cancelled/Account Closed)
                    |
                    v
                LEGAL_DEFAULT
```

**Implementation:**

```typescript
// src/services/dunning/DunningEngine.ts
interface PaymentAttempt {
  id: string;
  paymentId: string;
  contractId: string;
  mandateId: string;
  attemptNumber: number;
  scheduledAt: Date;
  executedAt?: Date;
  status: "SCHEDULED" | "IN_PROGRESS" | "SUCCESS" | "FAILED";
  returnCode?: string;
  failureReason?: string;
}

export class DunningEngine {
  async processFailedPayment(payment: Payment) {
    const returnCode = payment.failureCode;

    // Classify failure
    const isRetriable = this.isRetriableFailure(returnCode);

    if (!isRetriable) {
      await this.triggerLegalEscalation(payment);
      return;
    }

    // Check retry count
    const attemptCount = await this.getAttemptCount(payment.id);
    if (attemptCount >= 1) {
      // Max 1 retry per PRD
      await this.triggerLegalEscalation(payment);
      return;
    }

    // Schedule retry at T+10 days
    const retryDate = new Date();
    retryDate.setDate(retryDate.getDate() + 10);

    await this.scheduleRetry({
      paymentId: payment.id,
      contractId: payment.contractId,
      mandateId: payment.mandateId,
      attemptNumber: attemptCount + 1,
      scheduledAt: retryDate,
    });

    // Send notification to payer
    await NotificationService.send({
      userId: payment.payerId,
      type: "PAYMENT_FAILED_RETRY_SCHEDULED",
      channels: ["EMAIL", "SMS", "WHATSAPP"],
      data: {
        amount: payment.amount,
        failureReason: payment.failureReason,
        retryDate: retryDate.toISOString(),
        legalImplications: "Section 25 PSS Act penalty applies",
      },
    });
  }

  private isRetriableFailure(returnCode?: string): boolean {
    const retriableCodes = ["R03", "R09", "R13"]; // Insufficient funds variants
    const nonRetriableCodes = ["R04", "R05", "R07", "R10"]; // Account closed, mandate cancelled

    if (!returnCode) return false;

    return (
      retriableCodes.includes(returnCode) &&
      !nonRetriableCodes.includes(returnCode)
    );
  }

  private async triggerLegalEscalation(payment: Payment) {
    // Update payment status
    await PaymentRepository.updateStatus(payment.id, "LEGAL_DEFAULT");

    // Trigger legal notice generation
    await EventBus.publish({
      type: "LEGAL_ESCALATION_REQUIRED",
      data: {
        contractId: payment.contractId,
        paymentId: payment.id,
        amount: payment.amount,
        failureCode: payment.failureCode,
        failureReason: payment.failureReason,
      },
    });

    // Notify merchant
    await NotificationService.send({
      userId: payment.merchantId,
      type: "PAYMENT_LEGAL_ESCALATION",
      channels: ["EMAIL", "DASHBOARD"],
      data: { contractId: payment.contractId, amount: payment.amount },
    });
  }
}

// Background worker to execute scheduled retries
export class RetryWorker {
  async run() {
    while (true) {
      const retries = await PaymentAttemptRepository.findDue();

      for (const retry of retries) {
        try {
          await this.executeRetry(retry);
        } catch (error) {
          logger.error("Retry execution failed", { retry, error });
          await this.handleRetryError(retry, error);
        }
      }

      await sleep(60000); // Check every minute
    }
  }

  private async executeRetry(retry: PaymentAttempt) {
    // Mark as in-progress
    await PaymentAttemptRepository.updateStatus(retry.id, "IN_PROGRESS");

    const gateway = await PaymentGatewayFactory.createForMerchant(
      retry.merchantId,
    );

    const result = await gateway.executePayment({
      mandateId: retry.mandateId,
      amount: retry.amount,
      currency: "INR",
      description: `Retry attempt ${retry.attemptNumber}`,
      idempotencyKey: `retry-${retry.id}`,
      metadata: {
        contractId: retry.contractId,
        originalPaymentId: retry.paymentId,
        attemptNumber: retry.attemptNumber.toString(),
      },
    });

    if (result.status === "SUCCESS") {
      await PaymentAttemptRepository.updateStatus(retry.id, "SUCCESS");
      await PaymentRepository.updateStatus(retry.paymentId, "COMPLETED");

      // Notify success
      await NotificationService.send({
        userId: retry.payerId,
        type: "PAYMENT_RETRY_SUCCESS",
        channels: ["EMAIL", "SMS"],
        data: { amount: retry.amount },
      });
    } else {
      await PaymentAttemptRepository.updateStatus(retry.id, "FAILED");
      await DunningEngine.processFailedPayment(result);
    }
  }
}
```

---

### 3.4 Evidence Layer (Blockchain Integration)

**Design Goals:**

- Store only hashes on-chain (privacy)
- Queue writes if blockchain is down (resilience)
- Gas optimization (batch writes)

**Smart Contract (Solidity):**

```solidity
// contracts/EvidenceRegistry.sol
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract EvidenceRegistry {
    struct Evidence {
        bytes32 contractHash;
        bytes32 mandateHash; // hash of UMRN
        bytes32 transactionHash; // hash of payment ID + status
        uint256 timestamp;
        address submitter;
    }

    mapping(bytes32 => Evidence) public evidenceRecords;
    mapping(bytes32 => bool) public exists;

    event EvidenceRecorded(
        bytes32 indexed evidenceId,
        bytes32 contractHash,
        bytes32 mandateHash,
        uint256 timestamp
    );

    function recordEvidence(
        bytes32 evidenceId,
        bytes32 contractHash,
        bytes32 mandateHash,
        bytes32 transactionHash
    ) external returns (bool) {
        require(!exists[evidenceId], "Evidence already exists");

        evidenceRecords[evidenceId] = Evidence({
            contractHash: contractHash,
            mandateHash: mandateHash,
            transactionHash: transactionHash,
            timestamp: block.timestamp,
            submitter: msg.sender
        });

        exists[evidenceId] = true;

        emit EvidenceRecorded(evidenceId, contractHash, mandateHash, block.timestamp);

        return true;
    }

    function getEvidence(bytes32 evidenceId) external view returns (
        bytes32 contractHash,
        bytes32 mandateHash,
        bytes32 transactionHash,
        uint256 timestamp,
        address submitter
    ) {
        require(exists[evidenceId], "Evidence not found");

        Evidence memory evidence = evidenceRecords[evidenceId];
        return (
            evidence.contractHash,
            evidence.mandateHash,
            evidence.transactionHash,
            evidence.timestamp,
            evidence.submitter
        );
    }
}
```

**Backend Integration:**

```typescript
// src/services/evidence/BlockchainService.ts
import { ethers } from "ethers";
import EvidenceRegistryABI from "./abi/EvidenceRegistry.json";

export class BlockchainService {
  private provider: ethers.providers.Provider;
  private wallet: ethers.Wallet;
  private contract: ethers.Contract;
  private queue: EvidenceQueue;

  constructor() {
    this.provider = new ethers.providers.JsonRpcProvider(
      process.env.POLYGON_RPC_URL,
    );
    this.wallet = new ethers.Wallet(
      process.env.BLOCKCHAIN_PRIVATE_KEY!,
      this.provider,
    );
    this.contract = new ethers.Contract(
      process.env.EVIDENCE_CONTRACT_ADDRESS!,
      EvidenceRegistryABI,
      this.wallet,
    );
    this.queue = new EvidenceQueue();
  }

  async recordEvidence(evidence: {
    contractId: string;
    contractHash: string;
    mandateId: string;
    paymentId: string;
    paymentStatus: string;
  }): Promise<{ txHash?: string; queued: boolean }> {
    try {
      // Generate opaque hashes (no PII on-chain)
      const evidenceId = ethers.utils.keccak256(
        ethers.utils.toUtf8Bytes(
          `${evidence.contractId}-${evidence.paymentId}`,
        ),
      );
      const contractHash = `0x${evidence.contractHash}`;
      const mandateHash = ethers.utils.keccak256(
        ethers.utils.toUtf8Bytes(evidence.mandateId),
      );
      const transactionHash = ethers.utils.keccak256(
        ethers.utils.toUtf8Bytes(
          `${evidence.paymentId}-${evidence.paymentStatus}`,
        ),
      );

      // Attempt on-chain write
      const tx = await this.contract.recordEvidence(
        evidenceId,
        contractHash,
        mandateHash,
        transactionHash,
        {
          gasLimit: 200000,
          maxFeePerGas: ethers.utils.parseUnits("50", "gwei"),
        },
      );

      const receipt = await tx.wait();

      // Store off-chain metadata with tx reference
      await this.storeOffChainEvidence({
        ...evidence,
        blockNumber: receipt.blockNumber,
        txHash: receipt.transactionHash,
        timestamp: new Date().toISOString(),
      });

      return { txHash: receipt.transactionHash, queued: false };
    } catch (error) {
      logger.error("Blockchain write failed, queuing", { evidence, error });

      // Queue for retry
      await this.queue.enqueue({
        ...evidence,
        queuedAt: new Date().toISOString(),
        retryCount: 0,
      });

      // Still store off-chain with signed timestamp
      await this.storeOffChainEvidence({
        ...evidence,
        status: "QUEUED",
        signedTimestamp: this.signTimestamp(evidence),
      });

      return { queued: true };
    }
  }

  private signTimestamp(evidence: any): string {
    const message = JSON.stringify({
      contractId: evidence.contractId,
      timestamp: new Date().toISOString(),
    });
    return this.wallet.signMessage(message);
  }

  private async storeOffChainEvidence(evidence: any) {
    const s3 = new S3Client();
    const key = `evidence/${evidence.contractId}/${evidence.paymentId}.json`;

    // Encrypt sensitive data
    const encrypted = await KMSService.encrypt(JSON.stringify(evidence));

    await s3.putObject({
      Bucket: process.env.EVIDENCE_BUCKET!,
      Key: key,
      Body: encrypted,
      ServerSideEncryption: "aws:kms",
      Metadata: {
        contractId: evidence.contractId,
        paymentId: evidence.paymentId,
      },
    });

    // Log reference in DB
    await EvidenceRepository.create({
      contractId: evidence.contractId,
      paymentId: evidence.paymentId,
      s3Key: key,
      blockNumber: evidence.blockNumber,
      txHash: evidence.txHash,
      status: evidence.status || "RECORDED",
    });
  }
}

// Background worker to process queue
export class EvidenceQueueWorker {
  async run() {
    const queue = new EvidenceQueue();

    while (true) {
      const items = await queue.getReady();

      for (const item of items) {
        if (item.retryCount >= 5) {
          // Max retries exceeded
          await queue.markFailed(item.id);
          logger.error("Evidence queue item failed permanently", { item });
          continue;
        }

        try {
          const blockchain = new BlockchainService();
          const result = await blockchain.recordEvidence(item.data);

          if (!result.queued) {
            await queue.markCompleted(item.id);
          }
        } catch (error) {
          await queue.incrementRetry(item.id);
        }
      }

      await sleep(300000); // Check every 5 minutes
    }
  }
}
```

---

## 4. API Wrapper Layer Design

**Design Patterns:** **Adapter**, **Facade**, **Proxy**, **Decorator**

### 4.1 Wrapper Architecture Principles

**Why Wrappers?**

1. **Provider Independence:** Swap Razorpay for Cashfree without changing business logic (Open/Closed Principle)
2. **Unified Error Handling:** Convert provider-specific errors to domain errors
3. **Retry & Circuit Breaking:** Resilience patterns at wrapper layer (Decorator Pattern)
4. **Audit & Compliance:** Log all external API calls with sanitized data (Aspect-Oriented Programming)
5. **Cost Optimization:** Cache provider responses where safe (Proxy Pattern)

**Layered Architecture Pattern:**

```
┌─────────────────────────────────────────┐
│  Service Layer (Business Logic)        │
│  - ContractService                      │
│  - PaymentService                       │
└─────────────────┬───────────────────────┘
                  │ Depends on Interface (DIP)
┌─────────────────▼───────────────────────┐
│  Wrapper Interface (Abstraction)        │
│  - IPaymentGateway                      │
│  - IESignProvider                       │
└─────────────────┬───────────────────────┘
                  │ Implemented by
┌─────────────────▼───────────────────────┐
│  Provider Adapter (Concrete Impl)       │
│  - RazorpayAdapter implements           │
│    IPaymentGateway                      │
│  - LeegalityAdapter implements          │
│    IESignProvider                       │
└─────────────────┬───────────────────────┘
                  │ Calls
┌─────────────────▼───────────────────────┐
│  External API (Third-Party SDK)         │
│  - Razorpay Java SDK                    │
│  - Leegality REST API                   │
└─────────────────────────────────────────┘
```

**Design Patterns in Action:**

- **Adapter**: Converts third-party API to our domain interface
- **Facade**: Simplifies complex API interactions into simple methods
- **Decorator**: Circuit breaker wraps adapter (Resilience4j)
- **Proxy**: Caching proxy for read-only operations
- **Strategy**: Select provider implementation at runtime (Factory)

### 4.2 eSign Wrapper (Adapter + Facade Patterns)

```java
// IESignProvider.java (Interface for Adapter Pattern)
public interface IESignProvider {
  ESignResponse initiateESign(ESignRequest request);
  ESignStatus getSignStatus(String requestId);
  byte[] downloadSignedDocument(String requestId);
  boolean validateWebhook(String signature, String payload);
}

// DTOs (Value Objects)
@Value
@Builder
public class ESignRequest {
  String documentUrl;
  List<Signer> signers;
  String callbackUrl;
  Map<String, String> metadata;

  @Value
  @Builder
  public static class Signer {
    String name;
    String email;
    String mobile;
    String aadhaarLast4;
  }
}

@Value
@Builder
public class ESignResponse {
  String requestId;
  String redirectUrl;
  Instant expiresAt;
  ESignStatus status;
}

public enum ESignStatus {
  PENDING,
  IN_PROGRESS,
  COMPLETED,
  FAILED,
  EXPIRED
}

// Leegality Adapter
export class LeegalityAdapter implements IESignProvider {
  private apiKey: string;
  private baseUrl: string;

  constructor(config: {
    apiKey: string;
    environment: "sandbox" | "production";
  }) {
    this.apiKey = config.apiKey;
    this.baseUrl =
      config.environment === "production"
        ? "https://api.leegality.com/v2"
// LeegalityAdapter.java (Adapter Pattern Implementation)
@Service
@Slf4j
public class LeegalityAdapter implements IESignProvider {

  private final WebClient webClient;
  private final CircuitBreaker circuitBreaker;
  private final AuditLogger auditLogger;
  private final String apiKey;
  private final String webhookSecret;

  @Autowired
  public LeegalityAdapter(
      WebClient.Builder webClientBuilder,
      @Value("${leegality.api.key}") String apiKey,
      @Value("${leegality.webhook.secret}") String webhookSecret,
      AuditLogger auditLogger) {

    this.apiKey = apiKey;
    this.webhookSecret = webhookSecret;
    this.auditLogger = auditLogger;

    // Facade: Simplify WebClient configuration
    this.webClient = webClientBuilder
      .baseUrl(getBaseUrl())
      .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
      .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .build();

    // Decorator: Wrap with circuit breaker
    this.circuitBreaker = CircuitBreaker.of("leegality", CircuitBreakerConfig.custom()
      .failureRateThreshold(50)
      .waitDurationInOpenState(Duration.ofMinutes(1))
      .build());
  }

  @Override
  @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
  public ESignResponse initiateESign(ESignRequest request) {
    return circuitBreaker.executeSupplier(() -> {
      // Transform domain request to Leegality API format (Adapter pattern)
      LeegalityESignRequest leegalityRequest = LeegalityESignRequest.builder()
        .document(new Document(request.getDocumentUrl(),
          "Contract-" + request.getMetadata().get("contractId") + ".pdf"))
        .signers(request.getSigners().stream()
          .map(s -> new LeegalitySigner(
            s.getName(),
            s.getEmail(),
            s.getMobile(),
            "aadhaar_esign"
          ))
          .collect(Collectors.toList()))
        .webhookUrl(request.getCallbackUrl())
        .metadata(request.getMetadata())
        .build();

      // Call external API
      LeegalityESignResponse response = webClient.post()
        .uri("/esign/create")
        .bodyValue(leegalityRequest)
        .retrieve()
        .bodyToMono(LeegalityESignResponse.class)
        .timeout(Duration.ofSeconds(30))
        .block();

      // Audit logging (AOP Aspect)
      auditLogger.log(AuditEvent.builder()
        .eventType("ESIGN_INITIATED")
        .service("leegality-adapter")
        .payload(sanitizeForLogging(leegalityRequest))
        .build());

      // Transform Leegality response to domain response (Adapter pattern)
      return ESignResponse.builder()
        .requestId(response.getRequestId())
        .redirectUrl(response.getSigningUrl())
        .expiresAt(response.getExpiresAt())
        .status(ESignStatus.PENDING)
        .build();
    });
  }

  @Override
  public boolean validateWebhook(String signature, String payload) {
    try {
      // HMAC validation for webhook security
      String expectedSignature = HmacUtils.hmacSha256Hex(webhookSecret, payload);
      return MessageDigest.isEqual(
        signature.getBytes(StandardCharsets.UTF_8),
        expectedSignature.getBytes(StandardCharsets.UTF_8)
      );
    } catch (Exception e) {
      log.error("Webhook validation failed: {}", e.getMessage());
      return false;
    }
  }

  // Template Method Pattern: Common structure for PII sanitization
  private Object sanitizeForLogging(Object data) {
    // Redact email, phone from logs
    if (data instanceof LeegalityESignRequest) {
      LeegalityESignRequest req = (LeegalityESignRequest) data;
      return req.toBuilder()
        .signers(req.getSigners().stream()
          .map(s -> s.toBuilder()
            .email(maskEmail(s.getEmail()))
            .phone(maskPhone(s.getPhone()))
            .build())
          .collect(Collectors.toList()))
        .build();
    }
    return data;
  }

  private String getBaseUrl() {
    return env.getProperty("spring.profiles.active", "dev").equals("production")
      ? "https://api.leegality.com/v2"
      : "https://sandbox.leegality.com/v2";
  }
}
    );
  }

  private async logAPICall(method: string, request: any, response: any) {
    await AuditLogger.log({
      service: "ESignProvider",
      provider: "Leegality",
      method,
      request: this.sanitizeRequest(request),
      response: { requestId: response.request_id },
      timestamp: new Date().toISOString(),
    });
  }

  private sanitizeRequest(request: any) {
    return {
      ...request,
      signers: request.signers?.map((s: any) => ({
        email: this.maskEmail(s.email),
        mobile: this.maskMobile(s.mobile),
      })),
    };
  }

  private maskEmail(email: string): string {
    const [name, domain] = email.split("@");
    return `${name.slice(0, 2)}***@${domain}`;
  }

  private maskMobile(mobile: string): string {
    return `***${mobile.slice(-4)}`;
  }
}

// Factory
export class ESignProviderFactory {
  static create(provider: "leegality" | "digio"): IESignProvider {
    switch (provider) {
      case "leegality":
        return new LeegalityAdapter({
          apiKey: process.env.LEEGALITY_API_KEY!,
          environment:
            process.env.NODE_ENV === "production" ? "production" : "sandbox",
        });
      case "digio":
        return new DigioAdapter({
          clientId: process.env.DIGIO_CLIENT_ID!,
          clientSecret: process.env.DIGIO_CLIENT_SECRET!,
        });
      default:
        throw new Error(`Unsupported eSign provider: ${provider}`);
    }
  }
}
```

---

## 5. Data Models & Database Schema

### 5.1 Core Entities

**ERD (Entity Relationship Diagram):**

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│  Merchants  │──────<│  Contracts  │>──────│   Payers    │
└─────────────┘       └──────┬──────┘       └─────────────┘
                             │
                             │
                      ┌──────▼──────┐
                      │  Mandates   │
                      └──────┬──────┘
                             │
                             │
                      ┌──────▼──────┐
                      │  Payments   │
                      └──────┬──────┘
                             │
               ┌─────────────┼─────────────┐
---

### 4.3 Design Patterns Summary & Best Practices

**Comprehensive Pattern Catalog for LegalPay:**

| Pattern | Purpose | Implementation | Benefits |
|---------|---------|----------------|----------|
| **Adapter** | Convert third-party APIs to domain interfaces | `RazorpayAdapter`, `LeegalityAdapter` | Provider independence, testability |
| **Factory** | Create objects without specifying exact class | `PaymentGatewayFactory`, `ESignProviderFactory` | Encapsulate creation logic, runtime selection |
| **Strategy** | Define family of algorithms, make them interchangeable | `RetryStrategy`, `NotificationStrategy` | Flexible retry policies, multi-channel notifications |
| **Repository** | Abstract data access layer | `ContractRepository`, `PaymentRepository` | Decouple domain from persistence, testability |
| **Builder** | Construct complex objects step-by-step | Lombok `@Builder` on DTOs | Immutable objects, readable construction |
| **State** | Object behavior changes with state | Spring State Machine for Contract lifecycle | Type-safe transitions, audit trail |
| **Observer** | Notify multiple objects of state changes | Spring `@EventListener` for domain events | Loose coupling, async processing |
| **Template Method** | Define algorithm skeleton, let subclasses override steps | `AbstractPaymentService` | Code reuse, enforce structure |
| **Chain of Responsibility** | Pass request through chain of handlers | Spring Filter chain, validation pipeline | Flexible request processing, middleware |
| **Decorator** | Add behavior to objects dynamically | Resilience4j circuit breaker, `@Aspect` for logging | Non-invasive cross-cutting concerns |
| **Proxy** | Provide surrogate for another object | Spring AOP proxies, Redis caching | Control access, lazy loading, caching |
| **Facade** | Simplified interface to complex subsystem | `PaymentOrchestrationService` | Reduce complexity, single entry point |
| **Singleton** | Ensure class has only one instance | Spring `@Service` beans | Shared resources, global state |
| **Value Object** | Immutable object representing descriptive aspect | `Money`, `ContractTerms`, `Address` | Immutability, equality by value |
| **Aggregate** | Cluster of domain objects treated as unit | Contract aggregate (Contract + Mandate + Payments) | Consistency boundary, transactional integrity |
| **Domain Event** | Something happened that domain experts care about | `ContractSignedEvent`, `PaymentFailedEvent` | Eventual consistency, async workflows |
| **Specification** | Business rule as reusable object | `IsRetriablePaymentFailure` | Testable rules, query composition |

**Architectural Patterns:**

| Pattern | Usage | Benefits |
|---------|-------|----------|
| **Layered Architecture** | Presentation → Service → Repository → Database | Separation of concerns, testability |
| **Hexagonal (Ports & Adapters)** | Core domain isolated from external concerns | Testability, technology independence |
| **Event-Driven Architecture** | RabbitMQ for async communication | Scalability, loose coupling |
| **CQRS (Lite)** | Separate read models for reporting | Optimized queries, scalability |
| **Saga Pattern** | Distributed transactions (payment → evidence → legal) | Eventual consistency, resilience |

**SOLID Compliance Matrix:**

| Principle | Example in LegalPay | Code Reference |
|-----------|---------------------|----------------|
| **Single Responsibility** | Each service has one reason to change | `ContractService` only manages contracts |
| **Open/Closed** | Add new payment gateway without modifying core | New adapter implements `IPaymentGateway` |
| **Liskov Substitution** | All adapters interchangeable | Any `IPaymentGateway` can replace another |
| **Interface Segregation** | Thin interfaces, not bloated | `IPaymentGateway` != `IESignProvider` |
| **Dependency Inversion** | Depend on abstractions, not concretions | Services depend on `IPaymentGateway` interface |

**Anti-Patterns Avoided:**

| Anti-Pattern | Why Avoided | Mitigation |
|--------------|-------------|------------|
| **God Object** | Single class doing everything | Split into microservices |
| **Anemic Domain Model** | Domain objects with no behavior | Rich domain models with validation |
| **Golden Hammer** | Using one solution for all problems | Multiple patterns for different needs |
| **Spaghetti Code** | Tangled dependencies | Layered architecture, DI |
| **Hard-Coded Dependencies** | Direct instantiation of dependencies | Spring DI, constructor injection |
| **Big Ball of Mud** | No clear structure | DDD bounded contexts, microservices |

**Best Practices Enforced:**

1. **Constructor Injection**: All dependencies injected via constructor (Spring best practice)
2. **Immutable DTOs**: All request/response objects immutable (`@Value`, `@Builder`)
3. **Fail Fast**: Validate inputs early, throw exceptions immediately
4. **Defensive Copying**: Return copies of mutable collections
5. **Null Safety**: Use `Optional<T>` for nullable returns
6. **Exception Hierarchy**: Custom exceptions extend base `LegalPayException`
7. **Logging Levels**: DEBUG → INFO → WARN → ERROR with proper context
8. **Test Pyramid**: Unit (80%) → Integration (15%) → E2E (5%)

---

## 5. Data Models & Database Schema

**Design Pattern:** **Repository Pattern** for data access abstraction

### 5.1 Entity Relationship Diagram

```

        ┌─────────────┐
        │  Merchants  │
        └──────┬──────┘
               │ 1:N
        ┌──────▼──────┐     ┌─────────────┐
        │  Contracts  │────>│   Payers    │
        └──────┬──────┘ N:1 └─────────────┘
               │ 1:1
        ┌──────▼──────┐
        │   Mandates  │
        └──────┬──────┘
               │ 1:N
        ┌──────▼──────┐
        │   Payments  │
        └──────┬──────┘
               │
        ┌──────▼──────┐ ┌───▼─────┐ ┌────▼────────┐
        │   Invoices  │ │ Evidence│ │ Legal Notices│
        └─────────────┘ └─────────┘ └─────────────┘

````

**Detailed Schemas:**

```sql
-- Merchants Table (Aggregate Root)
CREATE TABLE merchants (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  business_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  phone VARCHAR(20) NOT NULL,

  -- KYC
  pan VARCHAR(10) NOT NULL,
  gstin VARCHAR(15),

  -- Bank Details (for settlement)
  bank_account_number VARCHAR(20),
  bank_ifsc VARCHAR(11),
  bank_account_holder VARCHAR(255),

  -- Configuration
  payment_gateway VARCHAR(20) DEFAULT 'razorpay',
  esign_provider VARCHAR(20) DEFAULT 'leegality',
  auto_escalate_legal BOOLEAN DEFAULT true,

  -- API Keys (encrypted)
  razorpay_key_id_encrypted BYTEA,
  razorpay_key_secret_encrypted BYTEA,

  -- State
  status VARCHAR(20) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Payers Table (Entity)
CREATE TABLE payers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  phone VARCHAR(20) NOT NULL,

  -- Optional KYC
  pan VARCHAR(10),
  aadhaar_last_4 VARCHAR(4),

  -- Consent Tracking
  consent_given_at TIMESTAMP,
  consent_ip INET,
  consent_device VARCHAR(255),

  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),

  UNIQUE(email, phone)
);

-- Mandates Table
CREATE TABLE mandates (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  contract_id UUID NOT NULL REFERENCES contracts(id),
  merchant_id UUID NOT NULL REFERENCES merchants(id),
  payer_id UUID NOT NULL REFERENCES payers(id),

  -- Mandate Details
  type VARCHAR(20) NOT NULL, -- 'ENACH' | 'UPI_AUTOPAY'
  umrn VARCHAR(50), -- Unique Mandate Reference Number
  provider VARCHAR(20) NOT NULL,
  provider_mandate_id VARCHAR(100) NOT NULL,

  -- Amounts
  max_amount DECIMAL(15,2) NOT NULL,
  currency VARCHAR(3) DEFAULT 'INR',

  -- Schedule
  frequency VARCHAR(20), -- 'ADHOC' | 'MONTHLY' | 'QUARTERLY'
  start_date DATE NOT NULL,
  end_date DATE,

  -- Consent
  authorized_at TIMESTAMP,
  authorization_method VARCHAR(50), -- 'UPI_PIN' | 'NETBANKING_OTP'
  authorization_ip INET,

  -- State
  status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
  -- CREATED -> AUTHORIZED -> ACTIVE -> PAUSED -> CANCELLED

  -- Audit
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  cancelled_at TIMESTAMP,
  cancellation_reason TEXT,

  INDEX idx_contract_id (contract_id),
  INDEX idx_merchant_id (merchant_id),
  INDEX idx_umrn (umrn),
  UNIQUE(provider, provider_mandate_id)
);

-- Payments Table
CREATE TABLE payments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  contract_id UUID NOT NULL REFERENCES contracts(id),
  mandate_id UUID NOT NULL REFERENCES mandates(id),
  merchant_id UUID NOT NULL REFERENCES merchants(id),
  payer_id UUID NOT NULL REFERENCES payers(id),

  -- Payment Details
  amount DECIMAL(15,2) NOT NULL,
  currency VARCHAR(3) DEFAULT 'INR',
  due_date DATE NOT NULL,

  -- Gateway Tracking
  gateway VARCHAR(20) NOT NULL,
  gateway_payment_id VARCHAR(100),
  gateway_txn_id VARCHAR(100), -- Bank UTR/Reference
  idempotency_key VARCHAR(100) UNIQUE NOT NULL,

  -- Pre-Debit Notifications
  predebit_notification_sent_at TIMESTAMP,
  predebit_notification_channels JSONB, -- ['EMAIL', 'SMS', 'WHATSAPP']

  -- Execution
  initiated_at TIMESTAMP,
  completed_at TIMESTAMP,

  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  -- PENDING -> INITIATED -> SUCCESS | FAILED -> RETRY_SCHEDULED -> LEGAL_DEFAULT

  -- Failure Tracking
  failure_code VARCHAR(10), -- 'R03', 'R04', etc.
  failure_reason TEXT,

  -- Retry Management
  retry_count INT DEFAULT 0,
  next_retry_at TIMESTAMP,

  -- Audit
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),

  INDEX idx_contract_id (contract_id),
  INDEX idx_mandate_id (mandate_id),
  INDEX idx_status (status),
  INDEX idx_due_date (due_date),
  INDEX idx_next_retry_at (next_retry_at) WHERE status = 'RETRY_SCHEDULED'
);

-- Evidence Table
CREATE TABLE evidence (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  contract_id UUID NOT NULL REFERENCES contracts(id),
  payment_id UUID REFERENCES payments(id),

  -- Off-Chain Storage
  s3_bucket VARCHAR(100) NOT NULL,
  s3_key VARCHAR(500) NOT NULL,
  kms_key_id VARCHAR(255),

  -- On-Chain Reference
  blockchain_network VARCHAR(20), -- 'polygon-mainnet'
  contract_address VARCHAR(42),
  tx_hash VARCHAR(66),
  block_number BIGINT,

  -- Hashes (for verification)
  contract_hash VARCHAR(64) NOT NULL,
  mandate_hash VARCHAR(64),
  transaction_hash VARCHAR(64),

  -- Status
  status VARCHAR(20) DEFAULT 'PENDING',
  -- PENDING -> QUEUED -> RECORDED -> FAILED

  -- Timestamps
  queued_at TIMESTAMP,
  recorded_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW(),

  INDEX idx_contract_id (contract_id),
  INDEX idx_tx_hash (tx_hash)
);

-- Audit Log (Append-Only)
CREATE TABLE audit_log (
  id BIGSERIAL PRIMARY KEY,
  event_type VARCHAR(50) NOT NULL,
  service VARCHAR(50) NOT NULL,
  entity_type VARCHAR(50),
  entity_id UUID,

  -- Actor
  user_id UUID,
  user_type VARCHAR(20), -- 'MERCHANT' | 'PAYER' | 'SYSTEM'
  ip_address INET,
  user_agent TEXT,

  -- Event Data (JSON)
  payload JSONB NOT NULL,

  -- Immutability
  timestamp TIMESTAMP DEFAULT NOW() NOT NULL,
  hash VARCHAR(64), -- SHA-256 of previous hash + current record

  INDEX idx_entity (entity_type, entity_id),
  INDEX idx_timestamp (timestamp),
  INDEX idx_event_type (event_type)
);
````

---

## 6. State Machines & Workflows

**Design Pattern:** **State Pattern** - Encapsulate state-specific behavior and transitions

### 6.1 Contract Lifecycle State Machine

```
DRAFT
  |
  | merchant creates contract
  |
  v
ESIGN_INITIATED
  |
  | payer completes eSign
  |
  v
SIGNED
  |
  | system creates mandate
  |
  v
MANDATE_CREATED
  |
  | payer authorizes mandate
  |
  v
ACTIVE
  |
  +---> PAYMENT_DUE (T-15 days)
  |       |
  |       v
  |     PAYMENT_INITIATED
  |       |
  |       +---> SUCCESS --> COMPLETED
  |       |
  |       +---> FAILED --> RETRY_SCHEDULED
  |                          |
  |                          +---> SUCCESS --> COMPLETED
  |                          |
  |                          +---> FAILED --> LEGAL_DEFAULT
  |
  +---> CANCELLED (by merchant/payer)
  |
  +---> EXPIRED (past due date, no payment)
```

**Implementation (Spring State Machine):**

```java
// ContractStatus.java (Enum for states)
public enum ContractStatus {
  DRAFT,
  ESIGN_INITIATED,
  SIGNED,
  MANDATE_CREATED,
  ACTIVE,
  PAYMENT_DUE,
  PAYMENT_INITIATED,
  COMPLETED,
  LEGAL_DEFAULT,
  CANCELLED,
  EXPIRED
}

// ContractEvent.java (Events that trigger transitions)
public enum ContractEvent {
  INITIATE_ESIGN,
  ESIGN_COMPLETED,
  MANDATE_CREATED,
  MANDATE_AUTHORIZED,
  PAYMENT_DUE_REACHED,
  PAYMENT_INITIATED,
  PAYMENT_SUCCESS,
  PAYMENT_FAILED,
  RETRY_FAILED,
  CANCEL_CONTRACT,
  CONTRACT_EXPIRED
}

// ContractStateMachineConfig.java (State Pattern via Spring State Machine)
@Configuration
@EnableStateMachine
public class ContractStateMachineConfig
    extends StateMachineConfigurerAdapter<ContractStatus, ContractEvent> {

  @Override
  public void configure(StateMachineStateConfigurer<ContractStatus, ContractEvent> states)
      throws Exception {
    states
      .withStates()
      .initial(ContractStatus.DRAFT)
      .state(ContractStatus.ESIGN_INITIATED)
      .state(ContractStatus.SIGNED)
      .state(ContractStatus.MANDATE_CREATED)
      .state(ContractStatus.ACTIVE)
      .state(ContractStatus.PAYMENT_DUE)
      .state(ContractStatus.PAYMENT_INITIATED)
      .end(ContractStatus.COMPLETED)
      .end(ContractStatus.LEGAL_DEFAULT)
      .end(ContractStatus.CANCELLED)
      .end(ContractStatus.EXPIRED);
  }

  @Override
  public void configure(StateMachineTransitionConfigurer<ContractStatus, ContractEvent> transitions)
      throws Exception {
    transitions
      // DRAFT → ESIGN_INITIATED
      .withExternal()
        .source(ContractStatus.DRAFT)
        .target(ContractStatus.ESIGN_INITIATED)
        .event(ContractEvent.INITIATE_ESIGN)
        .action(initiateESignAction())
      .and()

      // ESIGN_INITIATED → SIGNED
      .withExternal()
        .source(ContractStatus.ESIGN_INITIATED)
        .target(ContractStatus.SIGNED)
        .event(ContractEvent.ESIGN_COMPLETED)
        .action(processSignedContractAction())
        .guard(eSignSuccessfulGuard())
      .and()

      // SIGNED → MANDATE_CREATED
      .withExternal()
        .source(ContractStatus.SIGNED)
        .target(ContractStatus.MANDATE_CREATED)
        .event(ContractEvent.MANDATE_CREATED)
        .action(createMandateAction())
      .and()

      // MANDATE_CREATED → ACTIVE
      .withExternal()
        .source(ContractStatus.MANDATE_CREATED)
        .target(ContractStatus.ACTIVE)
        .event(ContractEvent.MANDATE_AUTHORIZED)
        .action(publishContractActiveEvent())
      .and()

      // ACTIVE → PAYMENT_DUE
      .withExternal()
        .source(ContractStatus.ACTIVE)
        .target(ContractStatus.PAYMENT_DUE)
        .event(ContractEvent.PAYMENT_DUE_REACHED)
        .action(schedulePreDebitNotifications())
      .and()

      // PAYMENT_DUE → PAYMENT_INITIATED
      .withExternal()
        .source(ContractStatus.PAYMENT_DUE)
        .target(ContractStatus.PAYMENT_INITIATED)
        .event(ContractEvent.PAYMENT_INITIATED)
        .action(executePaymentAction())
      .and()

      // PAYMENT_INITIATED → COMPLETED (success)
      .withExternal()
        .source(ContractStatus.PAYMENT_INITIATED)
        .target(ContractStatus.COMPLETED)
        .event(ContractEvent.PAYMENT_SUCCESS)
        .action(recordSuccessfulPaymentAction())
      .and()

      // PAYMENT_INITIATED → LEGAL_DEFAULT (failed retry)
      .withExternal()
        .source(ContractStatus.PAYMENT_INITIATED)
        .target(ContractStatus.LEGAL_DEFAULT)
        .event(ContractEvent.RETRY_FAILED)
        .action(initiateLegalNoticeAction());
  }

  // Action beans (State-specific behavior)
  @Bean
  public Action<ContractStatus, ContractEvent> initiateESignAction() {
    return context -> {
      Contract contract = context.getMessage().getHeaders().get("contract", Contract.class);
      log.info("Initiating eSign for contract: {}", contract.getId());
      eSignService.initiateESign(contract);
    };
  }

  @Bean
  public Action<ContractStatus, ContractEvent> processSignedContractAction() {
    return context -> {
      Contract contract = context.getMessage().getHeaders().get("contract", Contract.class);
      log.info("Processing signed contract: {}", contract.getId());
      contractService.computePostSignHash(contract);
      blockchainService.recordEvidence(contract);
    };
  }

  // Guard beans (Transition validation)
  @Bean
  public Guard<ContractStatus, ContractEvent> eSignSuccessfulGuard() {
    return context -> {
      Contract contract = context.getMessage().getHeaders().get("contract", Contract.class);
      return contract.getPostSignHash() != null && !contract.getPostSignHash().isEmpty();
    };
  }
}

// ContractService.java (Uses state machine)
@Service
@Slf4j
public class ContractService {

  @Autowired
  private StateMachine<ContractStatus, ContractEvent> stateMachine;

  @Autowired
  private ContractRepository contractRepository;

  @Transactional
  public void transitionState(UUID contractId, ContractEvent event) {
    Contract contract = contractRepository.findById(contractId)
      .orElseThrow(() -> new ContractNotFoundException(contractId));

    // Create message with contract context
    Message<ContractEvent> message = MessageBuilder
      .withPayload(event)
      .setHeader("contract", contract)
      .build();

    // Send event to state machine
    boolean success = stateMachine.sendEvent(message);

    if (success) {
      // Update contract status in database
      ContractStatus newStatus = stateMachine.getState().getId();
      contract.setStatus(newStatus);
      contractRepository.save(contract);

      log.info("Contract {} transitioned to {}", contractId, newStatus);
    } else {
      throw new IllegalStateTransitionException(
        "Cannot transition contract " + contractId + " with event " + event
      );
    }
  }
}
    [ContractStatus.EXPIRED]: [],
  };

  static canTransition(from: ContractStatus, to: ContractStatus): boolean {
    return this.transitions[from]?.includes(to) ?? false;
  }

  static async transition(
    contractId: string,
    toStatus: ContractStatus,
    metadata?: Record<string, any>,
  ): Promise<void> {
    const contract = await ContractRepository.findById(contractId);

    if (!this.canTransition(contract.status, toStatus)) {
      throw new InvalidStateTransitionError(
        `Cannot transition from ${contract.status} to ${toStatus}`,
      );
    }

    // Execute pre-transition hooks
    await this.executePreHook(contract, toStatus, metadata);

    // Update status
    await ContractRepository.updateStatus(contractId, toStatus);

    // Log transition
    await AuditLogger.log({
      eventType: "CONTRACT_STATUS_CHANGED",
      entityType: "CONTRACT",
      entityId: contractId,
      payload: {
        from: contract.status,
        to: toStatus,
        metadata,
      },
    });

    // Execute post-transition hooks
    await this.executePostHook(contract, toStatus, metadata);
  }

  private static async executePreHook(
    contract: Contract,
    toStatus: ContractStatus,
    metadata?: Record<string, any>,
  ) {
    switch (toStatus) {
      case ContractStatus.SIGNED:
        // Verify signature hash integrity
        if (contract.preSignHash === metadata?.postSignHash) {
          throw new Error("Contract was not properly signed (hash mismatch)");
        }
        break;

      case ContractStatus.PAYMENT_INITIATED:
        // Verify pre-debit notification was sent
        const payment = await PaymentRepository.findByContractId(contract.id);
        if (!payment.predebitNotificationSentAt) {
          throw new Error("Pre-debit notification not sent");
        }
        break;
    }
  }

  private static async executePostHook(
    contract: Contract,
    toStatus: ContractStatus,
    metadata?: Record<string, any>,
  ) {
    switch (toStatus) {
      case ContractStatus.SIGNED:
        // Trigger mandate creation
        await EventBus.publish({
          type: "CONTRACT_SIGNED",
          data: { contractId: contract.id },
        });
        break;

      case ContractStatus.ACTIVE:
        // Schedule pre-debit notification
        await this.schedulePreDebitNotification(contract);
        break;

      case ContractStatus.LEGAL_DEFAULT:
        // Trigger legal notice generation
        await EventBus.publish({
          type: "LEGAL_ESCALATION_REQUIRED",
          data: { contractId: contract.id },
        });
        break;
    }
  }

  private static async schedulePreDebitNotification(contract: Contract) {
    const payment = await PaymentRepository.findByContractId(contract.id);
    const notificationDate = new Date(payment.dueDate);
    notificationDate.setDate(notificationDate.getDate() - 1); // T-24h

    await NotificationScheduler.schedule({
      executeAt: notificationDate,
      type: "PRE_DEBIT_NOTIFICATION",
      data: {
        paymentId: payment.id,
        amount: payment.amount,
        dueDate: payment.dueDate,
      },
    });
  }
}
```

---

## 7. Security & Compliance Architecture

### 7.1 Authentication & Authorization

**JWT-Based Auth:**

```typescript
// src/middleware/auth.ts
export interface JWTPayload {
  userId: string;
  userType: "MERCHANT" | "PAYER" | "ADMIN";
  merchantId?: string;
  permissions: string[];
  iat: number;
  exp: number;
}

export class AuthMiddleware {
  static async authenticate(req: Request, res: Response, next: NextFunction) {
    const token = req.headers.authorization?.split(" ")[1];

    if (!token) {
      return res.status(401).json({ error: "No token provided" });
    }

    try {
      const payload = jwt.verify(token, process.env.JWT_SECRET!) as JWTPayload;

      // Check token revocation (Redis blacklist)
      const isRevoked = await RedisClient.getInstance().exists(
        `revoked:${token}`,
      );
      if (isRevoked) {
        return res.status(401).json({ error: "Token revoked" });
      }

      req.user = payload;
      next();
    } catch (error) {
      return res.status(401).json({ error: "Invalid token" });
    }
  }

  static authorize(permissions: string[]) {
    return (req: Request, res: Response, next: NextFunction) => {
      const userPermissions = req.user?.permissions || [];

      const hasPermission = permissions.some((p) =>
        userPermissions.includes(p),
      );

      if (!hasPermission) {
        return res.status(403).json({ error: "Insufficient permissions" });
      }

      next();
    };
  }
}

// Usage
app.get(
  "/api/v1/contracts/:id",
  AuthMiddleware.authenticate,
  AuthMiddleware.authorize(["contracts:read"]),
  ContractController.getById,
);
```

### 7.2 Data Encryption

**Encryption at Rest:**

```typescript
// src/services/encryption/KMSService.ts
import { KMSClient, EncryptCommand, DecryptCommand } from "@aws-sdk/client-kms";

export class KMSService {
  private client: KMSClient;
  private keyId: string;

  constructor() {
    this.client = new KMSClient({ region: process.env.AWS_REGION });
    this.keyId = process.env.KMS_KEY_ID!;
  }

  async encrypt(plaintext: string): Promise<string> {
    const command = new EncryptCommand({
      KeyId: this.keyId,
      Plaintext: Buffer.from(plaintext),
    });

    const response = await this.client.send(command);
    return Buffer.from(response.CiphertextBlob!).toString("base64");
  }

  async decrypt(ciphertext: string): Promise<string> {
    const command = new DecryptCommand({
      CiphertextBlob: Buffer.from(ciphertext, "base64"),
    });

    const response = await this.client.send(command);
    return Buffer.from(response.Plaintext!).toString("utf-8");
  }
}

// Usage: Encrypt merchant API keys
export class MerchantRepository {
  static async create(merchant: CreateMerchantDTO) {
    const kms = new KMSService();

    const encryptedRazorpayKey = await kms.encrypt(merchant.razorpayKeyId);
    const encryptedRazorpaySecret = await kms.encrypt(
      merchant.razorpayKeySecret,
    );

    return db.query(
      `INSERT INTO merchants (business_name, email, razorpay_key_id_encrypted, razorpay_key_secret_encrypted)
       VALUES ($1, $2, $3, $4) RETURNING *`,
      [
        merchant.businessName,
        merchant.email,
        encryptedRazorpayKey,
        encryptedRazorpaySecret,
      ],
    );
  }
}
```

**Encryption in Transit:**

```nginx
# nginx.conf
server {
    listen 443 ssl http2;
    server_name api.legalpay.in;

    ssl_certificate /etc/letsencrypt/live/legalpay.in/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/legalpay.in/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
    ssl_prefer_server_ciphers on;

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    location / {
        proxy_pass http://backend:3000;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 7.3 PII Redaction & Logging

```typescript
// src/utils/logger.ts
export class SecureLogger {
  private static piiPatterns = {
    email: /([a-zA-Z0-9._-]+)@([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})/g,
    phone: /(\+?\d{1,3}[-.\s]?)?(\d{10})/g,
    aadhaar: /\d{4}\s?\d{4}\s?\d{4}/g,
    pan: /[A-Z]{5}\d{4}[A-Z]/g,
  };

  static redactPII(data: any): any {
    if (typeof data === "string") {
      return this.redactString(data);
    }

    if (Array.isArray(data)) {
      return data.map((item) => this.redactPII(item));
    }

    if (typeof data === "object" && data !== null) {
      const redacted: any = {};
      for (const [key, value] of Object.entries(data)) {
        if (this.isSensitiveKey(key)) {
          redacted[key] = "***REDACTED***";
        } else {
          redacted[key] = this.redactPII(value);
        }
      }
      return redacted;
    }

    return data;
  }

  private static redactString(str: string): string {
    return str
      .replace(this.piiPatterns.email, (match, name, domain) => {
        return `${name.slice(0, 2)}***@${domain}`;
      })
      .replace(this.piiPatterns.phone, (match, code, number) => {
        return `${code || ""}***${number.slice(-4)}`;
      })
      .replace(this.piiPatterns.aadhaar, "****-****-****")
      .replace(this.piiPatterns.pan, "*****-****-*");
  }

  private static isSensitiveKey(key: string): boolean {
    const sensitiveKeys = [
      "password",
      "secret",
      "apiKey",
      "accessToken",
      "refreshToken",
      "aadhaar",
      "otp",
    ];
    return sensitiveKeys.some((sk) => key.toLowerCase().includes(sk));
  }

  static log(level: "info" | "warn" | "error", message: string, data?: any) {
    const redacted = data ? this.redactPII(data) : {};

    console.log(
      JSON.stringify({
        timestamp: new Date().toISOString(),
        level,
        message,
        data: redacted,
      }),
    );
  }
}
```

---

## 8. Error Handling & Resilience

### 8.1 Circuit Breaker Pattern

```typescript
// src/utils/CircuitBreaker.ts
export class CircuitBreaker {
  private failureCount = 0;
  private successCount = 0;
  private lastFailureTime?: Date;
  private state: "CLOSED" | "OPEN" | "HALF_OPEN" = "CLOSED";

  constructor(
    private config: {
      failureThreshold: number;
      resetTimeout: number; // milliseconds
      successThreshold?: number;
    },
  ) {}

  async execute<T>(fn: () => Promise<T>): Promise<T> {
    if (this.state === "OPEN") {
      if (this.shouldAttemptReset()) {
        this.state = "HALF_OPEN";
      } else {
        throw new Error("Circuit breaker is OPEN");
      }
    }

    try {
      const result = await fn();
      this.onSuccess();
      return result;
    } catch (error) {
      this.onFailure();
      throw error;
    }
  }

  private onSuccess() {
    this.failureCount = 0;

    if (this.state === "HALF_OPEN") {
      this.successCount++;
      if (this.successCount >= (this.config.successThreshold || 2)) {
        this.state = "CLOSED";
        this.successCount = 0;
      }
    }
  }

  private onFailure() {
    this.failureCount++;
    this.lastFailureTime = new Date();

    if (this.failureCount >= this.config.failureThreshold) {
      this.state = "OPEN";
    }
  }

  private shouldAttemptReset(): boolean {
    if (!this.lastFailureTime) return false;

    const elapsed = Date.now() - this.lastFailureTime.getTime();
    return elapsed >= this.config.resetTimeout;
  }
}
```

### 8.2 Retry Policy with Exponential Backoff

```typescript
// src/utils/RetryPolicy.ts
export class RetryPolicy {
  constructor(
    private config: {
      maxRetries: number;
      backoff: "fixed" | "exponential";
      baseDelay?: number; // milliseconds
      retryableErrors?: string[];
    },
  ) {}

  async execute<T>(fn: () => Promise<T>): Promise<T> {
    let lastError: Error;

    for (let attempt = 0; attempt <= this.config.maxRetries; attempt++) {
      try {
        return await fn();
      } catch (error: any) {
        lastError = error;

        if (!this.isRetriable(error) || attempt === this.config.maxRetries) {
          throw error;
        }

        const delay = this.calculateDelay(attempt);
        await this.sleep(delay);
      }
    }

    throw lastError!;
  }

  private isRetriable(error: any): boolean {
    if (!this.config.retryableErrors) return true;

    return this.config.retryableErrors.some(
      (code) => error.code === code || error.message?.includes(code),
    );
  }

  private calculateDelay(attempt: number): number {
    const baseDelay = this.config.baseDelay || 1000;

    if (this.config.backoff === "exponential") {
      return baseDelay * Math.pow(2, attempt);
    }

    return baseDelay;
  }

  private sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
}
```

---

## 9. Observability & Monitoring

### 9.1 Metrics (Prometheus + Grafana)

```typescript
// src/utils/metrics.ts
import client from "prom-client";

export class Metrics {
  private static registry = new client.Registry();

  static httpRequestDuration = new client.Histogram({
    name: "http_request_duration_seconds",
    help: "Duration of HTTP requests in seconds",
    labelNames: ["method", "route", "status_code"],
    buckets: [0.1, 0.3, 0.5, 0.7, 1, 3, 5, 7, 10],
    registers: [this.registry],
  });

  static paymentStatus = new client.Counter({
    name: "payment_status_total",
    help: "Count of payments by status",
    labelNames: ["status", "gateway"],
    registers: [this.registry],
  });

  static blockchainWriteLatency = new client.Histogram({
    name: "blockchain_write_latency_seconds",
    help: "Latency of blockchain evidence writes",
    buckets: [1, 5, 10, 30, 60, 120],
    registers: [this.registry],
  });

  static getRegistry() {
    return this.registry;
  }
}

// Middleware
export const metricsMiddleware = (
  req: Request,
  res: Response,
  next: NextFunction,
) => {
  const start = Date.now();

  res.on("finish", () => {
    const duration = (Date.now() - start) / 1000;

    Metrics.httpRequestDuration
      .labels(
        req.method,
        req.route?.path || req.path,
        res.statusCode.toString(),
      )
      .observe(duration);
  });

  next();
};

// Metrics endpoint
app.get("/metrics", async (req, res) => {
  res.set("Content-Type", Metrics.getRegistry().contentType);
  res.end(await Metrics.getRegistry().metrics());
});
```

### 9.2 Distributed Tracing (Jaeger/OpenTelemetry)

```typescript
// src/utils/tracing.ts
import { NodeTracerProvider } from "@opentelemetry/sdk-trace-node";
import { JaegerExporter } from "@opentelemetry/exporter-jaeger";
import { SimpleSpanProcessor } from "@opentelemetry/sdk-trace-base";

export function initTracing() {
  const provider = new NodeTracerProvider();

  const exporter = new JaegerExporter({
    endpoint:
      process.env.JAEGER_ENDPOINT || "http://localhost:14268/api/traces",
  });

  provider.addSpanProcessor(new SimpleSpanProcessor(exporter));
  provider.register();

  return provider.getTracer("legalpay");
}

// Usage
const tracer = initTracing();

export async function executePaymentWithTracing(paymentId: string) {
  const span = tracer.startSpan("executePayment");
  span.setAttribute("payment.id", paymentId);

  try {
    const result = await PaymentService.execute(paymentId);
    span.setStatus({ code: 0 }); // OK
    return result;
  } catch (error) {
    span.setStatus({ code: 2, message: error.message }); // ERROR
    throw error;
  } finally {
    span.end();
  }
}
```

---

## 10. Deployment & Infrastructure

### 10.1 Local Development Setup (No Containers)

**Philosophy**: Native installation for local dev (faster, no disk overhead). Containers **only** for production (security isolation).

**💡 Cost-Saving Alternative**: Skip local setup entirely and use **Railway.app free tier** for development (see [Bootstrap_Strategy_Free_Minimal_Cost.md](Bootstrap_Strategy_Free_Minimal_Cost.md)).

#### 10.1.1 Prerequisites Installation

**macOS:**

```bash
# Install Homebrew if not already installed
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Node.js 20 LTS
brew install node@20
echo 'export PATH="/opt/homebrew/opt/node@20/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Install PostgreSQL 15
brew install postgresql@15
echo 'export PATH="/opt/homebrew/opt/postgresql@15/bin:$PATH"' >> ~/.zshrc

# Install Redis
brew install redis

# Install RabbitMQ
brew install rabbitmq
echo 'export PATH="/opt/homebrew/opt/rabbitmq/sbin:$PATH"' >> ~/.zshrc

source ~/.zshrc
```

**Linux (Ubuntu/Debian):**

```bash
# Node.js 20 LTS
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# PostgreSQL 15
sudo apt-get install -y postgresql-15 postgresql-contrib

# Redis
sudo apt-get install -y redis-server

# RabbitMQ
sudo apt-get install -y rabbitmq-server
```

#### 10.1.2 Database & Services Setup

**PostgreSQL Configuration:**

```bash
# Start PostgreSQL (macOS)
brew services start postgresql@15

# Start PostgreSQL (Linux)
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Create database and user
psql postgres -c "CREATE USER legalpay WITH PASSWORD 'dev_password';"
psql postgres -c "CREATE DATABASE legalpay OWNER legalpay;"
psql postgres -c "GRANT ALL PRIVILEGES ON DATABASE legalpay TO legalpay;"

# Verify connection
psql -U legalpay -d legalpay -h localhost -c "SELECT version();"
```

**Redis Configuration:**

```bash
# Start Redis (macOS)
brew services start redis

# Start Redis (Linux)
sudo systemctl start redis-server
sudo systemctl enable redis-server

# Test connection
redis-cli ping  # Should return "PONG"
```

**RabbitMQ Configuration:**

```bash
# Start RabbitMQ (macOS)
brew services start rabbitmq

# Start RabbitMQ (Linux)
sudo systemctl start rabbitmq-server
sudo systemctl enable rabbitmq-server

# Enable management plugin
rabbitmq-plugins enable rabbitmq_management

# Create user and vhost
rabbitmqctl add_user legalpay dev_password
rabbitmqctl add_vhost legalpay_vhost
rabbitmqctl set_permissions -p legalpay_vhost legalpay ".*" ".*" ".*"

# Access management UI: http://localhost:15672
# Default credentials: guest/guest
```

#### 10.1.3 Application Setup

**Environment Configuration** (`.env.local`):

```bash
# Database
DATABASE_URL=postgresql://legalpay:dev_password@localhost:5432/legalpay

# Redis
REDIS_URL=redis://localhost:6379

# RabbitMQ
RABBITMQ_URL=amqp://legalpay:dev_password@localhost:5672/legalpay_vhost

# Application
NODE_ENV=development
PORT=3000
LOG_LEVEL=debug

# Payment Gateway (Test Mode)
RAZORPAY_KEY_ID=rzp_test_xxx
RAZORPAY_KEY_SECRET=test_secret_xxx

# eSign Provider
LEEGALITY_API_KEY=test_key_xxx

# Blockchain (Mumbai Testnet)
POLYGON_RPC_URL=https://rpc-mumbai.maticvigil.com
POLYGON_PRIVATE_KEY=0x...
```

**Install Dependencies & Run:**

```bash
# Clone repository
cd /Volumes/Mac_backup\ 1/LegalPayApp

# Install dependencies
npm install

# Run database migrations
npm run migrate:dev

# Start API server
npm run dev

# In separate terminal: Start background workers
npm run worker:dev
```

**Development Scripts** (`package.json`):

```json
{
  "scripts": {
    "dev": "nodemon --watch src src/server.ts",
    "worker:dev": "nodemon --watch src src/workers/index.ts",
    "migrate:dev": "node scripts/migrate.js",
    "seed:dev": "node scripts/seed.js",
    "test": "jest --coverage",
    "lint": "eslint src --ext .ts"
  }
}
```

#### 10.1.4 Verification

```bash
# Check all services are running
ps aux | grep postgres  # PostgreSQL
ps aux | grep redis     # Redis
ps aux | grep rabbitmq  # RabbitMQ

# Test API
curl http://localhost:3000/health
# {"status":"ok","timestamp":"2026-01-26T10:30:00.000Z"}

# Test database
psql -U legalpay -d legalpay -c "SELECT COUNT(*) FROM contracts;"

# Monitor logs
tail -f logs/app.log
```

### 10.2 Production Deployment Options

#### Option A: Railway.app (Bootstrap - ₹0/mo)

**Best For:** MVP launch, small startups, testing

**Free Tier ($5/mo credit included):**

- Spring Boot API (256MB RAM)
- PostgreSQL (shared 1GB)
- Redis (shared 256MB)
- RabbitMQ (256MB)
- Auto-deploy from GitHub
- Custom domain + SSL (free)

**Setup:**

```bash
npm install -g @railway/cli
railway login
railway init
railway add postgresql redis
railway up
```

**Pros:** Zero cost, instant deployment, managed services  
**Cons:** Limited to 512MB RAM per service, no auto-scaling  
**Monthly Cost:** ₹0 (within $5 credit)

#### Option B: Kubernetes (Production - ₹15,000+/mo)

**Best For:** Scale (> 1000 users), compliance requirements, funded startups

**🔒 Production Security**: Use **gVisor** sandboxed runtime to prevent container escape attacks.

**What is gVisor?**

- User-space kernel that intercepts syscalls
- Prevents direct access to host kernel
- Google uses this for GCP, Cloud Run, App Engine
- Supported by GKE, EKS, AKS via RuntimeClass

**Enable gVisor on GKE**:

```bash
# Create GKE cluster with gVisor node pool
gcloud container clusters create legalpay-prod \
  --enable-sandbox \
  --sandbox-type=gvisor \
  --region=asia-south1
```

**RuntimeClass Configuration**:

```yaml
# k8s/runtimeclass.yaml
apiVersion: node.k8s.io/v1
kind: RuntimeClass
metadata:
  name: gvisor
handler: runsc # gVisor runtime handler
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: legalpay-api
  annotations:
    # PCI DSS v4.0 Requirement 2.2.4: Secure container runtime
    compliance.pci-dss: "2.2.4"
spec:
  replicas: 3
  selector:
    matchLabels:
      app: legalpay-api
  template:
    metadata:
      labels:
        app: legalpay-api
    spec:
      runtimeClassName: gvisor # ⚠️ CRITICAL: Use sandboxed runtime
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
        seccompProfile:
          type: RuntimeDefault
      containers:
        - name: api
          image: legalpay/api:latest
          ports:
            - containerPort: 3000
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop:
                - ALL
          env:
            - name: DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: legalpay-secrets
                  key: database-url
            - name: RAZORPAY_KEY_ID
              valueFrom:
                secretKeyRef:
                  name: legalpay-secrets
                  key: razorpay-key-id
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /health
              port: 3000
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /ready
              port: 3000
            initialDelaySeconds: 5
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: legalpay-api
spec:
  selector:
    app: legalpay-api
  ports:
    - protocol: TCP
      port: 80
      targetPort: 3000
  type: LoadBalancer
```

---

## 11. Implementation Roadmap

### Week 1-2: Foundation & Setup

**Java Backend:**

- [x] Database schema setup (PostgreSQL with Spring Data JPA)
- [x] Base project structure (Maven multi-module: api, services, domain)
- [x] Spring Boot 3.2 project initialization
- [x] Spring Security configuration (JWT authentication)
- [x] Audit logger service (Spring Data JPA batch inserts)
- [x] Spring Cloud Gateway setup
- [x] Resilience4j configuration (circuit breaker, retry, rate limiter)

**DevOps:**

- [x] Local development setup (PostgreSQL, Redis, RabbitMQ via brew)
- [x] Git repository structure (Maven multi-module monorepo)
- [x] IntelliJ IDEA project setup

### Week 3-4: Core Services

**Java Services:**

- [x] Contract Service (PDF generation with iText, S3 upload, hashing)
- [x] eSign Wrapper (Leegality/Digio REST client with WebClient)
- [x] Payment Gateway Wrapper (Razorpay Java SDK, Cashfree integration)
- [x] Mandate Service (eNACH/UPI Autopay orchestration)
- [x] Template engine (Thymeleaf for HTML → PDF conversion)

**Infrastructure:**

- [x] Redis integration (idempotency cache, distributed locks)
- [x] RabbitMQ integration (Spring AMQP for async messaging)

### Week 5-6: Payment Execution

**Java Services:**

- [x] Payment Service orchestration (Spring State Machine)
- [x] Pre-debit notification scheduler (Spring Scheduler + Cron)
- [x] Dunning Engine (Quartz Scheduler + return code classification)
- [x] Notification Service (Twilio SDK, Gupshup WhatsApp API)

**Features:**

- [x] Circuit breaker patterns for all external APIs
- [x] Idempotency key handling (Redis 24h TTL)
- [x] Webhook HMAC validation

### Week 7-8: Evidence & Legal

**Java Services:**

- [x] Blockchain service (Web3j + Polygon Mumbai testnet)
- [x] Evidence queue worker (RabbitMQ listener for async blockchain writes)
- [x] Legal notice generator (Section 25 PSS Act compliance)
- [x] GST invoice generation (e-invoicing IRP integration)

**Security:**

- [x] Bouncy Castle integration (SHA-256, HMAC, digital signatures)
- [x] KMS encryption for PII (AWS KMS SDK)

### Week 9-10: Testing & Hardening

**All Services:**

- [x] Unit tests (JUnit 5 + Mockito, 80%+ coverage via JaCoCo)
- [x] Integration tests (Spring Boot Test with TestContainers)
- [x] End-to-end tests (full payment flow with WireMock for external APIs)
- [x] Load testing (JMeter: 10K concurrent users, p95 < 200ms)
- [x] Security audit (OWASP Dependency Check, SonarQube)

**Performance Benchmarks:**

- [x] PDF generation: 100 PDFs/sec sustained
- [x] Payment API: 5K requests/sec (Virtual Threads)
- [x] Database: Optimize indexes for queries

### Week 11-12: Production Readiness

**Observability:**

- [x] Micrometer metrics (Spring Actuator + Prometheus)
- [x] Grafana dashboards (JVM metrics, API latency, payment success rate)
- [x] Distributed tracing (Spring Cloud Sleuth + Jaeger)
- [x] Alerting setup (PagerDuty webhook integration)

**Deployment:**

- [x] Kubernetes manifests (Deployment, Service, ConfigMap, Secret)
- [x] CI/CD pipeline (GitHub Actions: Maven build → Docker image → GKE deploy)
- [x] Production secrets management (AWS Secrets Manager + Spring Cloud Config)
- [x] API documentation (SpringDoc OpenAPI 3.0)

**Production Checklist:**

- [x] JVM tuning (G1GC, heap sizing for 4GB RAM pods)
- [x] Connection pool tuning (HikariCP: 20 max connections)
- [x] Redis cluster setup (AWS ElastiCache with failover)
- [x] PostgreSQL read replicas (for reporting queries)

**Future Enhancements:**

- [ ] Kotlin migration (gradual refactor for more concise code)
- [ ] GraalVM native image (faster cold starts for serverless)
- [ ] Rust microservices (only if extreme performance needed)

---

- [x] Payment Service orchestration
- [x] Pre-debit notification scheduler
- [x] Dunning Engine (retry logic with return code classification)
- [x] Notification Service (Twilio/Gupshup integration)

**Java:**

- [x] Crypto Service (SHA-256 hashing, HMAC validation)
- [x] Digital signature verification (for eSign certificates)

### Week 7-8: Evidence & Legal

**Node.js:**

- [x] Blockchain service (Web3.js + Polygon integration)
- [x] Evidence queue worker (RabbitMQ consumer)
- [x] Legal notice generator
- [x] GST invoice generation module

**Java:**

- [x] Batch processing for bulk PDF generation
- [x] Webhook validator service (HMAC verification at scale)

### Week 9-10: Testing & Hardening

**All Services:**

- [x] Unit tests (Node: Jest 80%+ coverage, Java: JUnit 5 80%+ coverage)
- [x] Integration tests (Node ↔ Java service communication)
- [x] End-to-end tests (full payment flow)
- [x] Load testing (10K concurrent users via k6)
- [x] Security audit (OWASP Top 10, SonarQube)

**Performance Testing:**

- [x] Java PDF service: 100 PDFs/sec sustained
- [x] Node.js API: 5K req/sec (p95 < 200ms)

### Week 11-12: Production Readiness

**Observability:**

- [x] Prometheus metrics (Node + Java Spring Actuator)
- [x] Grafana dashboards (unified Node + Java metrics)
- [x] Jaeger distributed tracing (OpenTelemetry)
- [x] Alerting setup (PagerDuty integration)

**Deployment:**

- [x] Kubernetes manifests (Node services + Java services)
- [x] CI/CD pipeline (GitHub Actions: build → test → deploy)
- [x] Production secrets management (AWS Secrets Manager)
- [x] API documentation (Swagger/OpenAPI for both stacks)

**Optional (Future):**

- [ ] Rust PDF service (if Java still has latency issues)
- [ ] Rust crypto service (if security audit requires memory-safe ops)

---

## 12. Edge Cases & Mitigation

| Edge Case                              | Impact                              | Mitigation                                                                                      |
| -------------------------------------- | ----------------------------------- | ----------------------------------------------------------------------------------------------- |
| **Webhook delivery failure**           | Missed eSign/payment status updates | Implement webhook retry (3x exp backoff); poll API as fallback every 5min for 24h               |
| **Blockchain gas spike**               | Evidence write too expensive        | Queue writes; wait for gas < threshold; use off-chain signed timestamp as fallback              |
| **Bank mandate limit changed**         | UPI Autopay capped mid-contract     | Check limits before payment; if exceeded, pause mandate and notify merchant to migrate to eNACH |
| **Payer changes phone number**         | OTP/notifications fail              | Allow payer to update contact via magic link; re-verify via Aadhaar OTP                         |
| **Razorpay outage (> 1h)**             | Cannot execute payments             | Fallback to Cashfree if configured; queue payments for retry when primary is up                 |
| **S3 corruption/data loss**            | Contract PDF unavailable            | Multi-region replication (S3 cross-region); integrity checks via SHA-256 before serving         |
| **Race condition (duplicate payment)** | Double debit                        | Use idempotency keys; distributed lock (Redis) on `mandate_id + due_date`                       |
| **Merchant deletes account**           | Orphaned contracts                  | Soft delete only; retain data for 7 years (legal requirement); anonymize PII after 3 years      |
| **NPCI downtime (eNACH)**              | Mandate creation fails              | Retry after 1h; if > 24h, notify payer to retry authorization                                   |
| **Legal notice undelivered**           | Statutory timeline violated         | Multi-channel delivery (email + SMS + WhatsApp + registered post); log all attempts             |

---

## Appendix A: Return Code Map

| Code | Description                     | Retriable | Action                       |
| ---- | ------------------------------- | --------- | ---------------------------- |
| R01  | Insufficient Funds              | ✅ Yes    | Retry T+10                   |
| R02  | Account Closed                  | ❌ No     | Legal escalation             |
| R03  | No Account/Unable to Locate     | ❌ No     | Legal escalation             |
| R04  | Invalid Account Number          | ❌ No     | Legal escalation             |
| R05  | Unauthorized Debit              | ❌ No     | Notify merchant; investigate |
| R06  | Returned per ODFI Request       | ❌ No     | Legal escalation             |
| R07  | Authorization Revoked           | ❌ No     | Mark mandate cancelled       |
| R08  | Payment Stopped                 | ❌ No     | Legal escalation             |
| R09  | Uncollected Funds               | ✅ Yes    | Retry T+10                   |
| R10  | Customer Advises Not Authorized | ❌ No     | Dispute; freeze contract     |
| R13  | Invalid ACH Routing Number      | ❌ No     | Contact merchant to fix      |
| R14  | Representative Payee Deceased   | ❌ No     | Mark payer inactive          |
| R15  | Beneficiary Deceased            | ❌ No     | Mark payer inactive          |

---

## Appendix B: API Endpoint Catalog

### Contract APIs

- `POST /api/v1/contracts` - Create new contract
- `GET /api/v1/contracts/:id` - Get contract details
- `POST /api/v1/contracts/:id/esign` - Initiate eSign
- `GET /api/v1/contracts/:id/pdf` - Download contract PDF
- `DELETE /api/v1/contracts/:id` - Cancel contract

### Mandate APIs

- `POST /api/v1/mandates` - Create mandate
- `GET /api/v1/mandates/:id` - Get mandate details
- `POST /api/v1/mandates/:id/pause` - Pause mandate
- `POST /api/v1/mandates/:id/resume` - Resume mandate
- `DELETE /api/v1/mandates/:id` - Cancel mandate

### Payment APIs

- `GET /api/v1/payments` - List payments (paginated)
- `GET /api/v1/payments/:id` - Get payment details
- `POST /api/v1/payments/:id/retry` - Manual retry

### Webhook Endpoints

- `POST /webhooks/esign/leegality` - Leegality eSign callback
- `POST /webhooks/esign/digio` - Digio eSign callback
- `POST /webhooks/payment/razorpay` - Razorpay payment callback
- `POST /webhooks/payment/cashfree` - Cashfree payment callback

---

**Document Version:** 1.0  
**Last Updated:** 26 January 2026  
**Next Review:** Before Sprint 1 kickoff
