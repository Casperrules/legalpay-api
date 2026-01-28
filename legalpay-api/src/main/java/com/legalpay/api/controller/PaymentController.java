package com.legalpay.api.controller;

import com.legalpay.api.dto.CreatePaymentRequest;
import com.legalpay.api.dto.PaymentOrderResponse;
import com.legalpay.api.dto.VerifyPaymentRequest;
import com.legalpay.domain.entity.Contract;
import com.legalpay.domain.entity.RazorpayPayment;
import com.legalpay.domain.repository.ContractRepository;
import com.legalpay.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ContractRepository contractRepository;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    /**
     * Create payment order for a contract
     */
    @PostMapping("/create-order")
    @PreAuthorize("hasAnyRole('PAYER', 'MERCHANT')")
    public ResponseEntity<?> createPaymentOrder(
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            RazorpayPayment payment = paymentService.createPaymentOrder(
                request.getContractId(), 
                ipAddress, 
                userAgent
            );
            
            Contract contract = payment.getContract();
            
            PaymentOrderResponse response = new PaymentOrderResponse(
                payment.getRazorpayOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                razorpayKeyId,
                "Payment for Contract",
                contract.getMerchant().getBusinessName(),
                contract.getPayer().getEmail(),
                contract.getPayer().getPhone()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Verify payment after Razorpay callback
     */
    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('PAYER', 'MERCHANT')")
    public ResponseEntity<?> verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        
        try {
            RazorpayPayment payment = paymentService.capturePayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature(),
                "online"
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Payment verified successfully");
            response.put("paymentId", payment.getId());
            response.put("contractId", payment.getContract().getId());
            response.put("status", payment.getStatus());
            
            return ResponseEntity.ok(response);
            
        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Payment verification failed");
            return ResponseEntity.status(403).body(error);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Helper method to get client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
