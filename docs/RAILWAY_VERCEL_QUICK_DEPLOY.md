# Railway + Vercel Deployment Quick Reference

## Overview

- **Backend (API):** Railway (https://railway.app)
- **Frontend (UI):** Vercel (https://vercel.com)
- **Database:** PostgreSQL (Railway managed)
- **Estimated Time:** 2-3 hours

---

## Step 1: Push to GitHub

```bash
cd /Volumes/Mac_backup\ 1/LegalPayApp

# Initialize git (if not done)
git init
git add .
git commit -m "Initial commit: LegalPay production ready"

# Add remote (replace with your repo)
git remote add origin https://github.com/YOUR_USERNAME/LegalPayApp.git
git branch -M main
git push -u origin main
```

---

## Step 2: Deploy Backend on Railway

### 2.1 Create Railway Project

1. Go to https://railway.app/new
2. Click **"Deploy from GitHub repo"**
3. Authorize GitHub access
4. Select `LegalPayApp` repository
5. Railway auto-detects Spring Boot project

### 2.2 Add PostgreSQL Database

1. In Railway dashboard → Click **"+ New"**
2. Select **"Database"** → **"Add PostgreSQL"**
3. Railway automatically creates `DATABASE_URL` variable

### 2.3 Configure Environment Variables

**Method A: Railway Dashboard** (Recommended)

1. Click your backend service → **Variables** tab
2. Click **"+ New Variable"**
3. Add each variable below:

```bash
SPRING_PROFILES_ACTIVE=prod
PORT=8080
JWT_SECRET=<Generate below>
FRONTEND_URL=https://your-app.vercel.app
RAZORPAY_KEY_ID=rzp_test_xxxxx
RAZORPAY_KEY_SECRET=your_secret
RAZORPAY_WEBHOOK_SECRET=whsec_xxxxx
RESEND_ENABLED=true
RESEND_API_KEY=re_xxxxx
EMAIL_FROM=noreply@yourdomain.com
BLOCKCHAIN_ENABLED=false
```

**Generate JWT Secret:**

```bash
openssl rand -base64 32
# Copy output and paste as JWT_SECRET value
```

**Method B: Railway CLI**

```bash
npm install -g @railway/cli
railway login
railway link  # Select your project
railway variables set SPRING_PROFILES_ACTIVE=prod
railway variables set PORT=8080
railway variables set JWT_SECRET=$(openssl rand -base64 32)
# ... (add rest)
```

### 2.4 Get Backend URL

1. Railway dashboard → Your service → **Settings** → **Domains**
2. Copy the generated URL (e.g., `legalpay-api-production.up.railway.app`)
3. **Save this URL** - you'll need it for Vercel setup

---

## Step 3: Deploy Frontend on Vercel

### 3.1 Import GitHub Repository

1. Go to https://vercel.com/new
2. Click **"Import Git Repository"**
3. Select `LegalPayApp` from GitHub
4. Click **"Import"**

### 3.2 Configure Build Settings

| Setting              | Value                |
| -------------------- | -------------------- |
| **Framework Preset** | Vite (auto-detected) |
| **Root Directory**   | `frontend`           |
| **Build Command**    | `npm run build`      |
| **Output Directory** | `dist`               |
| **Install Command**  | `npm install`        |

### 3.3 Add Environment Variable

1. Scroll to **"Environment Variables"**
2. Click **"Add"**
3. Enter:

```
Name:  VITE_API_URL
Value: https://legalpay-api-production.up.railway.app
```

**Replace with your actual Railway backend URL from Step 2.4**

### 3.4 Deploy

1. Click **"Deploy"**
2. Wait 2-3 minutes
3. Get your URL: `https://your-project.vercel.app`

### 3.5 Update Railway Backend

**IMPORTANT:** Go back to Railway and update `FRONTEND_URL`:

Railway Dashboard → Backend service → Variables → Edit `FRONTEND_URL`:

```
FRONTEND_URL=https://your-project.vercel.app
```

This enables CORS and email links to work correctly.

---

## Step 4: Setup External Services

### 4.1 Razorpay (Payment Gateway)

1. **Sign up:** https://dashboard.razorpay.com/signup
2. **Get Test API Keys:**
   - Dashboard → Settings → API Keys
   - Click **"Generate Test Key"**
   - Copy `Key ID` and `Key Secret`
3. **Add to Railway:**
   ```
   RAZORPAY_KEY_ID=rzp_test_xxxxx
   RAZORPAY_KEY_SECRET=your_secret
   ```
4. **Setup Webhook:**
   - Dashboard → Settings → Webhooks
   - Add URL: `https://your-railway-app.railway.app/api/webhooks/razorpay`
   - Copy webhook secret
   - Add to Railway: `RAZORPAY_WEBHOOK_SECRET=whsec_xxxxx`

### 4.2 Resend (Email Service)

1. **Sign up:** https://resend.com/signup
2. **Get API Key:**
   - Go to https://resend.com/api-keys
   - Click **"Create API Key"**
   - Name: "LegalPay Production"
   - Copy API key (starts with `re_...`)
3. **Add to Railway:**
   ```
   RESEND_ENABLED=true
   RESEND_API_KEY=re_xxxxx
   EMAIL_FROM=noreply@yourdomain.com
   ```

**Email from options:**

- Test domain: `onboarding@resend.dev` (works immediately)
- Custom domain: `noreply@yourdomain.com` (requires DNS setup)

### 4.3 Blockchain (Optional - Can Skip for MVP)

**To disable:**

```bash
BLOCKCHAIN_ENABLED=false
```

**To enable:** See [Blockchain_Integration_Guide.md](./Blockchain_Integration_Guide.md)

---

## Step 5: Test Deployment

### 5.1 Health Check

```bash
# Test backend
curl https://your-railway-app.railway.app/actuator/health
# Expected: {"status":"UP"}

# Test frontend
curl -I https://your-vercel-app.vercel.app
# Expected: HTTP/2 200
```

### 5.2 End-to-End Test

1. **Open frontend:** `https://your-vercel-app.vercel.app`
2. **Register new merchant:**
   - Click "Sign Up"
   - Fill form
   - Submit
3. **Check email:**
   - If `RESEND_ENABLED=true`: Check inbox
   - If `RESEND_ENABLED=false`: Check Railway logs for verification URL
4. **Verify email** → Click link
5. **Login** with credentials
6. **Create test contract**
7. **Verify in Railway logs** (payments, blockchain if enabled)

---

## Custom Domain Setup (Optional)

### Frontend Domain (Vercel)

1. Vercel → Project → Settings → Domains
2. Add domain: `app.yourdomain.com`
3. Add DNS records:

```bash
Type: CNAME
Name: app
Value: cname.vercel-dns.com
```

4. Wait 5-30 minutes for propagation
5. Update Railway:
   ```
   FRONTEND_URL=https://app.yourdomain.com
   ```

### Backend Domain (Railway)

1. Railway → Service → Settings → Domains
2. Add custom domain: `api.yourdomain.com`
3. Add DNS record:

```bash
Type: CNAME
Name: api
Value: <your-railway-domain>.railway.app
```

4. Update Vercel:
   ```
   VITE_API_URL=https://api.yourdomain.com
   ```

---

## Environment Variables Summary

### Railway (Backend) - Complete List

| Variable                         | Example                   | Required | Notes                         |
| -------------------------------- | ------------------------- | -------- | ----------------------------- |
| `SPRING_PROFILES_ACTIVE`         | `prod`                    | ✅       | Spring Boot profile           |
| `PORT`                           | `8080`                    | ✅       | Server port                   |
| `DATABASE_URL`                   | (auto-set)                | ✅       | PostgreSQL URL                |
| `JWT_SECRET`                     | (generated)               | ✅       | Use `openssl rand -base64 32` |
| `FRONTEND_URL`                   | `https://app.vercel.app`  | ✅       | Vercel frontend URL           |
| `RAZORPAY_KEY_ID`                | `rzp_test_xxxxx`          | ✅       | From Razorpay dashboard       |
| `RAZORPAY_KEY_SECRET`            | `secret_here`             | ✅       | From Razorpay dashboard       |
| `RAZORPAY_WEBHOOK_SECRET`        | `whsec_xxxxx`             | ✅       | From Razorpay webhooks        |
| `RESEND_ENABLED`                 | `true`                    | ✅       | Enable email sending          |
| `RESEND_API_KEY`                 | `re_xxxxx`                | ✅       | From Resend dashboard         |
| `EMAIL_FROM`                     | `noreply@yourdomain.com`  | ✅       | Sender email                  |
| `BLOCKCHAIN_ENABLED`             | `false`                   | ⚠️       | Optional (true/false)         |
| `BLOCKCHAIN_NETWORK`             | `polygon-mainnet`         | ➖       | If blockchain enabled         |
| `BLOCKCHAIN_RPC_URL`             | `https://polygon-rpc.com` | ➖       | If blockchain enabled         |
| `BLOCKCHAIN_PRIVATE_KEY`         | `0xYOUR_KEY`              | ➖       | If blockchain enabled         |
| `BLOCKCHAIN_CONTRACT_ADDRESS`    | `0xADDRESS`               | ➖       | If blockchain enabled         |
| `BLOCKCHAIN_GAS_PRICE`           | `50000000000`             | ➖       | 50 Gwei                       |
| `BLOCKCHAIN_GAS_LIMIT`           | `300000`                  | ➖       | Gas limit                     |
| `BLOCKCHAIN_CONFIRMATION_BLOCKS` | `10`                      | ➖       | Confirmations                 |

### Vercel (Frontend)

| Variable       | Example                            | Required |
| -------------- | ---------------------------------- | -------- |
| `VITE_API_URL` | `https://legalpay-api.railway.app` | ✅       |

---

## Monitoring

### Railway Logs

```bash
# Install CLI
npm install -g @railway/cli

# View logs
railway login
railway link
railway logs
```

Or in Railway Dashboard → Service → **Deployments** → Click latest → **Logs**

### Vercel Logs

Vercel Dashboard → Project → **Deployments** → Click latest → **Logs**

---

## Cost Breakdown (Monthly)

| Service        | Free Tier          | Cost (MVP)            |
| -------------- | ------------------ | --------------------- |
| **Railway**    | $5 credit          | $5-20                 |
| **Vercel**     | 100GB bandwidth    | Free                  |
| **Resend**     | 3,000 emails/month | Free                  |
| **Razorpay**   | -                  | 2% + ₹3 per txn       |
| **Blockchain** | -                  | ₹250-500 (if enabled) |
| **Total**      |                    | **$5-20/month**       |

---

## Troubleshooting

### Backend won't deploy

1. Check Railway logs for errors
2. Verify Java 21 in `pom.xml`
3. Ensure `railway.toml` and `nixpacks.toml` exist

### Frontend can't connect to backend

1. Verify `VITE_API_URL` in Vercel matches Railway URL
2. Verify `FRONTEND_URL` in Railway matches Vercel URL
3. Check CORS in browser console (F12)
4. Test backend health: `curl https://railway-url/actuator/health`

### Emails not sending

1. Check `RESEND_ENABLED=true` in Railway
2. Verify `RESEND_API_KEY` is correct
3. Check Railway logs: `railway logs | grep Email`
4. Check Resend dashboard for delivery status

### Database connection errors

1. Verify PostgreSQL service is running in Railway
2. Check `DATABASE_URL` is set (auto-set by Railway)
3. Check Railway logs for connection errors

---

## Next Steps

After successful deployment:

1. ✅ **Complete Razorpay KYC** for live payments
2. ✅ **Setup custom domain** (optional)
3. ✅ **Configure email domain** in Resend (optional)
4. ✅ **Enable monitoring** (Railway metrics, Sentry)
5. ✅ **Setup backups** (Railway PostgreSQL auto-backups enabled)
6. ✅ **Load test** with expected user volume

## Support Resources

- **Railway Docs:** https://docs.railway.app
- **Vercel Docs:** https://vercel.com/docs
- **Razorpay Docs:** https://razorpay.com/docs/
- **Resend Docs:** https://resend.com/docs
- **LegalPay Docs:** See [docs/](./README.md)
