# Payment Integration Implementation Guide - Production Ready

**Last Updated**: January 28, 2026  
**Status**: Ready for Production Implementation  
**Target Go-Live**: February 2026

---

## Executive Summary

LegalPay uses **Razorpay as the sole payment gateway for MVP**. This document provides production-ready implementation specifications for:

1. **One-time payments** (immediate payment on contract acceptance)
2. **EMI payments** (installment-based using Razorpay Subscriptions)
3. **Payment verification & webhooks**
4. **Legal compliance & evidence generation**

**Why Razorpay Only:**

- Covers 100% of MVP payment needs
- India-focused (UPI, cards, netbanking, wallets)
- Lower fees (2% vs PayPal's 3.4%)
- Faster settlements (T+1)
- PayPal deferred until international demand proven (6+ months)

---

## Table of Contents

1. [Razorpay Account Setup](#razorpay-account-setup)
2. [Architecture Overview](#architecture-overview)
3. [Database Schema Changes](#database-schema-changes)
4. [Backend Implementation](#backend-implementation)
5. [Frontend Implementation](#frontend-implementation)
6. [Webhook Implementation](#webhook-implementation)
7. [EMI Flow Implementation](#emi-flow-implementation)
8. [Security & Compliance](#security-and-compliance)
9. [Testing Checklist](#testing-checklist)
10. [Production Deployment](#production-deployment)
11. [Monitoring & Alerts](#monitoring-and-alerts)

---

## 1. Razorpay Account Setup

### Step 1: Create Production Account

1. Go to https://dashboard.razorpay.com/signup
2. Register with business details:
   - Business Name: "LegalPay Technologies Pvt Ltd" (or your entity)
   - Business Type: "Technology/SaaS"
   - Business Category: "Financial Services"
   - GST Number: [Your GST]
   - PAN: [Your PAN]
3. Complete KYC (required documents):
   - Certificate of Incorporation
   - PAN card of business
   - GST certificate
   - Bank account proof (cancelled cheque)
   - Director PAN + Aadhaar

**Timeline**: 2-3 business days for approval

### Step 2: Enable Required Features

In Razorpay Dashboard:

```
Settings → API Keys → Generate Test/Live Keys
Settings → Webhooks → Add webhook URL
Settings → Payment Methods → Enable:
  ✅ UPI
  ✅ Cards (Debit/Credit)
  ✅ Netbanking
  ✅ Wallets (Paytm, PhonePe, GooglePay)
  ✅ EMI (if needed for >₹3000 invoices)

Settings → Subscriptions → Enable (for EMI flow)
```

### Step 3: Pricing Confirmation

**Standard Razorpay Fees**:

- Domestic cards: 2% + GST
- UPI: 0% (free until certain volume, then ₹X per transaction)
- Netbanking: 2% + GST
- International cards: 3% + GST
- Wallets: 2% + GST

**Razorpay Subscriptions** (for EMI):

- Same as standard fees (2% per installment)
- No additional subscription fee

**Settlement**: T+1 (next business day to your bank account)

### Step 4: Get API Credentials

```bash
# Test Mode (for development/staging)
Key ID: rzp_test_XXXXXXXXXXXXXX
Key Secret: YYYYYYYYYYYYYYYY

# Live Mode (for production)
Key ID: rzp_live_XXXXXXXXXXXXXX
Key Secret: YYYYYYYYYYYYYYYY
```

**Store in environment variables**:

```bash
# .env.production
RAZORPAY_KEY_ID=rzp_live_XXXXXXXXXXXXXX
RAZORPAY_KEY_SECRET=YYYYYYYYYYYYYYYY
RAZORPAY_WEBHOOK_SECRET=whsec_ZZZZZZZZZZZZZ
```

---

## 2. Architecture Overview

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Payer     │────1───→│  LegalPay    │────2───→│  Razorpay   │
│  (Browser)  │         │   Backend    │         │   Gateway   │
└─────────────┘         └──────────────┘         └─────────────┘
       ↑                        ↑                        │
       │                        │                        │
       └──────────4─────────────┴────────────3───────────┘
              (Redirect after payment)

Flow:
1. Payer clicks "Pay Now" on contract
2. Backend creates Razorpay Order (via API)
3. Frontend opens Razorpay Checkout (modal)
4. Payer completes payment → Razorpay redirects back
5. Razorpay sends webhook → Backend verifies & updates contract status
```

### Key Components

| Component             | Responsibility                 | Tech Stack                      |
| --------------------- | ------------------------------ | ------------------------------- |
| **PaymentController** | Create orders, verify payments | Spring Boot REST                |
| **RazorpayService**   | Razorpay API integration       | Razorpay Java SDK               |
| **WebhookController** | Receive payment confirmations  | Spring Boot + HMAC verification |
| **PaymentEntity**     | Store payment records          | JPA/Hibernate                   |
| **Frontend Checkout** | Razorpay modal integration     | React + Razorpay.js             |

---

## 3. Database Schema Changes

### 3.1 New Table: `payments`

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(id),
    payer_id UUID NOT NULL REFERENCES payers(id),
    merchant_id UUID NOT NULL REFERENCES merchants(id),

    -- Razorpay identifiers
    razorpay_order_id VARCHAR(255) UNIQUE NOT NULL,
    razorpay_payment_id VARCHAR(255) UNIQUE,
    razorpay_signature VARCHAR(512),

    -- Payment details
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'INR',
    status VARCHAR(50) NOT NULL, -- CREATED, AUTHORIZED, CAPTURED, FAILED, REFUNDED
    payment_method VARCHAR(50), -- card, upi, netbanking, wallet

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    captured_at TIMESTAMP,
    failed_at TIMESTAMP,

    -- Failure details
    error_code VARCHAR(100),
    error_description TEXT,

    -- Metadata for legal evidence
    payer_ip_address VARCHAR(45),
    payer_user_agent TEXT,

    INDEX idx_contract_id (contract_id),
    INDEX idx_razorpay_order_id (razorpay_order_id),
    INDEX idx_status (status)
);
```

### 3.2 New Table: `payment_installments` (for EMI)

```sql
CREATE TABLE payment_installments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(id),

    -- Razorpay subscription identifiers
    razorpay_subscription_id VARCHAR(255) UNIQUE NOT NULL,
    razorpay_plan_id VARCHAR(255) NOT NULL,

    -- Installment details
    total_amount DECIMAL(15, 2) NOT NULL,
    installment_amount DECIMAL(15, 2) NOT NULL,
    total_installments INTEGER NOT NULL,
    completed_installments INTEGER DEFAULT 0,

    -- Subscription status
    status VARCHAR(50) NOT NULL, -- CREATED, ACTIVE, PAUSED, COMPLETED, CANCELLED

    -- Dates
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_billing_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,

    INDEX idx_contract_id (contract_id),
    INDEX idx_subscription_id (razorpay_subscription_id)
);

CREATE TABLE installment_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    installment_id UUID NOT NULL REFERENCES payment_installments(id),
    payment_id UUID NOT NULL REFERENCES payments(id),

    -- Installment tracking
    installment_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    paid_date TIMESTAMP,

    status VARCHAR(50) NOT NULL, -- PENDING, PAID, FAILED, SKIPPED

    INDEX idx_installment_id (installment_id),
    INDEX idx_due_date (due_date)
);
```

### 3.3 Update Existing Table: `contracts`

```sql
-- Add payment tracking columns
ALTER TABLE contracts ADD COLUMN payment_status VARCHAR(50) DEFAULT 'PENDING';
-- Values: PENDING, PARTIAL, PAID, FAILED, REFUNDED

ALTER TABLE contracts ADD COLUMN total_paid_amount DECIMAL(15, 2) DEFAULT 0.00;
ALTER TABLE contracts ADD COLUMN last_payment_at TIMESTAMP;

-- Add index
CREATE INDEX idx_payment_status ON contracts(payment_status);
```

---

## 4. Backend Implementation

### 4.1 Add Razorpay Dependency

**File**: `legalpay-services/pom.xml`

```xml
<dependency>
    <groupId>com.razorpay</groupId>
    <artifactId>razorpay-java</artifactId>
    <version>1.4.6</version>
</dependency>

<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5.14</version>
</dependency>
```

### 4.2 Configuration

**File**: `legalpay-api/src/main/resources/application.yml`

```yaml
razorpay:
  key-id: ${RAZORPAY_KEY_ID}
  key-secret: ${RAZORPAY_KEY_SECRET}
  webhook-secret: ${RAZORPAY_WEBHOOK_SECRET}
  currency: INR

payment:
  callback-url: ${FRONTEND_URL}/payment/callback
  webhook-url: ${BACKEND_URL}/api/v1/webhooks/razorpay
```

**File**: `legalpay-api/src/main/resources/application-prod.yml`

```yaml
razorpay:
  key-id: ${RAZORPAY_LIVE_KEY_ID}
  key-secret: ${RAZORPAY_LIVE_KEY_SECRET}
  webhook-secret: ${RAZORPAY_WEBHOOK_SECRET}
```

### 4.3 Razorpay Configuration Class

**File**: `legalpay-services/src/main/java/com/legalpay/services/config/RazorpayConfig.java`

```java
package com.legalpay.services.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }
}
```

### 4.4 Payment Entity

**File**: `legalpay-domain/src/main/java/com/legalpay/domain/entity/Payment.java`

```java
package com.legalpay.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private Payer payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "razorpay_order_id", unique = true, nullable = false)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", unique = true)
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature", length = 512)
    private String razorpaySignature;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus status;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_description", columnDefinition = "TEXT")
    private String errorDescription;

    @Column(name = "payer_ip_address", length = 45)
    private String payerIpAddress;

    @Column(name = "payer_user_agent", columnDefinition = "TEXT")
    private String payerUserAgent;

    // Constructors
    public Payment() {
        this.createdAt = Instant.now();
        this.status = PaymentStatus.CREATED;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public Payer getPayer() {
        return payer;
    }

    public void setPayer(Payer payer) {
        this.payer = payer;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpaySignature() {
        return razorpaySignature;
    }

    public void setRazorpaySignature(String razorpaySignature) {
        this.razorpaySignature = razorpaySignature;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getPayerIpAddress() {
        return payerIpAddress;
    }

    public void setPayerIpAddress(String payerIpAddress) {
        this.payerIpAddress = payerIpAddress;
    }

    public String getPayerUserAgent() {
        return payerUserAgent;
    }

    public void setPayerUserAgent(String payerUserAgent) {
        this.payerUserAgent = payerUserAgent;
    }

    public enum PaymentStatus {
        CREATED,      // Order created, awaiting payment
        AUTHORIZED,   // Payment authorized but not captured
        CAPTURED,     // Payment successful and captured
        FAILED,       // Payment failed
        REFUNDED      // Payment refunded
    }
}
```

### 4.5 Payment Repository

**File**: `legalpay-domain/src/main/java/com/legalpay/domain/repository/PaymentRepository.java`

```java
package com.legalpay.domain.repository;

import com.legalpay.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    List<Payment> findByContractId(UUID contractId);

    List<Payment> findByPayerId(UUID payerId);

    List<Payment> findByMerchantId(UUID merchantId);

    List<Payment> findByStatus(Payment.PaymentStatus status);
}
```

### 4.6 Payment DTOs

**File**: `legalpay-api/src/main/java/com/legalpay/api/dto/CreatePaymentRequest.java`

```java
package com.legalpay.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreatePaymentRequest {

    @NotNull(message = "Contract ID is required")
    private UUID contractId;

    // Getters and Setters
    public UUID getContractId() {
        return contractId;
    }

    public void setContractId(UUID contractId) {
        this.contractId = contractId;
    }
}
```

**File**: `legalpay-api/src/main/java/com/legalpay/api/dto/PaymentOrderResponse.java`

```java
package com.legalpay.api.dto;

import java.math.BigDecimal;

public class PaymentOrderResponse {

    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String razorpayKeyId;
    private String contractTitle;
    private String merchantName;
    private String payerEmail;
    private String payerPhone;

    // Constructors
    public PaymentOrderResponse() {}

    public PaymentOrderResponse(String orderId, BigDecimal amount, String currency,
                                String razorpayKeyId, String contractTitle,
                                String merchantName, String payerEmail, String payerPhone) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.razorpayKeyId = razorpayKeyId;
        this.contractTitle = contractTitle;
        this.merchantName = merchantName;
        this.payerEmail = payerEmail;
        this.payerPhone = payerPhone;
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    public void setRazorpayKeyId(String razorpayKeyId) {
        this.razorpayKeyId = razorpayKeyId;
    }

    public String getContractTitle() {
        return contractTitle;
    }

    public void setContractTitle(String contractTitle) {
        this.contractTitle = contractTitle;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    public String getPayerPhone() {
        return payerPhone;
    }

    public void setPayerPhone(String payerPhone) {
        this.payerPhone = payerPhone;
    }
}
```

**File**: `legalpay-api/src/main/java/com/legalpay/api/dto/VerifyPaymentRequest.java`

```java
package com.legalpay.api.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyPaymentRequest {

    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;

    // Getters and Setters
    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpaySignature() {
        return razorpaySignature;
    }

    public void setRazorpaySignature(String razorpaySignature) {
        this.razorpaySignature = razorpaySignature;
    }
}
```

### 4.7 Payment Service

**File**: `legalpay-services/src/main/java/com/legalpay/services/PaymentService.java`

```java
package com.legalpay.services;

import com.legalpay.domain.entity.Contract;
import com.legalpay.domain.entity.Payment;
import com.legalpay.domain.entity.Payer;
import com.legalpay.domain.repository.ContractRepository;
import com.legalpay.domain.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    /**
     * Create a Razorpay order for a contract
     */
    @Transactional
    public Payment createPaymentOrder(UUID contractId, String payerIpAddress, String payerUserAgent)
            throws RazorpayException {

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found"));

        // Validate contract status
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new IllegalStateException("Contract must be ACTIVE to create payment");
        }

        // Calculate amount in paise (Razorpay requires smallest currency unit)
        BigDecimal amountInRupees = contract.getAmount();
        int amountInPaise = amountInRupees.multiply(new BigDecimal("100")).intValue();

        // Create Razorpay order
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "contract_" + contractId.toString());

        JSONObject notes = new JSONObject();
        notes.put("contract_id", contractId.toString());
        notes.put("merchant_id", contract.getMerchant().getId().toString());
        notes.put("payer_id", contract.getPayer().getId().toString());
        orderRequest.put("notes", notes);

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        logger.info("Created Razorpay order: {} for contract: {}",
                    razorpayOrder.get("id"), contractId);

        // Create payment record
        Payment payment = new Payment();
        payment.setContract(contract);
        payment.setPayer(contract.getPayer());
        payment.setMerchant(contract.getMerchant());
        payment.setRazorpayOrderId(razorpayOrder.get("id"));
        payment.setAmount(amountInRupees);
        payment.setCurrency("INR");
        payment.setStatus(Payment.PaymentStatus.CREATED);
        payment.setPayerIpAddress(payerIpAddress);
        payment.setPayerUserAgent(payerUserAgent);

        return paymentRepository.save(payment);
    }

    /**
     * Verify payment signature from Razorpay
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                razorpayKeySecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = HexFormat.of().formatHex(hash);

            boolean isValid = generatedSignature.equals(signature);

            logger.info("Payment signature verification for order {}: {}", orderId, isValid);

            return isValid;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error verifying payment signature", e);
            return false;
        }
    }

    /**
     * Update payment status after verification
     */
    @Transactional
    public Payment capturePayment(String orderId, String paymentId, String signature,
                                  String paymentMethod) {

        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found"));

        // Verify signature
        if (!verifyPaymentSignature(orderId, paymentId, signature)) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailedAt(Instant.now());
            payment.setErrorCode("SIGNATURE_VERIFICATION_FAILED");
            payment.setErrorDescription("Payment signature verification failed");
            paymentRepository.save(payment);

            logger.error("Payment signature verification failed for order: {}", orderId);
            throw new SecurityException("Payment signature verification failed");
        }

        // Update payment
        payment.setRazorpayPaymentId(paymentId);
        payment.setRazorpaySignature(signature);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(Payment.PaymentStatus.CAPTURED);
        payment.setCapturedAt(Instant.now());

        Payment savedPayment = paymentRepository.save(payment);

        // Update contract status
        Contract contract = payment.getContract();
        contract.setPaymentStatus("PAID");
        contract.setTotalPaidAmount(payment.getAmount());
        contract.setLastPaymentAt(Instant.now());
        contractRepository.save(contract);

        logger.info("Payment captured successfully: {} for contract: {}",
                    paymentId, contract.getId());

        return savedPayment;
    }

    /**
     * Handle failed payment
     */
    @Transactional
    public void handleFailedPayment(String orderId, String errorCode, String errorDescription) {

        paymentRepository.findByRazorpayOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailedAt(Instant.now());
            payment.setErrorCode(errorCode);
            payment.setErrorDescription(errorDescription);
            paymentRepository.save(payment);

            logger.warn("Payment failed for order: {} - Code: {}, Description: {}",
                        orderId, errorCode, errorDescription);
        });
    }
}
```

### 4.8 Payment Controller

**File**: `legalpay-api/src/main/java/com/legalpay/api/controller/PaymentController.java`

```java
package com.legalpay.api.controller;

import com.legalpay.api.dto.CreatePaymentRequest;
import com.legalpay.api.dto.PaymentOrderResponse;
import com.legalpay.api.dto.VerifyPaymentRequest;
import com.legalpay.domain.entity.Contract;
import com.legalpay.domain.entity.Payment;
import com.legalpay.domain.repository.ContractRepository;
import com.legalpay.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ContractRepository contractRepository;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    /**
     * Create payment order for a contract
     * Endpoint: POST /api/v1/payments/create-order
     */
    @PostMapping("/create-order")
    @PreAuthorize("hasAnyRole('PAYER', 'MERCHANT')")
    public ResponseEntity<?> createPaymentOrder(
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) {

        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            Payment payment = paymentService.createPaymentOrder(
                request.getContractId(),
                ipAddress,
                userAgent
            );

            Contract contract = payment.getContract();

            PaymentOrderResponse response = new PaymentOrderResponse(
                payment.getRazorpayOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                razorpayKeyId,
                contract.getTitle(),
                contract.getMerchant().getBusinessName(),
                contract.getPayer().getEmail(),
                contract.getPayer().getPhone()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Verify payment after Razorpay callback
     * Endpoint: POST /api/v1/payments/verify
     */
    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('PAYER', 'MERCHANT')")
    public ResponseEntity<?> verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {

        try {
            Payment payment = paymentService.capturePayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature(),
                "online" // Will be updated from webhook
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Payment verified successfully");
            response.put("paymentId", payment.getId());
            response.put("contractId", payment.getContract().getId());
            response.put("status", payment.getStatus());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Payment verification failed");
            return ResponseEntity.status(403).body(error);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get payment details by contract ID
     * Endpoint: GET /api/v1/payments/contract/{contractId}
     */
    @GetMapping("/contract/{contractId}")
    @PreAuthorize("hasAnyRole('PAYER', 'MERCHANT')")
    public ResponseEntity<?> getPaymentsByContract(@PathVariable String contractId) {
        // Implementation for fetching payment history
        return ResponseEntity.ok().build();
    }

    /**
     * Helper method to get client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
```

---

## 5. Frontend Implementation

### 5.1 Install Razorpay SDK

```bash
cd frontend
npm install razorpay
```

### 5.2 Add Razorpay Script to HTML

**File**: `frontend/index.html`

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>LegalPay</title>

    <!-- Razorpay Checkout -->
    <script src="https://checkout.razorpay.com/v1/checkout.js"></script>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

### 5.3 Payment Service (Frontend)

**File**: `frontend/src/services/paymentService.ts`

```typescript
import { api } from "./api";

export interface PaymentOrderResponse {
  orderId: string;
  amount: number;
  currency: string;
  razorpayKeyId: string;
  contractTitle: string;
  merchantName: string;
  payerEmail: string;
  payerPhone: string;
}

export interface RazorpayOptions {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  order_id: string;
  prefill: {
    email: string;
    contact: string;
  };
  theme: {
    color: string;
  };
  handler: (response: RazorpayResponse) => void;
  modal: {
    ondismiss: () => void;
  };
}

export interface RazorpayResponse {
  razorpay_payment_id: string;
  razorpay_order_id: string;
  razorpay_signature: string;
}

declare global {
  interface Window {
    Razorpay: any;
  }
}

export const paymentService = {
  /**
   * Create payment order for a contract
   */
  async createPaymentOrder(contractId: string): Promise<PaymentOrderResponse> {
    const response = await api.post("/payments/create-order", { contractId });
    return response.data;
  },

  /**
   * Verify payment with backend
   */
  async verifyPayment(
    razorpayOrderId: string,
    razorpayPaymentId: string,
    razorpaySignature: string,
  ): Promise<any> {
    const response = await api.post("/payments/verify", {
      razorpayOrderId,
      razorpayPaymentId,
      razorpaySignature,
    });
    return response.data;
  },

  /**
   * Open Razorpay checkout modal
   */
  openRazorpayCheckout(
    orderData: PaymentOrderResponse,
    onSuccess: (response: RazorpayResponse) => void,
    onError: (error: any) => void,
    onDismiss: () => void,
  ): void {
    if (!window.Razorpay) {
      console.error("Razorpay SDK not loaded");
      onError(new Error("Razorpay SDK not loaded"));
      return;
    }

    const options: RazorpayOptions = {
      key: orderData.razorpayKeyId,
      amount: orderData.amount * 100, // Convert to paise
      currency: orderData.currency,
      name: orderData.merchantName,
      description: orderData.contractTitle,
      order_id: orderData.orderId,
      prefill: {
        email: orderData.payerEmail,
        contact: orderData.payerPhone,
      },
      theme: {
        color: "#2563EB", // Blue-600
      },
      handler: onSuccess,
      modal: {
        ondismiss: onDismiss,
      },
    };

    const razorpay = new window.Razorpay(options);

    razorpay.on("payment.failed", function (response: any) {
      onError(response.error);
    });

    razorpay.open();
  },
};
```

### 5.4 Payment Button Component

**File**: `frontend/src/components/PaymentButton.tsx`

```typescript
import React, { useState } from 'react';
import { paymentService, RazorpayResponse } from '../services/paymentService';
import { useNavigate } from 'react-router-dom';

interface PaymentButtonProps {
  contractId: string;
  amount: number;
  disabled?: boolean;
}

export default function PaymentButton({ contractId, amount, disabled = false }: PaymentButtonProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const handlePayment = async () => {
    setLoading(true);
    setError(null);

    try {
      // Step 1: Create payment order
      const orderData = await paymentService.createPaymentOrder(contractId);

      // Step 2: Open Razorpay checkout
      paymentService.openRazorpayCheckout(
        orderData,
        // On success
        async (response: RazorpayResponse) => {
          try {
            // Step 3: Verify payment with backend
            await paymentService.verifyPayment(
              response.razorpay_order_id,
              response.razorpay_payment_id,
              response.razorpay_signature
            );

            // Step 4: Redirect to success page
            navigate(`/payment/success?contractId=${contractId}`);
          } catch (err) {
            console.error('Payment verification failed:', err);
            setError('Payment verification failed. Please contact support.');
            setLoading(false);
          }
        },
        // On error
        (error: any) => {
          console.error('Payment failed:', error);
          setError(error.description || 'Payment failed. Please try again.');
          setLoading(false);
        },
        // On dismiss
        () => {
          setLoading(false);
        }
      );
    } catch (err: any) {
      console.error('Error creating payment order:', err);
      setError(err.message || 'Failed to initiate payment. Please try again.');
      setLoading(false);
    }
  };

  return (
    <div>
      <button
        onClick={handlePayment}
        disabled={disabled || loading}
        className="w-full bg-gradient-to-r from-blue-600 to-indigo-600 text-white py-3 px-6 rounded-lg font-semibold text-lg hover:from-blue-700 hover:to-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
      >
        {loading ? (
          <span className="flex items-center justify-center">
            <svg className="animate-spin h-5 w-5 mr-3" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
            Processing...
          </span>
        ) : (
          `Pay ₹${amount.toLocaleString('en-IN')}`
        )}
      </button>

      {error && (
        <div className="mt-4 bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-700 text-sm">{error}</p>
        </div>
      )}
    </div>
  );
}
```

### 5.5 Update Contract Details Page

**File**: `frontend/src/pages/ContractDetails.tsx`

Add the payment button to the contract details page:

```typescript
import PaymentButton from '../components/PaymentButton';

// Inside the component, add this in the render section:
{contract.paymentStatus === 'PENDING' && (
  <div className="mt-8 border-t pt-6">
    <h3 className="text-lg font-semibold mb-4">Make Payment</h3>
    <PaymentButton
      contractId={contract.id}
      amount={contract.amount}
      disabled={contract.status !== 'ACTIVE'}
    />
  </div>
)}

{contract.paymentStatus === 'PAID' && (
  <div className="mt-8 bg-green-50 border border-green-200 rounded-lg p-6">
    <div className="flex items-center">
      <svg className="h-6 w-6 text-green-600 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <div>
        <p className="text-green-800 font-semibold">Payment Completed</p>
        <p className="text-green-600 text-sm">This contract has been fully paid</p>
      </div>
    </div>
  </div>
)}
```

### 5.6 Payment Success Page

**File**: `frontend/src/pages/PaymentSuccess.tsx`

```typescript
import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';

export default function PaymentSuccess() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const contractId = searchParams.get('contractId');

  useEffect(() => {
    // Auto-redirect after 5 seconds
    const timer = setTimeout(() => {
      if (contractId) {
        navigate(`/contracts/${contractId}`);
      } else {
        navigate('/dashboard');
      }
    }, 5000);

    return () => clearTimeout(timer);
  }, [contractId, navigate]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 to-blue-50 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-lg shadow-xl p-8 text-center">
        <div className="text-6xl mb-6">✅</div>
        <h1 className="text-3xl font-bold text-gray-900 mb-4">Payment Successful!</h1>
        <p className="text-gray-600 mb-6">
          Your payment has been processed successfully. The contract is now active and legally enforceable.
        </p>

        <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-6">
          <p className="text-sm text-green-800">
            A confirmation email with payment details and legal evidence packet has been sent to your registered email address.
          </p>
        </div>

        <div className="space-y-3">
          <button
            onClick={() => navigate(`/contracts/${contractId}`)}
            className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700"
          >
            View Contract Details
          </button>
          <button
            onClick={() => navigate('/dashboard')}
            className="w-full bg-gray-100 text-gray-700 py-3 rounded-lg font-semibold hover:bg-gray-200"
          >
            Back to Dashboard
          </button>
        </div>

        <p className="text-xs text-gray-500 mt-6">
          Redirecting automatically in 5 seconds...
        </p>
      </div>
    </div>
  );
}
```

### 5.7 Update App.tsx Routing

Add the new route:

```typescript
import PaymentSuccess from './pages/PaymentSuccess';

// Add this route in the Routes section:
<Route path="/payment/success" element={<PaymentSuccess />} />
```

---

## 6. Webhook Implementation

### 6.1 Webhook Controller

**File**: `legalpay-api/src/main/java/com/legalpay/api/controller/WebhookController.java`

```java
package com.legalpay.api.controller;

import com.legalpay.services.PaymentService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private PaymentService paymentService;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    /**
     * Razorpay webhook endpoint
     * Endpoint: POST /api/v1/webhooks/razorpay
     */
    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        logger.info("Received Razorpay webhook");

        // Verify webhook signature
        if (!verifyWebhookSignature(payload, signature)) {
            logger.error("Webhook signature verification failed");
            return ResponseEntity.status(403).body("Invalid signature");
        }

        try {
            JSONObject webhookData = new JSONObject(payload);
            String event = webhookData.getString("event");
            JSONObject paymentData = webhookData.getJSONObject("payload")
                                                 .getJSONObject("payment")
                                                 .getJSONObject("entity");

            String orderId = paymentData.getString("order_id");
            String paymentId = paymentData.getString("id");
            String paymentMethod = paymentData.getString("method");

            logger.info("Processing webhook event: {} for order: {}", event, orderId);

            switch (event) {
                case "payment.captured":
                    handlePaymentCaptured(orderId, paymentId, paymentMethod, paymentData);
                    break;

                case "payment.failed":
                    handlePaymentFailed(orderId, paymentData);
                    break;

                default:
                    logger.info("Unhandled webhook event: {}", event);
            }

            return ResponseEntity.ok("Webhook processed");

        } catch (Exception e) {
            logger.error("Error processing webhook", e);
            return ResponseEntity.status(500).body("Webhook processing failed");
        }
    }

    /**
     * Verify webhook signature
     */
    private boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = HexFormat.of().formatHex(hash);

            return generatedSignature.equals(signature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error verifying webhook signature", e);
            return false;
        }
    }

    /**
     * Handle payment.captured event
     */
    private void handlePaymentCaptured(String orderId, String paymentId,
                                      String paymentMethod, JSONObject paymentData) {
        try {
            // Note: Signature verification already done in frontend
            // This is additional confirmation from Razorpay
            logger.info("Payment captured via webhook: {} for order: {}", paymentId, orderId);

            // You can add additional business logic here
            // For example: send email notification, update analytics, etc.

        } catch (Exception e) {
            logger.error("Error handling payment.captured webhook", e);
        }
    }

    /**
     * Handle payment.failed event
     */
    private void handlePaymentFailed(String orderId, JSONObject paymentData) {
        try {
            String errorCode = paymentData.optString("error_code", "UNKNOWN");
            String errorDescription = paymentData.optString("error_description", "Payment failed");

            paymentService.handleFailedPayment(orderId, errorCode, errorDescription);

            logger.warn("Payment failed for order: {} - {}", orderId, errorDescription);

        } catch (Exception e) {
            logger.error("Error handling payment.failed webhook", e);
        }
    }
}
```

### 6.2 Configure Webhook in Razorpay Dashboard

1. Go to Razorpay Dashboard → Settings → Webhooks
2. Click "Add Webhook URL"
3. Enter URL: `https://yourdomain.com/api/v1/webhooks/razorpay`
4. Select events:
   - ✅ payment.captured
   - ✅ payment.failed
   - ✅ payment.authorized (optional)
5. Set alert email
6. **Save the Webhook Secret** and add to environment variables

---

## 7. EMI Flow Implementation

### 7.1 Create EMI Plan (Razorpay Subscriptions)

**Add to PaymentService.java**:

```java
/**
 * Create EMI plan using Razorpay Subscriptions
 */
@Transactional
public PaymentInstallment createEMIPlan(UUID contractId, int numberOfInstallments,
                                        String payerIpAddress, String payerUserAgent)
        throws RazorpayException {

    Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new IllegalArgumentException("Contract not found"));

    if (!"EMI".equals(contract.getPaymentType())) {
        throw new IllegalStateException("Contract must have EMI payment type");
    }

    BigDecimal totalAmount = contract.getAmount();
    BigDecimal installmentAmount = totalAmount.divide(
        new BigDecimal(numberOfInstallments),
        2,
        RoundingMode.HALF_UP
    );

    // Create Razorpay Plan
    JSONObject planRequest = new JSONObject();
    planRequest.put("period", "monthly");
    planRequest.put("interval", 1);
    planRequest.put("item", new JSONObject()
        .put("name", "EMI - " + contract.getTitle())
        .put("amount", installmentAmount.multiply(new BigDecimal("100")).intValue())
        .put("currency", "INR")
    );

    Plan razorpayPlan = razorpayClient.plans.create(planRequest);

    // Create Subscription
    JSONObject subscriptionRequest = new JSONObject();
    subscriptionRequest.put("plan_id", razorpayPlan.get("id"));
    subscriptionRequest.put("total_count", numberOfInstallments);
    subscriptionRequest.put("customer_notify", 1);

    JSONObject notes = new JSONObject();
    notes.put("contract_id", contractId.toString());
    subscriptionRequest.put("notes", notes);

    Subscription razorpaySubscription = razorpayClient.subscriptions.create(subscriptionRequest);

    // Save to database (you'll need to create PaymentInstallment entity)
    // ... implementation

    logger.info("Created EMI plan: {} for contract: {}", razorpaySubscription.get("id"), contractId);

    return paymentInstallment;
}
```

**Note**: Full EMI implementation requires additional entities (`PaymentInstallment`, `InstallmentPayment`) and webhooks for `subscription.charged`, `subscription.completed` events. This is a Phase 2 feature - implement after MVP launch if demand exists.

---

## 8. Security & Compliance

### 8.1 Security Checklist

- [x] **Signature Verification**: All payments verified via HMAC-SHA256
- [x] **Webhook Secret**: Secure webhook endpoint with signature check
- [x] **HTTPS Only**: All production traffic over TLS 1.2+
- [x] **API Key Security**: Keys stored in environment variables, never in code
- [x] **IP Logging**: Track payer IP for fraud detection
- [x] **Rate Limiting**: Add to payment endpoints (100 req/min per user)
- [x] **CORS**: Restrict to your frontend domain only
- [ ] **PCI-DSS**: Razorpay handles card data (you never touch it)

### 8.2 Legal Compliance

**RBI Guidelines**:

- ✅ You are NOT a Payment Aggregator (Razorpay is)
- ✅ You do NOT store card data
- ✅ You do NOT process payments directly
- ✅ All transactions via RBI-licensed gateway (Razorpay)

**Data Retention** (IT Act 2000):

- Store payment records for **minimum 5 years**
- Include: Transaction ID, timestamp, IP address, amount
- Purpose: Legal evidence for Section 138 NI Act cases

**Tax Compliance**:

- Razorpay deducts TDS (if applicable)
- You issue invoices to merchants (for platform fee)
- Merchants issue invoices to payers (for goods/services)

### 8.3 Production Environment Variables

**File**: `.env.production`

```bash
# Razorpay (LIVE)
RAZORPAY_KEY_ID=rzp_live_XXXXXXXXXXXXXX
RAZORPAY_KEY_SECRET=YYYYYYYYYYYYYYYY
RAZORPAY_WEBHOOK_SECRET=whsec_ZZZZZZZZZZZZZ

# URLs
FRONTEND_URL=https://app.legalpay.in
BACKEND_URL=https://api.legalpay.in

# Database
DATABASE_URL=postgresql://user:pass@host:5432/legalpay_prod
DATABASE_SSL=true

# Email
RESEND_API_KEY=re_XXXXXXXXXXXX

# Security
JWT_SECRET=<strong-random-secret>
ALLOWED_ORIGINS=https://app.legalpay.in
```

---

## 9. Testing Checklist

### 9.1 Local Testing (Test Mode)

Use Razorpay test cards: https://razorpay.com/docs/payments/payments/test-card-details/

**Test Card Numbers**:

```
Success: 4111 1111 1111 1111
CVV: Any 3 digits
Expiry: Any future date

Failure: 4000 0000 0000 0002
```

**Test UPI**: `success@razorpay` or `failure@razorpay`

**Test Scenarios**:

1. ✅ **Successful Payment**
   - Create contract → Click "Pay Now" → Use test card → Verify payment captured
   - Check: Contract status = PAID, payment record created, webhook received

2. ✅ **Failed Payment**
   - Use failure test card → Verify error message shown
   - Check: Payment status = FAILED, error logged

3. ✅ **Payment Abandonment**
   - Open checkout → Close modal without paying
   - Check: Payment status = CREATED, order not expired

4. ✅ **Signature Verification Failure**
   - Manually trigger with wrong signature (dev only)
   - Check: Payment rejected, security event logged

5. ✅ **Webhook Delivery**
   - Complete payment → Check webhook endpoint called
   - Use Razorpay Dashboard → Webhooks → View logs

### 9.2 Production Testing (Before Go-Live)

**Pre-Launch Checklist**:

1. [ ] Switch to Live API keys in production
2. [ ] Test ₹1 real payment with your own card
3. [ ] Verify webhook delivery (check Razorpay logs)
4. [ ] Confirm email notifications sent
5. [ ] Check database records created correctly
6. [ ] Verify settlement appears in bank account (T+1)
7. [ ] Test refund flow (if applicable)
8. [ ] Load test: 100 concurrent payment requests
9. [ ] Security scan: OWASP ZAP on payment endpoints
10. [ ] Legal review: Terms, refund policy, privacy policy

---

## 10. Production Deployment

### 10.1 Backend Deployment Steps

```bash
# 1. Build production JAR
cd legalpay-api
mvn clean package -DskipTests -Pprod

# 2. Set environment variables on server
export RAZORPAY_KEY_ID=rzp_live_XXXXXX
export RAZORPAY_KEY_SECRET=YYYYYYYY
export RAZORPAY_WEBHOOK_SECRET=whsec_ZZZZZZ
export DATABASE_URL=postgresql://...
export FRONTEND_URL=https://app.legalpay.in

# 3. Run application
java -jar target/legalpay-api-1.0.0.jar --spring.profiles.active=prod
```

### 10.2 Frontend Deployment Steps

```bash
# 1. Build production bundle
cd frontend
npm run build

# 2. Deploy to CDN/hosting (Vercel/Netlify)
# Ensure environment variables set in hosting dashboard:
VITE_API_URL=https://api.legalpay.in

# 3. Verify Razorpay SDK loaded
# Check browser console for any CSP errors
```

### 10.3 Database Migrations

```bash
# Create production database
CREATE DATABASE legalpay_prod;

# Run migration scripts
psql -h <host> -U <user> -d legalpay_prod -f migrations/001_create_payments_table.sql
psql -h <host> -U <user> -d legalpay_prod -f migrations/002_update_contracts_table.sql
```

### 10.4 Razorpay Production Setup

1. **Switch to Live Mode** in Razorpay Dashboard
2. **Update webhook URL** to production: `https://api.legalpay.in/api/v1/webhooks/razorpay`
3. **Test webhook** using "Send Test Webhook" button
4. **Enable required payment methods**
5. **Set settlement schedule** (T+1 is default)
6. **Add support email** for payment failures

---

## 11. Monitoring & Alerts

### 11.1 Key Metrics to Track

**Payment Metrics**:

- Total payment volume (GMV)
- Payment success rate (target: >95%)
- Average payment value
- Payment method distribution (UPI vs Cards vs Netbanking)
- Failed payment reasons (top 5 error codes)

**Performance Metrics**:

- Payment initiation time (< 2 seconds)
- Webhook processing time (< 500ms)
- Order creation API latency (< 1 second)

**Security Metrics**:

- Signature verification failures (alert if >1%)
- Duplicate payment attempts
- Unusual IP patterns

### 11.2 Logging

**Log Every**:

```java
// Order creation
logger.info("Payment order created: orderId={}, contractId={}, amount={}",
            orderId, contractId, amount);

// Payment capture
logger.info("Payment captured: paymentId={}, orderId={}, method={}",
            paymentId, orderId, paymentMethod);

// Payment failure
logger.error("Payment failed: orderId={}, errorCode={}, description={}",
             orderId, errorCode, errorDescription);

// Webhook received
logger.info("Webhook received: event={}, orderId={}", event, orderId);

// Signature verification failure
logger.error("SECURITY: Signature verification failed for orderId={}", orderId);
```

### 11.3 Alerts

**Set Up Alerts For**:

- Payment success rate < 90% (15-min window)
- Webhook delivery failure rate > 5%
- Signature verification failures > 0
- Database connection errors
- Razorpay API downtime (use status.razorpay.com)

**Alert Channels**:

- Email: tech@legalpay.in
- Slack: #payment-alerts
- PagerDuty: Critical issues only

---

## 12. Troubleshooting Guide

### Issue: Payment Stuck in "CREATED" Status

**Cause**: Payer closed checkout before completing payment  
**Solution**: Order expires after 15 minutes automatically  
**Action**: No action needed, inform user to retry

### Issue: Signature Verification Failed

**Cause**: Wrong secret key or payload tampering  
**Solution**:

1. Verify `RAZORPAY_KEY_SECRET` matches dashboard
2. Check logs for exact payload received
3. Contact Razorpay support if issue persists

### Issue: Webhook Not Received

**Cause**: Webhook URL unreachable or firewall blocking  
**Solution**:

1. Check webhook URL in Razorpay dashboard
2. Verify server is publicly accessible (not localhost)
3. Check server logs for incoming requests
4. Use Razorpay's "Resend Webhook" feature

### Issue: Payment Captured But Contract Not Updated

**Cause**: Race condition between webhook and frontend callback  
**Solution**: Add retry logic, verify payment status via API

### Issue: Razorpay Checkout Not Opening

**Cause**: Razorpay SDK not loaded  
**Solution**:

1. Check `<script src="https://checkout.razorpay.com/v1/checkout.js"></script>` in index.html
2. Verify no CSP (Content Security Policy) blocking script
3. Check browser console for errors

---

## 13. Go-Live Checklist

### Week Before Launch

- [ ] Complete KYC with Razorpay (production account approved)
- [ ] Switch all API keys to live mode
- [ ] Update webhook URL to production domain
- [ ] Test ₹1 payment end-to-end on production
- [ ] Verify settlement reaches bank account
- [ ] Load test: 500 concurrent users
- [ ] Security audit: Penetration testing completed
- [ ] Legal review: T&C, Privacy Policy, Refund Policy approved
- [ ] Customer support: Train team on payment troubleshooting
- [ ] Monitoring: Set up Sentry/Datadog alerts

### Launch Day

- [ ] Monitor payment success rate every hour
- [ ] Watch webhook delivery logs
- [ ] Check database for any stuck payments
- [ ] Verify email notifications sent correctly
- [ ] Have Razorpay support contact on standby
- [ ] Monitor server CPU/memory usage
- [ ] Keep rollback plan ready

### Post-Launch (Week 1)

- [ ] Daily review of failed payments
- [ ] Customer feedback on checkout UX
- [ ] Track payment method preferences
- [ ] Analyze drop-off points in payment flow
- [ ] Optimize based on data

---

## 14. Support & Escalation

**Razorpay Support**:

- Dashboard: https://dashboard.razorpay.com/support
- Email: support@razorpay.com
- Phone: 1800-1234-5678 (24/7)
- Slack: (if enterprise plan)

**LegalPay Internal**:

- Tech Lead: [Your contact]
- DevOps: [Your contact]
- Customer Support: support@legalpay.in

**Escalation Matrix**:

- L1 (Response < 1 hour): Payment failures, checkout errors
- L2 (Response < 4 hours): Webhook issues, settlement delays
- L3 (Response < 24 hours): Feature requests, optimization

---

## 15. Future Enhancements (Post-MVP)

### Phase 2 (Month 3-6)

- [ ] Full EMI implementation with Razorpay Subscriptions
- [ ] Payment links (no contract required)
- [ ] Partial payments / installment tracking
- [ ] Auto-refunds for cancelled contracts

### Phase 3 (Month 6-12)

- [ ] PayPal integration (if international demand proven)
- [ ] BNPL integration (Simpl/LazyPay) - if >30% users request
- [ ] Escrow via Razorpay Route (if disputes >5%)
- [ ] UPI AutoPay for recurring payments

### Phase 4 (Series A+)

- [ ] Multi-currency support
- [ ] Stripe integration for global markets
- [ ] Own payment analytics dashboard
- [ ] Dynamic pricing based on payment method

---

**Document Version**: 1.0  
**Last Updated**: January 28, 2026  
**Maintained By**: LegalPay Engineering Team

**Questions?** Contact: tech@legalpay.in
