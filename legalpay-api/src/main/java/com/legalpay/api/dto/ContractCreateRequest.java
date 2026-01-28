package com.legalpay.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a contract
 */
public class ContractCreateRequest {

    @NotNull(message = "Merchant ID is required")
    private UUID merchantId;

    @NotNull(message = "Payer ID is required")
    private UUID payerId;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "1000.00", message = "Principal amount must be at least ₹1000")
    @DecimalMax(value = "10000000.00", message = "Principal amount cannot exceed ₹1 crore")
    private BigDecimal principalAmount;

    @DecimalMin(value = "0.00", message = "Interest rate cannot be negative")
    @DecimalMax(value = "36.00", message = "Interest rate cannot exceed 36% (legal limit)")
    private BigDecimal interestRate;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in future")
    private LocalDate endDate;

    @NotBlank(message = "Payment type is required")
    @Pattern(regexp = "ONE_TIME|EMI", message = "Payment type must be ONE_TIME or EMI")
    private String paymentType;

    @Pattern(regexp = "DAILY|WEEKLY|MONTHLY|QUARTERLY", message = "Invalid payment frequency")
    private String paymentFrequency; // Required only for EMI contracts

    public ContractCreateRequest() {}

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
}
