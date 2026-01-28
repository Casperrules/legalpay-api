# Product Requirements Document (PRD): LegalPay - Contract-to-Cash Automation Platform

**Version:** 1.0  
**Date:** 26 January 2026  
**Status:** MVP Definition  
**Author:** Senior Fintech Product Manager & Legal-Tech Specialist

---

## 1. Executive Summary

**LegalPay** bridges the gap between digital agreement execution and banking repayment. It creates a legally binding financial loop where a contract is eSigned, a mandate is registered, and payments are auto-deducted. If a deduction bounces, the system automatically creates a legally admissible "Digital Evidence Packet" ready for litigation under Section 25 of the PSS Act.

**The Core Loop:**

1.  **Contract Gen:** Commercial Agreement generated.
2.  **Signature & Mandate:** Counterparty eSigns via Aadhaar and authorizes eNACH/UPI Autopay in a single flow.
3.  **Execution:** Auto-Generate Invoice -> Auto-debit T-15 days or on due date.
4.  **Escalation:** On failure -> Automated Retry (T+10) -> Legal Notice Trigger.

---

## 2. Indian Legal Compliance Framework

### 2.1 Enforceability of eNACH Bounces (Section 25 PSS Act)

Traditionally, Section 138 of the Negotiable Instruments (NI) Act covers criminal liability for cheque bounces. For digital mandates, **Section 25 of the Payment and Settlement Systems Act, 2007** acts as the modern equivalent.

- **Requirement:** When an Electronic Funds Transfer (EFT) initiated by a periodic mandate fails due to "Insufficient Funds," it acts as a dishonour similar to a cheque bounce.
- **Legal Consequence:** Non-payment carries criminal liability (imprisonment up to 2 years or twice the amount fine).
- **System Design:** The Platform must generate a "Dishonour Memo" equivalent (Bank Return Code) which serves as primary evidence for filing a summary suit.
- **Statutory Timelines:** (a) Issue legal notice within 30 days of receiving the bank return memo; (b) Allow 15 days for payment post-notice; (c) If unpaid, file complaint within 30 days thereafter. The system will auto-track these deadlines and create reminders on D+0, D+25, and D+45.
- **Evidence Pack:** Store bank return code and memo, the auto-generated legal notice, and delivery proofs (email/SMS/WhatsApp delivery status and read receipts where available).
- **Electronic Evidence:** A Section 63(4) certificate is mandatory for electronic records; the Supreme Court in Arjun Panditrao Khotkar (2020) affirmed the need for such certification under the prior 65B regime. This PRD aligns with the comparable requirement under the 2023 Act.

### 2.2 Digital Contract Enforceability (Aadhaar eSign & IT Act, 2000)

To ensure the underlying debt is valid, the contract must be legally non-repudiable.

- **Mechanism:** We use **Aadhaar-based eSign** (backed by CDAC/NSDL).
- **Legal Standing:** Under the Information Technology Act, 2000 (Second Schedule), Aadhaar eSign is treated as equivalent to a "wet-ink" signature. It creates a presumption of validity that the signer cannot easily deny in court.
- **App Logic:** The PDF contract is hashed _before_ and _after_ signature to prevent tampering.

### 2.3 RBI Digital Lending Guidelines (Flow of Funds)

Strict adherence to RBI's Master Directions on Digital Lending (September 2022; as updated periodically) is mandatory.

- **Rule:** Repayments must flow directly from the Borrower’s bank account to the Regulated Entity (RE) / Lender’s bank account without passing through a third-party pool account. No FLDGs with unregulated entities.
- **Implementation:** The Platform acts as a Loan Service Provider (LSP). We invoke the payment gateway's "Split Settlement" or "Direct Settlement" APIs where the beneficiary VPA/Account is strictly the Lender's, ensuring zero touch on funds by our platform.

### 2.4 Data Residency & DPDP Act, 2023

- **Data Localization (RBI 2018 circular):** All payment data is stored only in India. Backups and logs reside in India regions.
- **DPDP Compliance:** Consent-based processing with clear purpose specification, retention controls, and deletion on request/expiry. Sensitive identifiers (Aadhaar/OTP) are never stored. PII is encrypted at rest (AES-256) and in transit (TLS 1.2+).
- **Section 63(4) Certificate:** An authorized officer issues and signs the electronic evidence certificate (Class 3 DSC), referencing file hashes, timestamps, storage pathway, and system process.

### 2.5 Stamp Duty & eStamping (Indian Stamp Act; State Schedules)

- **Duly Stamped Agreements:** Contracts underlying payment obligations must be duly stamped per the relevant State’s schedule. Unstamped/insufficiently stamped instruments are inadmissible in evidence until duty/penalty is paid.
- **eStamp Integration:** Integrate with eStamp providers (e.g., via Leegality/Digio) to affix state-appropriate duty at signing time; store the eStamp certificate number, date, state, and hash in the audit log.
- **Jurisdiction & Governing Law:** Contracts should include governing law (India) and forum selection/arbitration clause consistent with business needs.

---

## 3. Financial Workflow & MVP Features

### 3.1 Mandate Lifecycle: eNACH vs. UPI Autopay

The app will support a hybrid mandate model based on ticket size.

| Feature        | UPI Autopay                       | eNACH (Netbanking/Debit Card)        |
| :------------- | :-------------------------------- | :----------------------------------- |
| **Use Case**   | Ticket size < ₹15,000 (Recurring) | High Value / Corporate Loans         |
| **Setup Time** | Instant (Real-time approval)      | T+1 to T+2 days (NPCI Processing)    |
| **Max Limit**  | ₹1 Lakh (varies by bank/app)      | ₹1 Crore+ (Corporate), ₹10L (Retail) |
| **User Exp**   | PIN Authorization on phone        | Netbanking Login or Debit Card OTP   |

Operational considerations:

- Maintain a provider/bank limit registry (UPI Autopay caps vary by bank/category); fetch and refresh limits periodically.
- Display effective limit to users in real time; route high-ticket debits beyond UPI cap to eNACH automatically.
- Support mandate pause/cancel and amendment (amount/date) flows with full audit trail.
- **Revocation & Consent:** Provide an in-app “cancel mandate” option with clear TAT expectations and NPCI-compliant flow; retain explicit consent logs (device, IP, timestamp, OTP/PIN redaction) for mandate creation, changes, and revocation.

### 3.2 Smart Retry Logic ("Dunning")

A naive retry system causes excessive bank penalty charges. We implement a "Smart Dunning" algorithm.

- **Trigger:** On transaction failure with Bank Code `R03` (Insufficient Funds).
- **Logic:**
  1.  **Immediate Action:** Send Push Notification/WhatsApp/SMS to Payer notifying of bounce and Section 25 implications.
  2.  **Wait Period:** Wait for **T+10 days** (statistical probability of salary credit/fund replenishment).
  3.  **Retry:** Execute Payment Retry.
  4.  **Failure Event:** If Retry fails, mark status as `LC_DEFAULT` (Legal Case Default) and trigger the Legal Notice module.
- **Retry Policy Guardrails:** Max 1 automated retry at T+10 for insufficient funds. No retries for fatal codes (e.g., mandate cancelled/UMRN blocked/account closed). Maintain a return-code map to distinguish retriable vs non-retriable failures. Respect gateway/bank retry and daily attempt caps.

### 3.3 The Pre-Debit Rule (24-Hour Notification)

NPCI and RBI guidelines require a pre-debit notification for all renewals/subscriptions.

- **Requirement:** Notification must be sent at least **24 hours prior** to the execution timestamp.
- **Content:** Amount, Date of Debit, and Reference Number.
- **System Action:** The backend scheduler triggers notifications and logs the `Notification_Sent_Timestamp` with delivery status for auditability.
- **Notifications:** Optional T-48h reminder; mandatory T-24h pre-debit notification including amount, debit date/time, mandate reference/UMRN, opt-out/pause link (where supported), dispute link, and support contact.

### 3.4 Automated GST Invoicing Module

To facilitate professional billing and tax compliance, the app includes an automated invoice engine.

- **Logic:** Mapping Contract Payment Schedule -> Invoice Template.
- **Trigger:** Generated at **T-5 days** before the scheduled debit date.
- **Delivery:** Sent as a PDF attachment with the "Pre-Debit Notification" email/WhatsApp.
- **Compliance:**
  - Auto-population of **GSTIN**, **PAN**, and **HSN/SAC Codes**.
  - Sequential Invoice Numbering (e.g., `INV-2026-001`) unique to the Merchant.
  - "Original for Recipient" watermarking.
- **Tax Treatment:** Convenience/service fees are taxable; compute GST based on place of supply and merchant registration. Show GST breakup and total. Merchants may absorb or pass through convenience fee to payer as a separate line item.
- **E-Invoicing (IRP):** For merchants under mandatory e-invoicing thresholds, integrate with IRP to generate IRN/QR code; store IRN and acknowledgment in the audit record.
- **Value:** This converts the transaction from a simple "money transfer" to a formal "settlement of debt against valid invoice," strengthening the legal case in event of a dispute.

### 3.5 Dispute & Chargeback Handling

- **Dispute Intake:** Capture dispute reason codes and attachments; pause retries while in dispute.
- **Pack Generation:** Provide a dispute pack within 24h containing the signed contract hash, mandate consent event, pre-debit notifications, and transaction logs.
- **Resolution:** Support merchant resolution actions (refund/partial refund/retry defer) with audit trail.

### 3.6 Communications Compliance (TRAI DLT, WhatsApp BPA)

- **TRAI DLT (SMS):** Register headers, templates, and consent under DLT; send only via approved templates; store entity and template IDs in the audit log.
- **WhatsApp Business API:** Use user opt-in; send approved templates for pre-debit notices; log message IDs and delivery status; include opt-out/pause link where possible.
- **Email:** Implement DKIM/SPF/DMARC; preserve full headers for evidence of service.

---

## 4. Three-Way Blockchain Confirmation (The Evidence Layer)

To satisfy **Section 63(4) of the Bharatiya Sakshya Adhiniyam, 2023** (which replaces Section 65B of the Evidence Act), we must prove the electronic record has not been tampered with.

### 4.1 Implementation: The "Truth Triangle"

We store a cryptographic proof on a public ledger (e.g., Polygon/Ethereum) linking three entities:

1.  **The Contract:** `SHA-256(Signed_PDF_Content)`
2.  **The Mandate:** `Mandate_UMRN` (Unique Mandate Reference Number)
3.  **The Transaction:** `Bank_Txn_ID` + `Return_Code` (if bounced)

- **On-Chain Privacy:** Only hashes and opaque IDs are stored on-chain. No PII (names, PAN, Aadhaar, phone, addresses) or raw financial data is written to the chain. Raw evidence remains off-chain (S3 India region) encrypted with KMS, referenced by content hash.

### 4.2 The Audit Trail

- **On Execution:** A smart contract function `recordEvidence(contractHash, mandateID, paymentStatus)` is called.
- **On Dispute:** The system generates a "Certificate of Electronic Evidence".
- **Court Admissibility:** The certificate shows that the Hash of the PDF presented in court matches the Hash stored on the immutable blockchain at the time of agreement. This proves the file was created _then_ and has not been altered _since_.
- **Resilience:** If on-chain write fails (gas/network), queue evidence with a signed, timestamped off-chain record and replay when available. Persist block number and tx hash in the audit record for verification.

---

## 5. Revenue Model (Nominal Fee Structure)

This model aims for high volume, low margin, banking on "Convenience Fee" logic.

### 5.1 Pricing Strategy

- **Mandate Setup Fee:** **₹100** (One-time, paid by Creditor/Merchant; may be transparently passed to the payer as a "Compliance/Onboarding Fee").
- **Executive Fee:** **₹20** per successful deduction (configurable: payer pays or merchant absorbs; always disclosed as a separate line item on invoice with GST, where applicable).

### 5.2 Cost Analysis & Margin

| Component         | Estimated Cost (B2B API)              | Revenue              | Net Margin          |
| :---------------- | :------------------------------------ | :------------------- | :------------------ |
| **Aadhaar eSign** | ₹15 - ₹20 (Provider: Digio/Leegality) | Part of Setup (₹100) | ~₹70 Surplus        |
| **eNACH Reg.**    | ₹5 - ₹8 (Provider: NPCI/Sponsor Bank) | Part of Setup (₹100) | (Included above)    |
| **Transaction**   | ₹2 - ₹5 (Razorpay/Cashfree)           | ₹20 per txn          | **₹15 - ₹18 / txn** |

_Note: The setup surplus buffers against users who churn before the first payment._

**Sensitivity (Per-Transaction Margin):** ₹12–₹19 based on payment gateway tiered pricing, messaging costs, and failure rates. Target ≥ 65% gross margin at scale.

## 6. MVP Roadmap

### Phase 1: Core Integrations (Weeks 1-4)

Absolute minimum third-party tools required to function:

1.  **Payments & Mandates:**
    - **Razorpay** OR **Cashfree** (for Subscription API/Nach/UPI Autopay).
2.  **eSign Provider:**
    - **Leegality** OR **Digio** (Best for Aadhaar eSign + Stamp Duty integration if needed later).
3.  **Communication:**
    - **Twilio** or **Gupshup** (WhatsApp API for 24h Pre-debit alerts).
    - **PDF Generation:** `Puppeteer` or `jsPDF` for on-the-fly GST Invoice creation.
4.  **Security & Monitoring:**
    - HMAC webhook signature validation; idempotency keys on payment actions; structured logging with PII redaction; dashboards/alerts for retries and evidence writes.
5.  **Database:**
    - PostgreSQL (Transactional Data).
    - AWS S3 (Encrypted Contract Storage).

### Phase 2: Evidence Layer (Weeks 5-6)

1.  **Blockchain Integration:**
    - **Polygon (MATIC)** (Low gas fees, high speed).
    - Simple Smart Contract for `EvidenceRegistry`.

---

_Product Owner: [Adarsh Dubey]_
