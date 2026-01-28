package com.legalpay.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mandate_id", nullable = false)
    private Mandate mandate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(precision = 15, scale = 2)
    private BigDecimal lateFee;

    @Column(length = 100)
    private String gatewayPaymentId;

    @Column(length = 100)
    private String idempotencyKey;

    @Column
    private Instant executedAt;

    @Column(length = 500)
    private String gatewayResponse;

    @Column
    private Integer retryCount = 0;

    @Column
    private Instant nextRetryAt;

    @Column(length = 10)
    private String returnCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.SCHEDULED;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public enum PaymentStatus {
        SCHEDULED, PROCESSING, SUCCESS, FAILED, PENDING, CANCELLED
    }

    public Payment() {}

    public static PaymentBuilder builder() { return new PaymentBuilder(); }

    public static class PaymentBuilder {
        private UUID id; private Mandate mandate; private LocalDate dueDate;
        private BigDecimal amount; private BigDecimal lateFee;
        private String gatewayPaymentId; private String idempotencyKey;
        private Instant executedAt; private String gatewayResponse;
        private Integer retryCount = 0; private Instant nextRetryAt; private String returnCode;
        private PaymentStatus status = PaymentStatus.SCHEDULED;
        private Instant createdAt; private Instant updatedAt;

        public PaymentBuilder id(UUID id) { this.id = id; return this; }
        public PaymentBuilder mandate(Mandate mandate) { this.mandate = mandate; return this; }
        public PaymentBuilder dueDate(LocalDate dueDate) { this.dueDate = dueDate; return this; }
        public PaymentBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public PaymentBuilder lateFee(BigDecimal lateFee) { this.lateFee = lateFee; return this; }
        public PaymentBuilder gatewayPaymentId(String gatewayPaymentId) { this.gatewayPaymentId = gatewayPaymentId; return this; }
        public PaymentBuilder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public PaymentBuilder executedAt(Instant executedAt) { this.executedAt = executedAt; return this; }
        public PaymentBuilder gatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; return this; }
        public PaymentBuilder retryCount(Integer retryCount) { this.retryCount = retryCount; return this; }
        public PaymentBuilder nextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; return this; }
        public PaymentBuilder returnCode(String returnCode) { this.returnCode = returnCode; return this; }
        public PaymentBuilder status(PaymentStatus status) { this.status = status; return this; }
        public PaymentBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public PaymentBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        
        public Payment build() {
            Payment p = new Payment();
            p.id = this.id; p.mandate = this.mandate; p.dueDate = this.dueDate;
            p.amount = this.amount; p.lateFee = this.lateFee;
            p.gatewayPaymentId = this.gatewayPaymentId; p.idempotencyKey = this.idempotencyKey;
            p.executedAt = this.executedAt; p.gatewayResponse = this.gatewayResponse;
            p.retryCount = this.retryCount; p.nextRetryAt = this.nextRetryAt; p.returnCode = this.returnCode;
            p.status = this.status; p.createdAt = this.createdAt; p.updatedAt = this.updatedAt;
            return p;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Mandate getMandate() { return mandate; }
    public void setMandate(Mandate mandate) { this.mandate = mandate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getLateFee() { return lateFee; }
    public void setLateFee(BigDecimal lateFee) { this.lateFee = lateFee; }
    public String getGatewayPaymentId() { return gatewayPaymentId; }
    public void setGatewayPaymentId(String gatewayPaymentId) { this.gatewayPaymentId = gatewayPaymentId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }
    public String getGatewayResponse() { return gatewayResponse; }
    public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public String getReturnCode() { return returnCode; }
    public void setReturnCode(String returnCode) { this.returnCode = returnCode; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
