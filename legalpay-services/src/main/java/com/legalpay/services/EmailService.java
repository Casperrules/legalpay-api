package com.legalpay.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Value("${spring.mail.username:noreply@legalpay.in}")
    private String fromEmail;
    
    public void sendVerificationEmail(String email, String name, String token, boolean isMerchant) {
        String verifyUrl = frontendUrl + "/verify-email?token=" + token;
        String userType = isMerchant ? "Merchant" : "Payer";
        
        String message = String.format("""
            Hi %s,
            
            Thank you for registering with LegalPay as a %s!
            
            Please verify your email address by clicking the link below:
            %s
            
            This link will expire in 24 hours.
            
            If you didn't register for this account, please ignore this email.
            
            Best regards,
            LegalPay Team
            """, name, userType, verifyUrl);
        
        // In development, just log the email
        System.out.println("=".repeat(80));
        System.out.println("EMAIL TO: " + email);
        System.out.println("SUBJECT: Verify Your LegalPay Account");
        System.out.println("VERIFICATION URL: " + verifyUrl);
        System.out.println(message);
        System.out.println("=".repeat(80));
        
        // TODO: In production, integrate with Resend or SMTP
    }
    
    public void sendPasswordResetEmail(String email, String name, String token, boolean isMerchant) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        
        String message = String.format("""
            Hi %s,
            
            We received a request to reset your password for your LegalPay account.
            
            Click the link below to reset your password:
            %s
            
            This link will expire in 1 hour.
            
            If you didn't request a password reset, please ignore this email.
            
            Best regards,
            LegalPay Team
            """, name, resetUrl);
        
        // In development, just log the email
        System.out.println("=".repeat(80));
        System.out.println("EMAIL TO: " + email);
        System.out.println("SUBJECT: Reset Your LegalPay Password");
        System.out.println("RESET URL: " + resetUrl);
        System.out.println(message);
        System.out.println("=".repeat(80));
        
        // TODO: In production, integrate with Resend or SMTP
    }
    
    public void sendWelcomeEmail(String email, String name, boolean isMerchant) {
        String userType = isMerchant ? "Merchant" : "Payer";
        String loginUrl = frontendUrl + "/login";
        
        String message = String.format("""
            Hi %s,
            
            Welcome to LegalPay! Your email has been verified successfully.
            
            You can now login to your %s account:
            %s
            
            Best regards,
            LegalPay Team
            """, name, userType, loginUrl);
        
        System.out.println("=".repeat(80));
        System.out.println("EMAIL TO: " + email);
        System.out.println("SUBJECT: Welcome to LegalPay!");
        System.out.println(message);
        System.out.println("=".repeat(80));
    }
}
