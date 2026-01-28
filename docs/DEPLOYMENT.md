# LegalPay - Complete Production Deployment Guide

## Quick Start Checklist

**For Railway + Vercel deployment (recommended):**

- [ ] **1. Push code to GitHub**
- [ ] **2. Deploy Backend on Railway**
  - [ ] Import GitHub repo
  - [ ] Add PostgreSQL database
  - [ ] Configure 15+ environment variables (see [Backend Environment Variables](#backend-environment-variables))
- [ ] **3. Deploy Frontend on Vercel**
  - [ ] Import GitHub repo
  - [ ] Set root directory to `frontend`
  - [ ] Add `VITE_API_URL` environment variable
- [ ] **4. Setup Services**
  - [ ] Razorpay account + KYC
  - [ ] Resend email account + API key
  - [ ] (Optional) Polygon blockchain wallet
- [ ] **5. Test end-to-end** (user registration → email → login → contract creation)

**Estimated time:** 2-3 hours

---

## Backend Environment Variables

**Complete list for Railway:**

```bash
# Core
SPRING_PROFILES_ACTIVE=prod
PORT=8080
DATABASE_URL=(auto-set by Railway PostgreSQL)
JWT_SECRET=(generate with: openssl rand -base64 32)
FRONTEND_URL=https://your-app.vercel.app

# Payment Gateway (Razorpay)
RAZORPAY_KEY_ID=rzp_live_xxxxx
RAZORPAY_KEY_SECRET=your_secret
RAZORPAY_WEBHOOK_SECRET=whsec_xxxxx

# Email Service (Resend)
RESEND_ENABLED=true
RESEND_API_KEY=re_xxxxx
EMAIL_FROM=noreply@yourdomain.com

# Blockchain (Optional - can disable)
BLOCKCHAIN_ENABLED=false
BLOCKCHAIN_NETWORK=polygon-mainnet
BLOCKCHAIN_RPC_URL=https://polygon-rpc.com
BLOCKCHAIN_PRIVATE_KEY=0xYOUR_PRIVATE_KEY
BLOCKCHAIN_CONTRACT_ADDRESS=0xDEPLOYED_ADDRESS
BLOCKCHAIN_GAS_PRICE=50000000000
BLOCKCHAIN_GAS_LIMIT=300000
BLOCKCHAIN_CONFIRMATION_BLOCKS=10
```

**Copy-paste template for Railway (MVP - Blockchain disabled):**

```bash
SPRING_PROFILES_ACTIVE=prod
PORT=8080
JWT_SECRET=REPLACE_WITH_GENERATED_SECRET
FRONTEND_URL=https://your-app.vercel.app
RAZORPAY_KEY_ID=rzp_test_xxxxx
RAZORPAY_KEY_SECRET=test_secret
RAZORPAY_WEBHOOK_SECRET=whsec_test
RESEND_ENABLED=true
RESEND_API_KEY=re_xxxxx
EMAIL_FROM=noreply@yourdomain.com
BLOCKCHAIN_ENABLED=false
```

## Frontend Environment Variables

**For Vercel:**

```bash
VITE_API_URL=https://your-railway-app.railway.app
```

**How to get Railway backend URL:**

- Railway Dashboard → Your service → Settings → Domains
- Copy the generated URL (e.g., `legalpay-api-production.up.railway.app`)

---

## Table of Contents

1. [Quick Start Checklist](#quick-start-checklist)
2. [Backend Environment Variables](#backend-environment-variables)
3. [Frontend Environment Variables](#frontend-environment-variables)
4. [Prerequisites](#prerequisites)
5. [Infrastructure Setup](#infrastructure-setup)
6. [Database Setup (PostgreSQL)](#database-setup-postgresql)
7. [Backend Deployment](#backend-deployment)
8. [Frontend Deployment](#frontend-deployment)
9. [Payment Gateway Setup (Razorpay)](#payment-gateway-setup-razorpay)
10. [Blockchain Setup (Optional)](#blockchain-setup-optional)
11. [Email Configuration](#email-configuration)
12. [SSL/HTTPS Setup](#sslhttps-setup)
13. [Monitoring & Logging](#monitoring--logging)
14. [Backup & Disaster Recovery](#backup--disaster-recovery)
15. [Post-Deployment Testing](#post-deployment-testing)
16. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Accounts & Services

- ✅ **GitHub Account** - For code repository
- ✅ **Railway Account** - Backend hosting ([Sign up](https://railway.app))
- ✅ **Vercel Account** - Frontend hosting ([Sign up](https://vercel.com))
- ✅ **Razorpay Account** - Payment gateway ([Sign up](https://dashboard.razorpay.com/signup))
- ✅ **Resend Account** - Email service ([Sign up](https://resend.com/signup))
- ✅ **Domain Name** (Optional) - Your custom domain (e.g., legalpay.in)

### Software Requirements (Local Development)

- Java 21+
- Node.js 18+
- Maven 3.8+
- Git
- OpenSSL (for generating secrets)

### Estimated Costs (Monthly)

**Railway + Vercel (Recommended for MVP)**

- Railway (Backend + Database): $5-20/month
- Vercel (Frontend): Free tier (sufficient)
- Resend (Email): Free (3,000 emails/month)
- Razorpay: Transaction-based (2% + ₹3 per transaction)
- Blockchain (optional): ₹250-500/month (1000 transactions)
- **Total: $5-20/month (~₹400-1600)**

**AWS (Enterprise)**

- EC2 t3.small: $15/month
- RDS PostgreSQL db.t3.micro: $15/month
- S3 + CloudFront: $5/month
- **Total: $35/month (~₹2900)**

---

## Infrastructure Setup

### Option A: Railway + Vercel (Recommended for MVP)

**Best for:** Quick deployment, auto-scaling, zero DevOps

#### Step 1: Setup Railway Account

```bash
# Install Railway CLI
npm install -g @railway/cli

# Login
railway login

# Link to GitHub
railway link
```

#### Step 2: Setup Vercel Account

```bash
# Install Vercel CLI
npm install -g vercel

# Login
vercel login
```

### Option B: AWS Setup

**Best for:** Enterprise, full control, compliance requirements

#### Step 1: Create AWS Account

1. Go to https://aws.amazon.com
2. Sign up (requires credit card)
3. Verify email and phone

#### Step 2: Setup IAM User

```bash
# Install AWS CLI
curl "https://awscli.amazonaws.com/AWSCLIV2.pkg" -o "AWSCLIV2.pkg"
sudo installer -pkg AWSCLIV2.pkg -target /

# Configure
aws configure
# Enter: Access Key ID, Secret Access Key, Region (ap-south-1), Format (json)
```

### Option C: DigitalOcean Setup

**Best for:** Budget-conscious, simple infrastructure

```bash
# Install doctl
brew install doctl

# Authenticate
doctl auth init

# Enter your API token from DigitalOcean dashboard
```

---

## Database Setup (PostgreSQL)

### Option A: Railway PostgreSQL (Easiest)

```bash
# Create new PostgreSQL database in Railway dashboard
railway add --database postgresql

# Get connection string
railway variables
# Copy DATABASE_URL
```

### Option B: AWS RDS PostgreSQL

1. **Create RDS Instance:**
   - Go to RDS Console
   - Click "Create database"
   - Engine: PostgreSQL 14
   - Template: Free tier (for testing) or Production
   - DB instance: db.t3.micro
   - Master username: `legalpay_admin`
   - Master password: (generate strong password)
   - VPC: Default
   - Public access: Yes (for initial setup)
   - Database name: `legalpay_prod`

2. **Configure Security Group:**
   - Inbound rules → Edit
   - Add rule: PostgreSQL (5432), Source: Your IP
   - Add rule: PostgreSQL (5432), Source: EC2 Security Group

3. **Get Connection Details:**

```bash
# Endpoint: your-db.xxxxx.ap-south-1.rds.amazonaws.com
# Port: 5432
# Database: legalpay_prod
# Username: legalpay_admin
# Password: <your-password>

# Connection string:
DATABASE_URL=jdbc:postgresql://your-db.xxxxx.ap-south-1.rds.amazonaws.com:5432/legalpay_prod
DB_USERNAME=legalpay_admin
DB_PASSWORD=your-password
```

### Option C: DigitalOcean Managed PostgreSQL

```bash
# Create database cluster
doctl databases create legalpay-db --engine pg --region blr1 --size db-s-1vcpu-1gb

# Get connection details
doctl databases connection legalpay-db

# Enable connection pooling
doctl databases pool create legalpay-db legalpay_pool --db legalpay --size 10
```

### Database Schema Migration

```bash
# Connect to database
psql "postgresql://user:password@host:5432/database"

# Create user
CREATE USER legalpay_app WITH PASSWORD 'strong_password_here';

# Create database
CREATE DATABASE legalpay_prod;

# Grant privileges
GRANT ALL PRIVILEGES ON DATABASE legalpay_prod TO legalpay_app;

# Run migrations (Flyway will auto-create tables on first run)
# Just start the application with correct DATABASE_URL
```

---

## Backend Deployment

### Option A: Railway Deployment

#### Step 1: Prepare Repository

```bash
cd legalpay-api

# Create Procfile
cat > Procfile << 'EOF'
web: java -Dserver.port=$PORT -jar target/legalpay-api-1.0.0-SNAPSHOT.jar
EOF

# Create railway.json
cat > railway.json << 'EOF'
{
  "build": {
    "builder": "nixpacks",
    "buildCommand": "mvn clean install -DskipTests"
  },
  "deploy": {
    "startCommand": "java -Dserver.port=$PORT -Dspring.profiles.active=prod -jar target/legalpay-api-1.0.0-SNAPSHOT.jar",
    "healthcheckPath": "/actuator/health",
    "healthcheckTimeout": 300,
    "restartPolicyType": "on-failure",
    "restartPolicyMaxRetries": 10
  }
}
EOF
```

#### Step 2: Configure Environment Variables on Railway

**Method A: Using Railway CLI**

```bash
# Initialize Railway project
railway init

# Core Configuration
railway variables set SPRING_PROFILES_ACTIVE=prod
railway variables set PORT=8080

# Database (auto-provided if you added PostgreSQL via Railway)
# DATABASE_URL is automatically set by Railway PostgreSQL service

# JWT Security
railway variables set JWT_SECRET=$(openssl rand -base64 32)

# Frontend URL (your Vercel deployment URL)
railway variables set FRONTEND_URL=https://your-app.vercel.app

# Payment Gateway (Razorpay)
railway variables set RAZORPAY_KEY_ID=rzp_live_xxxxxxxxxxxxx
railway variables set RAZORPAY_KEY_SECRET=your_razorpay_secret_here
railway variables set RAZORPAY_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxx

# Email Service (Resend) - Get API key from https://resend.com
railway variables set RESEND_ENABLED=true
railway variables set RESEND_API_KEY=re_xxxxxxxxxxxxx
railway variables set EMAIL_FROM=noreply@yourdomain.com

# Blockchain (Optional - Polygon)
railway variables set BLOCKCHAIN_ENABLED=true
railway variables set BLOCKCHAIN_NETWORK=polygon-mainnet
railway variables set BLOCKCHAIN_RPC_URL=https://polygon-rpc.com
railway variables set BLOCKCHAIN_PRIVATE_KEY=0xYOUR_WALLET_PRIVATE_KEY
railway variables set BLOCKCHAIN_CONTRACT_ADDRESS=0xDEPLOYED_CONTRACT_ADDRESS
railway variables set BLOCKCHAIN_GAS_PRICE=50000000000
railway variables set BLOCKCHAIN_GAS_LIMIT=300000
railway variables set BLOCKCHAIN_CONFIRMATION_BLOCKS=10

# Deploy
railway up

# Get deployment URL
railway domain
```

**Method B: Using Railway Dashboard** (Recommended - Easier)

1. Go to https://railway.app/dashboard
2. Select your project → **Variables** tab
3. Click **+ New Variable** and add each of these:

| Variable Name                    | Example Value                 | Description                          |
| -------------------------------- | ----------------------------- | ------------------------------------ |
| `SPRING_PROFILES_ACTIVE`         | `prod`                        | Spring Boot profile                  |
| `PORT`                           | `8080`                        | Server port                          |
| `DATABASE_URL`                   | (Auto-set by Railway)         | PostgreSQL connection string         |
| `JWT_SECRET`                     | `generated-random-string`     | Use `openssl rand -base64 32`        |
| `FRONTEND_URL`                   | `https://legalpay.vercel.app` | Your Vercel frontend URL             |
| **Payment Gateway**              |                               |                                      |
| `RAZORPAY_KEY_ID`                | `rzp_live_xxxx`               | From Razorpay dashboard              |
| `RAZORPAY_KEY_SECRET`            | `secret_here`                 | From Razorpay dashboard              |
| `RAZORPAY_WEBHOOK_SECRET`        | `whsec_xxxx`                  | From Razorpay webhooks               |
| **Email Service**                |                               |                                      |
| `RESEND_ENABLED`                 | `true`                        | Enable production emails             |
| `RESEND_API_KEY`                 | `re_xxxx`                     | Get from https://resend.com/api-keys |
| `EMAIL_FROM`                     | `noreply@yourdomain.com`      | Sender email address                 |
| **Blockchain (Optional)**        |                               |                                      |
| `BLOCKCHAIN_ENABLED`             | `true` or `false`             | Enable audit trail on Polygon        |
| `BLOCKCHAIN_NETWORK`             | `polygon-mainnet`             | Use Mumbai for testing               |
| `BLOCKCHAIN_RPC_URL`             | `https://polygon-rpc.com`     | Polygon RPC endpoint                 |
| `BLOCKCHAIN_PRIVATE_KEY`         | `0xYOUR_PRIVATE_KEY`          | Wallet private key (keep secure!)    |
| `BLOCKCHAIN_CONTRACT_ADDRESS`    | `0xCONTRACT_ADDR`             | Deployed smart contract address      |
| `BLOCKCHAIN_GAS_PRICE`           | `50000000000`                 | 50 Gwei (adjust based on network)    |
| `BLOCKCHAIN_GAS_LIMIT`           | `300000`                      | Gas limit per transaction            |
| `BLOCKCHAIN_CONFIRMATION_BLOCKS` | `10`                          | Confirmations before marking success |

4. Click **Deploy** to apply changes

**Quick Copy-Paste Template for Railway Dashboard:**

```bash
# Generate JWT secret first
openssl rand -base64 32

# Then paste these in Railway (replace values):
SPRING_PROFILES_ACTIVE=prod
PORT=8080
JWT_SECRET=<generated-above>
FRONTEND_URL=https://your-app.vercel.app
RAZORPAY_KEY_ID=rzp_live_xxxxx
RAZORPAY_KEY_SECRET=your_secret
RAZORPAY_WEBHOOK_SECRET=whsec_xxxxx
RESEND_ENABLED=true
RESEND_API_KEY=re_xxxxx
EMAIL_FROM=noreply@yourdomain.com
BLOCKCHAIN_ENABLED=false
```

**Note:** `DATABASE_URL` is automatically injected by Railway when you add PostgreSQL. Don't manually set it.

#### Step 3: Configure Custom Domain

```bash
# Add custom domain
railway domain add api.yourdomain.com

# Add CNAME record in your DNS:
# Type: CNAME
# Name: api
# Value: <railway-domain>.railway.app
```

### Option B: AWS EC2 Deployment

#### Step 1: Create EC2 Instance

```bash
# Create key pair
aws ec2 create-key-pair --key-name legalpay-prod --query 'KeyMaterial' --output text > legalpay-prod.pem
chmod 400 legalpay-prod.pem

# Launch instance
aws ec2 run-instances \
  --image-id ami-0dee22c13ea7a9a67 \
  --instance-type t3.small \
  --key-name legalpay-prod \
  --security-group-ids sg-xxxxxxxx \
  --subnet-id subnet-xxxxxxxx \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=legalpay-backend}]'

# Get instance IP
aws ec2 describe-instances --filters "Name=tag:Name,Values=legalpay-backend" --query 'Reservations[0].Instances[0].PublicIpAddress'
```

#### Step 2: Install Dependencies on EC2

```bash
# SSH into instance
ssh -i legalpay-prod.pem ec2-user@<instance-ip>

# Update system
sudo yum update -y

# Install Java 21
sudo amazon-linux-extras enable corretto21
sudo yum install -y java-21-amazon-corretto

# Install Maven
sudo yum install -y maven

# Install Git
sudo yum install -y git

# Install PostgreSQL client
sudo yum install -y postgresql15

# Create application user
sudo useradd -r -s /bin/false legalpay
```

#### Step 3: Deploy Application

```bash
# Clone repository
sudo mkdir -p /opt/legalpay
sudo chown legalpay:legalpay /opt/legalpay
sudo -u legalpay git clone https://github.com/yourusername/legalpay.git /opt/legalpay

# Build application
cd /opt/legalpay
sudo -u legalpay mvn clean install -DskipTests

# Create .env file
sudo -u legalpay cat > /opt/legalpay/.env << 'EOF'
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://your-rds-endpoint:5432/legalpay_prod
DB_USERNAME=legalpay_admin
DB_PASSWORD=your-password
RAZORPAY_KEY_ID=rzp_live_XXXXXX
RAZORPAY_KEY_SECRET=YYYYYY
JWT_SECRET=<generated-secret>
FRONTEND_URL=https://yourdomain.com
BACKEND_URL=https://api.yourdomain.com
EOF

# Create systemd service
sudo cat > /etc/systemd/system/legalpay.service << 'EOF'
[Unit]
Description=LegalPay Backend API
After=network.target

[Service]
Type=simple
User=legalpay
WorkingDirectory=/opt/legalpay/legalpay-api
EnvironmentFile=/opt/legalpay/.env
ExecStart=/usr/bin/java -Xmx512m -Xms256m -Dspring.profiles.active=prod -jar /opt/legalpay/legalpay-api/target/legalpay-api-1.0.0-SNAPSHOT.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Start service
sudo systemctl daemon-reload
sudo systemctl enable legalpay
sudo systemctl start legalpay

# Check status
sudo systemctl status legalpay

# View logs
sudo journalctl -u legalpay -f
```

#### Step 4: Setup Nginx Reverse Proxy

```bash
# Install Nginx
sudo amazon-linux-extras install nginx1 -y

# Configure Nginx
sudo cat > /etc/nginx/conf.d/legalpay.conf << 'EOF'
server {
    listen 80;
    server_name api.yourdomain.com;

    client_max_body_size 10M;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /actuator/health {
        proxy_pass http://localhost:8080/actuator/health;
        access_log off;
    }
}
EOF

# Start Nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

### Option C: Docker Deployment

#### Step 1: Create Dockerfile

```bash
cd legalpay-api

cat > Dockerfile << 'EOF'
FROM maven:3.9.6-amazoncorretto-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY legalpay-domain/pom.xml ./legalpay-domain/
COPY legalpay-services/pom.xml ./legalpay-services/
COPY legalpay-api/pom.xml ./legalpay-api/
RUN mvn dependency:go-offline

COPY legalpay-domain ./legalpay-domain
COPY legalpay-services ./legalpay-services
COPY legalpay-api ./legalpay-api
RUN mvn clean install -DskipTests

FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build /app/legalpay-api/target/legalpay-api-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
EOF
```

#### Step 2: Create docker-compose.yml

```bash
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  postgres:
    image: postgres:14-alpine
    environment:
      POSTGRES_DB: legalpay_prod
      POSTGRES_USER: legalpay
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U legalpay"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build: .
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: jdbc:postgresql://postgres:5432/legalpay_prod
      DB_USERNAME: legalpay
      DB_PASSWORD: ${DB_PASSWORD}
      RAZORPAY_KEY_ID: ${RAZORPAY_KEY_ID}
      RAZORPAY_KEY_SECRET: ${RAZORPAY_KEY_SECRET}
      JWT_SECRET: ${JWT_SECRET}
      FRONTEND_URL: ${FRONTEND_URL}
      BACKEND_URL: ${BACKEND_URL}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    restart: unless-stopped

volumes:
  postgres_data:
EOF
```

#### Step 3: Deploy with Docker

```bash
# Build and start
docker-compose up -d

# View logs
docker-compose logs -f backend

# Stop
docker-compose down

# Update deployment
git pull
docker-compose build backend
docker-compose up -d backend
```

---

## Frontend Deployment

### Option A: Vercel (Recommended)

**Best for:** Automatic deployments, global CDN, zero configuration

#### Step 1: Prepare Frontend

```bash
cd frontend

# Update .env.production
cat > .env.production << 'EOF'
VITE_API_URL=https://your-railway-app.railway.app
EOF

# Test build locally
npm run build
```

#### Step 2: Deploy via Vercel Dashboard (Easiest Method)

1. **Push code to GitHub** (if not already done)

```bash
cd /path/to/LegalPayApp
git add .
git commit -m "Ready for deployment"
git push origin main
```

2. **Import Project on Vercel**
   - Visit https://vercel.com/new
   - Click **Import Git Repository**
   - Select your GitHub repository
   - Authorize Vercel to access GitHub

3. **Configure Build Settings**

| Setting          | Value                    |
| ---------------- | ------------------------ |
| Framework Preset | **Vite** (auto-detected) |
| Root Directory   | `frontend`               |
| Build Command    | `npm run build`          |
| Output Directory | `dist`                   |
| Install Command  | `npm install`            |

4. **Add Environment Variables**

Click **Environment Variables** section → **Add**:

```
Name:  VITE_API_URL
Value: https://your-railway-app.railway.app
Environment: Production ✓
```

**How to get Railway backend URL:**

```bash
# Option 1: Railway CLI
railway status
# Look for: Deployment URL

# Option 2: Railway Dashboard
# Go to your backend service → Settings → Domains
# Copy the Railway-provided URL (e.g., legalpay-api-production.up.railway.app)
```

**Complete example:**

```
VITE_API_URL=https://legalpay-api-production.up.railway.app
```

5. **Deploy**
   - Click **Deploy** button
   - Wait 2-3 minutes for build
   - You'll get a URL like: `https://legalpay-xyz123.vercel.app`

#### Step 3: Update Railway Backend with Vercel URL

**Critical:** Railway backend needs to know the frontend URL for CORS and email links.

**Method A: Railway Dashboard**

1. Go to https://railway.app/dashboard
2. Select your backend service
3. Go to **Variables** tab
4. Find `FRONTEND_URL` variable (or add new)
5. Update value to: `https://legalpay-xyz123.vercel.app`
6. Click **Deploy** to apply

**Method B: Railway CLI**

```bash
railway variables set FRONTEND_URL=https://legalpay-xyz123.vercel.app
```

**Why this matters:**

- ✅ Enables CORS for frontend API calls
- ✅ Email verification links point to correct domain
- ✅ Password reset redirects work
- ✅ Payment callback URLs configured correctly

#### Step 4: Configure Custom Domain (Optional but Recommended)

**In Vercel Dashboard:**

1. Go to your project → **Settings** → **Domains**
2. Click **Add Domain**
3. Enter your domain:
   - Root domain: `yourdomain.com`
   - Or subdomain: `app.yourdomain.com`

4. **Add DNS Records** (in your domain registrar like GoDaddy/Namecheap/Cloudflare):

**For root domain (`yourdomain.com`):**

```bash
Type: A
Name: @
Value: 76.76.21.21

Type: AAAA
Name: @
Value: 2606:4700:3033::ac43:bd43
```

**For subdomain (`app.yourdomain.com`):**

```bash
Type: CNAME
Name: app
Value: cname.vercel-dns.com
```

**For both root and www:**

```bash
# Root domain
Type: A
Name: @
Value: 76.76.21.21

# www subdomain
Type: CNAME
Name: www
Value: cname.vercel-dns.com
```

5. **Wait for DNS propagation** (5-30 minutes)
6. Vercel automatically provisions **SSL certificate** ✅

7. **Update Railway backend again:**

```bash
railway variables set FRONTEND_URL=https://app.yourdomain.com
```

#### Step 5: Verify Deployment

```bash
# Test frontend loads
curl -I https://legalpay-xyz123.vercel.app
# Expected: HTTP/2 200

# Test with custom domain (if configured)
curl -I https://app.yourdomain.com
# Expected: HTTP/2 200

# Test API connectivity
# Open browser: https://legalpay-xyz123.vercel.app
# Open Console (F12) → Network tab
# Try to login - should see API calls to Railway backend
```

**Troubleshooting Frontend-Backend Connection:**

If frontend can't reach backend:

1. Check Railway `FRONTEND_URL` matches Vercel URL exactly
2. Check Vercel `VITE_API_URL` matches Railway backend URL exactly
3. Verify Railway backend is running: `curl https://your-railway-app.railway.app/actuator/health`
4. Check browser console for CORS errors

---

#### Alternative: Deploy via Vercel CLI

```bash
# Login
vercel login

# Deploy
vercel --prod

# Or via Vercel Dashboard:
# 1. Go to https://vercel.com
# 2. Import Git Repository
# 3. Select 'frontend' folder as root
# 4. Add environment variable: VITE_API_URL=https://api.yourdomain.com
# 5. Deploy
```

#### Step 3: Configure Custom Domain

```bash
# In Vercel dashboard:
# Settings → Domains → Add Domain
# Add: yourdomain.com

# Update DNS:
# Type: A
# Name: @
# Value: 76.76.21.21

# Type: CNAME
# Name: www
# Value: cname.vercel-dns.com
```

### Option B: Netlify

```bash
# Install Netlify CLI
npm install -g netlify-cli

# Login
netlify login

# Deploy
cd frontend
netlify deploy --prod

# Set environment variables in Netlify dashboard
# Site settings → Build & deploy → Environment → Edit variables
# Add: VITE_API_URL=https://api.yourdomain.com
```

### Option C: AWS S3 + CloudFront

#### Step 1: Build Frontend

```bash
cd frontend
npm run build
# Creates 'dist' folder
```

#### Step 2: Create S3 Bucket

```bash
# Create bucket
aws s3 mb s3://yourdomain.com --region ap-south-1

# Enable static website hosting
aws s3 website s3://yourdomain.com --index-document index.html --error-document index.html

# Upload build
aws s3 sync dist/ s3://yourdomain.com --delete

# Set bucket policy
aws s3api put-bucket-policy --bucket yourdomain.com --policy '{
  "Version": "2012-10-17",
  "Statement": [{
    "Sid": "PublicReadGetObject",
    "Effect": "Allow",
    "Principal": "*",
    "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::yourdomain.com/*"
  }]
}'
```

#### Step 3: Create CloudFront Distribution

```bash
# Create distribution
aws cloudfront create-distribution --distribution-config file://cloudfront-config.json

# cloudfront-config.json:
{
  "Origins": {
    "Items": [{
      "Id": "S3-yourdomain.com",
      "DomainName": "yourdomain.com.s3.amazonaws.com",
      "S3OriginConfig": {
        "OriginAccessIdentity": ""
      }
    }]
  },
  "DefaultRootObject": "index.html",
  "Comment": "LegalPay Frontend",
  "Enabled": true
}

# Get CloudFront domain
# Create Route53 record pointing to CloudFront distribution
```

---

## Payment Gateway Setup (Razorpay)

### Step 1: Complete Razorpay KYC

1. **Sign up:** https://dashboard.razorpay.com/signup
2. **Complete KYC:**
   - Company details
   - PAN card
   - Bank account details
   - GST certificate (if registered)
   - Business proof (incorporation certificate)

3. **Verification:** 24-48 hours

### Step 2: Get Live API Keys

```bash
# After KYC approval:
# Dashboard → Settings → API Keys → Generate Live Key

# Copy:
RAZORPAY_KEY_ID=rzp_live_XXXXXXXXXXXXXX
RAZORPAY_KEY_SECRET=YYYYYYYYYYYYYYYY
```

### Step 3: Configure Webhooks

```bash
# Dashboard → Settings → Webhooks → Add New Webhook

# Webhook URL:
https://api.yourdomain.com/api/v1/webhooks/razorpay

# Active Events:
# ✅ payment.captured
# ✅ payment.failed
# ✅ payment.authorized
# ✅ refund.created
# ✅ refund.processed

# Secret:
RAZORPAY_WEBHOOK_SECRET=whsec_ZZZZZZZZZZZZZZZ
```

### Step 4: Test Live Integration

```bash
# Use live API keys in .env
RAZORPAY_KEY_ID=rzp_live_XXXXXX
RAZORPAY_KEY_SECRET=YYYYYY

# Make small test payment (₹10)
# Verify in Razorpay dashboard → Transactions
```

---

## Blockchain Setup (Optional)

LegalPay uses **Polygon blockchain** for immutable audit trails of contracts, payments, and legal notices.

**When to use:**

- ✅ Compliance requires audit trail
- ✅ Dispute resolution needs proof
- ✅ Multi-party contract verification

**When to skip:**

- For MVP testing
- Cost sensitivity (₹0.05-0.20 per transaction)
- Simple use cases

### Quick Decision Guide

```bash
# Disable blockchain (recommended for MVP)
BLOCKCHAIN_ENABLED=false

# Enable blockchain (production with compliance)
BLOCKCHAIN_ENABLED=true
```

### Full Setup Guide

**See detailed guide:** [docs/Blockchain_Integration_Guide.md](./Blockchain_Integration_Guide.md)

### Production Deployment (5 Steps)

#### Step 1: Get MATIC Tokens

**Buy from Indian Exchange:**

- **WazirX:** https://wazirx.com
- **CoinDCX:** https://coindcx.com
- **ZebPay:** https://zebpay.com

**Amount needed:**

- Initial: 10-20 MATIC (~₹500-1000)
- Monthly (1000 txs): ~5-10 MATIC (~₹250-500)

**Transfer to MetaMask:**

1. Install MetaMask: https://metamask.io
2. Create wallet → Save recovery phrase securely
3. Add Polygon network
4. Copy wallet address
5. Withdraw MATIC from exchange to MetaMask

#### Step 2: Deploy Smart Contract to Polygon Mainnet

**Using Remix IDE:**

1. Go to https://remix.ethereum.org
2. Create new file: `AuditTrail.sol`
3. Copy contract from [contracts/AuditTrail.sol](../contracts/AuditTrail.sol)
4. **Compile:**
   - Compiler: `0.8.20`
   - Click **Compile AuditTrail.sol**
5. **Deploy:**
   - Environment: **Injected Provider - MetaMask**
   - Switch MetaMask to **Polygon Mainnet**
   - Click **Deploy** (costs ~0.01 MATIC, ~₹0.50)
6. **Copy deployed contract address** (starts with `0x...`)
7. **Verify on PolygonScan** (optional):
   - Go to https://polygonscan.com
   - Search contract address
   - Click **Verify and Publish**

**Example contract address:** `0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb`

#### Step 3: Export Private Key from MetaMask

⚠️ **Security Warning:** Never share or commit private key!

1. Open MetaMask
2. Click three dots → **Account Details**
3. Click **Show Private Key**
4. Enter password
5. Copy private key (starts with `0x...`)

#### Step 4: Configure Railway Environment Variables

**In Railway Dashboard:**

Go to your backend service → **Variables** → Add these:

| Variable                         | Value                     | Description                  |
| -------------------------------- | ------------------------- | ---------------------------- |
| `BLOCKCHAIN_ENABLED`             | `true`                    | Enable blockchain logging    |
| `BLOCKCHAIN_NETWORK`             | `polygon-mainnet`         | Use Mumbai for testing       |
| `BLOCKCHAIN_RPC_URL`             | `https://polygon-rpc.com` | Free public RPC              |
| `BLOCKCHAIN_PRIVATE_KEY`         | `0xYOUR_PRIVATE_KEY`      | From MetaMask (keep secure!) |
| `BLOCKCHAIN_CONTRACT_ADDRESS`    | `0xDEPLOYED_ADDRESS`      | From Remix deployment        |
| `BLOCKCHAIN_GAS_PRICE`           | `50000000000`             | 50 Gwei (adjust if needed)   |
| `BLOCKCHAIN_GAS_LIMIT`           | `300000`                  | Gas limit per transaction    |
| `BLOCKCHAIN_CONFIRMATION_BLOCKS` | `10`                      | Wait for 10 blocks (~30 sec) |

**Or via Railway CLI:**

```bash
railway variables set BLOCKCHAIN_ENABLED=true
railway variables set BLOCKCHAIN_NETWORK=polygon-mainnet
railway variables set BLOCKCHAIN_RPC_URL=https://polygon-rpc.com
railway variables set BLOCKCHAIN_PRIVATE_KEY=0xYOUR_PRIVATE_KEY
railway variables set BLOCKCHAIN_CONTRACT_ADDRESS=0xDEPLOYED_ADDRESS
railway variables set BLOCKCHAIN_GAS_PRICE=50000000000
railway variables set BLOCKCHAIN_GAS_LIMIT=300000
railway variables set BLOCKCHAIN_CONFIRMATION_BLOCKS=10
```

#### Step 5: Verify Blockchain Integration

**Test transaction:**

```bash
# Create a contract via API
curl -X POST https://your-railway-app.railway.app/api/v1/contracts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "payerEmail": "test@example.com",
    "principalAmount": 100000,
    "title": "Blockchain Test Contract"
  }'

# Check Railway logs
railway logs | grep "Blockchain"

# Should see:
# "Blockchain transaction submitted: 0x1234..."
# "Transaction confirmed in block: 12345678"
```

**Verify on PolygonScan:**

1. Go to https://polygonscan.com
2. Search your contract address
3. Click **Events** tab
4. See `AuditEntryCreated` events

### Cost Analysis

| Activity              | MATIC Cost   | INR Cost (₹) | Frequency    |
| --------------------- | ------------ | ------------ | ------------ |
| Deploy contract       | ~0.01        | ~₹0.50       | One-time     |
| Contract created      | ~0.001-0.005 | ₹0.05-0.25   | Per contract |
| Payment logged        | ~0.001-0.005 | ₹0.05-0.25   | Per payment  |
| Monthly (1000 events) | ~5-10        | ₹250-500     | Monthly      |

**vs Ethereum:** 100x cheaper (Ethereum costs ₹50+ per transaction)

### Alternative RPC Providers

If `polygon-rpc.com` is slow:

```bash
# Alchemy (Free tier: 300M compute units/month)
BLOCKCHAIN_RPC_URL=https://polygon-mainnet.g.alchemy.com/v2/YOUR_API_KEY

# Infura (Free tier: 100k requests/day)
BLOCKCHAIN_RPC_URL=https://polygon-mainnet.infura.io/v3/YOUR_PROJECT_ID

# QuickNode (Paid: $9/month)
BLOCKCHAIN_RPC_URL=https://YOUR-ENDPOINT.matic.quiknode.pro/YOUR_KEY/
```

### Security Best Practices

✅ **DO:**

- Store private key in Railway environment variables only
- Use dedicated wallet for blockchain (not personal wallet)
- Keep minimal MATIC balance (10-20 MATIC)
- Monitor gas prices and adjust
- Enable 2FA on Railway account

❌ **DON'T:**

- Commit private key to Git
- Share private key in Slack/email
- Use personal wallet with large funds
- Hardcode contract address in code

### Monitoring & Maintenance

**Check wallet balance:**

```bash
# Visit https://polygonscan.com/address/YOUR_WALLET_ADDRESS
# Or in MetaMask: Switch to Polygon network
```

**Refill MATIC when low:**

- Buy more from exchange
- Transfer to same wallet address
- No configuration changes needed

**View all blockchain logs:**

```bash
# In Railway logs
railway logs | grep "Blockchain"

# Or query PostgreSQL
SELECT * FROM blockchain_audit_log
WHERE status = 'CONFIRMED'
ORDER BY created_at DESC
LIMIT 10;
```

### Disable Blockchain

If you want to turn off blockchain temporarily:

**In Railway:**

```bash
railway variables set BLOCKCHAIN_ENABLED=false
```

Or in Railway Dashboard:

- Variables → Find `BLOCKCHAIN_ENABLED`
- Change value to `false`
- Click Deploy

All blockchain logs will be skipped. No errors thrown.

---

---

## Email Configuration

LegalPay uses **Resend** for transactional emails (verification, password reset, welcome messages).

### Step 1: Sign Up for Resend

1. Go to https://resend.com/signup
2. Verify your email
3. Choose plan:
   - **Free tier:** 3,000 emails/month, 100 emails/day
   - **Pro:** $20/month for 50,000 emails

### Step 2: Get API Key

1. Go to https://resend.com/api-keys
2. Click **Create API Key**
3. Name: `LegalPay Production`
4. Permissions: **Full Access** (or **Sending access** minimum)
5. Copy the API key (starts with `re_...`)
   - ⚠️ **Save it immediately** - shown only once!

### Step 3: Configure Email Domain (Optional but Recommended)

**Option A: Use Resend's Test Domain** (Quick Start)

- From email: `onboarding@resend.dev`
- ✅ Works immediately
- ❌ May go to spam
- ✅ Good for testing

**Option B: Custom Domain** (Production Recommended)

1. **Add Domain in Resend Dashboard**
   - Go to https://resend.com/domains
   - Click **Add Domain**
   - Enter: `yourdomain.com`

2. **Add DNS Records** (in your domain registrar):

```bash
# SPF Record
Type: TXT
Name: @
Value: v=spf1 include:resend.com ~all

# DKIM Record (Resend will provide the exact value)
Type: TXT
Name: resend._domainkey
Value: <DKIM-value-from-resend-dashboard>

# DMARC Record
Type: TXT
Name: _dmarc
Value: v=DMARC1; p=none; rua=mailto:dmarc@yourdomain.com
```

3. **Verify Domain**
   - Wait 5-30 minutes for DNS propagation
   - Resend will auto-verify
   - Status changes to ✅ **Verified**

### Step 4: Configure Environment Variables

**In Railway Dashboard** (Backend):

| Variable         | Value                    | Description          |
| ---------------- | ------------------------ | -------------------- |
| `RESEND_ENABLED` | `true`                   | Enable email sending |
| `RESEND_API_KEY` | `re_xxxxxxxxxxxxx`       | API key from Resend  |
| `EMAIL_FROM`     | `noreply@yourdomain.com` | Sender email address |

**Example values:**

```bash
# With custom domain (recommended)
RESEND_ENABLED=true
RESEND_API_KEY=re_abc123def456
EMAIL_FROM=noreply@legalpay.in

# Or with Resend test domain (for testing)
RESEND_ENABLED=true
RESEND_API_KEY=re_abc123def456
EMAIL_FROM=onboarding@resend.dev
```

**In Railway CLI:**

```bash
railway variables set RESEND_ENABLED=true
railway variables set RESEND_API_KEY=re_abc123def456
railway variables set EMAIL_FROM=noreply@yourdomain.com
```

### Step 5: Test Email Sending

**Local Testing** (Development):

```bash
# In .env.local
RESEND_ENABLED=false  # Emails log to console instead

# Start backend and register
# Check backend logs for email content and verification URLs
```

**Production Testing:**

1. Deploy backend with `RESEND_ENABLED=true`
2. Register a test user via frontend
3. Check email inbox (including spam folder)
4. Verify email links work correctly

**Test API directly:**

```bash
curl -X POST https://your-railway-app.railway.app/api/v1/auth/register/merchant \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@yourdomain.com",
    "password": "Test@1234",
    "businessName": "Test Company",
    "phoneNumber": "+919876543210"
  }'

# Check test@yourdomain.com inbox for verification email
```

### Step 6: Monitor Email Delivery

**Resend Dashboard:**

1. Go to https://resend.com/emails
2. See all sent emails with delivery status
3. Check bounce rates and spam reports

**Email deliverability checklist:**

- ✅ Use custom domain (not resend.dev)
- ✅ Add all DNS records (SPF, DKIM, DMARC)
- ✅ Verify domain shows green checkmark
- ✅ Test with https://www.mail-tester.com
- ✅ Monitor bounce rates < 2%

### Cost Estimates

| Volume               | Cost      |
| -------------------- | --------- |
| 0-3,000 emails/month | **Free**  |
| 3,001-50,000 emails  | $20/month |
| 50,001-1M emails     | $80/month |

**For LegalPay MVP:** Free tier (3,000/month) supports ~1,000 users registering monthly.

### Troubleshooting

**Emails not sending:**

```bash
# Check Railway logs
railway logs

# Verify environment variables set
railway variables | grep RESEND

# Check Resend dashboard for errors
# https://resend.com/emails → Filter by "Failed"
```

**Emails going to spam:**

- ✅ Use custom domain
- ✅ Verify all DNS records (SPF, DKIM, DMARC)
- ✅ Warm up domain (start with low volume)
- ✅ Include unsubscribe link (good practice)
- ✅ Avoid spam trigger words

---

### Alternative Email Providers

If Resend doesn't work for your region:

#### Option B: Gmail (Development Only)

```bash
# Enable 2FA on Gmail account
# Create App Password:
# Google Account → Security → 2-Step Verification → App passwords

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=app-password-here
MAIL_FROM=noreply@yourdomain.com
```

### Option B: SendGrid (Recommended for Production)

```bash
# Sign up: https://sendgrid.com
# Create API key: Settings → API Keys → Create API Key

MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=your-sendgrid-api-key
MAIL_FROM=noreply@yourdomain.com
```

### Option C: AWS SES (Cost-effective)

```bash
# Setup AWS SES
aws ses verify-email-identity --email-address noreply@yourdomain.com

# Get SMTP credentials from SES console

MAIL_HOST=email-smtp.ap-south-1.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=your-smtp-username
MAIL_PASSWORD=your-smtp-password
MAIL_FROM=noreply@yourdomain.com
```

---

## SSL/HTTPS Setup

### Option A: Automatic (Vercel/Railway/Netlify)

✅ SSL certificates are automatically provisioned and renewed

### Option B: Let's Encrypt (AWS EC2/DigitalOcean)

```bash
# Install Certbot
sudo yum install -y certbot python3-certbot-nginx

# Generate certificate
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com -d api.yourdomain.com

# Test auto-renewal
sudo certbot renew --dry-run

# Auto-renewal cron job (already added by certbot)
# Certificates renew automatically every 60 days
```

### Option C: AWS Certificate Manager (CloudFront/ALB)

```bash
# Request certificate
aws acm request-certificate \
  --domain-name yourdomain.com \
  --subject-alternative-names www.yourdomain.com api.yourdomain.com \
  --validation-method DNS \
  --region us-east-1

# Add CNAME records for validation
# Wait for validation (5-30 minutes)
# Attach to CloudFront/ALB
```

---

## Monitoring & Logging

### Backend Monitoring

#### Setup Application Insights

```bash
# Add to application-prod.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

#### Setup CloudWatch (AWS)

```bash
# Install CloudWatch agent on EC2
sudo yum install -y amazon-cloudwatch-agent

# Configure
sudo cat > /opt/aws/amazon-cloudwatch-agent/etc/config.json << 'EOF'
{
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [{
          "file_path": "/opt/legalpay/logs/application.log",
          "log_group_name": "/legalpay/application",
          "log_stream_name": "{instance_id}"
        }]
      }
    }
  }
}
EOF

# Start agent
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config \
  -m ec2 \
  -s \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/config.json
```

#### Setup Uptime Monitoring

```bash
# Use UptimeRobot (free tier):
# 1. Sign up at https://uptimerobot.com
# 2. Add monitors:
#    - https://api.yourdomain.com/actuator/health
#    - https://yourdomain.com
# 3. Configure alerts to your email
```

### Log Management

```bash
# View backend logs (Railway)
railway logs

# View backend logs (EC2)
sudo journalctl -u legalpay -f

# View backend logs (Docker)
docker-compose logs -f backend

# View Nginx logs
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

---

## Backup & Disaster Recovery

### Database Backups

#### Railway PostgreSQL

```bash
# Automatic daily backups (retained for 7 days)
# Manual backup:
railway run pg_dump > backup-$(date +%Y%m%d).sql
```

#### AWS RDS

```bash
# Enable automated backups (RDS Console):
# Backup retention: 7 days
# Backup window: 03:00-04:00 UTC

# Create manual snapshot
aws rds create-db-snapshot \
  --db-instance-identifier legalpay-prod \
  --db-snapshot-identifier legalpay-manual-$(date +%Y%m%d)

# Restore from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier legalpay-restored \
  --db-snapshot-identifier legalpay-manual-20260128
```

#### Manual Backup Script

```bash
#!/bin/bash
# backup.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups"
DB_HOST="your-db-host"
DB_NAME="legalpay_prod"
DB_USER="legalpay_admin"

# Create backup
pg_dump -h $DB_HOST -U $DB_USER -d $DB_NAME | gzip > $BACKUP_DIR/legalpay_$DATE.sql.gz

# Upload to S3
aws s3 cp $BACKUP_DIR/legalpay_$DATE.sql.gz s3://legalpay-backups/

# Delete local backups older than 7 days
find $BACKUP_DIR -name "legalpay_*.sql.gz" -mtime +7 -delete

# Crontab: Daily at 2 AM
# 0 2 * * * /opt/legalpay/backup.sh
```

### Application Backups

```bash
# Backup environment configs
cp .env /backups/env-$(date +%Y%m%d).bak

# Backup uploaded files (if any)
tar -czf /backups/uploads-$(date +%Y%m%d).tar.gz /opt/legalpay/uploads

# Backup to S3
aws s3 sync /backups s3://legalpay-backups/
```

---

## Post-Deployment Testing

### Health Checks

```bash
# Backend health
curl https://api.yourdomain.com/actuator/health

# Expected response:
# {"status":"UP"}

# Frontend
curl -I https://yourdomain.com

# Expected: HTTP/2 200
```

### End-to-End Testing

#### 1. User Registration

```bash
# Register merchant
curl -X POST https://api.yourdomain.com/api/v1/auth/register/merchant \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test-merchant@example.com",
    "password": "Test@1234",
    "companyName": "Test Law Firm",
    "firstName": "John",
    "lastName": "Doe"
  }'

# Check email for verification link
```

#### 2. Login

```bash
curl -X POST https://api.yourdomain.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test-merchant@example.com",
    "password": "Test@1234"
  }'

# Save token
```

#### 3. Create Contract

```bash
curl -X POST https://api.yourdomain.com/api/v1/contracts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "payerEmail": "payer@example.com",
    "principalAmount": 100000,
    "interestRate": 12,
    "title": "Legal Services Agreement",
    "startDate": "2026-02-01",
    "endDate": "2026-08-01",
    "paymentType": "ONE_TIME"
  }'
```

#### 4. Test Payment Flow

1. Login as payer
2. View contract
3. Click "Pay Now"
4. Use test card: 4111 1111 1111 1111
5. Verify payment success
6. Check Razorpay dashboard

#### 5. Verify Blockchain Logging (if enabled)

```bash
# Check database
psql $DATABASE_URL -c "SELECT * FROM blockchain_audit_logs ORDER BY created_at DESC LIMIT 5;"

# Check PolygonScan
# https://polygonscan.com/address/YOUR_CONTRACT_ADDRESS
```

### Performance Testing

```bash
# Install Apache Bench
brew install apache-bench

# Test API performance
ab -n 1000 -c 10 https://api.yourdomain.com/actuator/health

# Expected: <500ms average response time
```

### Security Testing

```bash
# SSL check
curl https://www.ssllabs.com/ssltest/analyze.html?d=yourdomain.com

# Security headers
curl -I https://yourdomain.com

# Should include:
# X-Content-Type-Options: nosniff
# X-Frame-Options: DENY
# Strict-Transport-Security: max-age=31536000
```

---

## Troubleshooting

### Backend Won't Start

**Issue:** Application fails to start

```bash
# Check logs
railway logs  # Railway
sudo journalctl -u legalpay -f  # EC2
docker-compose logs backend  # Docker

# Common issues:
# 1. Database connection failed
#    - Verify DATABASE_URL
#    - Check network/firewall rules
#    - Test connection: psql $DATABASE_URL

# 2. Missing environment variables
#    - Check all required vars in .env
#    - Verify RAZORPAY_KEY_SECRET not empty

# 3. Port already in use
#    - Check: lsof -i :8080
#    - Kill process: kill -9 <PID>
```

### Database Connection Issues

```bash
# Test connection
psql "postgresql://user:pass@host:5432/db"

# Check firewall
telnet your-db-host 5432

# Check RDS security group
aws ec2 describe-security-groups --group-ids sg-xxxxx

# Allow EC2 instance
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 5432 \
  --source-group sg-yyyyy
```

### Payment Integration Issues

**Issue:** Razorpay checkout not opening

```bash
# Check browser console for errors
# Verify Razorpay script loaded:
# https://checkout.razorpay.com/v1/checkout.js

# Test API keys
curl -u rzp_live_XXX:YYY https://api.razorpay.com/v1/payments

# Check webhook delivery
# Dashboard → Settings → Webhooks → View logs
```

**Issue:** Payment signature verification failing

```bash
# Verify RAZORPAY_KEY_SECRET matches dashboard
# Check backend logs for signature mismatch
# Ensure using correct key pair (live with live, test with test)
```

### SSL Certificate Issues

```bash
# Check certificate
openssl s_client -connect yourdomain.com:443

# Renew Let's Encrypt certificate
sudo certbot renew

# Force renewal
sudo certbot renew --force-renewal
```

### High Memory Usage

```bash
# Check Java memory
# Reduce Xmx in systemd service or Dockerfile

# Current setting: -Xmx512m
# For 1GB RAM server, use: -Xmx768m

# Restart service
sudo systemctl restart legalpay
```

### Slow Database Queries

```bash
# Enable query logging
# application-prod.yml:
logging:
  level:
    org.hibernate.SQL: DEBUG

# Check slow queries
psql $DATABASE_URL -c "
  SELECT query, mean_exec_time, calls
  FROM pg_stat_statements
  ORDER BY mean_exec_time DESC
  LIMIT 10;
"

# Add indexes
CREATE INDEX idx_contract_merchant ON contracts(merchant_id);
CREATE INDEX idx_payment_status ON razorpay_payments(status);
```

---

## Rollback Procedure

### Rollback Backend (Railway)

```bash
# List deployments
railway deployments

# Rollback to previous
railway rollback <deployment-id>
```

### Rollback Backend (EC2)

```bash
# Keep previous JAR
sudo cp /opt/legalpay/legalpay-api/target/legalpay-api-1.0.0-SNAPSHOT.jar \
       /opt/legalpay/legalpay-api-previous.jar

# Rollback
sudo systemctl stop legalpay
sudo cp /opt/legalpay/legalpay-api-previous.jar \
       /opt/legalpay/legalpay-api/target/legalpay-api-1.0.0-SNAPSHOT.jar
sudo systemctl start legalpay
```

### Rollback Database

```bash
# Restore from backup
psql $DATABASE_URL < backup-20260128.sql

# Or AWS RDS snapshot restore (see Backup section)
```

---

## Production Checklist

Before going live:

- [ ] **Database:**
  - [ ] PostgreSQL production instance running
  - [ ] Automated backups enabled
  - [ ] Connection pooling configured

- [ ] **Backend:**
  - [ ] Deployed to production environment
  - [ ] HTTPS/SSL enabled
  - [ ] Health checks passing
  - [ ] Logs collecting properly
  - [ ] Auto-restart on failure configured

- [ ] **Frontend:**
  - [ ] Deployed to CDN
  - [ ] Custom domain configured
  - [ ] HTTPS enabled
  - [ ] CORS configured correctly

- [ ] **Payment Gateway:**
  - [ ] Razorpay KYC completed
  - [ ] Live API keys configured
  - [ ] Webhooks configured and tested
  - [ ] Test payment successful

- [ ] **Blockchain (Optional):**
  - [ ] Smart contract deployed to Polygon Mainnet
  - [ ] Wallet funded with MATIC
  - [ ] Test transaction confirmed

- [ ] **Email:**
  - [ ] SMTP configured
  - [ ] Test email sent successfully
  - [ ] SPF/DKIM records added to DNS

- [ ] **Security:**
  - [ ] SSL certificates installed
  - [ ] Environment secrets secured
  - [ ] CORS properly configured
  - [ ] Rate limiting enabled
  - [ ] Security headers configured

- [ ] **Monitoring:**
  - [ ] Uptime monitoring setup
  - [ ] Error alerting configured
  - [ ] Log aggregation working
  - [ ] Performance metrics collecting

- [ ] **Testing:**
  - [ ] End-to-end flow tested
  - [ ] Payment flow tested
  - [ ] Email notifications tested
  - [ ] Mobile responsiveness verified

- [ ] **Documentation:**
  - [ ] Runbook created
  - [ ] Environment variables documented
  - [ ] Backup/restore procedures tested
  - [ ] Incident response plan ready

---

## Support & Maintenance

### Regular Maintenance Tasks

**Daily:**

- Check application health
- Monitor error rates
- Review payment transactions

**Weekly:**

- Review logs for errors
- Check database performance
- Monitor disk usage
- Review security alerts

**Monthly:**

- Update dependencies
- Review and rotate logs
- Test backup restoration
- Update SSL certificates (if manual)
- Review and optimize costs

### Getting Help

- **Backend Issues:** Check [legalpay-api logs]
- **Payment Issues:** Razorpay Support (support@razorpay.com)
- **Blockchain Issues:** [Blockchain_Integration_Guide.md](docs/Blockchain_Integration_Guide.md)
- **Infrastructure Issues:** Check cloud provider documentation

---

## Cost Optimization

### Backend Optimization

```bash
# Use smaller instance during low traffic
# EC2: t3.micro instead of t3.small (~50% savings)

# Use reserved instances (1-year commitment)
# Saves ~40% vs on-demand

# Auto-scaling (Railway/AWS)
# Scale down during night hours (2 AM - 8 AM IST)
```

### Database Optimization

```bash
# Use connection pooling
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2

# Use read replicas for reports (future)
# Cache frequently accessed data with Redis
```

### CDN Optimization

```bash
# Cloudflare free tier (alternative to CloudFront)
# - Free SSL
# - Free DDoS protection
# - Free CDN caching
```

---

**Your LegalPay platform is now production-ready!** 🚀

For ongoing support, refer to:

- [QUICK_START.md](QUICK_START.md) - Development setup
- [Payment_Integration_Guide.md](docs/Payment_Integration_Implementation_Guide.md) - Payment details
- [Blockchain_Integration_Guide.md](docs/Blockchain_Integration_Guide.md) - Blockchain details

Backend will start on `http://localhost:8080`

#### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend will start on `http://localhost:3000`

### Production Build

#### Backend

```bash
# Build production JAR
mvn clean package -DskipTests -Pprod

# Run with production profile
java -jar legalpay-api/target/legalpay-api-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

#### Frontend

```bash
cd frontend
npm run build

# Output will be in frontend/dist/
# Deploy dist/ to Vercel, Netlify, or any static host
```

## Payment Integration

### Razorpay Setup

1. **Test Mode** (Development):
   - Use test API keys (starts with `rzp_test_`)
   - Test cards: https://razorpay.com/docs/payments/payments/test-card-details/
   - Success card: `4111 1111 1111 1111`, CVV: any 3 digits

2. **Live Mode** (Production):
   - Complete business KYC on Razorpay Dashboard
   - Activate live mode
   - Use live API keys (starts with `rzp_live_`)
   - Configure webhook URL: `https://api.your-domain.com/api/v1/webhooks/razorpay`

### Webhook Configuration

1. Go to Razorpay Dashboard → Settings → Webhooks
2. Add webhook URL: `https://api.your-domain.com/api/v1/webhooks/razorpay`
3. Select events:
   - ✅ payment.captured
   - ✅ payment.failed
4. Save webhook secret to `.env` as `RAZORPAY_WEBHOOK_SECRET`

### Payment Flow

1. **Contract Created** → Status: DRAFT
2. **eSign Completed** → Status: SIGNED
3. **Payment Button Appears** → Razorpay checkout opens
4. **Payment Success** → Status: ACTIVE, paymentStatus: PAID
5. **Webhook Confirmation** → Backend updates contract

## Database Setup (Production)

### PostgreSQL Setup

```bash
# Install PostgreSQL
# macOS: brew install postgresql@14
# Ubuntu: sudo apt install postgresql-14

# Create database
createdb legalpay

# Create user
psql -c "CREATE USER legalpay WITH PASSWORD 'your_password';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE legalpay TO legalpay;"

# Update .env
DATABASE_URL=jdbc:postgresql://localhost:5432/legalpay
DB_USERNAME=legalpay
DB_PASSWORD=your_password
```

### Run Migrations

```bash
# Tables will be created automatically on first run
# Check ddl-auto setting in application-prod.yml
```

## Environment Variables Reference

### Backend (.env)

| Variable                  | Description              | Required | Default                |
| ------------------------- | ------------------------ | -------- | ---------------------- |
| `RAZORPAY_KEY_ID`         | Razorpay API Key ID      | ✅       | -                      |
| `RAZORPAY_KEY_SECRET`     | Razorpay API Secret      | ✅       | -                      |
| `RAZORPAY_WEBHOOK_SECRET` | Webhook signature secret | ✅       | -                      |
| `FRONTEND_URL`            | Frontend domain          | ✅       | http://localhost:3000  |
| `BACKEND_URL`             | Backend domain           | ✅       | http://localhost:8080  |
| `DATABASE_URL`            | PostgreSQL connection    | Prod     | jdbc:h2:mem:legalpaydb |
| `DB_USERNAME`             | Database username        | Prod     | sa                     |
| `DB_PASSWORD`             | Database password        | Prod     | -                      |
| `JWT_SECRET`              | JWT signing key          | ✅       | -                      |
| `RESEND_API_KEY`          | Email service API key    | Optional | -                      |

### Frontend (.env)

| Variable       | Description     | Required | Default               |
| -------------- | --------------- | -------- | --------------------- |
| `VITE_API_URL` | Backend API URL | ✅       | http://localhost:8080 |

## Testing Payment Integration

### Test Scenario 1: One-Time Payment

```bash
# 1. Start backend and frontend
# 2. Create a contract
# 3. Complete eSign (contract status → SIGNED)
# 4. Click "Pay Now" button
# 5. Use test card: 4111 1111 1111 1111
# 6. Verify payment success page
# 7. Check contract status → ACTIVE, paymentStatus → PAID
```

### Test Scenario 2: Failed Payment

```bash
# Use failure test card: 4000 0000 0000 0002
# Verify error handling and status remains PENDING
```

### Verify Webhook

```bash
# Check backend logs for:
# "Webhook received: event=payment.captured, orderId=..."
# "Payment captured successfully: <paymentId> for contract: <contractId>"
```

## Deployment

### Option 1: Docker (Recommended)

Create `Dockerfile` (backend):

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY legalpay-api/target/legalpay-api-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

Create `docker-compose.yml`:

```yaml
version: "3.8"
services:
  postgres:
    image: postgres:14-alpine
    environment:
      POSTGRES_DB: legalpay
      POSTGRES_USER: legalpay
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/legalpay
      DB_USERNAME: legalpay
      DB_PASSWORD: ${DB_PASSWORD}
      RAZORPAY_KEY_ID: ${RAZORPAY_KEY_ID}
      RAZORPAY_KEY_SECRET: ${RAZORPAY_KEY_SECRET}
      RAZORPAY_WEBHOOK_SECRET: ${RAZORPAY_WEBHOOK_SECRET}
      FRONTEND_URL: ${FRONTEND_URL}
      BACKEND_URL: ${BACKEND_URL}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      - postgres

volumes:
  postgres_data:
```

Deploy:

```bash
docker-compose up -d
```

### Option 2: Vercel (Frontend) + Railway (Backend)

**Frontend (Vercel):**

```bash
cd frontend
npm run build
# Deploy dist/ to Vercel
# Set environment variable: VITE_API_URL=https://your-backend.railway.app
```

**Backend (Railway):**

1. Connect GitHub repo to Railway
2. Select `legalpay-api` as root directory
3. Add environment variables from `.env`
4. Railway will auto-build and deploy

### Option 3: AWS EC2

```bash
# 1. Launch EC2 instance (Ubuntu 22.04)
# 2. Install Java 21
sudo apt update
sudo apt install openjdk-21-jre-headless

# 3. Copy JAR file
scp legalpay-api/target/*.jar ubuntu@<ec2-ip>:~/

# 4. Run as service
sudo nano /etc/systemd/system/legalpay.service
```

Service file:

```ini
[Unit]
Description=LegalPay Backend
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu
ExecStart=/usr/bin/java -jar legalpay-api-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
Environment="RAZORPAY_KEY_ID=rzp_live_XXX"
Environment="RAZORPAY_KEY_SECRET=YYY"
Restart=always

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable legalpay
sudo systemctl start legalpay
```

## Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Payment Metrics

```bash
# Check Razorpay Dashboard for:
# - Payment success rate
# - Failed payment reasons
# - Settlement status
```

### Application Logs

```bash
# Check for payment-related logs:
tail -f logs/spring-boot-logger.log | grep -i payment
```

## Security Checklist

- [ ] HTTPS enabled (Let's Encrypt or CloudFlare)
- [ ] JWT_SECRET is strong random string (64+ characters)
- [ ] Database password is strong
- [ ] Razorpay webhook secret configured
- [ ] CORS allowed origins restricted to your domain
- [ ] Database backups configured
- [ ] Error messages don't expose sensitive data
- [ ] Rate limiting enabled on payment endpoints
- [ ] Firewall configured (only 80, 443, 8080 open)

## Troubleshooting

### Payment Not Processing

```bash
# Check backend logs
tail -f logs/spring-boot-logger.log | grep -i razorpay

# Verify Razorpay credentials
curl http://localhost:8080/api/v1/payments/health

# Check webhook delivery in Razorpay Dashboard
```

### Webhook Not Received

1. Verify webhook URL is publicly accessible
2. Check webhook logs in Razorpay Dashboard
3. Verify `RAZORPAY_WEBHOOK_SECRET` matches
4. Check firewall allows incoming POST to webhook endpoint

### Database Connection Error

```bash
# Test PostgreSQL connection
psql -h localhost -U legalpay -d legalpay

# Check DATABASE_URL format
# Should be: jdbc:postgresql://host:5432/database
```

## Support

- **Documentation**: See `docs/` folder for detailed guides
- **Payment Integration**: `docs/Payment_Integration_Implementation_Guide.md`
- **Security**: `docs/Security_Hardening_Guide.md`
- **Razorpay Support**: https://razorpay.com/support/

## License

Proprietary - LegalPay Technologies Pvt Ltd
