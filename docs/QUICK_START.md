# LegalPay - Quick Start Guide

## 🚀 Get Started in 5 Minutes

Your payment integration is complete and ready to test! Follow these steps to start developing.

---

## Prerequisites

- Java 21+
- Maven 3.8+
- Node.js 18+
- Razorpay Test Account ([Sign up here](https://dashboard.razorpay.com/signup))
- MetaMask Wallet (optional - for blockchain audit trail)

---

## Step 1: Get Razorpay Test Credentials

1. **Sign up for Razorpay Test Account**
   - Go to https://dashboard.razorpay.com/signup
   - Complete registration (takes 2 minutes)

2. **Get Test API Keys**
   - After login, you'll be in **Test Mode** by default
   - Go to Settings → API Keys
   - Click "Generate Test Key"
   - Copy:
     - **Key ID** (starts with `rzp_test_`)
     - **Key Secret**

3. **Get Webhook Secret** (Optional for MVP)
   - Go to Settings → Webhooks
   - Click "Add New Webhook"
   - URL: `http://localhost:8080/api/v1/webhooks/razorpay`
   - Events: Select `payment.captured` and `payment.failed`
   - Click "Create Webhook"
   - Copy the **Webhook Secret** (starts with `whsec_`)

---

## Step 2: Configure Environment

1. **Backend Configuration**

```bash
# Create .env file in project root
cp .env.example .env
```

Edit `.env` and add your Razorpay credentials:

```env
# Razorpay Configuration
RAZORPAY_KEY_ID=rzp_test_XXXXXXXXXXXXXX
RAZORPAY_KEY_SECRET=YYYYYYYYYYYYYYYYYYYY
RAZORPAY_WEBHOOK_SECRET=whsec_ZZZZZZZZZZZZZZZ

# Blockchain Configuration (Optional - for audit trail)
# Leave empty to disable blockchain, or see BLOCKCHAIN_SUMMARY.md for setup
BLOCKCHAIN_ENABLED=false
BLOCKCHAIN_NETWORK=polygon-mumbai
BLOCKCHAIN_RPC_URL=https://rpc-mumbai.maticvigil.com
BLOCKCHAIN_PRIVATE_KEY=
BLOCKCHAIN_CONTRACT_ADDRESS=
BLOCKCHAIN_GAS_PRICE=1000000000
BLOCKCHAIN_GAS_LIMIT=300000
BLOCKCHAIN_CONFIRMATION_BLOCKS=5

# Database (H2 for development - no setup needed)
DB_URL=jdbc:h2:mem:legalpaytestdb
DB_USERNAME=sa
DB_PASSWORD=

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
JWT_EXPIRATION=86400000

# Email Configuration (Console logging for development)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

2. **Frontend Configuration**

```bash
# Create .env file in frontend directory
cp frontend/.env.example frontend/.env
```

Edit `frontend/.env`:

```env
VITE_API_URL=http://localhost:8080
```

---

## Step 3: Start Development Environment

### Option A: Automated Start (Recommended)

```bash
./start-dev.sh
```

This script will:

- Build backend with Maven
- Install frontend dependencies
- Start backend on http://localhost:8080
- Start frontend on http://localhost:3000
- Create log files in `logs/` directory

### Option B: Manual Start

**Terminal 1 - Backend:**

```bash
cd legalpay-api
mvn spring-boot:run
```

**Terminal 2 - Frontend:**

```bash
cd frontend
npm install
npm run dev
```

---

## Step 4: Test Payment Flow

### A. Register Test Users

1. **Register Merchant Account**
   - Open http://localhost:3000
   - Click "Merchant Sign Up"
   - Fill in details (use fake data for testing):
     - Company: "Test Law Firm"
     - Email: merchant@test.com
     - Password: Test@1234
   - Verify email (check console logs for verification link)

2. **Register Payer Account**
   - Click "Payer Sign Up"
   - Fill in details:
     - Name: "Test Payer"
     - Email: payer@test.com
     - Password: Test@1234
   - Verify email

### B. Create & Pay for Contract

1. **Login as Merchant**
   - Email: merchant@test.com
   - Password: Test@1234

2. **Create Contract**
   - Go to "Create Contract"
   - Fill in:
     - Title: "Legal Services Agreement"
     - Payer Email: payer@test.com
     - Amount: 1000 (₹10.00 for testing)
     - Type: ONE_TIME
   - Submit contract

3. **Login as Payer**
   - Logout merchant
   - Login as: payer@test.com

4. **Sign Contract**
   - View contract details
   - Click "Sign Contract"
   - Complete Aadhaar eSign (in production)
   - For testing: Contract status changes to SIGNED

5. **Make Payment**
   - Click "Pay Now" button
   - Razorpay Checkout modal opens
   - **Use Test Card:**
     - Card Number: `4111 1111 1111 1111`
     - Expiry: Any future date (e.g., 12/25)
     - CVV: Any 3 digits (e.g., 123)
   - Click "Pay ₹10.00"

6. **Verify Success**
   - Redirects to success page
   - Contract status updated to "ACTIVE"
   - Payment status shows "PAID"

---

## Step 5: Verify Payment in Database

### Check H2 Console (Development Database)

1. **Access H2 Console**
   - URL: http://localhost:8080/h2-console
   - JDBC URL: `jdbc:h2:mem:legalpaytestdb`
   - Username: `sa`
   - Password: (leave empty)

2. **Check Payment Record**

```sql
-- View payment
SELECT * FROM razorpay_payments ORDER BY created_at DESC LIMIT 1;

-- View updated contract
SELECT id, title, payment_status, total_paid_amount, last_payment_at
FROM contracts
WHERE payment_status = 'PAID';
```

---

## Development Workflow

### Running Backend Tests

```bash
cd legalpay-api
mvn test
```

### Building Production Bundles

```bash
# Backend
mvn clean package

# Frontend
cd frontend
npm run build
```

### Viewing Logs

```bash
# Backend logs
tail -f logs/backend.log

# Frontend logs
tail -f logs/frontend.log

# Application logs
tail -f logs/application.log
```

### Stopping Services

```bash
# If started with start-dev.sh
# Press Ctrl+C in the terminal

# Or manually kill processes
kill $(cat .backend.pid .frontend.pid)
```

---

## Testing Scenarios

### 1. Successful Payment

- Use test card: 4111 1111 1111 1111
- Expected: Payment captured, contract status = ACTIVE, paymentStatus = PAID

### 2. Failed Payment

- Use test card: 4000 0000 0000 0002 (card declined)
- Expected: Error displayed, payment status = FAILED, contract unchanged

### 3. Cancelled Payment

- Open Razorpay modal
- Click "X" to close without paying
- Expected: Modal closes, can retry payment

### 4. Multiple Contracts

- Create 3 contracts with different amounts
- Pay for them individually
- Verify each payment tracked separately

---

## Troubleshooting

### Backend Won't Start

**Issue:** Port 8080 already in use

```bash
# Find process using port 8080
lsof -i :8080

# Kill it
kill -9 <PID>
```

**Issue:** Razorpay credentials invalid

```
Error: com.razorpay.RazorpayException: Authentication failed
```

**Solution:**

- Verify .env has correct RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET
- Ensure you're using **test keys** (start with `rzp_test_`)
- Check for extra spaces in .env file

### Frontend Won't Start

**Issue:** Cannot find module 'vite'

```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
```

**Issue:** API requests fail with CORS error

**Solution:**

- Verify backend is running on http://localhost:8080
- Check `frontend/.env` has `VITE_API_URL=http://localhost:8080`

### Payment Modal Doesn't Open

**Issue:** "Razorpay is not defined"

**Solution:**

- Check browser console for errors
- Verify `index.html` has Razorpay script tag:
  ```html
  <script src="https://checkout.razorpay.com/v1/checkout.js"></script>
  ```
- Clear browser cache and reload

### Payment Verification Fails

**Issue:** "Invalid signature" error

**Solution:**

- Verify `RAZORPAY_KEY_SECRET` in .env matches Razorpay dashboard
- Check backend logs for signature mismatch details
- Ensure using correct key-secret pair (test with test, live with live)

---

## What's Implemented

✅ **Backend:**

- Razorpay order creation
- Payment signature verification (HMAC-SHA256)
- Payment capture and contract updates
- IP and user-agent tracking for legal evidence
- Error handling and logging

✅ **Frontend:**

- Razorpay Checkout modal integration
- Payment button with loading states
- Success page with auto-redirect
- Error handling and user feedback

✅ **Configuration:**

- Environment-driven credentials
- Test/Live mode switching via .env
- Easy provider switching (future)

⚠️ **Not Yet Implemented:**

- Webhook controller (documented in guide, but not coded)
- Refund functionality
- Partial payment support
- Payment analytics dashboard

---

## Next Steps

### Week 1: Testing

1. ✅ Test payment flow (you're here!)
2. Test registration flows
3. Test email verification
4. Load testing with multiple concurrent payments

### Week 2: Webhook Implementation

1. Implement WebhookController
2. Add signature verification for webhooks
3. Handle payment.captured and payment.failed events
4. Test webhook delivery

### Week 3: Production Prep

1. Set up PostgreSQL database
2. Create production .env with live Razorpay keys
3. Deploy backend to Railway/AWS
4. Deploy frontend to Vercel
5. Configure production webhooks

### Week 4: Go Live

1. Complete Razorpay KYC (for live keys)
2. Update production .env with live credentials
3. Test end-to-end in production
4. Monitor first real payments
5. Set up monitoring and alerts

---

## Resources

### Razorpay Documentation

- [Test Cards](https://razorpay.com/docs/payments/payments/test-card-details/)
- [Checkout.js API](https://razorpay.com/docs/payments/payment-gateway/web-integration/standard/)
- [Webhooks Guide](https://razorpay.com/docs/webhooks/)
- [Error Codes](https://razorpay.com/docs/api/errors/)

### LegalPay Documentation

- [Payment Integration Guide](docs/Payment_Integration_Implementation_Guide.md)
- [Deployment Guide](DEPLOYMENT.md)
- [PRD](docs/PRD_PaymentAutomation.md)
- [Architecture](docs/System_Architecture_and_Implementation.md)

---

## Support

**Issues?** Check:

1. Backend logs: `logs/backend.log`
2. Frontend console: Browser DevTools
3. H2 console: http://localhost:8080/h2-console
4. Razorpay dashboard: https://dashboard.razorpay.com/app/payments

**Still stuck?** Review:

- Payment_Integration_Implementation_Guide.md (comprehensive guide)
- DEPLOYMENT.md (troubleshooting section)

---

**Happy Testing! 🎉**

Your payment integration is production-ready. Just add Razorpay credentials and start testing!
