# Email Integration Setup Guide

## Overview

LegalPay uses **Resend** for transactional emails (verification, password reset, welcome). Resend offers:

- ✅ **3,000 emails/month FREE**
- ✅ **HTML templates** with professional styling
- ✅ **99.9% deliverability**
- ✅ **Simple API** (Java SDK included)

---

## Setup Steps

### 1. Get Resend API Key (5 minutes)

1. Sign up at https://resend.com/signup
2. Verify your email
3. Go to **API Keys** → **Create API Key**
4. Copy the key (starts with `re_...`)

### 2. Configure Environment Variables

Add to `.env.local` (development):

```bash
RESEND_ENABLED=false  # Set to true when you have API key
RESEND_API_KEY=re_xxxxxxxxxxxxx
EMAIL_FROM=noreply@yourdomain.com  # Or use resend's test domain
FRONTEND_URL=http://localhost:3000
```

Add to Railway/Production:

```bash
RESEND_ENABLED=true
RESEND_API_KEY=re_xxxxxxxxxxxxx
EMAIL_FROM=noreply@yourdomain.com
FRONTEND_URL=https://your-frontend-url.vercel.app
```

### 3. Development Mode (Without API Key)

If `RESEND_ENABLED=false`, emails are logged to console:

```
================================================================================
EMAIL TO: user@example.com
SUBJECT: Verify Your LegalPay Account
ACTION URL: http://localhost:3000/verify-email?token=abc123
BODY (stripped HTML): Hi John, Thank you for registering...
================================================================================
```

Check backend logs to see verification/reset URLs during development.

---

## Email Types Sent

| Event              | Subject                      | Trigger                       |
| ------------------ | ---------------------------- | ----------------------------- |
| **Registration**   | Verify Your LegalPay Account | New merchant/payer signup     |
| **Email Verified** | Welcome to LegalPay!         | User clicks verification link |
| **Password Reset** | Reset Your LegalPay Password | User requests password reset  |

---

## Production Deployment

### Option 1: Use Resend's Test Domain (Free, Immediate)

```bash
EMAIL_FROM=onboarding@resend.dev
```

✅ Works immediately, no DNS setup needed
❌ Emails may go to spam

### Option 2: Custom Domain (Recommended for Production)

1. Add your domain in Resend dashboard
2. Add DNS records (SPF, DKIM, DMARC)
3. Verify domain
4. Use: `EMAIL_FROM=noreply@yourdomain.com`

**DNS Records Example:**

```
TXT  @  v=spf1 include:resend.com ~all
TXT  resend._domainkey  [DKIM key from Resend]
TXT  _dmarc  v=DMARC1; p=none; rua=mailto:dmarc@yourdomain.com
```

---

## Testing Email Integration

### Manual Test (Development)

```bash
# Start backend
cd legalpay-api
mvn spring-boot:run

# Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register/merchant \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "businessName": "Test Business",
    "phoneNumber": "+919876543210"
  }'

# Check backend logs for verification URL
```

### Production Test

Set `RESEND_ENABLED=true` and check:

1. Email delivery (inbox, not spam)
2. Links work correctly
3. HTML renders properly
4. Unsubscribe links (if added)

---

## Cost Estimates

| Plan               | Free Tier | Paid           |
| ------------------ | --------- | -------------- |
| **Emails/month**   | 3,000     | $20 for 50,000 |
| **Custom domains** | Unlimited | Unlimited      |
| **Support**        | Community | Email          |

**For LegalPay MVP:** Free tier (3,000 emails) is sufficient for 1,000 users/month.

---

## Troubleshooting

### Emails not sending in production

1. Check Railway logs: `railway logs`
2. Verify `RESEND_ENABLED=true`
3. Verify `RESEND_API_KEY` is set correctly
4. Check Resend dashboard → **Logs** for delivery status

### Emails going to spam

1. Use custom domain (not resend.dev)
2. Add all DNS records (SPF, DKIM, DMARC)
3. Warm up domain (start with small volumes)
4. Test with https://www.mail-tester.com

### HTML not rendering

- Some email clients block remote images
- Test with Gmail, Outlook, Apple Mail
- Use inline CSS (already included in templates)

---

## Alternative Providers

If Resend doesn't work for India:

### Option B: SendGrid

```xml
<dependency>
    <groupId>com.sendgrid</groupId>
    <artifactId>sendgrid-java</artifactId>
    <version>4.9.3</version>
</dependency>
```

**Free tier:** 100 emails/day
**Pros:** Established provider
**Cons:** Lower free tier

### Option C: AWS SES

**Cost:** $0.10 per 1,000 emails
**Pros:** Cheapest for high volume
**Cons:** Requires AWS setup, domain verification

### Option D: SMTP (Gmail/Outlook)

Add to `application.yml`:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

**Pros:** Free for low volume
**Cons:** 500 emails/day limit, may be blocked

---

## Implementation Details

### Email Service Architecture

```
AuthController
    ↓
EmailService.sendVerificationEmail()
    ↓
┌─ RESEND_ENABLED=true ──→ Resend API ──→ User Inbox
└─ RESEND_ENABLED=false ─→ Console Logs ─→ Developer sees URL
```

### HTML Templates

All emails use responsive HTML with:

- ✅ Professional styling
- ✅ Call-to-action buttons
- ✅ Mobile-friendly layout
- ✅ Accessible color contrast
- ✅ Security warnings (for password reset)

### Security Features

- ✅ **Token expiry:** 24h for verification, 1h for password reset
- ✅ **One-time use:** Tokens invalidated after use
- ✅ **HTTPS links:** All URLs use HTTPS in production
- ✅ **No reply address:** Prevents phishing attempts

---

## Next Steps

1. **MVP (Local):** Keep `RESEND_ENABLED=false`, use console logs
2. **Staging:** Enable Resend with test domain
3. **Production:** Add custom domain, enable monitoring

For production checklist, see [DEPLOYMENT.md](./DEPLOYMENT.md).
