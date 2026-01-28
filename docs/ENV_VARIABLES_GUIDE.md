# Environment Variables Cheat Sheet

## 🚀 Quick Copy-Paste Templates

### Railway Backend (Production - MVP)

```bash
# === CORE CONFIGURATION ===
SPRING_PROFILES_ACTIVE=prod
PORT=8080

# === SECURITY ===
# Generate with: openssl rand -base64 32
JWT_SECRET=REPLACE_WITH_GENERATED_SECRET_HERE

# === FRONTEND URL ===
# Update after Vercel deployment
FRONTEND_URL=https://your-app.vercel.app

# === DATABASE ===
# Auto-set by Railway PostgreSQL - DO NOT SET MANUALLY
# DATABASE_URL=postgresql://...

# === PAYMENT GATEWAY (RAZORPAY) ===
# Get from: https://dashboard.razorpay.com/app/keys
RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxxxxx
RAZORPAY_KEY_SECRET=your_razorpay_secret_here
RAZORPAY_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxx

# === EMAIL SERVICE (RESEND) ===
# Get from: https://resend.com/api-keys
RESEND_ENABLED=true
RESEND_API_KEY=re_xxxxxxxxxxxxx
EMAIL_FROM=noreply@yourdomain.com

# === BLOCKCHAIN (OPTIONAL - DISABLED FOR MVP) ===
BLOCKCHAIN_ENABLED=false
```

---

### Railway Backend (Production - With Blockchain)

```bash
# === CORE CONFIGURATION ===
SPRING_PROFILES_ACTIVE=prod
PORT=8080
JWT_SECRET=REPLACE_WITH_GENERATED_SECRET_HERE
FRONTEND_URL=https://your-app.vercel.app

# === PAYMENT GATEWAY ===
RAZORPAY_KEY_ID=rzp_live_xxxxxxxxxxxxx
RAZORPAY_KEY_SECRET=your_razorpay_secret_here
RAZORPAY_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxx

# === EMAIL SERVICE ===
RESEND_ENABLED=true
RESEND_API_KEY=re_xxxxxxxxxxxxx
EMAIL_FROM=noreply@yourdomain.com

# === BLOCKCHAIN (POLYGON MAINNET) ===
BLOCKCHAIN_ENABLED=true
BLOCKCHAIN_NETWORK=polygon-mainnet
BLOCKCHAIN_RPC_URL=https://polygon-rpc.com
BLOCKCHAIN_PRIVATE_KEY=0xYOUR_METAMASK_PRIVATE_KEY_HERE
BLOCKCHAIN_CONTRACT_ADDRESS=0xDEPLOYED_CONTRACT_ADDRESS_HERE
BLOCKCHAIN_GAS_PRICE=50000000000
BLOCKCHAIN_GAS_LIMIT=300000
BLOCKCHAIN_CONFIRMATION_BLOCKS=10
```

---

### Vercel Frontend (Production)

```bash
# === BACKEND API URL ===
# Get from Railway: Dashboard → Service → Settings → Domains
VITE_API_URL=https://your-railway-app.railway.app
```

---

## 📋 How to Add Variables

### Railway Dashboard

1. Go to https://railway.app/dashboard
2. Select your **backend service**
3. Click **Variables** tab
4. Click **+ New Variable**
5. Paste variable name and value
6. Repeat for each variable
7. Click **Deploy** to apply

**Screenshot guide:**

```
┌─────────────────────────────────────────┐
│ Variables                                │
├─────────────────────────────────────────┤
│ [ + New Variable ] [ + Reference ]      │
│                                          │
│ SPRING_PROFILES_ACTIVE = prod           │
│ PORT = 8080                              │
│ JWT_SECRET = abc123xyz...                │
│ FRONTEND_URL = https://app.vercel.app    │
│ ...                                      │
│                                          │
│ [Deploy]                                 │
└─────────────────────────────────────────┘
```

### Railway CLI

```bash
# Install CLI
npm install -g @railway/cli

# Login
railway login

# Link to project
railway link

# Set variables one by one
railway variables set SPRING_PROFILES_ACTIVE=prod
railway variables set PORT=8080
railway variables set JWT_SECRET=$(openssl rand -base64 32)
# ... (continue for each)

# Or set multiple at once (not recommended - hard to debug)
railway variables set \
  SPRING_PROFILES_ACTIVE=prod \
  PORT=8080 \
  JWT_SECRET=$(openssl rand -base64 32)
```

### Vercel Dashboard

1. Go to https://vercel.com/dashboard
2. Select your **frontend project**
3. Click **Settings**
4. Click **Environment Variables**
5. Add variable:
   - Name: `VITE_API_URL`
   - Value: `https://your-railway-app.railway.app`
   - Environment: **Production** ✓
6. Click **Save**
7. Redeploy: Deployments → Click latest → **Redeploy**

---

## 🔑 How to Get Each Value

### JWT_SECRET

**Generate with OpenSSL:**

```bash
openssl rand -base64 32
```

**Example output:**

```
dGhpc2lzYXJhbmRvbWx5Z2VuZXJhdGVkand0c2VjcmV0a2V5
```

**Copy this entire string and use as `JWT_SECRET` value**

---

### RAZORPAY Keys

**Test Mode** (for development):

1. Go to https://dashboard.razorpay.com/app/keys
2. Switch to **Test Mode** (toggle at top)
3. Copy **Key ID** → `RAZORPAY_KEY_ID=rzp_test_xxxxx`
4. Click **Show** next to Key Secret
5. Copy **Key Secret** → `RAZORPAY_KEY_SECRET=xxxxx`

**Live Mode** (for production):

1. Complete KYC verification
2. Switch to **Live Mode**
3. Generate live API keys
4. Copy Key ID and Secret

**Webhook Secret:**

1. Dashboard → Settings → **Webhooks**
2. Click **Add Webhook**
3. URL: `https://your-railway-app.railway.app/api/webhooks/razorpay`
4. Events: Select all payment events
5. Click **Create Webhook**
6. Copy **Secret** → `RAZORPAY_WEBHOOK_SECRET=whsec_xxxxx`

---

### RESEND API Key

1. Go to https://resend.com/api-keys
2. Click **Create API Key**
3. Name: `LegalPay Production`
4. Permissions: **Full Access**
5. Click **Create**
6. Copy API key (starts with `re_...`)
   - ⚠️ **Shown only once!** Save immediately
7. Use as `RESEND_API_KEY=re_xxxxx`

**Email From Address:**

- **Test domain:** `EMAIL_FROM=onboarding@resend.dev` (works immediately)
- **Custom domain:** `EMAIL_FROM=noreply@yourdomain.com` (requires DNS setup)

---

### Railway Backend URL

**Method 1: Railway Dashboard**

1. Go to https://railway.app/dashboard
2. Select your **backend service**
3. Go to **Settings** → **Domains**
4. Copy the generated Railway domain
   - Example: `legalpay-api-production.up.railway.app`
5. Use as: `FRONTEND_URL=https://legalpay-api-production.up.railway.app`

**Method 2: Railway CLI**

```bash
railway status
# Look for: Deployment URL
```

---

### Vercel Frontend URL

**Method 1: Vercel Dashboard**

1. Go to https://vercel.com/dashboard
2. Select your **frontend project**
3. Copy deployment URL
   - Example: `legalpay-xyz123.vercel.app`
4. Use in Railway as: `FRONTEND_URL=https://legalpay-xyz123.vercel.app`

**Method 2: After Deployment**

- Vercel shows URL immediately after first deployment
- Format: `https://<project-name>-<random>.vercel.app`

---

### Blockchain Keys (If Enabled)

**Private Key:**

1. Open MetaMask
2. Click three dots → **Account Details**
3. Click **Show Private Key**
4. Enter password
5. Copy private key (starts with `0x...`)
6. Use as `BLOCKCHAIN_PRIVATE_KEY=0x...`

⚠️ **Security:** Never share or commit to Git!

**Contract Address:**

1. Deploy smart contract using Remix
2. Copy deployed contract address from Remix
3. Use as `BLOCKCHAIN_CONTRACT_ADDRESS=0x...`

**RPC URL:**

- Public: `https://polygon-rpc.com` (free)
- Alchemy: `https://polygon-mainnet.g.alchemy.com/v2/YOUR_KEY`
- Infura: `https://polygon-mainnet.infura.io/v3/YOUR_PROJECT_ID`

---

## ✅ Validation Checklist

### Railway Backend

After adding all variables:

- [ ] `SPRING_PROFILES_ACTIVE=prod` ✓
- [ ] `PORT=8080` ✓
- [ ] `JWT_SECRET` (32+ characters) ✓
- [ ] `FRONTEND_URL` (starts with https://) ✓
- [ ] `DATABASE_URL` (auto-set by Railway) ✓
- [ ] `RAZORPAY_KEY_ID` (starts with rzp\_) ✓
- [ ] `RAZORPAY_KEY_SECRET` (not empty) ✓
- [ ] `RAZORPAY_WEBHOOK_SECRET` (starts with whsec\_) ✓
- [ ] `RESEND_ENABLED=true` ✓
- [ ] `RESEND_API_KEY` (starts with re\_) ✓
- [ ] `EMAIL_FROM` (valid email format) ✓
- [ ] `BLOCKCHAIN_ENABLED=false` (or true with all blockchain vars) ✓

Click **Deploy** in Railway to apply all changes.

### Vercel Frontend

- [ ] `VITE_API_URL` (matches Railway backend URL) ✓
- [ ] Starts with `https://` ✓
- [ ] No trailing slash ✓

Click **Save** and **Redeploy** in Vercel.

---

## 🔄 Update Workflow

### When Backend URL Changes

1. **Update in Vercel:**
   - Vercel → Settings → Environment Variables
   - Edit `VITE_API_URL` to new Railway URL
   - Redeploy

### When Frontend URL Changes

1. **Update in Railway:**
   - Railway → Variables
   - Edit `FRONTEND_URL` to new Vercel URL
   - Deploy

### When Switching from Test to Live (Razorpay)

1. **Update in Railway:**

   ```bash
   RAZORPAY_KEY_ID=rzp_live_xxxxx  # Change from rzp_test_
   RAZORPAY_KEY_SECRET=new_live_secret
   RAZORPAY_WEBHOOK_SECRET=new_live_webhook_secret
   ```

2. **Update Webhook URL in Razorpay:**
   - Use live Railway URL
   - Recreate webhook for live mode

---

## 🐛 Common Mistakes

### ❌ Wrong:

```bash
FRONTEND_URL=your-app.vercel.app              # Missing https://
VITE_API_URL=https://railway-url/             # Trailing slash
JWT_SECRET=mysecret                            # Too short/weak
DATABASE_URL=postgresql://...                  # Manual (Railway sets auto)
RESEND_API_KEY="re_xxxxx"                      # Quotes not needed
```

### ✅ Correct:

```bash
FRONTEND_URL=https://your-app.vercel.app
VITE_API_URL=https://railway-url
JWT_SECRET=dGhpc2lzYXJhbmRvbWx5Z2VuZXJhdGVkand0c2VjcmV0a2V5
# DATABASE_URL auto-set by Railway
RESEND_API_KEY=re_xxxxx
```

---

## 📞 Support

If variables not working:

1. **Check Railway logs:** `railway logs` or Railway Dashboard → Logs
2. **Check Vercel logs:** Vercel Dashboard → Deployments → Logs
3. **Verify all required variables set**
4. **Ensure no typos in variable names** (case-sensitive!)
5. **Redeploy after changes**

See [DEPLOYMENT.md](./DEPLOYMENT.md) for full deployment guide.
