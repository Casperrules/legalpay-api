# LegalPay Deployment Guide - Bootstrap (Minimal Cost)

**Target**: Launch production-ready app at near-zero cost for initial 6-12 months
**Audience**: Solo founder / small team with limited budget
**Expected Cost**: ₹0 - ₹500/month for first 1000 users

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Free Tier Strategy](#free-tier-strategy)
3. [Backend Deployment](#backend-deployment)
4. [Frontend Deployment](#frontend-deployment)
5. [Database Setup](#database-setup)
6. [Cost Breakdown](#cost-breakdown)
7. [Scaling Path](#scaling-path)

---

## Architecture Overview

### Current Stack

- **Backend**: Spring Boot 3.2.1 (Java 21)
- **Frontend**: React + Vite + TypeScript
- **Database**: H2 (in-memory) → PostgreSQL (production)
- **Auth**: JWT with Spring Security

### Production Architecture (Bootstrap)

```
┌─────────────┐
│   Vercel    │ ← Frontend (React) - FREE
└─────────────┘
       ↓
┌─────────────┐
│  Railway    │ ← Backend (Spring Boot) - FREE tier
└─────────────┘
       ↓
┌─────────────┐
│  Neon DB    │ ← PostgreSQL - FREE tier
└─────────────┘
```

---

## Free Tier Strategy

### Option 1: Fully Free (Recommended for MVP)

| Service        | Purpose          | Free Limits                      | Cost After   |
| -------------- | ---------------- | -------------------------------- | ------------ |
| **Vercel**     | Frontend hosting | Unlimited bandwidth, 100GB/month | $20/month    |
| **Railway**    | Backend hosting  | 500 hrs/month, 512MB RAM         | $5/month     |
| **Neon**       | PostgreSQL DB    | 10GB storage, 0.5GB RAM          | $19/month    |
| **Cloudflare** | CDN + DNS        | Unlimited bandwidth              | Free forever |

**Total**: ₹0/month for 6-12 months

### Option 2: Hybrid (Better Performance)

| Service      | Purpose           | Cost     | Limits              |
| ------------ | ----------------- | -------- | ------------------- |
| **Vercel**   | Frontend          | Free     | Same as above       |
| **Render**   | Backend           | $7/month | 512MB RAM, 0.1 CPU  |
| **Supabase** | PostgreSQL + Auth | Free     | 500MB DB, 50K users |

**Total**: ₹580/month (~$7)

---

## Backend Deployment

### Option A: Railway (FREE - Recommended)

#### Prerequisites

1. GitHub account
2. Railway account (sign up with GitHub)

#### Step 1: Prepare Application

```bash
# 1. Update application.yml for production
cd /Volumes/Mac_backup\ 1/LegalPayApp/legalpay-api/src/main/resources

# Create application-prod.yml (already exists, verify settings)
```

**Verify `application-prod.yml`:**

```yaml
spring:
  datasource:
    url: ${DATABASE_URL} # Railway injects this
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate # Use Flyway in production
    show-sql: false

server:
  port: ${PORT:8080} # Railway sets PORT dynamically

jwt:
  secret: ${JWT_SECRET} # Set in Railway environment
  expiration: 36000000

logging:
  level:
    root: INFO
    com.legalpay: INFO
```

#### Step 2: Create Dockerfile

```bash
cd /Volumes/Mac_backup\ 1/LegalPayApp
```

Create `Dockerfile`:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY legalpay-api/target/legalpay-api-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx400m", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

#### Step 3: Deploy to Railway

1. **Push to GitHub**:

```bash
git init
git add .
git commit -m "Initial production setup"
git remote add origin <your-repo-url>
git push -u origin main
```

2. **Deploy via Railway Dashboard**:
   - Go to https://railway.app
   - Click "New Project" → "Deploy from GitHub repo"
   - Select your LegalPay repository
   - Railway auto-detects Spring Boot
   - Click "Deploy"

3. **Add PostgreSQL Database**:
   - In Railway project, click "New" → "Database" → "Add PostgreSQL"
   - Railway auto-injects `DATABASE_URL`

4. **Set Environment Variables**:

```bash
JWT_SECRET=your-super-secret-256-bit-key-here-change-this
SPRING_PROFILES_ACTIVE=prod
```

5. **Get Public URL**:
   - Railway provides: `https://<your-app>.up.railway.app`
   - Copy this URL for frontend configuration

**Build Command**: `mvn clean package -DskipTests`
**Start Command**: `java -jar legalpay-api/target/legalpay-api-1.0.0-SNAPSHOT.jar`

---

### Option B: Render (Paid - Better Uptime)

Similar steps, but:

- Free tier has cold starts (app sleeps after 15 min inactivity)
- Paid tier ($7/month) = always-on + better performance

---

## Frontend Deployment

### Vercel (Recommended - FREE)

#### Step 1: Prepare Frontend

```bash
cd /Volumes/Mac_backup\ 1/LegalPayApp/frontend

# Create .env.production
cat > .env.production << EOF
VITE_API_URL=https://<your-railway-app>.up.railway.app
EOF
```

#### Step 2: Update `package.json` Build Script

```json
{
  "scripts": {
    "build": "vite build",
    "preview": "vite preview"
  }
}
```

#### Step 3: Deploy to Vercel

**Option 1: Vercel CLI (Fastest)**

```bash
# Install Vercel CLI
npm i -g vercel

# Login
vercel login

# Deploy
cd frontend
vercel --prod
```

**Option 2: Vercel Dashboard**

1. Go to https://vercel.com
2. Import GitHub repository
3. Set root directory: `frontend`
4. Build command: `npm run build`
5. Output directory: `dist`
6. Environment variables:
   - `VITE_API_URL` = `https://<railway-backend-url>`
7. Click "Deploy"

**Result**: Your app will be live at `https://<your-app>.vercel.app`

#### Custom Domain (Optional - FREE with Vercel)

1. Buy domain from Hostinger (₹99/year) or use Freenom (free)
2. Add to Vercel: Settings → Domains
3. Update DNS records

---

## Database Setup

### Option A: Neon (Recommended - FREE)

1. **Sign Up**: https://neon.tech
2. **Create Project**: "LegalPay Production"
3. **Get Connection String**:

```
postgres://user:pass@ep-example.us-east-2.aws.neon.tech/legalpay?sslmode=require
```

4. **Add to Railway Environment**:

```bash
DATABASE_URL=<neon-connection-string>
```

**Free Tier Limits**:

- 10GB storage (enough for 50,000+ contracts)
- 0.5GB RAM
- Always-on compute

### Option B: Railway PostgreSQL (Included)

Railway bundles PostgreSQL - easier but limited:

- 100MB storage on free tier
- Good for MVP testing, not production

### Option C: Supabase (FREE + Auth Bonus)

If you want built-in authentication:

1. Sign up: https://supabase.com
2. Create project
3. Get connection string
4. Bonus: Use Supabase Auth instead of JWT (future enhancement)

---

## Database Migration (H2 → PostgreSQL)

### Step 1: Add Flyway Migration

```bash
cd /Volumes/Mac_backup\ 1/LegalPayApp/legalpay-domain
mkdir -p src/main/resources/db/migration
```

Create `V1__Initial_Schema.sql`:

```sql
-- Merchants
CREATE TABLE merchants (
    id UUID PRIMARY KEY,
    business_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING_KYC',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Payers
CREATE TABLE payers (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Contracts
CREATE TABLE contracts (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    payer_id UUID NOT NULL REFERENCES payers(id),
    principal_amount DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    emi_amount DECIMAL(15,2),
    start_date DATE,
    end_date DATE NOT NULL,
    payment_type VARCHAR(20) DEFAULT 'EMI',
    payment_frequency VARCHAR(20),
    status VARCHAR(50) DEFAULT 'DRAFT',
    pdf_url VARCHAR(500),
    esign_document_id VARCHAR(255),
    blockchain_hash VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes
CREATE INDEX idx_contracts_merchant ON contracts(merchant_id);
CREATE INDEX idx_contracts_payer ON contracts(payer_id);
CREATE INDEX idx_contracts_status ON contracts(status);
```

### Step 2: Update `pom.xml` (legalpay-domain)

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Step 3: Update `application-prod.yml`

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
```

---

## Cost Breakdown

### Year 1 Projection (Bootstrap Mode)

| Service               | Months 1-6 | Months 7-12    | Notes                                |
| --------------------- | ---------- | -------------- | ------------------------------------ |
| **Frontend (Vercel)** | ₹0         | ₹0             | Free forever for personal projects   |
| **Backend (Railway)** | ₹0         | ₹415/month     | Free tier → Hobby ($5) after 500 hrs |
| **Database (Neon)**   | ₹0         | ₹0             | 10GB free tier                       |
| **Domain**            | ₹99/year   | -              | Optional: legalpay.in                |
| **SSL**               | ₹0         | ₹0             | Free with Vercel/Railway             |
| **TOTAL**             | **₹99**    | **₹415/month** | ~₹3,000/year                         |

### Cost at 1000 Users

Assuming:

- 500 contracts/month
- 2000 API calls/day
- 50MB DB growth/month

**Estimated**: ₹500-800/month (still within free tiers)

---

## Production Checklist

### Security

- [ ] Change JWT secret to 256-bit random key
- [ ] Enable HTTPS only (handled by Vercel/Railway)
- [ ] Set CORS allowed origins to frontend domain only
- [ ] Add rate limiting (use Railway's built-in or add Spring rate limiter)
- [ ] Enable SQL injection protection (already handled by JPA)
- [ ] Remove H2 console endpoint

### Monitoring

- [ ] Set up Railway/Render logs
- [ ] Add Sentry for error tracking (free tier: 5000 events/month)
- [ ] Set up UptimeRobot (free: 50 monitors, 5-min checks)
- [ ] Add Google Analytics to frontend

### Performance

- [ ] Enable gzip compression (Vercel does this automatically)
- [ ] Add database indexes on frequently queried fields
- [ ] Implement API response caching (Redis free tier: Upstash)
- [ ] Optimize images with Cloudinary free tier (25GB/month)

### Compliance (India)

- [ ] Add Privacy Policy page
- [ ] Add Terms of Service
- [ ] Cookie consent banner (GDPR/DPDPA compliance)
- [ ] Email verification flow
- [ ] Data export API (DPDPA right to access)

---

## Deployment Commands

### Full Production Build

```bash
# Backend
cd /Volumes/Mac_backup\ 1/LegalPayApp
mvn clean package -DskipTests -Pprod

# Frontend
cd frontend
npm run build

# Test production build locally
npm run preview
```

### Environment Variables Checklist

**Backend (Railway/Render)**:

```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=<postgres-connection-string>
JWT_SECRET=<256-bit-secret>
PORT=8080
```

**Frontend (Vercel)**:

```bash
VITE_API_URL=https://<backend-domain>
```

---

## Scaling Path

### When to Upgrade (Signs)

1. **Railway Free Tier Exhausted** (500 hrs/month):
   - Upgrade to Hobby ($5/month) for always-on

2. **Database > 10GB**:
   - Upgrade Neon to Scale ($19/month) for 100GB

3. **Backend RAM > 512MB**:
   - Railway Pro ($20/month) for 2GB RAM

4. **Traffic > 100K requests/month**:
   - Add Redis cache (Upstash free tier)
   - Consider AWS/GCP for better pricing at scale

### Long-term Architecture (₹5000-10000/month)

When you hit 5000+ users:

```
Cloudflare (CDN) → Vercel (Frontend)
                ↓
         Load Balancer
                ↓
        2x Backend Instances (Railway/Render)
                ↓
        PostgreSQL (Neon Scale)
                ↓
        Redis Cache (Upstash)
```

---

## Alternative: Single VPS Approach

### Cheapest Option: Hetzner/Contabo VPS

**Cost**: €2.99/month (₹270/month)

**Specs**:

- 1 vCPU
- 2GB RAM
- 20GB SSD
- 20TB traffic

**Setup**:

1. Install Docker + Docker Compose
2. Run everything in containers:
   - Nginx (reverse proxy)
   - Spring Boot backend
   - React frontend (static files)
   - PostgreSQL
   - Certbot (SSL)

**Downside**: You manage everything (updates, security, backups)

---

## Backup Strategy (Essential!)

### Automated Daily Backups

**Neon**: Built-in point-in-time restore (7 days on free tier)

**Manual Backup Script** (run daily via GitHub Actions):

```bash
#!/bin/bash
# backup-db.sh
pg_dump $DATABASE_URL > backup-$(date +%Y%m%d).sql
# Upload to Google Drive or S3-compatible storage (Wasabi: $6/TB)
```

**GitHub Actions Cron**:

```yaml
name: Daily Backup
on:
  schedule:
    - cron: "0 2 * * *" # 2 AM UTC daily
jobs:
  backup:
    runs-on: ubuntu-latest
    steps:
      - name: Backup Database
        run: pg_dump ${{ secrets.DATABASE_URL }} > backup.sql
      - name: Upload to Storage
        # Add your upload logic
```

---

## Support & Resources

### Free Monitoring Tools

- **UptimeRobot**: https://uptimerobot.com (50 monitors free)
- **Sentry**: https://sentry.io (5000 errors/month free)
- **LogRocket**: https://logrocket.com (1000 sessions/month free)

### Indian Payment Gateway Integration (Next Phase)

- **Razorpay**: No setup fee, 2% per transaction
- **Cashfree**: Similar pricing
- **PayU**: Negotiable rates for startups

### Community & Help

- **Railway Discord**: https://discord.gg/railway
- **Neon Community**: https://discord.gg/neon
- **Spring Boot India**: LinkedIn groups

---

## Estimated Timeline

| Task                   | Time        | Complexity |
| ---------------------- | ----------- | ---------- |
| PostgreSQL migration   | 2 hours     | Low        |
| Railway backend deploy | 1 hour      | Low        |
| Vercel frontend deploy | 30 mins     | Very Low   |
| Environment config     | 1 hour      | Low        |
| Testing & fixes        | 4 hours     | Medium     |
| Domain setup           | 30 mins     | Low        |
| **TOTAL**              | **9 hours** | -          |

**Recommendation**: Deploy in one weekend sprint.

---

## Quick Start Commands

```bash
# 1. Prepare backend
cd /Volumes/Mac_backup\ 1/LegalPayApp
mvn clean package -DskipTests

# 2. Create Dockerfile
cat > Dockerfile << 'EOF'
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY legalpay-api/target/legalpay-api-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx400m", "-jar", "app.jar"]
EOF

# 3. Deploy backend to Railway
# (Use Railway dashboard or CLI)

# 4. Deploy frontend to Vercel
cd frontend
npm i -g vercel
vercel --prod

# 5. Done! 🎉
```

---

## Conclusion

With this bootstrap deployment strategy:

- ✅ **₹0-500/month** for first year
- ✅ Production-ready infrastructure
- ✅ Handles 1000+ users comfortably
- ✅ Easy to scale when revenue comes in
- ✅ Professional deployment (not localhost!)

**Next Steps**: See [Bootstrap_Strategy_Free_Minimal_Cost.md](./Bootstrap_Strategy_Free_Minimal_Cost.md) for go-to-market strategy.

---

**Last Updated**: January 2026
**Maintainer**: LegalPay Engineering Team
