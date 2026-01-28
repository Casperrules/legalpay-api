package com.legalpay.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contracts")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_id", nullable = false)
    private Payer payer;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentType paymentType = PaymentType.ONE_TIME;

    @Column(length = 20)
    private String paymentFrequency; // Only for EMI contracts

    @Column(precision = 15, scale = 2)
    private BigDecimal emiAmount; // Only for EMI contracts

    @Column(length = 500)
    private String pdfUrl;

    @Column(length = 64)
    private String sha256Hash;

    @Column(length = 100)
    private String eSignDocumentId;

    @Column
    private Instant signedAt;

    @Column(length = 500)
    private String signedPdfUrl;

    @Column(length = 66)
    private String blockchainTxHash;

    @Column
    private Instant blockchainRecordedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractStatus status = ContractStatus.DRAFT;

    @Column(length = 20)
    private String paymentStatus = "PENDING"; // PENDING, PARTIAL, PAID, FAILED, REFUNDED

    @Column(precision = 15, scale = 2)
    private BigDecimal totalPaidAmount = BigDecimal.ZERO;

    @Column
    private Instant lastPaymentAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public enum ContractStatus {
        DRAFT, PENDING_ESIGN, SIGNED, ACTIVE, COMPLETED, DEFAULTED, CANCELLED, LEGAL_NOTICE_SENT
    }

    public enum PaymentType {
        ONE_TIME,  // Single payment of full principal amount
        EMI        // Recurring payments (daily/weekly/monthly/quarterly)
    }

    public Contract() {}

    public static ContractBuilder builder() { return new ContractBuilder(); }

    public static class ContractBuilder {
        private UUID id; private Merchant merchant; private Payer payer;
        private BigDecimal principalAmount; private BigDecimal interestRate;
        private LocalDate startDate; private LocalDate endDate; 
        private PaymentType paymentType = PaymentType.ONE_TIME;
        private String paymentFrequency;
        private BigDecimal emiAmount; private String pdfUrl; private String sha256Hash;
        private String eSignDocumentId; private Instant signedAt; private String signedPdfUrl;
        private String blockchainTxHash; private Instant blockchainRecordedAt;
        private ContractStatus status = ContractStatus.DRAFT;
        private Instant createdAt; private Instant updatedAt;

        public ContractBuilder id(UUID id) { this.id = id; return this; }
        public ContractBuilder merchant(Merchant merchant) { this.merchant = merchant; return this; }
        public ContractBuilder payer(Payer payer) { this.payer = payer; return this; }
        public ContractBuilder principalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; return this; }
        public ContractBuilder interestRate(BigDecimal interestRate) { this.interestRate = interestRate; return this; }
        public ContractBuilder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public ContractBuilder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public ContractBuilder paymentType(PaymentType paymentType) { this.paymentType = paymentType; return this; }
        public ContractBuilder paymentFrequency(String paymentFrequency) { this.paymentFrequency = paymentFrequency; return this; }
        public ContractBuilder emiAmount(BigDecimal emiAmount) { this.emiAmount = emiAmount; return this; }
        public ContractBuilder pdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; return this; }
        public ContractBuilder sha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; return this; }
        public ContractBuilder eSignDocumentId(String eSignDocumentId) { this.eSignDocumentId = eSignDocumentId; return this; }
        public ContractBuilder signedAt(Instant signedAt) { this.signedAt = signedAt; return this; }
        public ContractBuilder signedPdfUrl(String signedPdfUrl) { this.signedPdfUrl = signedPdfUrl; return this; }
        public ContractBuilder blockchainTxHash(String blockchainTxHash) { this.blockchainTxHash = blockchainTxHash; return this; }
        public ContractBuilder blockchainRecordedAt(Instant blockchainRecordedAt) { this.blockchainRecordedAt = blockchainRecordedAt; return this; }
        public ContractBuilder status(ContractStatus status) { this.status = status; return this; }
        public ContractBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public ContractBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        
        public Contract build() {
            Contract c = new Contract();
            c.id = this.id; c.merchant = this.merchant; c.payer = this.payer;
            c.principalAmount = this.principalAmount; c.interestRate = this.interestRate;
            c.startDate = this.startDate; c.endDate = this.endDate; 
            c.paymentType = this.paymentType != null ? this.paymentType : PaymentType.ONE_TIME;
            c.paymentFrequency = this.paymentFrequency;
            c.emiAmount = this.emiAmount; c.pdfUrl = this.pdfUrl; c.sha256Hash = this.sha256Hash;
            c.eSignDocumentId = this.eSignDocumentId; c.signedAt = this.signedAt; c.signedPdfUrl = this.signedPdfUrl;
            c.blockchainTxHash = this.blockchainTxHash; c.blockchainRecordedAt = this.blockchainRecordedAt;
            c.status = this.status; c.createdAt = this.createdAt; c.updatedAt = this.updatedAt;
            return c;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Merchant getMerchant() { return merchant; }
    public void setMerchant(Merchant merchant) { this.merchant = merchant; }
    public Payer getPayer() { return payer; }
    public void setPayer(Payer payer) { this.payer = payer; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }
    public String getPaymentFrequency() { return paymentFrequency; }
    public void setPaymentFrequency(String paymentFrequency) { this.paymentFrequency = paymentFrequency; }
    public BigDecimal getEmiAmount() { return emiAmount; }
    public void setEmiAmount(BigDecimal emiAmount) { this.emiAmount = emiAmount; }
    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }
    public String getESignDocumentId() { return eSignDocumentId; }
    public void setESignDocumentId(String eSignDocumentId) { this.eSignDocumentId = eSignDocumentId; }
    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant signedAt) { this.signedAt = signedAt; }
    public String getSignedPdfUrl() { return signedPdfUrl; }
    public void setSignedPdfUrl(String signedPdfUrl) { this.signedPdfUrl = signedPdfUrl; }
    public String getBlockchainTxHash() { return blockchainTxHash; }
    public void setBlockchainTxHash(String blockchainTxHash) { this.blockchainTxHash = blockchainTxHash; }
    public Instant getBlockchainRecordedAt() { return blockchainRecordedAt; }
    public void setBlockchainRecordedAt(Instant blockchainRecordedAt) { this.blockchainRecordedAt = blockchainRecordedAt; }
    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { this.status = status; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public BigDecimal getTotalPaidAmount() { return totalPaidAmount; }
    public void setTotalPaidAmount(BigDecimal totalPaidAmount) { this.totalPaidAmount = totalPaidAmount; }
    public Instant getLastPaymentAt() { return lastPaymentAt; }
    public void setLastPaymentAt(Instant lastPaymentAt) { this.lastPaymentAt = lastPaymentAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
