package com.legalpay.services.contract;

import com.legalpay.domain.entity.BlockchainAuditLog.EventType;
import com.legalpay.domain.entity.Contract;
import com.legalpay.domain.entity.Merchant;
import com.legalpay.domain.entity.Payer;
import com.legalpay.domain.repository.ContractRepository;
import com.legalpay.services.blockchain.BlockchainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Contract Service - Core business logic for contract management
 * Pattern: Service Layer with Transaction Management
 */
@Service
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);

    private final ContractRepository contractRepository;
    private final PdfGenerationService pdfGenerationService;
    private final BlockchainService blockchainService;

    public ContractService(ContractRepository contractRepository, 
                          PdfGenerationService pdfGenerationService,
                          BlockchainService blockchainService) {
        this.contractRepository = contractRepository;
        this.pdfGenerationService = pdfGenerationService;
        this.blockchainService = blockchainService;
    }

    /**
     * Create a new contract in DRAFT state
     */
    @Transactional
    public Contract createContract(ContractRequest request, Merchant merchant, Payer payer) {
        log.info("Creating {} contract for merchant={} payer={}", 
                request.getPaymentType(), merchant.getId(), payer.getId());

        // Validate EMI contracts have required fields
        if ("EMI".equals(request.getPaymentType()) && request.getPaymentFrequency() == null) {
            throw new IllegalArgumentException("Payment frequency is required for EMI contracts");
        }

        // Determine payment type
        Contract.PaymentType paymentType = "EMI".equals(request.getPaymentType()) 
                ? Contract.PaymentType.EMI 
                : Contract.PaymentType.ONE_TIME;

        // Calculate payment amount based on type
        BigDecimal paymentAmount;
        if (paymentType == Contract.PaymentType.ONE_TIME) {
            paymentAmount = request.getPrincipalAmount(); // Full amount
        } else {
            paymentAmount = calculateEMI(request); // EMI amount
        }

        Contract contract = Contract.builder()
                .merchant(merchant)
                .payer(payer)
                .principalAmount(request.getPrincipalAmount())
                .interestRate(request.getInterestRate())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .paymentType(paymentType)
                .paymentFrequency(request.getPaymentFrequency())
                .emiAmount(paymentAmount)
                .status(Contract.ContractStatus.DRAFT)
                .build();

        contract = contractRepository.save(contract);
        
        // Generate PDF (async in production, sync for demo)
        String pdfUrl = pdfGenerationService.generateContractPdf(contract);
        contract.setPdfUrl(pdfUrl);
        contract.setSha256Hash(calculateHash(pdfUrl));
        
        contract = contractRepository.save(contract);
        
        // Log to blockchain for immutable audit trail
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contractId", contract.getId().toString());
        metadata.put("merchantId", merchant.getId().toString());
        metadata.put("payerId", payer.getId().toString());
        metadata.put("amount", contract.getPrincipalAmount().toString());
        metadata.put("paymentType", contract.getPaymentType().name());
        metadata.put("status", contract.getStatus().name());
        
        blockchainService.logEventAsync(
            EventType.CONTRACT_CREATED,
            contract.getId(),
            "Contract",
            merchant.getId(),
            metadata
        );
        
        return contract;
    }

    /**
     * Initiate eSign process
     */
    @Transactional
    public Contract initiateESign(UUID contractId) {
        Contract contract = getContract(contractId);
        
        if (contract.getStatus() != Contract.ContractStatus.DRAFT) {
            throw new IllegalStateException("Contract must be in DRAFT state to initiate eSign");
        }

        // DUMMY: In production, call Digio/Leegality API
        String dummyESignDocId = "esign_" + UUID.randomUUID();
        contract.setESignDocumentId(dummyESignDocId);
        contract.setStatus(Contract.ContractStatus.PENDING_ESIGN);
        
        log.info("Contract {} sent for eSign. DocumentId={}", contractId, dummyESignDocId);
        
        return contractRepository.save(contract);
    }

    /**
     * Mark contract as signed (called by webhook)
     */
    @Transactional
    public Contract markAsSigned(UUID contractId, String signedPdfUrl) {
        Contract contract = getContract(contractId);
        contract.setStatus(Contract.ContractStatus.SIGNED);
        contract.setSignedPdfUrl(signedPdfUrl);
        contract.setSignedAt(java.time.Instant.now());
        
        log.info("Contract {} marked as signed", contractId);
        
        contract = contractRepository.save(contract);
        
        // Log signing to blockchain
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contractId", contractId.toString());
        metadata.put("signedAt", contract.getSignedAt().toString());
        metadata.put("signedPdfUrl", signedPdfUrl);
        metadata.put("sha256Hash", contract.getSha256Hash());
        
        blockchainService.logEventAsync(
            EventType.CONTRACT_SIGNED,
            contractId,
            "Contract",
            contract.getPayer().getId(),
            metadata
        );
        
        return contract;
    }

    /**
     * Get contract by ID
     */
    public Contract getContract(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found: " + contractId));
    }

    /**
     * List contracts for a merchant
     */
    public Page<Contract> listContracts(Merchant merchant, Pageable pageable) {
        return contractRepository.findByMerchant(merchant, pageable);
    }

    /**
     * List contracts for a payer
     */
    public Page<Contract> listContractsByPayer(Payer payer, Pageable pageable) {
        return contractRepository.findByPayer(payer, pageable);
    }

    /**
     * Calculate EMI using reducing balance method
     */
    private BigDecimal calculateEMI(ContractRequest request) {
        // Simple EMI calculation: P * r * (1+r)^n / ((1+r)^n - 1)
        // For demo, just divide principal by number of payments
        long months = java.time.temporal.ChronoUnit.MONTHS.between(
                request.getStartDate(), 
                request.getEndDate()
        );
        
        if (months <= 0) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        
        return request.getPrincipalAmount().divide(
                BigDecimal.valueOf(months), 
                2, 
                BigDecimal.ROUND_HALF_UP
        );
    }

    /**
     * Calculate SHA-256 hash of PDF
     */
    private String calculateHash(String pdfUrl) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pdfUrl.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error calculating hash", e);
            return "dummy_hash";
        }
    }

    /**
     * Request DTO for contract creation
     */
    public static class ContractRequest {
        private BigDecimal principalAmount;
        private BigDecimal interestRate;
        private LocalDate startDate;
        private LocalDate endDate;
        private String paymentType;
        private String paymentFrequency;

        public ContractRequest() {}

        public ContractRequest(BigDecimal principalAmount, BigDecimal interestRate, LocalDate startDate, LocalDate endDate, String paymentType, String paymentFrequency) {
            this.principalAmount = principalAmount;
            this.interestRate = interestRate;
            this.startDate = startDate;
            this.endDate = endDate;
            this.paymentType = paymentType;
            this.paymentFrequency = paymentFrequency;
        }

        public static ContractRequestBuilder builder() {
            return new ContractRequestBuilder();
        }

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

        public static class ContractRequestBuilder {
            private BigDecimal principalAmount;
            private BigDecimal interestRate;
            private LocalDate startDate;
            private LocalDate endDate;
            private String paymentType;
            private String paymentFrequency;

            public ContractRequestBuilder principalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; return this; }
            public ContractRequestBuilder interestRate(BigDecimal interestRate) { this.interestRate = interestRate; return this; }
            public ContractRequestBuilder startDate(LocalDate startDate) { this.startDate = startDate; return this; }
            public ContractRequestBuilder endDate(LocalDate endDate) { this.endDate = endDate; return this; }
            public ContractRequestBuilder paymentType(String paymentType) { this.paymentType = paymentType; return this; }
            public ContractRequestBuilder paymentFrequency(String paymentFrequency) { this.paymentFrequency = paymentFrequency; return this; }
            public ContractRequest build() { return new ContractRequest(principalAmount, interestRate, startDate, endDate, paymentType, paymentFrequency); }
        }
    }
}
