package com.legalpay.api.dto;

import com.legalpay.domain.entity.Contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for Contract
 */
public class ContractResponse {

    private UUID id;
    private UUID merchantId;
    private UUID payerId;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String paymentType;
    private String paymentFrequency;
    private BigDecimal emiAmount;
    private String pdfUrl;
    private String status;
    private String eSignDocumentId;
    private Instant createdAt;

    public ContractResponse() {}

    public ContractResponse(UUID id, UUID merchantId, UUID payerId, BigDecimal principalAmount, BigDecimal interestRate, LocalDate startDate, LocalDate endDate, String paymentType, String paymentFrequency, BigDecimal emiAmount, String pdfUrl, String status, String eSignDocumentId, Instant createdAt) {
        this.id = id;
        this.merchantId = merchantId;
        this.payerId = payerId;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.paymentType = paymentType;
        this.paymentFrequency = paymentFrequency;
        this.emiAmount = emiAmount;
        this.pdfUrl = pdfUrl;
        this.status = status;
        this.eSignDocumentId = eSignDocumentId;
        this.createdAt = createdAt;
    }

    public static ContractResponseBuilder builder() {
        return new ContractResponseBuilder();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
    public UUID getPayerId() { return payerId; }
    public void setPayerId(UUID payerId) { this.payerId = payerId; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public String getPaymentFrequency() { return paymentFrequency; }
    public void setPaymentFrequency(String paymentFrequency) { this.paymentFrequency = paymentFrequency; }
    public BigDecimal getEmiAmount() { return emiAmount; }
    public void setEmiAmount(BigDecimal emiAmount) { this.emiAmount = emiAmount; }
    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getESignDocumentId() { return eSignDocumentId; }
    public void setESignDocumentId(String eSignDocumentId) { this.eSignDocumentId = eSignDocumentId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static class ContractResponseBuilder {
        private UUID id;
        private UUID merchantId;
        private UUID payerId;
        private BigDecimal principalAmount;
        private BigDecimal interestRate;
        private LocalDate startDate;
        private LocalDate endDate;
        private String paymentType;
        private String paymentFrequency;
        private BigDecimal emiAmount;
        private String pdfUrl;
        private String status;
        private String eSignDocumentId;
        private Instant createdAt;

        public ContractResponseBuilder id(UUID id) { this.id = id; return this; }
        public ContractResponseBuilder merchantId(UUID merchantId) { this.merchantId = merchantId; return this; }
        public ContractResponseBuilder payerId(UUID payerId) { this.payerId = payerId; return this; }
        public ContractResponseBuilder principalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; return this; }
        public ContractResponseBuilder interestRate(BigDecimal interestRate) { this.interestRate = interestRate; return this; }
        public ContractResponseBuilder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
        public ContractResponseBuilder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
        public ContractResponseBuilder paymentType(String paymentType) { this.paymentType = paymentType; return this; }
        public ContractResponseBuilder paymentFrequency(String paymentFrequency) { this.paymentFrequency = paymentFrequency; return this; }
        public ContractResponseBuilder emiAmount(BigDecimal emiAmount) { this.emiAmount = emiAmount; return this; }
        public ContractResponseBuilder pdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; return this; }
        public ContractResponseBuilder status(String status) { this.status = status; return this; }
        public ContractResponseBuilder eSignDocumentId(String eSignDocumentId) { this.eSignDocumentId = eSignDocumentId; return this; }
        public ContractResponseBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public ContractResponse build() { return new ContractResponse(id, merchantId, payerId, principalAmount, interestRate, startDate, endDate, paymentType, paymentFrequency, emiAmount, pdfUrl, status, eSignDocumentId, createdAt); }
    }

    public static ContractResponse from(Contract contract) {
        return ContractResponse.builder()
                .id(contract.getId())
                .merchantId(contract.getMerchant().getId())
                .payerId(contract.getPayer().getId())
                .principalAmount(contract.getPrincipalAmount())
                .interestRate(contract.getInterestRate())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .paymentType(contract.getPaymentType() != null ? contract.getPaymentType().name() : null)
                .paymentFrequency(contract.getPaymentFrequency())
                .emiAmount(contract.getEmiAmount())
                .pdfUrl(contract.getPdfUrl())
                .status(contract.getStatus().name())
                .eSignDocumentId(contract.getESignDocumentId())
                .createdAt(contract.getCreatedAt())
                .build();
    }
}
