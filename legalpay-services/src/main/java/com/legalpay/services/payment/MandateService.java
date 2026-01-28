package com.legalpay.services.payment;

import com.legalpay.domain.entity.Contract;
import com.legalpay.domain.entity.Mandate;
import com.legalpay.domain.repository.MandateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Mandate Service - Manages eNACH/UPI Autopay mandates
 * Pattern: Service Layer with Gateway Abstraction
 */
@Service
public class MandateService {

    private static final Logger log = LoggerFactory.getLogger(MandateService.class);

    private final MandateRepository mandateRepository;
    private final PaymentGatewayService paymentGatewayService;

    public MandateService(MandateRepository mandateRepository, PaymentGatewayService paymentGatewayService) {
        this.mandateRepository = mandateRepository;
        this.paymentGatewayService = paymentGatewayService;
    }

    /**
     * Create a mandate for a signed contract
     */
    @Transactional
    public Mandate createMandate(Contract contract, MandateRequest request) {
        if (contract.getStatus() != Contract.ContractStatus.SIGNED) {
            throw new IllegalStateException("Contract must be signed before creating mandate");
        }

        log.info("Creating mandate for contract {}", contract.getId());

        // DUMMY: Create mandate via payment gateway
        String gatewayMandateId = paymentGatewayService.createMandate(contract, request);

        Mandate mandate = Mandate.builder()
                .contract(contract)
                .mandateType(request.getMandateType())
                .gatewayMandateId(gatewayMandateId)
                .maxAmount(contract.getEmiAmount())
                .frequency(contract.getPaymentFrequency())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankIfsc(request.getBankIfsc())
                .bankAccountHolder(request.getBankAccountHolder())
                .upiId(request.getUpiId())
                .status(Mandate.MandateStatus.PENDING_AUTHORIZATION)
                .build();

        mandate = mandateRepository.save(mandate);
        
        // DUMMY: Get authorization URL
        String authUrl = paymentGatewayService.getMandateAuthUrl(gatewayMandateId);
        mandate.setAuthorizationUrl(authUrl);
        
        log.info("Mandate created with ID={}, authUrl={}", mandate.getId(), authUrl);
        
        return mandateRepository.save(mandate);
    }

    /**
     * Mark mandate as authorized (called by webhook)
     */
    @Transactional
    public Mandate markAsAuthorized(UUID mandateId) {
        Mandate mandate = getMandate(mandateId);
        mandate.setStatus(Mandate.MandateStatus.ACTIVE);
        mandate.setAuthorizedAt(java.time.Instant.now());
        
        log.info("Mandate {} authorized", mandateId);
        
        return mandateRepository.save(mandate);
    }

    /**
     * Get mandate by ID
     */
    public Mandate getMandate(UUID mandateId) {
        return mandateRepository.findById(mandateId)
                .orElseThrow(() -> new RuntimeException("Mandate not found: " + mandateId));
    }

    /**
     * Request DTO for mandate creation
     */
    public static class MandateRequest {
        private String mandateType; // ENACH or UPI_AUTOPAY
        private String bankAccountNumber;
        private String bankIfsc;
        private String bankAccountHolder;
        private String upiId; // For UPI Autopay

        public MandateRequest() {}

        public MandateRequest(String mandateType, String bankAccountNumber, String bankIfsc, String bankAccountHolder, String upiId) {
            this.mandateType = mandateType;
            this.bankAccountNumber = bankAccountNumber;
            this.bankIfsc = bankIfsc;
            this.bankAccountHolder = bankAccountHolder;
            this.upiId = upiId;
        }

        public static MandateRequestBuilder builder() {
            return new MandateRequestBuilder();
        }

        public String getMandateType() { return mandateType; }
        public void setMandateType(String mandateType) { this.mandateType = mandateType; }
        public String getBankAccountNumber() { return bankAccountNumber; }
        public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
        public String getBankIfsc() { return bankIfsc; }
        public void setBankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; }
        public String getBankAccountHolder() { return bankAccountHolder; }
        public void setBankAccountHolder(String bankAccountHolder) { this.bankAccountHolder = bankAccountHolder; }
        public String getUpiId() { return upiId; }
        public void setUpiId(String upiId) { this.upiId = upiId; }

        public static class MandateRequestBuilder {
            private String mandateType;
            private String bankAccountNumber;
            private String bankIfsc;
            private String bankAccountHolder;
            private String upiId;

            public MandateRequestBuilder mandateType(String mandateType) { this.mandateType = mandateType; return this; }
            public MandateRequestBuilder bankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; return this; }
            public MandateRequestBuilder bankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; return this; }
            public MandateRequestBuilder bankAccountHolder(String bankAccountHolder) { this.bankAccountHolder = bankAccountHolder; return this; }
            public MandateRequestBuilder upiId(String upiId) { this.upiId = upiId; return this; }
            public MandateRequest build() { return new MandateRequest(mandateType, bankAccountNumber, bankIfsc, bankAccountHolder, upiId); }
        }
    }
}
