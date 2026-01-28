package com.legalpay.domain.repository;

import com.legalpay.domain.entity.RazorpayPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RazorpayPaymentRepository extends JpaRepository<RazorpayPayment, UUID> {
    
    Optional<RazorpayPayment> findByRazorpayOrderId(String razorpayOrderId);
    
    Optional<RazorpayPayment> findByRazorpayPaymentId(String razorpayPaymentId);
    
    List<RazorpayPayment> findByContractId(UUID contractId);
    
    List<RazorpayPayment> findByPayerId(UUID payerId);
    
    List<RazorpayPayment> findByMerchantId(UUID merchantId);
    
    List<RazorpayPayment> findByStatus(RazorpayPayment.PaymentStatus status);
}
