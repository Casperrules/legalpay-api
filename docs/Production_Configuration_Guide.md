# Production Configuration Guide - Complete Setup

**Goal**: Take LegalPay from localhost to fully functional production with all features working end-to-end.

---

## Table of Contents

1. [Merchant Onboarding & KYC](#merchant-onboarding--kyc)
2. [Email Integration](#email-integration)
3. [eSign Integration (Digio)](#esign-integration-digio)
4. [Payment Gateway (Razorpay)](#payment-gateway-razorpay)
5. [SMS Notifications](#sms-notifications)
6. [Environment Variables - Complete List](#environment-variables---complete-list)
7. [Database Seeding](#database-seeding)
8. [End-to-End Testing](#end-to-end-testing)

---

## 1. Merchant Onboarding & KYC

### Current Gap

- No merchant registration UI
- No KYC verification flow
- Merchants are hard-coded in database seed

### Solution: Self-Service Merchant Onboarding

#### Step 1: Create Merchant Registration API

**File**: `legalpay-api/src/main/java/com/legalpay/api/dto/MerchantRegistrationRequest.java`

```java
package com.legalpay.api.dto;

public class MerchantRegistrationRequest {
    private String businessName;
    private String email;
    private String phoneNumber;
    private String password;
    private String gstNumber;  // For KYC
    private String panNumber;  // For KYC
    private String businessAddress;

    // Getters and setters...
}
```

**File**: `legalpay-services/src/main/java/com/legalpay/services/MerchantService.java`

```java
package com.legalpay.services;

import com.legalpay.api.dto.MerchantRegistrationRequest;
import com.legalpay.domain.entity.Merchant;
import com.legalpay.domain.repository.MerchantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public MerchantService(MerchantRepository merchantRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService) {
        this.merchantRepository = merchantRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public Merchant registerMerchant(MerchantRegistrationRequest request) {
        // Check if email already exists
        if (merchantRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // Create merchant with PENDING_KYC status
        Merchant merchant = new Merchant();
        merchant.setBusinessName(request.getBusinessName());
        merchant.setEmail(request.getEmail());
        merchant.setPhoneNumber(request.getPhoneNumber());
        merchant.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        merchant.setStatus(Merchant.Status.PENDING_KYC);
        merchant.setGstNumber(request.getGstNumber());
        merchant.setPanNumber(request.getPanNumber());

        Merchant saved = merchantRepository.save(merchant);

        // Send welcome email with KYC instructions
        emailService.sendMerchantWelcomeEmail(saved);

        return saved;
    }

    public Merchant verifyKYC(String merchantId, boolean approved) {
        Merchant merchant = merchantRepository.findById(UUID.fromString(merchantId))
            .orElseThrow(() -> new RuntimeException("Merchant not found"));

        if (approved) {
            merchant.setStatus(Merchant.Status.ACTIVE);
            emailService.sendKYCApprovalEmail(merchant);
        } else {
            merchant.setStatus(Merchant.Status.SUSPENDED);
            emailService.sendKYCRejectionEmail(merchant);
        }

        return merchantRepository.save(merchant);
    }
}
```

#### Step 2: Create Registration Frontend

**File**: `frontend/src/pages/MerchantRegistration.tsx`

```tsx
import { useState } from "react";
import { useNavigate } from "react-router-dom";

export default function MerchantRegistration() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    businessName: "",
    email: "",
    phoneNumber: "",
    password: "",
    confirmPassword: "",
    gstNumber: "",
    panNumber: "",
    businessAddress: "",
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (formData.password !== formData.confirmPassword) {
      alert("Passwords do not match");
      return;
    }

    try {
      const response = await fetch(
        "http://localhost:8080/api/v1/merchants/register",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            businessName: formData.businessName,
            email: formData.email,
            phoneNumber: formData.phoneNumber,
            password: formData.password,
            gstNumber: formData.gstNumber,
            panNumber: formData.panNumber,
            businessAddress: formData.businessAddress,
          }),
        },
      );

      if (response.ok) {
        alert(
          "✅ Registration successful! Please check your email for KYC instructions.",
        );
        navigate("/login");
      } else {
        const error = await response.json();
        alert("❌ Registration failed: " + error.message);
      }
    } catch (err) {
      alert("❌ Registration failed");
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="max-w-md w-full bg-white p-8 rounded-lg shadow">
        <h2 className="text-2xl font-bold mb-6">Register as Merchant</h2>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-1">
              Business Name
            </label>
            <input
              type="text"
              required
              value={formData.businessName}
              onChange={(e) =>
                setFormData({ ...formData, businessName: e.target.value })
              }
              className="w-full border rounded px-3 py-2"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Email</label>
            <input
              type="email"
              required
              value={formData.email}
              onChange={(e) =>
                setFormData({ ...formData, email: e.target.value })
              }
              className="w-full border rounded px-3 py-2"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">
              Phone Number
            </label>
            <input
              type="tel"
              required
              value={formData.phoneNumber}
              onChange={(e) =>
                setFormData({ ...formData, phoneNumber: e.target.value })
              }
              className="w-full border rounded px-3 py-2"
              placeholder="+91XXXXXXXXXX"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">GST Number</label>
            <input
              type="text"
              required
              value={formData.gstNumber}
              onChange={(e) =>
                setFormData({ ...formData, gstNumber: e.target.value })
              }
              className="w-full border rounded px-3 py-2"
              placeholder="22AAAAA0000A1Z5"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">PAN Number</label>
            <input
              type="text"
              required
              value={formData.panNumber}
              onChange={(e) =>
                setFormData({ ...formData, panNumber: e.target.value })
              }
              className="w-full border rounded px-3 py-2"
              placeholder="ABCDE1234F"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Password</label>
            <input
              type="password"
              required
              value={formData.password}
              onChange={(e) =>
                setFormData({ ...formData, password: e.target.value })
              }
              className="w-full border rounded px-3 py-2"
              minLength={8}
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">
              Confirm Password
            </label>
            <input
              type="password"
              required
              value={formData.confirmPassword}
              onChange={(e) =>
                setFormData({ ...formData, confirmPassword: e.target.value })
              }
              className="w-full border rounded px-3 py-2"
            />
          </div>

          <button
            type="submit"
            className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700"
          >
            Register
          </button>
        </form>

        <p className="mt-4 text-center text-sm">
          Already have an account?{" "}
          <a href="/login" className="text-blue-600 hover:underline">
            Login
          </a>
        </p>
      </div>
    </div>
  );
}
```

---

## 2. Email Integration

### Option A: Resend (Recommended - FREE)

**Why Resend?**

- 3,000 emails/month FREE
- Modern API (better than SendGrid)
- Great for transactional emails
- Easy setup

#### Step 1: Sign Up & Get API Key

1. Go to https://resend.com
2. Sign up with GitHub
3. Verify your domain OR use `onboarding@resend.dev` for testing
4. Get API key from dashboard: `re_123abc...`

#### Step 2: Add Resend to Backend

**Update `pom.xml`** (legalpay-services):

```xml
<dependency>
    <groupId>com.resend</groupId>
    <artifactId>resend-java</artifactId>
    <version>3.0.0</version>
</dependency>
```

**Create EmailService**:

**File**: `legalpay-services/src/main/java/com/legalpay/services/EmailService.java`

```java
package com.legalpay.services;

import com.resend.*;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final Resend resend;
    private final String fromEmail;

    public EmailService(@Value("${resend.api.key}") String apiKey,
                       @Value("${resend.from.email}") String fromEmail) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    public void sendMerchantWelcomeEmail(Merchant merchant) {
        String html = """
            <h1>Welcome to LegalPay!</h1>
            <p>Hi %s,</p>
            <p>Thank you for registering with LegalPay. Your account is currently pending KYC verification.</p>
            <p>Please submit the following documents to complete your registration:</p>
            <ul>
                <li>GST Certificate</li>
                <li>PAN Card</li>
                <li>Business Registration Proof</li>
            </ul>
            <p>We'll review your documents and activate your account within 24 hours.</p>
            <p>Best regards,<br>LegalPay Team</p>
        """.formatted(merchant.getBusinessName());

        sendEmail(merchant.getEmail(), "Welcome to LegalPay - KYC Pending", html);
    }

    public void sendKYCApprovalEmail(Merchant merchant) {
        String html = """
            <h1>KYC Approved! 🎉</h1>
            <p>Hi %s,</p>
            <p>Great news! Your KYC verification is complete and your account is now active.</p>
            <p>You can now:</p>
            <ul>
                <li>Create payment contracts</li>
                <li>Send eSign requests to customers</li>
                <li>Automate EMI collections</li>
            </ul>
            <p><a href="https://yourdomain.com/login">Login to Dashboard</a></p>
            <p>Best regards,<br>LegalPay Team</p>
        """.formatted(merchant.getBusinessName());

        sendEmail(merchant.getEmail(), "KYC Approved - Account Activated", html);
    }

    public void sendContractCreatedEmail(Contract contract, Payer payer, Merchant merchant) {
        String html = """
            <h1>New Payment Contract</h1>
            <p>Hi %s,</p>
            <p>%s has created a payment contract for you:</p>
            <ul>
                <li>Amount: ₹%,.2f</li>
                <li>Payment Type: %s</li>
                <li>Due Date: %s</li>
            </ul>
            <p><strong>Next Step:</strong> Please sign the contract electronically.</p>
            <p><a href="https://yourdomain.com/contracts/%s">View & Sign Contract</a></p>
            <p>Best regards,<br>LegalPay Team</p>
        """.formatted(
            payer.getFullName(),
            merchant.getBusinessName(),
            contract.getPrincipalAmount(),
            contract.getPaymentType(),
            contract.getEndDate(),
            contract.getId()
        );

        sendEmail(payer.getEmail(), "New Contract - Please Sign", html);
    }

    public void sendPaymentReminderEmail(Payment payment, Contract contract, Payer payer) {
        String html = """
            <h1>Payment Reminder</h1>
            <p>Hi %s,</p>
            <p>This is a friendly reminder that your payment is due:</p>
            <ul>
                <li>Amount: ₹%,.2f</li>
                <li>Due Date: %s</li>
            </ul>
            <p><a href="https://yourdomain.com/pay/%s">Pay Now</a></p>
            <p>Best regards,<br>LegalPay Team</p>
        """.formatted(
            payer.getFullName(),
            payment.getAmount(),
            payment.getDueDate(),
            payment.getId()
        );

        sendEmail(payer.getEmail(), "Payment Reminder", html);
    }

    private void sendEmail(String to, String subject, String html) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                .from(fromEmail)
                .to(to)
                .subject(subject)
                .html(html)
                .build();

            resend.emails().send(request);
        } catch (ResendException e) {
            // Log error - don't fail the main operation
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
}
```

#### Step 3: Configure Environment Variables

**File**: `application-prod.yml`

```yaml
resend:
  api:
    key: ${RESEND_API_KEY} # re_123abc...
  from:
    email: ${RESEND_FROM_EMAIL:noreply@yourdomain.com}
```

**Railway Environment Variables**:

```bash
RESEND_API_KEY=re_123abc...
RESEND_FROM_EMAIL=noreply@legalpay.in
```

### Option B: Gmail SMTP (Free but Limited)

If you want to use Gmail (100 emails/day limit):

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${GMAIL_USERNAME}
    password: ${GMAIL_APP_PASSWORD} # Not your Gmail password!
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

**Get Gmail App Password**:

1. Enable 2FA on Gmail
2. Go to Google Account → Security → App Passwords
3. Generate password for "LegalPay"
4. Use that password (not your Gmail password)

---

## 3. eSign Integration (Digio)

### Step 1: Sign Up for Digio

1. Go to https://www.digio.in
2. Sign up for developer account
3. Complete KYC (takes 1-2 days)
4. Get credentials:
   - **Client ID**: `DG_CLIENT_123`
   - **Client Secret**: `secret_xyz`
   - **Access Token**: Generated via API

### Step 2: Add Digio SDK

**Update `pom.xml`** (legalpay-services):

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

**Create ESignService**:

**File**: `legalpay-services/src/main/java/com/legalpay/services/ESignService.java`

```java
package com.legalpay.services;

import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ESignService {

    private final OkHttpClient client = new OkHttpClient();

    @Value("${digio.client.id}")
    private String clientId;

    @Value("${digio.client.secret}")
    private String clientSecret;

    @Value("${digio.base.url:https://api.digio.in/v2}")
    private String baseUrl;

    public String initiateESign(Contract contract, Payer payer, String pdfUrl) {
        // 1. Generate access token
        String accessToken = generateAccessToken();

        // 2. Create eSign request
        JSONObject payload = new JSONObject();
        payload.put("sign_type", "aadhaar");  // or "dsc" for Digital Signature
        payload.put("document_url", pdfUrl);
        payload.put("identifier", payer.getEmail());
        payload.put("sign_method", "mobile");
        payload.put("notify", true);
        payload.put("callback_url", "https://yourdomain.com/api/v1/webhooks/esign");

        JSONObject signer = new JSONObject();
        signer.put("identifier", payer.getEmail());
        signer.put("name", payer.getFullName());
        signer.put("reason", "Payment Contract Agreement");
        payload.put("signers", new JSONObject[]{signer});

        RequestBody body = RequestBody.create(
            payload.toString(),
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
            .url(baseUrl + "/client/document/uploadpdf")
            .header("Authorization", "Bearer " + accessToken)
            .post(body)
            .build();

        try {
            Response response = client.newCall(request).execute();
            JSONObject result = new JSONObject(response.body().string());

            // Return document ID
            return result.getString("id");
        } catch (IOException e) {
            throw new RuntimeException("Failed to initiate eSign", e);
        }
    }

    private String generateAccessToken() {
        String credentials = Credentials.basic(clientId, clientSecret);

        Request request = new Request.Builder()
            .url(baseUrl + "/client/authenticate")
            .header("Authorization", credentials)
            .post(RequestBody.create("", null))
            .build();

        try {
            Response response = client.newCall(request).execute();
            JSONObject result = new JSONObject(response.body().string());
            return result.getString("access_token");
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Digio token", e);
        }
    }
}
```

### Step 3: Configure Environment Variables

```yaml
digio:
  client:
    id: ${DIGIO_CLIENT_ID}
    secret: ${DIGIO_CLIENT_SECRET}
  base:
    url: https://api.digio.in/v2 # Sandbox: https://api.digio.in/v2/sandbox
```

**Railway Environment Variables**:

```bash
DIGIO_CLIENT_ID=DG_CLIENT_123
DIGIO_CLIENT_SECRET=secret_xyz
```

### Step 4: Handle eSign Webhook

**File**: `legalpay-api/src/main/java/com/legalpay/api/controller/WebhookController.java`

```java
package com.legalpay.api.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final ContractService contractService;

    @PostMapping("/esign")
    public ResponseEntity<String> handleESignWebhook(@RequestBody Map<String, Object> payload) {
        String documentId = (String) payload.get("id");
        String status = (String) payload.get("status");

        if ("signed".equals(status)) {
            // Update contract status to SIGNED
            contractService.markAsSigned(documentId);
        }

        return ResponseEntity.ok("Webhook received");
    }
}
```

---

## 4. Payment Gateway (Razorpay)

### Step 1: Sign Up for Razorpay

1. Go to https://razorpay.com
2. Sign up
3. Complete KYC
4. Get test credentials:
   - **Key ID**: `rzp_test_123abc`
   - **Key Secret**: `secret_xyz`

### Step 2: Add Razorpay SDK

**Update `pom.xml`** (legalpay-services):

```xml
<dependency>
    <groupId>com.razorpay</groupId>
    <artifactId>razorpay-java</artifactId>
    <version>1.4.6</version>
</dependency>
```

**Create PaymentService**:

**File**: `legalpay-services/src/main/java/com/legalpay/services/PaymentService.java`

```java
package com.legalpay.services;

import com.razorpay.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final RazorpayClient razorpay;

    public PaymentService(@Value("${razorpay.key.id}") String keyId,
                         @Value("${razorpay.key.secret}") String keySecret) throws RazorpayException {
        this.razorpay = new RazorpayClient(keyId, keySecret);
    }

    public String createPaymentOrder(Payment payment) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", payment.getAmount() * 100); // Convert to paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", payment.getId().toString());

        Order order = razorpay.orders.create(orderRequest);
        return order.get("id");
    }

    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);

            Utils.verifyPaymentSignature(attributes, keySecret);
            return true;
        } catch (RazorpayException e) {
            return false;
        }
    }

    // For recurring payments (eNACH)
    public String createSubscription(Contract contract, Payer payer) throws RazorpayException {
        // Create plan
        JSONObject planRequest = new JSONObject();
        planRequest.put("period", contract.getPaymentFrequency().toLowerCase());
        planRequest.put("interval", 1);
        planRequest.put("item", new JSONObject()
            .put("name", "EMI Payment")
            .put("amount", contract.getEmiAmount() * 100)
            .put("currency", "INR"));

        Plan plan = razorpay.plans.create(planRequest);

        // Create subscription
        JSONObject subscriptionRequest = new JSONObject();
        subscriptionRequest.put("plan_id", plan.get("id"));
        subscriptionRequest.put("customer_notify", 1);
        subscriptionRequest.put("total_count", calculateTotalEMIs(contract));
        subscriptionRequest.put("start_at", contract.getStartDate().getTime() / 1000);

        Subscription subscription = razorpay.subscriptions.create(subscriptionRequest);
        return subscription.get("id");
    }
}
```

### Step 3: Frontend Payment Integration

**File**: `frontend/src/pages/PaymentCheckout.tsx`

```tsx
import { useEffect } from "react";

declare global {
  interface Window {
    Razorpay: any;
  }
}

export default function PaymentCheckout({ payment, onSuccess }: any) {
  useEffect(() => {
    // Load Razorpay script
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    document.body.appendChild(script);
  }, []);

  const handlePayment = async () => {
    // Get order ID from backend
    const response = await fetch(
      `/api/v1/payments/${payment.id}/create-order`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
      },
    );
    const { orderId } = await response.json();

    // Open Razorpay checkout
    const options = {
      key: "rzp_test_123abc", // Use env variable in production
      amount: payment.amount * 100,
      currency: "INR",
      name: "LegalPay",
      description: "EMI Payment",
      order_id: orderId,
      handler: function (response: any) {
        // Verify payment on backend
        verifyPayment(response);
      },
      prefill: {
        email: payment.payerEmail,
        contact: payment.payerPhone,
      },
      theme: {
        color: "#3B82F6",
      },
    };

    const rzp = new window.Razorpay(options);
    rzp.open();
  };

  const verifyPayment = async (response: any) => {
    const res = await fetch(`/api/v1/payments/${payment.id}/verify`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
      body: JSON.stringify(response),
    });

    if (res.ok) {
      onSuccess();
    }
  };

  return (
    <button
      onClick={handlePayment}
      className="bg-blue-600 text-white px-6 py-2 rounded"
    >
      Pay ₹{payment.amount.toLocaleString("en-IN")}
    </button>
  );
}
```

---

## 5. SMS Notifications

### Option: Twilio (Recommended)

**Step 1: Sign Up**

1. https://www.twilio.com
2. Get free trial credits ($15)
3. Get credentials:
   - **Account SID**: `AC123abc...`
   - **Auth Token**: `secret123`
   - **Phone Number**: `+1234567890` (buy Indian number for ₹1.15/month)

**Step 2: Add Twilio SDK**

```xml
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>9.14.1</version>
</dependency>
```

**Step 3: SMS Service**

```java
@Service
public class SMSService {

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public SMSService(@Value("${twilio.account.sid}") String accountSid,
                     @Value("${twilio.auth.token}") String authToken,
                     @Value("${twilio.from.number}") String fromNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        Twilio.init(accountSid, authToken);
    }

    public void sendPaymentReminder(Payment payment, Payer payer) {
        String message = String.format(
            "Hi %s, reminder: Your payment of ₹%.2f is due on %s. Pay now: https://legalpay.in/pay/%s",
            payer.getFullName(),
            payment.getAmount(),
            payment.getDueDate(),
            payment.getId()
        );

        Message.creator(
            new PhoneNumber(payer.getPhoneNumber()),
            new PhoneNumber(fromNumber),
            message
        ).create();
    }
}
```

---

## 6. Environment Variables - Complete List

### Development (`.env.local`)

```bash
# Backend
SPRING_PROFILES_ACTIVE=dev
DATABASE_URL=jdbc:h2:mem:legalpay
JWT_SECRET=dev-secret-key-at-least-256-bits-long-change-in-production

# Email (Resend)
RESEND_API_KEY=re_test_123abc
RESEND_FROM_EMAIL=noreply@yourdomain.com

# eSign (Digio Sandbox)
DIGIO_CLIENT_ID=DG_TEST_123
DIGIO_CLIENT_SECRET=test_secret

# Payment (Razorpay Test)
RAZORPAY_KEY_ID=rzp_test_123abc
RAZORPAY_KEY_SECRET=test_secret_xyz

# SMS (Twilio)
TWILIO_ACCOUNT_SID=AC123abc
TWILIO_AUTH_TOKEN=secret123
TWILIO_FROM_NUMBER=+1234567890
```

### Production (Railway/Render)

```bash
# Backend
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=postgres://user:pass@neon.tech:5432/legalpay
JWT_SECRET=production-super-secret-256-bit-key-CHANGE-THIS

# Email (Resend)
RESEND_API_KEY=re_live_123abc
RESEND_FROM_EMAIL=noreply@legalpay.in

# eSign (Digio Production)
DIGIO_CLIENT_ID=DG_LIVE_123
DIGIO_CLIENT_SECRET=live_secret

# Payment (Razorpay Live)
RAZORPAY_KEY_ID=rzp_live_123abc
RAZORPAY_KEY_SECRET=live_secret_xyz

# SMS (Twilio)
TWILIO_ACCOUNT_SID=AC123abc
TWILIO_AUTH_TOKEN=secret123
TWILIO_FROM_NUMBER=+918012345678

# Frontend URL (for CORS)
FRONTEND_URL=https://legalpay.vercel.app
```

### Frontend (`.env.production`)

```bash
VITE_API_URL=https://your-backend.up.railway.app
VITE_RAZORPAY_KEY_ID=rzp_live_123abc
```

---

## 7. Database Seeding (Production)

### Update DataSeeder for Production

**File**: `legalpay-api/src/main/java/com/legalpay/api/config/DataSeeder.java`

```java
@Component
public class DataSeeder implements ApplicationRunner {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public void run(ApplicationArguments args) {
        // Only seed in dev/local environments
        if ("prod".equals(activeProfile)) {
            // In production, create only admin user
            createAdminUser();
            return;
        }

        // Development seeding
        seedMerchants();
        seedPayers();
        seedContracts();
    }

    private void createAdminUser() {
        if (merchantRepository.findByEmail("admin@legalpay.in").isEmpty()) {
            Merchant admin = new Merchant();
            admin.setBusinessName("LegalPay Admin");
            admin.setEmail("admin@legalpay.in");
            admin.setPhoneNumber("+918012345678");
            admin.setPasswordHash(passwordEncoder.encode("Admin@2026"));
            admin.setStatus(Merchant.Status.ACTIVE);
            merchantRepository.save(admin);
        }
    }
}
```

---

## 8. End-to-End Testing

### Test Scenario: Complete User Journey

#### Merchant Flow

1. **Register**: POST `/api/v1/merchants/register`
2. **Receive Email**: "Welcome - KYC Pending"
3. **Admin Approves KYC**: POST `/api/v1/merchants/{id}/verify-kyc`
4. **Receive Email**: "KYC Approved"
5. **Login**: POST `/api/v1/auth/login`
6. **Create Contract**: POST `/api/v1/contracts`
7. **Payer receives email**: "New Contract - Please Sign"

#### Payer Flow

1. **Receive Email**: Click "View & Sign Contract"
2. **eSign**: Complete Aadhaar OTP verification
3. **Contract Status**: Changes to "SIGNED"
4. **Auto-create Payments**: System generates EMI schedule
5. **Receive Payment Reminder**: 3 days before due date
6. **Pay**: Click payment link, complete Razorpay checkout
7. **Receive Confirmation**: SMS + Email

### Testing Checklist

```bash
# 1. Merchant Registration
curl -X POST http://localhost:8080/api/v1/merchants/register \
  -H "Content-Type: application/json" \
  -d '{
    "businessName": "Test Business",
    "email": "test@example.com",
    "phoneNumber": "+918012345678",
    "password": "Test@123",
    "gstNumber": "22AAAAA0000A1Z5",
    "panNumber": "ABCDE1234F"
  }'

# 2. Check email inbox for welcome email

# 3. Admin approves KYC
curl -X POST http://localhost:8080/api/v1/merchants/{id}/verify-kyc \
  -H "Authorization: Bearer {admin_token}" \
  -d '{"approved": true}'

# 4. Login as merchant
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@123"
  }'

# 5. Create contract
curl -X POST http://localhost:8080/api/v1/contracts \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "{merchant_id}",
    "payerId": "{payer_id}",
    "principalAmount": 100000,
    "interestRate": 12.0,
    "paymentType": "EMI",
    "paymentFrequency": "MONTHLY",
    "startDate": "2026-02-01",
    "endDate": "2027-02-01"
  }'
```

---

## 9. Going Live Checklist

### Before Launch

- [ ] All environment variables set in Railway/Render
- [ ] PostgreSQL database created and migrated
- [ ] Resend domain verified (or using `onboarding@resend.dev`)
- [ ] Digio KYC completed and live credentials obtained
- [ ] Razorpay KYC completed and live credentials obtained
- [ ] SSL certificates active (automatic with Railway/Vercel)
- [ ] CORS configured to allow only your frontend domain
- [ ] Rate limiting enabled (10 requests/second)
- [ ] Database backups configured (Neon does this automatically)
- [ ] Error tracking setup (Sentry)
- [ ] Analytics setup (Google Analytics + Mixpanel)

### Post-Launch

- [ ] Monitor Railway logs for errors
- [ ] Check Resend dashboard for email delivery rates
- [ ] Monitor Razorpay dashboard for payment success rates
- [ ] Set up alerts for payment failures
- [ ] Weekly backup verification
- [ ] Monthly security audit

---

## 10. Cost Summary (Month 1)

| Service    | Free Tier         | Cost if Exceeded   |
| ---------- | ----------------- | ------------------ |
| Railway    | 500 hrs free      | $5/month after     |
| Vercel     | Unlimited         | Free forever       |
| Neon DB    | 10GB free         | $19/month after    |
| Resend     | 3000 emails/month | $20/month after    |
| Razorpay   | No monthly fee    | 2% per transaction |
| Digio      | First 100 free    | ₹10-15 per eSign   |
| Twilio SMS | $15 trial         | ₹0.50 per SMS      |
| **Total**  | **₹0-500/month**  | Scales with usage  |

---

## Quick Start Commands

### 1. Set up all environment variables

```bash
# Copy and fill in your values
cp .env.example .env.production
```

### 2. Build and deploy

```bash
# Backend
mvn clean package -DskipTests
railway deploy

# Frontend
cd frontend
vercel --prod
```

### 3. Test email

```bash
curl -X POST https://your-backend.railway.app/api/v1/test/email \
  -H "Authorization: Bearer {token}" \
  -d '{"to": "your@email.com"}'
```

---

**Next**: See [Deployment_Guide_Bootstrap_Minimal_Cost.md](./Deployment_Guide_Bootstrap_Minimal_Cost.md) for detailed deployment steps.
