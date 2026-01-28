package com.legalpay.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpay.api.dto.ContractCreateRequest;
import com.legalpay.domain.entity.Contract;
import com.legalpay.domain.entity.Merchant;
import com.legalpay.domain.entity.Payer;
import com.legalpay.domain.repository.MerchantRepository;
import com.legalpay.domain.repository.PayerRepository;
import com.legalpay.services.contract.ContractService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ContractController
 * Pattern: MockMvc for REST API testing
 */
@WebMvcTest(ContractController.class)
class ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContractService contractService;

    @MockBean
    private MerchantRepository merchantRepository;

    @MockBean
    private PayerRepository payerRepository;

    @Test
    void createContract_ShouldReturn201_WhenValidRequest() throws Exception {
        // Given
        UUID merchantId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        
        ContractCreateRequest request = new ContractCreateRequest();
        request.setMerchantId(merchantId);
        request.setPayerId(payerId);
        request.setPrincipalAmount(new BigDecimal("100000"));
        request.setInterestRate(new BigDecimal("12.0"));
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusMonths(12));
        request.setPaymentFrequency("MONTHLY");

        Merchant merchant = Merchant.builder().id(merchantId).build();
        Payer payer = Payer.builder().id(payerId).build();
        
        Contract contract = Contract.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .payer(payer)
                .principalAmount(request.getPrincipalAmount())
                .status(Contract.ContractStatus.DRAFT)
                .build();

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(payerRepository.findById(payerId)).thenReturn(Optional.of(payer));
        when(contractService.createContract(any(), any(), any())).thenReturn(contract);

        // When/Then
        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.principalAmount").value(100000));
    }

    @Test
    void createContract_ShouldReturn400_WhenInvalidAmount() throws Exception {
        // Given
        ContractCreateRequest request = new ContractCreateRequest();
        request.setMerchantId(UUID.randomUUID());
        request.setPayerId(UUID.randomUUID());
        request.setPrincipalAmount(new BigDecimal("100")); // Too small
        request.setInterestRate(new BigDecimal("12.0"));
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusMonths(12));
        request.setPaymentFrequency("MONTHLY");

        // When/Then
        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getContract_ShouldReturn200_WhenContractExists() throws Exception {
        // Given
        UUID contractId = UUID.randomUUID();
        Contract contract = Contract.builder()
                .id(contractId)
                .merchant(Merchant.builder().id(UUID.randomUUID()).build())
                .payer(Payer.builder().id(UUID.randomUUID()).build())
                .principalAmount(new BigDecimal("100000"))
                .status(Contract.ContractStatus.DRAFT)
                .build();

        when(contractService.getContract(contractId)).thenReturn(contract);

        // When/Then
        mockMvc.perform(get("/api/v1/contracts/" + contractId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contractId.toString()));
    }
}
