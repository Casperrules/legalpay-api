package com.legalpay.services.payment;

import com.legalpay.domain.entity.Mandate;
import com.legalpay.domain.entity.Payment;
import com.legalpay.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payment Service - Orchestrates payment execution
 * Pattern: Service Layer with State Management
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayService paymentGatewayService;

    public PaymentService(PaymentRepository paymentRepository, PaymentGatewayService paymentGatewayService) {
        this.paymentRepository = paymentRepository;
        this.paymentGatewayService = paymentGatewayService;
    }

    /**
     * Schedule a payment for a mandate
     */
    @Transactional
    public Payment schedulePayment(Mandate mandate, LocalDate dueDate) {
        log.info("Scheduling payment for mandate {} due on {}", mandate.getId(), dueDate);

        Payment payment = Payment.builder()
                .mandate(mandate)
                .dueDate(dueDate)
                .amount(mandate.getMaxAmount())
                .status(Payment.PaymentStatus.SCHEDULED)
                .idempotencyKey(generateIdempotencyKey(mandate.getId(), dueDate))
                .build();

        return paymentRepository.save(payment);
    }

    /**
     * Execute a scheduled payment
     */
    @Transactional
    public Payment executePayment(UUID paymentId) {
        Payment payment = getPayment(paymentId);

        if (payment.getStatus() != Payment.PaymentStatus.SCHEDULED) {
            throw new IllegalStateException("Payment must be SCHEDULED to execute");
        }

        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        payment = paymentRepository.save(payment);

        try {
            // DUMMY: Execute via payment gateway
            PaymentGatewayService.PaymentResult result = paymentGatewayService.executePayment(
                    payment.getMandate().getGatewayMandateId(),
                    payment.getAmount().toString(),
                    payment.getIdempotencyKey()
            );

            if (result.isSuccess()) {
                payment.setStatus(Payment.PaymentStatus.SUCCESS);
                payment.setGatewayPaymentId(result.getGatewayPaymentId());
                payment.setExecutedAt(Instant.now());
                log.info("Payment {} executed successfully", paymentId);
            } else {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setReturnCode(result.getReturnCode());
                payment.setRetryCount(payment.getRetryCount() + 1);
                payment.setNextRetryAt(calculateNextRetry(payment.getRetryCount()));
                log.warn("Payment {} failed: {}", paymentId, result.getMessage());
            }

        } catch (Exception e) {
            log.error("Error executing payment " + paymentId, e);
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setRetryCount(payment.getRetryCount() + 1);
        }

        return paymentRepository.save(payment);
    }

    /**
     * Get payment by ID
     */
    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    /**
     * Generate idempotency key for retry safety
     */
    private String generateIdempotencyKey(UUID mandateId, LocalDate dueDate) {
        return String.format("payment_%s_%s", mandateId, dueDate);
    }

    /**
     * Calculate next retry time (exponential backoff)
     */
    private Instant calculateNextRetry(int retryCount) {
        // Retry after: 1 day, 3 days, 7 days
        long daysToAdd = switch (retryCount) {
            case 1 -> 1;
            case 2 -> 3;
            default -> 7;
        };
        
        return Instant.now().plus(java.time.Duration.ofDays(daysToAdd));
    }
}
