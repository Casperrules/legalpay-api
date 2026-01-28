package com.legalpay.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String businessName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "MERCHANT";

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 10)
    private String pan;

    @Column(length = 15)
    private String gstin;

    @Column(nullable = false)
    private boolean kycVerified = false;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(length = 255)
    private String verificationToken;

    @Column
    private Instant verificationTokenExpiry;

    @Column(length = 255)
    private String passwordResetToken;

    @Column
    private Instant passwordResetTokenExpiry;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public Merchant() {}

    public Merchant(UUID id, String businessName, String email, String password, String role, String phone, String pan, String gstin, boolean kycVerified, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.businessName = businessName;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : "MERCHANT";
        this.phone = phone;
        this.pan = pan;
        this.gstin = gstin;
        this.kycVerified = kycVerified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MerchantBuilder builder() {
        return new MerchantBuilder();
    }

    public static class MerchantBuilder {
        private UUID id;
        private String businessName;
        private String email;
        private String password;
        private String role = "MERCHANT";
        private String phone;
        private String pan;
        private String gstin;
        private boolean kycVerified;
        private Instant createdAt;
        private Instant updatedAt;

        MerchantBuilder() {}

        public MerchantBuilder id(UUID id) { this.id = id; return this; }
        public MerchantBuilder businessName(String businessName) { this.businessName = businessName; return this; }
        public MerchantBuilder email(String email) { this.email = email; return this; }
        public MerchantBuilder password(String password) { this.password = password; return this; }
        public MerchantBuilder role(String role) { this.role = role; return this; }
        public MerchantBuilder phone(String phone) { this.phone = phone; return this; }
        public MerchantBuilder pan(String pan) { this.pan = pan; return this; }
        public MerchantBuilder gstin(String gstin) { this.gstin = gstin; return this; }
        public MerchantBuilder kycVerified(boolean kycVerified) { this.kycVerified = kycVerified; return this; }
        public MerchantBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public MerchantBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Merchant build() {
            return new Merchant(id, businessName, email, password, role, phone, pan, gstin, kycVerified, createdAt, updatedAt);
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public boolean isKycVerified() { return kycVerified; }
    public void setKycVerified(boolean kycVerified) { this.kycVerified = kycVerified; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    public Instant getVerificationTokenExpiry() { return verificationTokenExpiry; }
    public void setVerificationTokenExpiry(Instant verificationTokenExpiry) { this.verificationTokenExpiry = verificationTokenExpiry; }
    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }
    public Instant getPasswordResetTokenExpiry() { return passwordResetTokenExpiry; }
    public void setPasswordResetTokenExpiry(Instant passwordResetTokenExpiry) { this.passwordResetTokenExpiry = passwordResetTokenExpiry; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
