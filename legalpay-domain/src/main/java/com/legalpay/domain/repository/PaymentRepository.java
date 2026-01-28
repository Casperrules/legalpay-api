package com.legalpay.domain.repository;

import com.legalpay.domain.entity.Payment;
import com.legalpay.domain.entity.Mandate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    List<Payment> findByMandateAndStatus(Mandate mandate, Payment.PaymentStatus status);
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.nextRetryAt <= CURRENT_TIMESTAMP")
    List<Payment> findPaymentsDueForRetry();
    
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    
    List<Payment> findByDueDateBetween(LocalDate start, LocalDate end);
}
