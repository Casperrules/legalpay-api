package com.legalpay.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mandates")
public class Mandate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false, unique = true)
    private Contract contract;

    @Column(nullable = false, length = 50)
    private String mandateType;

    @Column(length = 100)
    private String gatewayMandateId;

    @Column(precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @Column(length = 20)
    private String frequency;

    @Column(length = 20)
    private String bankAccountNumber;

    @Column(length = 11)
    private String bankIfsc;

    @Column(length = 255)
    private String bankAccountHolder;

    @Column(length = 100)
    private String upiId;

    @Column
    private Instant authorizedAt;

    @Column(length = 500)
    private String authorizationUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MandateStatus status = MandateStatus.CREATED;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public enum MandateStatus {
        CREATED, PENDING_AUTHORIZATION, AUTHORIZED, ACTIVE, PAUSED, REVOKED, EXPIRED
    }

    public Mandate() {}

    public static MandateBuilder builder() { return new MandateBuilder(); }

    public static class MandateBuilder {
        private UUID id; private Contract contract; private String mandateType;
        private String gatewayMandateId; private BigDecimal maxAmount; private String frequency;
        private String bankAccountNumber; private String bankIfsc; private String bankAccountHolder;
        private String upiId; private Instant authorizedAt; private String authorizationUrl;
        private MandateStatus status = MandateStatus.CREATED;
        private Instant createdAt; private Instant updatedAt;

        public MandateBuilder id(UUID id) { this.id = id; return this; }
        public MandateBuilder contract(Contract contract) { this.contract = contract; return this; }
        public MandateBuilder mandateType(String mandateType) { this.mandateType = mandateType; return this; }
        public MandateBuilder gatewayMandateId(String gatewayMandateId) { this.gatewayMandateId = gatewayMandateId; return this; }
        public MandateBuilder maxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; return this; }
        public MandateBuilder frequency(String frequency) { this.frequency = frequency; return this; }
        public MandateBuilder bankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; return this; }
        public MandateBuilder bankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; return this; }
        public MandateBuilder bankAccountHolder(String bankAccountHolder) { this.bankAccountHolder = bankAccountHolder; return this; }
        public MandateBuilder upiId(String upiId) { this.upiId = upiId; return this; }
        public MandateBuilder authorizedAt(Instant authorizedAt) { this.authorizedAt = authorizedAt; return this; }
        public MandateBuilder authorizationUrl(String authorizationUrl) { this.authorizationUrl = authorizationUrl; return this; }
        public MandateBuilder status(MandateStatus status) { this.status = status; return this; }
        public MandateBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public MandateBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        
        public Mandate build() {
            Mandate m = new Mandate();
            m.id = this.id; m.contract = this.contract; m.mandateType = this.mandateType;
            m.gatewayMandateId = this.gatewayMandateId; m.maxAmount = this.maxAmount; m.frequency = this.frequency;
            m.bankAccountNumber = this.bankAccountNumber; m.bankIfsc = this.bankIfsc; m.bankAccountHolder = this.bankAccountHolder;
            m.upiId = this.upiId; m.authorizedAt = this.authorizedAt; m.authorizationUrl = this.authorizationUrl;
            m.status = this.status; m.createdAt = this.createdAt; m.updatedAt = this.updatedAt;
            return m;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
    public String getMandateType() { return mandateType; }
    public void setMandateType(String mandateType) { this.mandateType = mandateType; }
    public String getGatewayMandateId() { return gatewayMandateId; }
    public void setGatewayMandateId(String gatewayMandateId) { this.gatewayMandateId = gatewayMandateId; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getBankIfsc() { return bankIfsc; }
    public void setBankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; }
    public String getBankAccountHolder() { return bankAccountHolder; }
    public void setBankAccountHolder(String bankAccountHolder) { this.bankAccountHolder = bankAccountHolder; }
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }
    public Instant getAuthorizedAt() { return authorizedAt; }
    public void setAuthorizedAt(Instant authorizedAt) { this.authorizedAt = authorizedAt; }
    public String getAuthorizationUrl() { return authorizationUrl; }
    public void setAuthorizationUrl(String authorizationUrl) { this.authorizationUrl = authorizationUrl; }
    public MandateStatus getStatus() { return status; }
    public void setStatus(MandateStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
