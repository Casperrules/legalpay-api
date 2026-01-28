package com.legalpay.services.contract;

import com.legalpay.domain.entity.Contract;
import com.legalpay.domain.entity.Merchant;
import com.legalpay.domain.entity.Payer;
import com.legalpay.domain.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContractService
 * Pattern: Mockito for isolated unit testing
 */
@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PdfGenerationService pdfGenerationService;

    @InjectMocks
    private ContractService contractService;

    private Merchant testMerchant;
    private Payer testPayer;
    private ContractService.ContractRequest testRequest;

    @BeforeEach
    void setUp() {
        testMerchant = Merchant.builder()
                .id(UUID.randomUUID())
                .businessName("Test Merchant")
                .email("merchant@test.com")
                .build();

        testPayer = Payer.builder()
                .id(UUID.randomUUID())
                .name("Test Payer")
                .email("payer@test.com")
                .build();

        testRequest = ContractService.ContractRequest.builder()
                .principalAmount(new BigDecimal("100000"))
                .interestRate(new BigDecimal("12.0"))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(12))
                .paymentFrequency("MONTHLY")
                .build();
    }

    @Test
    void createContract_ShouldCreateContractInDraftState() {
        // Given
        String dummyPdfUrl = "https://storage.test.com/contract.pdf";
        when(pdfGenerationService.generateContractPdf(any(Contract.class)))
                .thenReturn(dummyPdfUrl);
        
        when(contractRepository.save(any(Contract.class)))
                .thenAnswer(invocation -> {
                    Contract contract = invocation.getArgument(0);
                    contract.setId(UUID.randomUUID());
                    return contract;
                });

        // When
        Contract result = contractService.createContract(testRequest, testMerchant, testPayer);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMerchant()).isEqualTo(testMerchant);
        assertThat(result.getPayer()).isEqualTo(testPayer);
        assertThat(result.getStatus()).isEqualTo(Contract.ContractStatus.DRAFT);
        assertThat(result.getPrincipalAmount()).isEqualTo(testRequest.getPrincipalAmount());
        assertThat(result.getPdfUrl()).isEqualTo(dummyPdfUrl);
        
        verify(pdfGenerationService, times(1)).generateContractPdf(any(Contract.class));
        verify(contractRepository, times(2)).save(any(Contract.class));
    }

    @Test
    void createContract_ShouldCalculateEMICorrectly() {
        // Given
        when(pdfGenerationService.generateContractPdf(any(Contract.class)))
                .thenReturn("dummy_url");
        when(contractRepository.save(any(Contract.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Contract result = contractService.createContract(testRequest, testMerchant, testPayer);

        // Then
        BigDecimal expectedEMI = new BigDecimal("8333.33"); // 100000 / 12 months
        assertThat(result.getEmiAmount()).isEqualByComparingTo(expectedEMI);
    }

    @Test
    void initiateESign_ShouldChangeStatusToPendingESign() {
        // Given
        Contract contract = Contract.builder()
                .id(UUID.randomUUID())
                .status(Contract.ContractStatus.DRAFT)
                .build();
        
        when(contractRepository.findById(contract.getId()))
                .thenReturn(java.util.Optional.of(contract));
        when(contractRepository.save(any(Contract.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Contract result = contractService.initiateESign(contract.getId());

        // Then
        assertThat(result.getStatus()).isEqualTo(Contract.ContractStatus.PENDING_ESIGN);
        assertThat(result.getESignDocumentId()).isNotNull();
        verify(contractRepository, times(1)).save(any(Contract.class));
    }

    @Test
    void initiateESign_ShouldThrowException_WhenContractNotInDraftState() {
        // Given
        Contract contract = Contract.builder()
                .id(UUID.randomUUID())
                .status(Contract.ContractStatus.SIGNED)
                .build();
        
        when(contractRepository.findById(contract.getId()))
                .thenReturn(java.util.Optional.of(contract));

        // When/Then
        assertThatThrownBy(() -> contractService.initiateESign(contract.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT state");
    }

    @Test
    void markAsSigned_ShouldUpdateContractStatus() {
        // Given
        Contract contract = Contract.builder()
                .id(UUID.randomUUID())
                .status(Contract.ContractStatus.PENDING_ESIGN)
                .build();
        
        String signedPdfUrl = "https://storage.test.com/signed.pdf";
        
        when(contractRepository.findById(contract.getId()))
                .thenReturn(java.util.Optional.of(contract));
        when(contractRepository.save(any(Contract.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Contract result = contractService.markAsSigned(contract.getId(), signedPdfUrl);

        // Then
        assertThat(result.getStatus()).isEqualTo(Contract.ContractStatus.SIGNED);
        assertThat(result.getSignedPdfUrl()).isEqualTo(signedPdfUrl);
        assertThat(result.getSignedAt()).isNotNull();
    }
}
