package com.legalpay.api.controller;

import com.legalpay.services.PaymentService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private PaymentService paymentService;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    /**
     * Razorpay webhook endpoint
     */
    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        
        logger.info("Received Razorpay webhook");

        // Verify webhook signature
        if (!verifyWebhookSignature(payload, signature)) {
            logger.error("Webhook signature verification failed");
            return ResponseEntity.status(403).body("Invalid signature");
        }

        try {
            JSONObject webhookData = new JSONObject(payload);
            String event = webhookData.getString("event");
            JSONObject paymentData = webhookData.getJSONObject("payload")
                                                 .getJSONObject("payment")
                                                 .getJSONObject("entity");

            String orderId = paymentData.getString("order_id");
            String paymentId = paymentData.getString("id");
            String paymentMethod = paymentData.getString("method");

            logger.info("Processing webhook event: {} for order: {}", event, orderId);

            switch (event) {
                case "payment.captured":
                    handlePaymentCaptured(orderId, paymentId, paymentMethod, paymentData);
                    break;
                    
                case "payment.failed":
                    handlePaymentFailed(orderId, paymentData);
                    break;
                    
                default:
                    logger.info("Unhandled webhook event: {}", event);
            }

            return ResponseEntity.ok("Webhook processed");

        } catch (Exception e) {
            logger.error("Error processing webhook", e);
            return ResponseEntity.status(500).body("Webhook processing failed");
        }
    }

    /**
     * Verify webhook signature
     */
    private boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = HexFormat.of().formatHex(hash);
            
            return generatedSignature.equals(signature);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error verifying webhook signature", e);
            return false;
        }
    }

    /**
     * Handle payment.captured event
     */
    private void handlePaymentCaptured(String orderId, String paymentId, 
                                      String paymentMethod, JSONObject paymentData) {
        try {
            logger.info("Payment captured via webhook: {} for order: {}", paymentId, orderId);
            // Additional logic can be added here (e.g., send email notification)
        } catch (Exception e) {
            logger.error("Error handling payment.captured webhook", e);
        }
    }

    /**
     * Handle payment.failed event
     */
    private void handlePaymentFailed(String orderId, JSONObject paymentData) {
        try {
            String errorCode = paymentData.optString("error_code", "UNKNOWN");
            String errorDescription = paymentData.optString("error_description", "Payment failed");
            
            paymentService.handleFailedPayment(orderId, errorCode, errorDescription);
            
            logger.warn("Payment failed for order: {} - {}", orderId, errorDescription);
            
        } catch (Exception e) {
            logger.error("Error handling payment.failed webhook", e);
        }
    }
}
