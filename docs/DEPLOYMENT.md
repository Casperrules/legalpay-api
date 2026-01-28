# LegalPay - Complete Production Deployment Guide

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Infrastructure Setup](#infrastructure-setup)
3. [Database Setup (PostgreSQL)](#database-setup-postgresql)
4. [Backend Deployment](#backend-deployment)
5. [Frontend Deployment](#frontend-deployment)
6. [Payment Gateway Setup (Razorpay)](#payment-gateway-setup-razorpay)
7. [Blockchain Setup (Optional)](#blockchain-setup-optional)
8. [Email Configuration](#email-configuration)
9. [SSL/HTTPS Setup](#sslhttps-setup)
10. [Monitoring & Logging](#monitoring--logging)
11. [Backup & Disaster Recovery](#backup--disaster-recovery)
12. [Post-Deployment Testing](#post-deployment-testing)
13. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Accounts & Services

- ✅ **GitHub Account** - For code repository
- ✅ **Razorpay Account** - Payment gateway ([Sign up](https://dashboard.razorpay.com/signup))
- ✅ **Domain Name** - Your custom domain (e.g., legalpay.in)
- ✅ **Cloud Provider** - Choose one:
  - **Option A:** Railway + Vercel (Easiest, Recommended)
  - **Option B:** AWS EC2 + S3 (Full control)
  - **Option C:** DigitalOcean Droplet + Cloudflare (Budget-friendly)
  - **Option D:** Docker + Any VPS (Advanced)

### Software Requirements

- Java 21+
- Node.js 18+
- PostgreSQL 14+
- Maven 3.8+
- Git
- Docker (optional)
- OpenSSL (for SSL certificates)

### Estimated Costs (Monthly)

**Option A: Railway + Vercel (Recommended for MVP)**
- Railway (Backend + Database): $5-20/month
- Vercel (Frontend): Free tier (sufficient)
- Razorpay: Transaction-based (2% + ₹3 per transaction)
- **Total: $5-20/month (~₹400-1600)**

**Option B: AWS**
- EC2 t3.small: $15/month
- RDS PostgreSQL db.t3.micro: $15/month
- S3 + CloudFront: $5/month
- **Total: $35/month (~₹2900)**

**Option C: DigitalOcean**
- Droplet (2GB RAM): $12/month
- Managed PostgreSQL: $15/month
- Spaces + CDN: $5/month
- **Total: $32/month (~₹2650)**

---

## Infrastructure Setup

### Option A: Railway + Vercel (Recommended for Beginners)

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

#### Step 2: Deploy to Railway

```bash
# Initialize Railway project
railway init

# Set environment variables
railway variables set SPRING_PROFILES_ACTIVE=prod
railway variables set DATABASE_URL=<your-postgres-url>
railway variables set RAZORPAY_KEY_ID=<your-key>
railway variables set RAZORPAY_KEY_SECRET=<your-secret>
railway variables set JWT_SECRET=$(openssl rand -base64 32)

# Deploy
railway up

# Get deployment URL
railway domain
```

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

#### Step 1: Prepare Frontend

```bash
cd frontend

# Update .env.production
cat > .env.production << 'EOF'
VITE_API_URL=https://api.yourdomain.com
EOF

# Test build locally
npm run build
```

#### Step 2: Deploy to Vercel

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

### Detailed setup in: [Blockchain_Integration_Guide.md](docs/Blockchain_Integration_Guide.md)

### Quick Production Setup

#### Step 1: Buy MATIC

```bash
# Buy from Indian exchange:
# - WazirX: https://wazirx.com
# - CoinDCX: https://coindcx.com

# Amount needed: 10-20 MATIC (~₹500-1000 for first month)
```

#### Step 2: Deploy Contract to Polygon Mainnet

```bash
# Using Remix (https://remix.ethereum.org):
1. Switch MetaMask to Polygon Mainnet
2. Copy contract from contracts/AuditTrail.sol
3. Compile (Solidity 0.8.20)
4. Deploy (costs ~0.01 MATIC)
5. Verify on PolygonScan
6. Copy contract address
```

#### Step 3: Configure Backend

```bash
# Add to .env:
BLOCKCHAIN_ENABLED=true
BLOCKCHAIN_NETWORK=polygon-mainnet
BLOCKCHAIN_RPC_URL=https://polygon-rpc.com
BLOCKCHAIN_PRIVATE_KEY=your_metamask_private_key
BLOCKCHAIN_CONTRACT_ADDRESS=deployed_contract_address
BLOCKCHAIN_GAS_PRICE=50000000000
BLOCKCHAIN_GAS_LIMIT=300000
BLOCKCHAIN_CONFIRMATION_BLOCKS=10
```

**OR Disable Blockchain:**
```bash
BLOCKCHAIN_ENABLED=false
```

---

## Email Configuration

### Option A: Gmail (Development/Small Scale)

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
