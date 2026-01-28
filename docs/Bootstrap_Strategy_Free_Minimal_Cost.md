# LegalPay Bootstrap Strategy: Free & Minimal Cost

**Version:** 1.0  
**Date:** 27 January 2026  
**Target:** MVP launch with ₹0-₹5000/month total operating cost  
**Philosophy:** Bootstrap first, scale paid services based on revenue

---

## Executive Summary

This document replaces expensive third-party services with free/freemium alternatives to launch LegalPay with **near-zero operating costs** until revenue justifies paid tiers.

**Total Monthly Cost: ₹0 - ₹5,000** (vs ₹50,000+ with premium stack)

---

## Cost Comparison: Premium vs Bootstrap

| Service Category    | Premium Stack      | Monthly Cost             | Bootstrap Alternative            | Monthly Cost                         |
| ------------------- | ------------------ | ------------------------ | -------------------------------- | ------------------------------------ |
| **Payment Gateway** | Razorpay Business  | 2% + GST per transaction | Razorpay Standard                | 2% per transaction (no platform fee) |
| **eSign Provider**  | Leegality/Digio    | ₹50-150/signature        | **Aadhaar eSign Direct** (UIDAI) | ₹5-10/signature                      |
| **Cloud Hosting**   | AWS EC2/RDS        | ₹15,000-30,000/mo        | Railway/Render Free Tier         | ₹0 (free tier) → ₹2,000 at scale     |
| **Database**        | AWS RDS PostgreSQL | ₹5,000/mo                | Supabase Free Tier               | ₹0 (500MB limit) → ₹800/mo (Pro)     |
| **Redis Cache**     | AWS ElastiCache    | ₹3,000/mo                | Upstash Free Tier                | ₹0 (10K commands/day) → ₹500/mo      |
| **Message Queue**   | AWS SQS            | ₹2,000/mo                | Railway RabbitMQ                 | ₹0 (free tier)                       |
| **Object Storage**  | AWS S3             | ₹2,000/mo                | Cloudflare R2                    | ₹0 (10GB free)                       |
| **SMS/Email**       | Twilio + SendGrid  | ₹5,000/mo                | **TRAI DLT + Gmail SMTP**        | ₹500/mo (DLT registration only)      |
| **Blockchain**      | Polygon Mainnet    | ₹1,000/mo (gas fees)     | Polygon Mumbai Testnet           | ₹0 (free testnet)                    |
| **Monitoring**      | Datadog/New Relic  | ₹10,000/mo               | Grafana Cloud Free               | ₹0 (14-day retention)                |
| **SSL Certificate** | Paid cert          | ₹3,000/year              | Let's Encrypt                    | ₹0 (free, auto-renew)                |
| **Domain**          | .com domain        | ₹1,000/year              | .in domain                       | ₹500/year                            |
| **Total**           |                    | **₹48,000-70,000/mo**    |                                  | **₹0-5,000/mo**                      |

---

## 1. Payment Gateway: Razorpay Standard (Free Tier)

### Why Razorpay?

- **No platform fee** for startups (< ₹25L annual GMV)
- Only pay **2% per transaction** (vs 2% + ₹3 platform fee on business plan)
- UPI Autopay & eNACH supported (same as paid tier)
- Test mode is completely free

### Bootstrap Strategy

```yaml
Phase 1 (0-100 transactions/mo):
  Provider: Razorpay Test Mode
  Cost: ₹0
  Limitation: Cannot process real money (demo only)

Phase 2 (100-1000 transactions/mo):
  Provider: Razorpay Standard Account
  Cost: 2% per transaction
  Revenue Model: Pass cost to customer or absorb in pricing
  Example: ₹1000 payment → ₹20 fee → You charge ₹1020 from payer

Phase 3 (> 1000 transactions/mo):
  Provider: Razorpay Business Account
  Cost: Negotiate custom rates (1.5% + ₹3)
  Trigger: When revenue > ₹5L/month, negotiate better rates
```

### Setup (Free Tier)

```bash
# 1. Sign up at https://razorpay.com
# 2. KYC verification (free, takes 24 hours)
# 3. Get test API keys

# Environment variables
RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxxxxxx
RAZORPAY_KEY_SECRET=xxxxxxxxxxxxxxxxxxxxxxxx
RAZORPAY_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxx
```

**Java Integration** (same as premium):

```java
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@Service
public class RazorpayService {
    private final RazorpayClient client;

    public RazorpayService(
        @Value("${razorpay.key.id}") String keyId,
        @Value("${razorpay.key.secret}") String keySecret
    ) throws RazorpayException {
        this.client = new RazorpayClient(keyId, keySecret);
    }

    // Same implementation as premium tier
}
```

**Cost Savings:** ₹0 platform fee + only pay when you earn

---

## 2. eSign Provider: Direct Aadhaar eSign (UIDAI)

### Why Direct Integration?

- **Leegality/Digio markup:** ₹50-150 per signature
- **Direct UIDAI cost:** ₹5-10 per signature (90% cheaper)
- **Downside:** Requires ASP/KUA license (₹25,000 one-time + ₹50,000 bank guarantee)

### Bootstrap Strategy

**Option A: Partner with Existing ASP** (Recommended for MVP)

```yaml
Partner: Digio Startup Plan
Cost: ₹15/signature (negotiated volume pricing)
Volume: 100 signatures/mo free trial
Setup: Sign reseller agreement
Why: No upfront cost, pay-per-use
```

**Option B: DIY Aadhaar eSign** (After 500+ signatures/month)

```yaml
Cost Breakdown:
  ASP License: ₹25,000 (one-time)
  Bank Guarantee: ₹50,000 (refundable after 2 years)
  UIDAI API Charges: ₹5-10/signature
  Break-even: 500 signatures (saves ₹20,000/mo vs Leegality)

Timeline:
  Week 1-2: Apply for ASP license with UIDAI
  Week 3-4: Set up HSM for key storage (₹15,000)
  Week 5-6: Integrate UIDAI APIs
  Week 7-8: Testing & compliance audit
```

**Option C: Hybrid Approach** (Best for Bootstrap)

```yaml
Phase 1 (0-100 signatures/mo):
  Use: Digio Free Trial (100 signatures)
  Cost: ₹0

Phase 2 (100-500 signatures/mo):
  Use: Digio Pay-Per-Use
  Cost: ₹15/signature
  Monthly: ₹7,500/mo (at 500 signatures)

Phase 3 (> 500 signatures/mo):
  Use: Direct UIDAI integration
  Cost: ₹5/signature
  Monthly: ₹2,500/mo (at 500 signatures)
  Savings: ₹5,000/mo (200% ROI on ₹25K license)
```

### Implementation (Digio Free Tier)

```java
// Use Digio REST API (no SDK required)
@Service
public class DigioESignService {
    private final WebClient webClient;

    public DigioESignService(
        @Value("${digio.api.key}") String apiKey
    ) {
        this.webClient = WebClient.builder()
            .baseUrl("https://api.digio.in/v2")
            .defaultHeader("Authorization", "Basic " + apiKey)
            .build();
    }

    public CompletableFuture<String> initiateESign(ESignRequest request) {
        return webClient.post()
            .uri("/client/document/upload")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ESignResponse.class)
            .map(ESignResponse::getDocumentId)
            .toFuture();
    }
}
```

**Cost Savings:** ₹0 for first 100 signatures, then ₹15 vs ₹50-150

---

## 3. Cloud Hosting: Railway.app (Free Tier)

### Why Railway?

- **$5/month credit** (₹420/mo) on free tier
- PostgreSQL, Redis, RabbitMQ included
- Git-based deployment (no Docker knowledge needed)
- Auto-scaling (pay only for usage)

### Bootstrap Strategy

```yaml
Railway Free Tier Limits:
  - $5/month free credit (₹420)
  - 512MB RAM per service (enough for Spring Boot)
  - 8GB storage (shared across services)
  - 100GB network egress/mo
  - Custom domain support (free)

Services Deployed:
  - API Gateway (256MB RAM)
  - Contract Service (256MB RAM)
  - Payment Service (256MB RAM)
  - PostgreSQL (shared 1GB)
  - Redis (shared 256MB)

Estimated Monthly Cost: ₹0 (within free tier)
```

### Alternative: Render.com (Free Tier)

```yaml
Render Free Tier:
  - 750 hours/mo free (enough for 1 service 24/7)
  - PostgreSQL 90-day expiry (upgrade to ₹7/mo for persistence)
  - Redis 25MB free
  - Auto-sleep after 15min inactivity (wakes on request)
  - SSL + CDN included

Best For: MVP demo, not production (due to sleep delays)
```

### Alternative: AWS Free Tier (12 months)

```yaml
AWS Free Tier (First Year):
  - EC2: 750 hours/mo t2.micro (1 CPU, 1GB RAM)
  - RDS: 750 hours/mo db.t2.micro (PostgreSQL)
  - S3: 5GB storage + 20,000 GET requests/mo
  - CloudFront: 50GB data transfer/mo
  - Lambda: 1M requests/mo (for background jobs)

After Year 1: Migrate to Railway or upgrade to paid tier
```

### Deployment Setup (Railway)

```bash
# 1. Install Railway CLI
npm install -g @railway/cli

# 2. Login
railway login

# 3. Initialize project
railway init

# 4. Add PostgreSQL
railway add postgresql

# 5. Add Redis
railway add redis

# 6. Deploy
railway up

# 7. Set environment variables
railway variables set RAZORPAY_KEY_ID=rzp_test_xxx
railway variables set DATABASE_URL=${{RAILWAY_POSTGRESQL_URL}}
```

**railway.json** (Service Configuration):

```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "NIXPACKS",
    "buildCommand": "mvn clean package -DskipTests"
  },
  "deploy": {
    "startCommand": "java -jar target/legalpay-api.jar",
    "healthcheckPath": "/health",
    "healthcheckTimeout": 100,
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 10
  }
}
```

**Cost Savings:** ₹0 vs ₹15,000-30,000/mo on AWS

---

## 4. Database: Supabase (Free Tier)

### Why Supabase?

- **PostgreSQL 15** with 500MB free storage
- Built-in authentication (saves building user management)
- Real-time subscriptions (for live payment status)
- Row-level security (RLS) for data isolation
- **Free forever** tier (no credit card required)

### Free Tier Limits

```yaml
Storage: 500MB (enough for 10K contracts)
Bandwidth: 5GB/mo (enough for 50K API calls)
Realtime: 200 concurrent connections
Auth: Unlimited users
Backups: 7-day retention (daily backups)
```

### When to Upgrade?

```yaml
Triggers:
  - > 500MB data (₹1,500 contracts at 300KB/contract PDF)
  - > 5GB bandwidth/mo (₹100K API requests)
  - Need 30-day backups (compliance requirement)

Pro Plan Cost: ₹800/mo (₹25 = $1)
Includes:
  - 8GB storage
  - 50GB bandwidth
  - 30-day backups
  - Priority support
```

### Alternative: Neon.tech (Serverless PostgreSQL)

```yaml
Neon Free Tier:
  - 10GB storage (20x more than Supabase)
  - Autoscaling (sleep when idle)
  - Instant branching (great for testing)
  - No connection limit

Downside: 7-day data retention on free tier
Use Case: Development/staging, not production
```

### Setup (Supabase)

```bash
# 1. Sign up at https://supabase.com
# 2. Create project
# 3. Get connection string

# Environment variable
DATABASE_URL=postgresql://postgres:[password]@db.[project].supabase.co:5432/postgres
```

**Spring Boot Configuration**:

```yaml
# application.yml
spring:
  datasource:
    url: ${DATABASE_URL}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 5 # Supabase free tier limit
      connection-timeout: 30000
      idle-timeout: 600000
```

**Cost Savings:** ₹0 vs ₹5,000/mo on AWS RDS

---

## 5. Redis Cache: Upstash (Free Tier)

### Why Upstash?

- **10,000 commands/day free** (enough for 500 payments/day with idempotency checks)
- Serverless (pay only for usage)
- Global edge caching (low latency)
- No connection limit (unlike Redis Cloud free tier)

### Free Tier Limits

```yaml
Commands: 10,000/day (refreshes daily)
Storage: 256MB
Connections: Unlimited
Latency: < 50ms (global edge network)
```

### When to Upgrade?

```yaml
Triggers:
  - > 10,000 commands/day (₹2,000+ payments/day)
  - > 256MB cache size (₹10K+ active contracts)

Pay-as-you-go Pricing: ₹0.002/command (₹200/mo at 100K commands/day)
```

### Alternative: Redis Cloud Free Tier

```yaml
Redis Cloud Free:
  - 30MB storage (vs 256MB on Upstash)
  - 30 connections max (problematic for Spring Boot pool)
  - Single region (Mumbai)

Verdict: Use Upstash for better limits
```

### Setup (Upstash)

```bash
# 1. Sign up at https://upstash.com
# 2. Create Redis database (select Mumbai region)
# 3. Get connection URL

# Environment variable
REDIS_URL=rediss://default:[password]@[endpoint].upstash.io:6379
```

**Spring Boot Configuration**:

```yaml
# application.yml
spring:
  data:
    redis:
      url: ${REDIS_URL}
      ssl:
        enabled: true
      timeout: 2000ms
```

**Cost Savings:** ₹0 vs ₹3,000/mo on AWS ElastiCache

---

## 6. Message Queue: Railway RabbitMQ (Free)

### Why Railway RabbitMQ?

- **Included in Railway's $5/month credit**
- Official RabbitMQ Docker image (stable)
- Persistent storage (messages survive restarts)
- Management UI included

### Free Tier Limits

```yaml
RAM: 256MB (handles 10K messages/hour)
Storage: 2GB (persistent queue storage)
Connections: 100 concurrent (more than enough)
```

### Alternative: AWS SQS (Free Tier)

```yaml
AWS SQS Free Tier:
  - 1M requests/mo free (forever)
  - $0.40/million after that (₹33/million)

Best For: Production (more reliable than Railway)
Downside: Requires AWS account, more complex setup
```

### Setup (Railway RabbitMQ)

```bash
# Add RabbitMQ to Railway project
railway add rabbitmq

# Get connection URL
railway variables get RABBITMQ_URL

# Environment variable
RABBITMQ_URL=amqp://user:password@rabbitmq.railway.internal:5672
```

**Spring Boot Configuration**:

```yaml
# application.yml
spring:
  rabbitmq:
    addresses: ${RABBITMQ_URL}
    listener:
      simple:
        concurrency: 2
        max-concurrency: 5
        prefetch: 10
```

**Cost Savings:** ₹0 vs ₹2,000/mo on AWS

---

## 7. Object Storage: Cloudflare R2 (Free Tier)

### Why Cloudflare R2?

- **10GB free storage** (enough for 30-50 contracts with PDFs)
- **Zero egress fees** (AWS S3 charges ₹1,500/100GB egress)
- S3-compatible API (drop-in replacement)
- Automatic CDN distribution

### Free Tier Limits

```yaml
Storage: 10GB/mo
Class A Operations: 1M/mo (PUT, POST, LIST)
Class B Operations: 10M/mo (GET, HEAD)
Egress: Unlimited (₹0 bandwidth cost)
```

### When to Upgrade?

```yaml
Triggers:
  - > 10GB storage (₹30-50 contracts)
  - Need more write operations

Paid Pricing: ₹600/mo for 100GB storage (10x cheaper than S3)
```

### Setup (Cloudflare R2)

```bash
# 1. Sign up at https://cloudflare.com
# 2. Enable R2 in dashboard
# 3. Create bucket
# 4. Generate API token

# Environment variables
R2_ENDPOINT=https://[account-id].r2.cloudflarestorage.com
R2_ACCESS_KEY_ID=xxxxxxxxxxxxxxxxxxxxxx
R2_SECRET_ACCESS_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
R2_BUCKET_NAME=legalpay-contracts
```

**Spring Boot Integration** (S3-compatible):

```java
@Configuration
public class R2Config {
    @Bean
    public S3Client s3Client(
        @Value("${r2.endpoint}") String endpoint,
        @Value("${r2.access-key-id}") String accessKeyId,
        @Value("${r2.secret-access-key}") String secretAccessKey
    ) {
        return S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
            ))
            .region(Region.of("auto")) // R2 auto-region
            .build();
    }
}
```

**Cost Savings:** ₹0 vs ₹2,000/mo on AWS S3

---

## 8. SMS/Email: TRAI DLT + Gmail SMTP (Free)

### Why This Combo?

- **Gmail SMTP:** 500 emails/day free (enough for notifications)
- **TRAI DLT:** Required for commercial SMS in India (₹500 one-time registration)
- **SMS Gateway:** Use ValueFirst/MSG91 (₹0.10/SMS, pay-per-use)

### Bootstrap Strategy

```yaml
Email (0-500/day):
  Provider: Gmail SMTP
  Cost: ₹0
  Limit: 500 emails/day
  Use: Transactional emails (OTP, notifications)

Email (> 500/day):
  Provider: Resend.com (₹1,600/mo for 50K emails)
  Alternative: AWS SES (₹800/mo for 50K emails)

SMS (0-1000/mo):
  Provider: MSG91
  Cost: ₹0.10/SMS = ₹100/mo
  Requirement: TRAI DLT registration (₹500 one-time)

SMS (> 1000/mo):
  Provider: AWS SNS (₹0.005/SMS = ₹500/mo for 10K SMS)
```

### Setup (Gmail SMTP)

```yaml
# application.yml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${GMAIL_USERNAME} # your-app@gmail.com
    password: ${GMAIL_APP_PASSWORD} # NOT your Gmail password!
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
```

**Generate Gmail App Password:**

1. Go to https://myaccount.google.com/apppasswords
2. Create app password for "Mail"
3. Use 16-character password in `GMAIL_APP_PASSWORD`

**TRAI DLT Registration:**

```bash
# 1. Register at https://www.ucc-ashi.in
# 2. Pay ₹500 registration fee
# 3. Register your entity (company/individual)
# 4. Register SMS templates (e.g., OTP template)
# 5. Get DLT Entity ID and Template IDs

# Use MSG91 for actual SMS sending
# Environment variables
MSG91_AUTH_KEY=xxxxxxxxxxxxxxxxxxxxxx
MSG91_SENDER_ID=LGLPAY  # Your approved sender ID
MSG91_DLT_ENTITY_ID=1234567890123456789
```

**Cost Savings:** ₹0 email + ₹100/mo SMS vs ₹5,000/mo on Twilio

---

## 9. Blockchain: Polygon Mumbai Testnet (Free)

### Why Mumbai Testnet?

- **Free MATIC tokens** from faucet (unlimited)
- Same API as Mainnet (easy to migrate)
- Gas fees: ₹0 (testnet)
- Good for MVP proof-of-concept

### When to Move to Mainnet?

```yaml
Triggers:
  - Need legally admissible evidence (testnet resets)
  - Production launch with real money
  - Regulatory requirement for immutable audit trail

Mainnet Cost: ₹0.50/transaction (gas fees)
Monthly: ₹500/mo at 1000 contracts/mo
```

### Setup (Mumbai Testnet)

```bash
# 1. Create wallet at https://metamask.io
# 2. Get free MATIC from https://faucet.polygon.technology
# 3. Deploy smart contract to Mumbai

# Environment variables
POLYGON_RPC_URL=https://rpc-mumbai.maticvigil.com
POLYGON_PRIVATE_KEY=0xabcdef...  # Your wallet private key
POLYGON_CONTRACT_ADDRESS=0x123456...  # Deployed contract address
```

**Web3j Integration**:

```java
@Service
public class BlockchainService {
    private final Web3j web3j;
    private final Credentials credentials;

    public BlockchainService(
        @Value("${polygon.rpc.url}") String rpcUrl,
        @Value("${polygon.private.key}") String privateKey
    ) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.credentials = Credentials.create(privateKey);
    }

    public CompletableFuture<String> storeContractHash(String contractId, String sha256Hash) {
        // Gas price: 0 on testnet, ₹0.50 on mainnet
        // Same code works on both testnet and mainnet
    }
}
```

**Cost Savings:** ₹0 vs ₹1,000/mo on mainnet

---

## 10. Monitoring: Grafana Cloud (Free Tier)

### Why Grafana Cloud?

- **10K metrics/mo free** (enough for 5 services)
- **14-day log retention** (vs 3 days on New Relic free)
- Pre-built Spring Boot dashboards
- Prometheus + Loki + Tempo (full observability stack)

### Free Tier Limits

```yaml
Metrics: 10K series (Prometheus)
Logs: 50GB/mo (Loki)
Traces: 50GB/mo (Tempo)
Retention: 14 days
Alerting: Unlimited (email/Slack)
```

### Alternative: Sentry (Error Tracking)

```yaml
Sentry Free Tier:
  - 5K errors/mo free
  - 1 project
  - 30-day retention
  - Performance monitoring (100 transactions/mo)

Best For: Exception tracking, not metrics
Use Together: Grafana (metrics) + Sentry (errors)
```

### Setup (Grafana Cloud)

```bash
# 1. Sign up at https://grafana.com
# 2. Create stack
# 3. Get Prometheus remote write endpoint

# Environment variables
GRAFANA_PROMETHEUS_URL=https://prometheus-xxx.grafana.net/api/prom/push
GRAFANA_PROMETHEUS_USER=123456
GRAFANA_PROMETHEUS_PASSWORD=xxxxxxxx
```

**Spring Boot Configuration**:

```yaml
# application.yml
management:
  metrics:
    export:
      prometheus:
        enabled: true
        pushgateway:
          base-url: ${GRAFANA_PROMETHEUS_URL}
          username: ${GRAFANA_PROMETHEUS_USER}
          password: ${GRAFANA_PROMETHEUS_PASSWORD}
          push-rate: 1m
```

**Cost Savings:** ₹0 vs ₹10,000/mo on Datadog

---

## 11. SSL & Domain: Let's Encrypt + Namecheap

### SSL Certificate: Let's Encrypt (Free)

```bash
# Automatic SSL via Railway/Render (no manual setup)
# Or use Certbot for custom server:

sudo apt-get install certbot
sudo certbot certonly --standalone -d api.legalpay.in
# Certificate auto-renews every 90 days
```

### Domain: Namecheap (.in domain)

```yaml
Cost: ₹500/year for .in domain
Alternative: ₹800/year for .com domain

DNS: Use Cloudflare DNS (free)
  - DDoS protection
  - CDN
  - Analytics
```

**Cost Savings:** ₹0 SSL + ₹500/year domain vs ₹4,000/year

---

## 12. Total Cost Breakdown (Monthly)

| Service                     | Free Tier Limit    | Cost (₹0-100 users) | Cost (₹100-1000 users)  | When to Upgrade            |
| --------------------------- | ------------------ | ------------------- | ----------------------- | -------------------------- |
| **Payment Gateway**         | 2% transaction fee | ₹0 (test mode)      | 2% of GMV               | When processing real money |
| **eSign**                   | 100 signatures/mo  | ₹0 (Digio trial)    | ₹1,500 (100 signatures) | At 100 signatures/mo       |
| **Hosting (Railway)**       | $5/mo credit       | ₹0                  | ₹0                      | At 10K requests/day        |
| **Database (Supabase)**     | 500MB storage      | ₹0                  | ₹0                      | At 500MB data              |
| **Redis (Upstash)**         | 10K commands/day   | ₹0                  | ₹0                      | At 10K commands/day        |
| **RabbitMQ (Railway)**      | 256MB RAM          | ₹0                  | ₹0                      | At 100K messages/day       |
| **Storage (Cloudflare R2)** | 10GB               | ₹0                  | ₹0                      | At 10GB PDFs               |
| **SMS (MSG91)**             | Pay-per-use        | ₹100 (1000 SMS)     | ₹500 (5000 SMS)         | Always pay-per-use         |
| **Email (Gmail SMTP)**      | 500/day            | ₹0                  | ₹0                      | At 500 emails/day          |
| **Blockchain (Mumbai)**     | Testnet            | ₹0                  | ₹0                      | At production launch       |
| **Monitoring (Grafana)**    | 10K metrics/mo     | ₹0                  | ₹0                      | At 10K metrics/mo          |
| **Domain + SSL**            | Let's Encrypt      | ₹42/mo (₹500/year)  | ₹42/mo                  | N/A                        |
| **TRAI DLT Registration**   | One-time           | ₹500 (one-time)     | ₹0                      | N/A                        |
| **Total Monthly**           |                    | **₹0-200/mo**       | **₹2,000-3,000/mo**     |                            |

---

## 13. Migration Path to Paid Tiers

### Trigger Points

```yaml
Metric: GMV (Gross Merchandise Value)

₹0-1L GMV/mo:
  Action: Stay on free tier
  Cost: ₹0-500/mo

₹1L-10L GMV/mo:
  Action: Upgrade database to Supabase Pro (₹800/mo)
  Action: Upgrade hosting to Railway Pro (₹1,600/mo)
  Cost: ₹2,400/mo + 2% transaction fees

₹10L-50L GMV/mo:
  Action: Migrate to AWS (RDS + EC2 + S3)
  Action: Negotiate Razorpay rates (1.5% instead of 2%)
  Cost: ₹15,000/mo infra + 1.5% transaction fees

₹50L+ GMV/mo:
  Action: Get ASP license for direct eSign (₹5/signature)
  Action: Move to Polygon Mainnet (₹0.50/transaction)
  Cost: ₹30,000/mo infra + 1.2% transaction fees
```

### Revenue-Based Upgrades

```yaml
Rule: Spend < 5% of revenue on infrastructure

Example at ₹10L GMV/mo:
  Revenue: ₹10,00,000/mo (assume 1% platform fee = ₹10,000 revenue)
  Max Infra Cost: 5% of ₹10,000 = ₹500/mo
  Actual Cost: ₹2,400/mo (24% of revenue)

  Action: Increase platform fee to 3% OR optimize costs
```

---

## 14. Bootstrap Implementation Checklist

### Week 1: Free Tier Setup

- [ ] Sign up for Railway.app (hosting)
- [ ] Sign up for Supabase (database)
- [ ] Sign up for Upstash (Redis)
- [ ] Sign up for Cloudflare (R2 storage + DNS)
- [ ] Sign up for Grafana Cloud (monitoring)
- [ ] Register domain on Namecheap (₹500)
- [ ] Enable Let's Encrypt SSL via Railway

### Week 2: Payment & eSign

- [ ] Sign up for Razorpay (test mode)
- [ ] Complete KYC (required even for test mode)
- [ ] Sign up for Digio (100 free signatures)
- [ ] Register with TRAI DLT (₹500 for SMS)
- [ ] Sign up for MSG91 (pay-per-use SMS)

### Week 3: Development

- [ ] Deploy Spring Boot API to Railway
- [ ] Connect Supabase PostgreSQL
- [ ] Connect Upstash Redis
- [ ] Test Razorpay payment flow
- [ ] Test Digio eSign flow
- [ ] Set up Grafana dashboards

### Week 4: Testing & Launch

- [ ] Load test with free tier limits
- [ ] Set up monitoring alerts (Grafana)
- [ ] Enable error tracking (Sentry free tier)
- [ ] Launch MVP with test mode
- [ ] Onboard 10 beta merchants

### Month 2: Gradual Migration

- [ ] Switch Razorpay from test to live mode
- [ ] Monitor usage against free tier limits
- [ ] Upgrade services as needed (database first)
- [ ] Negotiate volume pricing with Digio

---

## 15. Cost Optimization Tips

### Database Optimization

```sql
-- Use database-level compression
ALTER TABLE contracts SET (toast_tuple_target = 128);

-- Archive old contracts to cheaper storage
CREATE TABLE contracts_archive (LIKE contracts);
-- Move contracts > 1 year old to archive table
```

### Caching Strategy

```java
// Cache expensive queries in Redis (free tier: 10K commands/day)
@Cacheable(value = "contracts", key = "#contractId")
public Contract getContract(UUID contractId) {
    // Only hit database on cache miss
}

// Use TTL to reduce cache size
@CacheEvict(value = "contracts", allEntries = true)
@Scheduled(cron = "0 0 * * * *") // Hourly
public void evictExpiredContracts() {}
```

### Storage Optimization

```java
// Compress PDFs before upload (reduce storage by 50%)
public byte[] compressPDF(byte[] pdfBytes) {
    // Use iText compression
    PdfReader reader = new PdfReader(pdfBytes);
    PdfWriter writer = new PdfWriter(output);
    writer.setCompressionLevel(9);
    // ...
}

// Store only hash on blockchain, not full PDF
String contractHash = DigestUtils.sha256Hex(pdfBytes);
blockchainService.storeHash(contractId, contractHash);
```

### API Rate Limiting

```java
// Prevent abuse of free tier limits
@RateLimiter(name = "api", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> createContract(@RequestBody ContractRequest request) {
    // Limit: 10 requests/minute per user (protects free tier quotas)
}
```

---

## 16. When to Scale Out of Free Tier?

### Success Metrics

```yaml
Monthly Active Merchants: 100+
Monthly Contracts Created: 1000+
Monthly GMV: ₹10L+
Monthly Revenue: ₹30K+ (3% platform fee)

At this scale:
  - Upgrade to Supabase Pro (₹800/mo) for 8GB database
  - Upgrade to Railway Pro (₹1,600/mo) for 2GB RAM
  - Keep other services on free tier

Total Cost: ₹2,400/mo
Revenue: ₹30,000/mo
Profit Margin: 92% (₹27,600/mo profit)
```

---

## 17. Comparison: Bootstrap vs Premium

| Metric                 | Bootstrap Stack        | Premium Stack                    |
| ---------------------- | ---------------------- | -------------------------------- |
| **Initial Setup Cost** | ₹1,000 (domain + DLT)  | ₹1,00,000 (AWS setup + licenses) |
| **Monthly Fixed Cost** | ₹0-500                 | ₹50,000-70,000                   |
| **Break-even GMV**     | ₹10,000/mo             | ₹50,00,000/mo                    |
| **Time to Market**     | 2 weeks                | 8 weeks                          |
| **Risk**               | Low (pay as you grow)  | High (upfront investment)        |
| **Scalability**        | Manual upgrades needed | Auto-scales                      |
| **Best For**           | MVP, small startups    | Funded startups, enterprises     |

---

## 18. Final Recommendation

**For Bootstrap Launch:**

1. **Start with 100% free tier** (₹0/mo)
2. **Use Razorpay test mode** until first paying customer
3. **Use Digio free trial** (100 signatures)
4. **Deploy on Railway free tier** (₹0/mo)
5. **Use Mumbai testnet** for blockchain (₹0/mo)

**Upgrade Sequence (Revenue-Driven):**

```
₹0 → ₹10K revenue/mo:
  ✅ Stay on free tier

₹10K → ₹50K revenue/mo:
  ⬆️ Upgrade database to Supabase Pro (₹800/mo)
  ⬆️ Upgrade hosting to Railway Pro (₹1,600/mo)

₹50K → ₹2L revenue/mo:
  ⬆️ Migrate to AWS (₹15,000/mo)
  ⬆️ Get ASP license for direct eSign (saves ₹5,000/mo)

₹2L+ revenue/mo:
  ⬆️ Premium tier everything
  ⬆️ Hire DevOps engineer
```

**Expected Timeline:**

- **Month 1-3:** 100% free tier (₹0/mo cost)
- **Month 4-6:** Selective upgrades (₹2,500/mo cost)
- **Month 7-12:** Full production stack (₹15,000/mo cost)
- **Year 2+:** Enterprise infrastructure (₹50,000+/mo cost)

---

**Total Savings in Year 1: ₹6,00,000+**

This allows you to validate product-market fit before committing to expensive infrastructure.

---

**Document Version:** 1.0  
**Last Updated:** 27 January 2026  
**Next Review:** After first 100 paying customers
