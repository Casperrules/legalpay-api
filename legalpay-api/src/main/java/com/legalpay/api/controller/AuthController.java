package com.legalpay.api.controller;

import com.legalpay.api.dto.*;
import com.legalpay.api.security.JwtUtil;
import com.legalpay.domain.entity.Merchant;
import com.legalpay.domain.entity.Payer;
import com.legalpay.domain.repository.MerchantRepository;
import com.legalpay.domain.repository.PayerRepository;
import com.legalpay.services.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final MerchantRepository merchantRepository;
    private final PayerRepository payerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthController(AuthenticationManager authenticationManager, 
                         UserDetailsService userDetailsService, 
                         JwtUtil jwtUtil, 
                         MerchantRepository merchantRepository, 
                         PayerRepository payerRepository,
                         PasswordEncoder passwordEncoder,
                         EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.merchantRepository = merchantRepository;
        this.payerRepository = payerRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        
        // Determine role and ID
        String role = "UNKNOWN";
        String id = "";
        String name = "";
        
        Optional<Merchant> merchant = merchantRepository.findByEmail(request.getEmail());
        if (merchant.isPresent()) {
            role = "MERCHANT";
            id = merchant.get().getId().toString();
            name = merchant.get().getBusinessName();
        } else {
            Optional<Payer> payer = payerRepository.findByEmail(request.getEmail());
            if (payer.isPresent()) {
                role = "PAYER";
                id = payer.get().getId().toString();
                name = payer.get().getName();
            }
        }
        
        final String jwt = jwtUtil.generateToken(userDetails, id, role);

        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwt)
                .role(role)
                .username(userDetails.getUsername())
                .name(name)
                .id(id)
                .build());
    }
    
    @PostMapping("/register/merchant")
    public ResponseEntity<?> registerMerchant(@RequestBody MerchantRegistrationRequest request) {
        // Check if email already exists
        if (merchantRepository.findByEmail(request.getEmail()).isPresent() || 
            payerRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Email already registered"));
        }
        
        // Create merchant
        Merchant merchant = new Merchant();
        merchant.setBusinessName(request.getBusinessName());
        merchant.setEmail(request.getEmail());
        merchant.setPhone(request.getPhoneNumber());
        merchant.setPassword(passwordEncoder.encode(request.getPassword()));
        merchant.setGstin(request.getGstNumber());
        merchant.setPan(request.getPanNumber());
        merchant.setEmailVerified(false);
        merchant.setKycVerified(false);
        
        // Generate verification token
        String token = UUID.randomUUID().toString();
        merchant.setVerificationToken(token);
        merchant.setVerificationTokenExpiry(Instant.now().plus(24, ChronoUnit.HOURS));
        
        merchantRepository.save(merchant);
        
        // Send verification email
        emailService.sendVerificationEmail(merchant.getEmail(), merchant.getBusinessName(), token, true);
        
        return ResponseEntity.ok(new MessageResponse("Registration successful! Please check your email to verify your account."));
    }
    
    @PostMapping("/register/payer")
    public ResponseEntity<?> registerPayer(@RequestBody PayerRegistrationRequest request) {
        // Check if email already exists
        if (merchantRepository.findByEmail(request.getEmail()).isPresent() || 
            payerRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Email already registered"));
        }
        
        // Create payer
        Payer payer = new Payer();
        payer.setName(request.getFullName());
        payer.setEmail(request.getEmail());
        payer.setPhone(request.getPhoneNumber());
        payer.setPassword(passwordEncoder.encode(request.getPassword()));
        payer.setEmailVerified(false);
        payer.setKycVerified(false);
        
        // Generate verification token
        String token = UUID.randomUUID().toString();
        payer.setVerificationToken(token);
        payer.setVerificationTokenExpiry(Instant.now().plus(24, ChronoUnit.HOURS));
        
        payerRepository.save(payer);
        
        // Send verification email
        emailService.sendVerificationEmail(payer.getEmail(), payer.getName(), token, false);
        
        return ResponseEntity.ok(new MessageResponse("Registration successful! Please check your email to verify your account."));
    }
    
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        // Try merchant first
        Optional<Merchant> merchantOpt = merchantRepository.findByVerificationToken(token);
        if (merchantOpt.isPresent()) {
            Merchant merchant = merchantOpt.get();
            
            if (merchant.getVerificationTokenExpiry().isBefore(Instant.now())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Verification link has expired"));
            }
            
            merchant.setEmailVerified(true);
            merchant.setVerificationToken(null);
            merchant.setVerificationTokenExpiry(null);
            merchantRepository.save(merchant);
            
            emailService.sendWelcomeEmail(merchant.getEmail(), merchant.getBusinessName(), true);
            
            return ResponseEntity.ok(new MessageResponse("Email verified successfully! You can now login."));
        }
        
        // Try payer
        Optional<Payer> payerOpt = payerRepository.findByVerificationToken(token);
        if (payerOpt.isPresent()) {
            Payer payer = payerOpt.get();
            
            if (payer.getVerificationTokenExpiry().isBefore(Instant.now())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Verification link has expired"));
            }
            
            payer.setEmailVerified(true);
            payer.setVerificationToken(null);
            payer.setVerificationTokenExpiry(null);
            payerRepository.save(payer);
            
            emailService.sendWelcomeEmail(payer.getEmail(), payer.getName(), false);
            
            return ResponseEntity.ok(new MessageResponse("Email verified successfully! You can now login."));
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("Invalid verification token"));
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody PasswordResetRequest request) {
        // Try merchant first
        Optional<Merchant> merchantOpt = merchantRepository.findByEmail(request.getEmail());
        if (merchantOpt.isPresent()) {
            Merchant merchant = merchantOpt.get();
            String token = UUID.randomUUID().toString();
            merchant.setPasswordResetToken(token);
            merchant.setPasswordResetTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
            merchantRepository.save(merchant);
            
            emailService.sendPasswordResetEmail(merchant.getEmail(), merchant.getBusinessName(), token, true);
            return ResponseEntity.ok(new MessageResponse("Password reset link sent to your email"));
        }
        
        // Try payer
        Optional<Payer> payerOpt = payerRepository.findByEmail(request.getEmail());
        if (payerOpt.isPresent()) {
            Payer payer = payerOpt.get();
            String token = UUID.randomUUID().toString();
            payer.setPasswordResetToken(token);
            payer.setPasswordResetTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
            payerRepository.save(payer);
            
            emailService.sendPasswordResetEmail(payer.getEmail(), payer.getName(), token, false);
            return ResponseEntity.ok(new MessageResponse("Password reset link sent to your email"));
        }
        
        // Return success even if email not found (security best practice)
        return ResponseEntity.ok(new MessageResponse("If that email exists, a password reset link has been sent"));
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetConfirmRequest request) {
        // Try merchant first
        Optional<Merchant> merchantOpt = merchantRepository.findByPasswordResetToken(request.getToken());
        if (merchantOpt.isPresent()) {
            Merchant merchant = merchantOpt.get();
            
            if (merchant.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Reset link has expired"));
            }
            
            merchant.setPassword(passwordEncoder.encode(request.getNewPassword()));
            merchant.setPasswordResetToken(null);
            merchant.setPasswordResetTokenExpiry(null);
            merchantRepository.save(merchant);
            
            return ResponseEntity.ok(new MessageResponse("Password reset successfully! You can now login."));
        }
        
        // Try payer
        Optional<Payer> payerOpt = payerRepository.findByPasswordResetToken(request.getToken());
        if (payerOpt.isPresent()) {
            Payer payer = payerOpt.get();
            
            if (payer.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Reset link has expired"));
            }
            
            payer.setPassword(passwordEncoder.encode(request.getNewPassword()));
            payer.setPasswordResetToken(null);
            payer.setPasswordResetTokenExpiry(null);
            payerRepository.save(payer);
            
            return ResponseEntity.ok(new MessageResponse("Password reset successfully! You can now login."));
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("Invalid reset token"));
    }
    
    // Helper classes
    public static class MessageResponse {
        private String message;
        
        public MessageResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
    
    public static class ErrorResponse {
        private String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
    }
}
