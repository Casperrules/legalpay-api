package com.legalpay.services;

import com.legalpay.domain.entity.BlockchainAuditLog.EventType;
import com.legalpay.domain.entity.Contract;
import com.legalpay.domain.entity.RazorpayPayment;
import com.legalpay.domain.repository.ContractRepository;
import com.legalpay.domain.repository.RazorpayPaymentRepository;
import com.legalpay.services.blockchain.BlockchainService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private RazorpayPaymentRepository razorpayPaymentRepository;

    @Autowired
    private ContractRepository contractRepository;
    
    @Autowired
    private BlockchainService blockchainService;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    /**
     * Create a Razorpay order for a contract
     */
    @Transactional
    public RazorpayPayment createPaymentOrder(UUID contractId, String payerIpAddress, String payerUserAgent) 
            throws RazorpayException {
        
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found"));

        // Validate contract status
        if (!"ACTIVE".equals(contract.getStatus().name()) && !"SIGNED".equals(contract.getStatus().name())) {
            throw new IllegalStateException("Contract must be ACTIVE or SIGNED to create payment");
        }

        // Calculate amount in paise (Razorpay requires smallest currency unit)
        BigDecimal amountInRupees = contract.getPrincipalAmount();
        int amountInPaise = amountInRupees.multiply(new BigDecimal("100")).intValue();

        // Create Razorpay order
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "contract_" + contractId.toString());
        
        JSONObject notes = new JSONObject();
        notes.put("contract_id", contractId.toString());
        notes.put("merchant_id", contract.getMerchant().getId().toString());
        notes.put("payer_id", contract.getPayer().getId().toString());
        orderRequest.put("notes", notes);

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);
        
        logger.info("Created Razorpay order: {} for contract: {}", 
                    razorpayOrder.get("id"), contractId);

        // Create payment record
        RazorpayPayment payment = new RazorpayPayment();
        payment.setContract(contract);
        payment.setPayer(contract.getPayer());
        payment.setMerchant(contract.getMerchant());
        payment.setRazorpayOrderId(razorpayOrder.get("id"));
        payment.setAmount(amountInRupees);
        payment.setCurrency("INR");
        payment.setStatus(RazorpayPayment.PaymentStatus.CREATED);
        payment.setPayerIpAddress(payerIpAddress);
        payment.setPayerUserAgent(payerUserAgent);

        return razorpayPaymentRepository.save(payment);
    }

    /**
     * Verify payment signature from Razorpay
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                razorpayKeySecret.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = HexFormat.of().formatHex(hash);
            
            boolean isValid = generatedSignature.equals(signature);
            
            logger.info("Payment signature verification for order {}: {}", orderId, isValid);
            
            return isValid;
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error verifying payment signature", e);
            return false;
        }
    }

    /**
     * Update payment status after verification
     */
    @Transactional
    public RazorpayPayment capturePayment(String orderId, String paymentId, String signature, 
                                  String paymentMethod) {
        
        RazorpayPayment payment = razorpayPaymentRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found"));

        // Verify signature
        if (!verifyPaymentSignature(orderId, paymentId, signature)) {
            payment.setStatus(RazorpayPayment.PaymentStatus.FAILED);
            payment.setFailedAt(Instant.now());
            payment.setErrorCode("SIGNATURE_VERIFICATION_FAILED");
            payment.setErrorDescription("Payment signature verification failed");
            razorpayPaymentRepository.save(payment);
            
            logger.error("Payment signature verification failed for order: {}", orderId);
            throw new SecurityException("Payment signature verification failed");
        }

        // Update payment
        payment.setRazorpayPaymentId(paymentId);
        payment.setRazorpaySignature(signature);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(RazorpayPayment.PaymentStatus.CAPTURED);
        payment.setCapturedAt(Instant.now());
        
        RazorpayPayment savedPayment = razorpayPaymentRepository.save(payment);

        // Update contract status
        Contract contract = payment.getContract();
        contract.setPaymentStatus("PAID");
        contract.setTotalPaidAmount(payment.getAmount());
        contract.setLastPaymentAt(Instant.now());
        contractRepository.save(contract);

        logger.info("Payment captured successfully: {} for contract: {}", 
                    paymentId, contract.getId());
        
        // Log payment to blockchain
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("paymentId", savedPayment.getId().toString());
        metadata.put("razorpayPaymentId", paymentId);
        metadata.put("razorpayOrderId", orderId);
        metadata.put("contractId", contract.getId().toString());
        metadata.put("amount", payment.getAmount().toString());
        metadata.put("currency", payment.getCurrency());
        metadata.put("paymentMethod", paymentMethod);
        metadata.put("status", "CAPTURED");
        
        blockchainService.logEventAsync(
            EventType.PAYMENT_COMPLETED,
            contract.getId(),
            "Payment",
            contract.getPayer().getId(),
            metadata
        );

        return savedPayment;
    }

    /**
     * Handle failed payment
     */
    @Transactional
    public void handleFailedPayment(String orderId, String errorCode, String errorDescription) {
        
        razorpayPaymentRepository.findByRazorpayOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(RazorpayPayment.PaymentStatus.FAILED);
            payment.setFailedAt(Instant.now());
            payment.setErrorCode(errorCode);
            payment.setErrorDescription(errorDescription);
            razorpayPaymentRepository.save(payment);
            
            logger.warn("Payment failed for order: {} - Code: {}, Description: {}", 
                        orderId, errorCode, errorDescription);
            
            // Log failure to blockchain
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("paymentId", payment.getId().toString());
            metadata.put("razorpayOrderId", orderId);
            metadata.put("contractId", payment.getContract().getId().toString());
            metadata.put("amount", payment.getAmount().toString());
            metadata.put("errorCode", errorCode);
            metadata.put("errorDescription", errorDescription);
            metadata.put("status", "FAILED");
            
            blockchainService.logEventAsync(
                EventType.PAYMENT_FAILED,
                payment.getContract().getId(),
                "Payment",
                payment.getPayer().getId(),
                metadata
            );
        });
    }
}
