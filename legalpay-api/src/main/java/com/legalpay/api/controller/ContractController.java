package com.legalpay.api.controller;

import com.legalpay.api.dto.ContractCreateRequest;
import com.legalpay.api.dto.ContractResponse;
import com.legalpay.domain.entity.Contract;
import com.legalpay.domain.entity.Merchant;
import com.legalpay.domain.entity.Payer;
import com.legalpay.domain.repository.MerchantRepository;
import com.legalpay.domain.repository.PayerRepository;
import com.legalpay.services.contract.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Contract API Controller
 * Follows REST best practices and OpenAPI documentation
 */
@RestController
@RequestMapping("/api/v1/contracts")
@Tag(name = "Contracts", description = "Contract management APIs")
public class ContractController {

    private static final Logger log = LoggerFactory.getLogger(ContractController.class);

    private final ContractService contractService;
    private final MerchantRepository merchantRepository;
    private final PayerRepository payerRepository;

    public ContractController(ContractService contractService, MerchantRepository merchantRepository, PayerRepository payerRepository) {
        this.contractService = contractService;
        this.merchantRepository = merchantRepository;
        this.payerRepository = payerRepository;
    }

    @PostMapping
    @Operation(summary = "Create a new contract")
    public ResponseEntity<ContractResponse> createContract(
            @Valid @RequestBody ContractCreateRequest request
    ) {
        log.info("Creating contract: {}", request);

        // DUMMY: Get merchant and payer (in production, use @AuthenticationPrincipal)
        Merchant merchant = merchantRepository.findById(request.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Merchant not found"));
        
        Payer payer = payerRepository.findById(request.getPayerId())
                .orElseThrow(() -> new RuntimeException("Payer not found"));

        ContractService.ContractRequest serviceRequest = ContractService.ContractRequest.builder()
                .principalAmount(request.getPrincipalAmount())
                .interestRate(request.getInterestRate())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .paymentType(request.getPaymentType())
                .paymentFrequency(request.getPaymentFrequency())
                .build();

        Contract contract = contractService.createContract(serviceRequest, merchant, payer);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ContractResponse.from(contract));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contract by ID")
    public ResponseEntity<ContractResponse> getContract(@PathVariable UUID id) {
        Contract contract = contractService.getContract(id);
        return ResponseEntity.ok(ContractResponse.from(contract));
    }

    @PostMapping("/{id}/esign")
    @Operation(summary = "Initiate eSign process")
    public ResponseEntity<ContractResponse> initiateESign(@PathVariable UUID id) {
        Contract contract = contractService.initiateESign(id);
        return ResponseEntity.ok(ContractResponse.from(contract));
    }

    @GetMapping
    @Operation(summary = "List contracts")
    public ResponseEntity<Page<ContractResponse>> listContracts(
            @RequestParam(required = false) UUID merchantId,
            @RequestParam(required = false) UUID payerId,
            Pageable pageable
    ) {
        if (merchantId == null && payerId == null) {
            throw new IllegalArgumentException("Either merchantId or payerId must be provided");
        }
        
        Page<Contract> contracts;
        if (merchantId != null) {
            Merchant merchant = merchantRepository.findById(merchantId)
                    .orElseThrow(() -> new RuntimeException("Merchant not found"));
            contracts = contractService.listContracts(merchant, pageable);
        } else {
            Payer payer = payerRepository.findById(payerId)
                    .orElseThrow(() -> new RuntimeException("Payer not found"));
            contracts = contractService.listContractsByPayer(payer, pageable);
        }
        
        return ResponseEntity.ok(contracts.map(ContractResponse::from));
    }
}
