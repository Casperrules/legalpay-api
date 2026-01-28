package com.legalpay.services;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    
    @Value("${email.from:noreply@legalpay.in}")
    private String fromEmail;
    
    @Value("${resend.api.key:}")
    private String resendApiKey;
    
    @Value("${resend.enabled:false}")
    private boolean resendEnabled;
    
    public void sendVerificationEmail(String email, String name, String token, boolean isMerchant) {
        String verifyUrl = frontendUrl + "/verify-email?token=" + token;
        String userType = isMerchant ? "Merchant" : "Payer";
        String subject = "Verify Your LegalPay Account";
        
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button { background-color: #4F46E5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Welcome to LegalPay!</h2>
                    <p>Hi %s,</p>
                    <p>Thank you for registering with LegalPay as a <strong>%s</strong>!</p>
                    <p>Please verify your email address by clicking the button below:</p>
                    <a href="%s" class="button">Verify Email Address</a>
                    <p>Or copy this link: <a href="%s">%s</a></p>
                    <p>This link will expire in 24 hours.</p>
                    <p>If you didn't register for this account, please ignore this email.</p>
                    <div class="footer">
                        <p>Best regards,<br>LegalPay Team</p>
                        <p style="color: #999;">This is an automated email. Please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """, name, userType, verifyUrl, verifyUrl, verifyUrl);
        
        sendEmail(email, subject, htmlContent, verifyUrl);
    }
    
    public void sendPasswordResetEmail(String email, String name, String token, boolean isMerchant) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        String subject = "Reset Your LegalPay Password";
        
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button { background-color: #DC2626; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                    .warning { background-color: #FEF3C7; padding: 12px; border-radius: 6px; margin: 15px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Password Reset Request</h2>
                    <p>Hi %s,</p>
                    <p>We received a request to reset your password for your LegalPay account.</p>
                    <p>Click the button below to reset your password:</p>
                    <a href="%s" class="button">Reset Password</a>
                    <p>Or copy this link: <a href="%s">%s</a></p>
                    <div class="warning">
                        <strong>⚠️ Security Notice:</strong> This link will expire in 1 hour.
                    </div>
                    <p>If you didn't request a password reset, please ignore this email or contact support if you're concerned.</p>
                    <div class="footer">
                        <p>Best regards,<br>LegalPay Team</p>
                        <p style="color: #999;">This is an automated email. Please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """, name, resetUrl, resetUrl, resetUrl);
        
        sendEmail(email, subject, htmlContent, resetUrl);
    }
    
    public void sendWelcomeEmail(String email, String name, boolean isMerchant) {
        String userType = isMerchant ? "Merchant" : "Payer";
        String loginUrl = frontendUrl + "/login";
        String subject = "Welcome to LegalPay!";
        
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button { background-color: #10B981; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>🎉 Welcome to LegalPay!</h2>
                    <p>Hi %s,</p>
                    <p>Your email has been verified successfully. Welcome aboard!</p>
                    <p>You can now login to your <strong>%s</strong> account and start using LegalPay:</p>
                    <a href="%s" class="button">Login to Dashboard</a>
                    <div class="footer">
                        <p>Best regards,<br>LegalPay Team</p>
                        <p style="color: #999;">This is an automated email. Please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """, name, userType, loginUrl);
        
        sendEmail(email, subject, htmlContent, loginUrl);
    }
    
    private void sendEmail(String to, String subject, String htmlContent, String debugUrl) {
        if (resendEnabled && resendApiKey != null && !resendApiKey.isEmpty()) {
            try {
                Resend resend = new Resend(resendApiKey);
                
                CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(to)
                    .subject(subject)
                    .html(htmlContent)
                    .build();
                
                CreateEmailResponse response = resend.emails().send(params);
                log.info("Email sent successfully to {} - ID: {}", to, response.getId());
                
            } catch (ResendException e) {
                log.error("Failed to send email via Resend to {}: {}", to, e.getMessage());
                logEmailToConsole(to, subject, debugUrl, htmlContent);
            }
        } else {
            // Development mode - log to console
            log.warn("Resend disabled - logging email to console");
            logEmailToConsole(to, subject, debugUrl, htmlContent);
        }
    }
    
    private void logEmailToConsole(String to, String subject, String debugUrl, String htmlContent) {
        log.info("=".repeat(80));
        log.info("EMAIL TO: {}", to);
        log.info("SUBJECT: {}", subject);
        if (debugUrl != null) {
            log.info("ACTION URL: {}", debugUrl);
        }
        log.info("BODY (stripped HTML): {}", htmlContent.replaceAll("<[^>]*>", ""));
        log.info("=".repeat(80));
    }
}
