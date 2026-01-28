# Testing Registration & Authentication Flow

## Overview

Complete self-service user registration system with email verification and password reset.

## Prerequisites

- Backend running on `http://localhost:8080`
- Frontend running on `http://localhost:3000`
- Console access to view email logs (dev mode)

## Test Scenarios

### 1. Merchant Registration Flow

**Steps:**

1. Navigate to `http://localhost:3000/login`
2. Click "Register as Merchant"
3. Fill in the form:
   - Business Name: "Test Legal Services Pvt Ltd"
   - Email: "test-merchant@example.com"
   - Phone: "+919876543210"
   - GST Number: "27AABCT1234A1Z5"
   - PAN Number: "AABCT1234A"
   - Password: "password123"
   - Confirm Password: "password123"
4. Click "Register"
5. **Expected:** Success message "Registration successful! Please check your email to verify your account."

**Backend Console Check:**

```
=== EMAIL: Verify Your LegalPay Account ===
To: test-merchant@example.com
Subject: Verify Your LegalPay Account

Click to verify: http://localhost:3000/verify-email?token=<UUID>
=======================================
```

6. Copy the verification URL from console
7. Paste in browser and visit
8. **Expected:** "Email Verified!" message, auto-redirect to login in 3 seconds

**Backend Console Check:**

```
=== EMAIL: Welcome to LegalPay! ===
To: test-merchant@example.com
Subject: Welcome to LegalPay!
=======================================
```

9. Login with `test-merchant@example.com` / `password123`
10. **Expected:** Successful login, redirect to dashboard

---

### 2. Payer Registration Flow

**Steps:**

1. Navigate to `http://localhost:3000/login`
2. Click "Register as Payer"
3. Fill in the form:
   - Full Name: "Rajesh Kumar"
   - Email: "test-payer@example.com"
   - Phone: "+919123456789"
   - Password: "password123"
   - Confirm Password: "password123"
4. Click "Register"
5. **Expected:** Success message about email verification

**Backend Console Check:**

```
=== EMAIL: Verify Your LegalPay Account ===
To: test-payer@example.com
Click to verify: http://localhost:3000/verify-email?token=<UUID>
```

6. Visit verification URL from console
7. **Expected:** Email verified, welcome email sent
8. Login and verify payer dashboard access

---

### 3. Password Reset Flow

**Steps:**

1. Navigate to `http://localhost:3000/login`
2. Click "Forgot password?"
3. Enter registered email: "test-merchant@example.com"
4. Click "Send Reset Link"
5. **Expected:** "Check Your Email" message

**Backend Console Check:**

```
=== EMAIL: Reset Your LegalPay Password ===
To: test-merchant@example.com
Subject: Reset Your LegalPay Password

Click to reset: http://localhost:3000/reset-password?token=<UUID>

Link expires in 1 hour.
=======================================
```

6. Copy reset URL from console
7. Visit URL in browser
8. Enter new password: "newpassword123"
9. Confirm password: "newpassword123"
10. Click "Reset Password"
11. **Expected:** Success message, auto-redirect to login
12. Login with new password
13. **Expected:** Successful login

---

### 4. Error Scenarios to Test

#### Duplicate Email Registration

1. Try registering with same email twice
2. **Expected:** HTTP 409 error (handled by backend, check browser console)

#### Invalid Verification Token

1. Visit `http://localhost:3000/verify-email?token=invalid-token-123`
2. **Expected:** "Verification Failed" message

#### Expired Verification Token

1. Register a user
2. Wait 24+ hours (or manually set token expiry in database)
3. Try to verify
4. **Expected:** "Token expired or invalid" error

#### Invalid Reset Token

1. Visit `http://localhost:3000/reset-password?token=invalid-token-123`
2. **Expected:** "Invalid Link" page with "Request New Link" button

#### Password Validation

1. Try passwords < 8 characters
2. Try mismatched passwords
3. **Expected:** Client-side validation errors

---

## API Endpoint Testing (cURL)

### Register Merchant

```bash
curl -X POST http://localhost:8080/api/v1/auth/register/merchant \
  -H "Content-Type: application/json" \
  -d '{
    "businessName": "API Test Services",
    "email": "api-merchant@example.com",
    "phone": "+919999999999",
    "gstNumber": "27AABCT1234A1Z5",
    "panNumber": "AABCT1234A",
    "password": "password123"
  }'
```

**Expected Response:**

```json
{
  "message": "Registration successful! Please check your email to verify your account."
}
```

### Register Payer

```bash
curl -X POST http://localhost:8080/api/v1/auth/register/payer \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "API Test User",
    "email": "api-payer@example.com",
    "phone": "+919888888888",
    "password": "password123"
  }'
```

### Verify Email

```bash
# Replace TOKEN with actual token from console
curl "http://localhost:8080/api/v1/auth/verify-email?token=TOKEN"
```

**Expected Response:**

```json
{
  "message": "Email verified successfully! You can now log in."
}
```

### Forgot Password

```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "api-merchant@example.com"
  }'
```

**Expected Response:**

```json
{
  "message": "If your email is registered, you will receive a password reset link shortly."
}
```

### Reset Password

```bash
# Replace TOKEN with actual token from console
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "TOKEN",
    "newPassword": "newpassword123"
  }'
```

**Expected Response:**

```json
{
  "message": "Password reset successful! You can now log in with your new password."
}
```

---

## Production Checklist

Before deploying to production:

- [ ] Replace console email logging with actual Resend integration in `EmailService.java`
- [ ] Update `FRONTEND_URL` in `application.yml` to production domain
- [ ] Configure SMTP settings for Resend API key
- [ ] Test email delivery to real email addresses
- [ ] Set up proper token expiry monitoring
- [ ] Add rate limiting to prevent abuse of registration/reset endpoints
- [ ] Configure HTTPS for all email links
- [ ] Add CAPTCHA to registration forms
- [ ] Set up email bounce handling
- [ ] Configure SPF/DKIM records for email domain
- [ ] Add registration analytics tracking
- [ ] Set up monitoring for failed verification attempts

---

## Database Verification

### Check Registered Users

```sql
-- Merchants
SELECT id, email, email_verified, business_name, created_at
FROM merchants
ORDER BY created_at DESC;

-- Payers
SELECT id, email, email_verified, full_name, created_at
FROM payers
ORDER BY created_at DESC;
```

### Check Active Tokens

```sql
-- Verification tokens
SELECT email, verification_token, verification_token_expiry
FROM merchants
WHERE email_verified = false;

-- Reset tokens
SELECT email, password_reset_token, password_reset_token_expiry
FROM merchants
WHERE password_reset_token IS NOT NULL;
```

### Manual Token Cleanup (if needed)

```sql
-- Clear expired verification tokens
UPDATE merchants
SET verification_token = NULL, verification_token_expiry = NULL
WHERE verification_token_expiry < NOW();

-- Clear expired reset tokens
UPDATE merchants
SET password_reset_token = NULL, password_reset_token_expiry = NULL
WHERE password_reset_token_expiry < NOW();
```

---

## Troubleshooting

### Issue: Email not showing in console

- Check backend is running and logs are visible
- Verify `EmailService` is properly injected
- Check for exceptions in backend console

### Issue: Verification link returns 404

- Ensure frontend dev server is running
- Verify route is added in `App.tsx`
- Check `FRONTEND_URL` in `application.yml`

### Issue: Token invalid immediately

- Check system clock synchronization
- Verify token expiry calculation in `AuthController`
- Check database timezone settings

### Issue: Can't login after verification

- Verify `emailVerified` flag is set to `true` in database
- Check password encoding is working
- Verify JWT token generation

### Issue: Password reset link expired quickly

- Default is 1 hour - check token generation timestamp
- Verify `passwordResetTokenExpiry` calculation
- Check if you're using cached/old link

---

## Next Steps

1. **Immediate:** Test all flows manually using this guide
2. **Short-term:** Integrate Resend for production email delivery
3. **Medium-term:** Add automated tests for registration flows
4. **Long-term:** Implement Aadhaar eKYC (after MVP launch)
