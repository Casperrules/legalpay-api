package com.legalpay.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payers")
public class Payer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "PAYER";

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 10)
    private String pan;

    @Column(length = 4)
    private String aadhaarLast4;

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

    public Payer() {}

    public Payer(UUID id, String name, String email, String password, String role, String phone, String pan, String aadhaarLast4, boolean kycVerified, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : "PAYER";
        this.phone = phone;
        this.pan = pan;
        this.aadhaarLast4 = aadhaarLast4;
        this.kycVerified = kycVerified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PayerBuilder builder() {
        return new PayerBuilder();
    }

    public static class PayerBuilder {
        private UUID id;
        private String name;
        private String email;
        private String password;
        private String role = "PAYER";
        private String phone;
        private String pan;
        private String aadhaarLast4;
        private boolean kycVerified;
        private Instant createdAt;
        private Instant updatedAt;

        PayerBuilder() {}

        public PayerBuilder id(UUID id) { this.id = id; return this; }
        public PayerBuilder name(String name) { this.name = name; return this; }
        public PayerBuilder email(String email) { this.email = email; return this; }
        public PayerBuilder password(String password) { this.password = password; return this; }
        public PayerBuilder role(String role) { this.role = role; return this; }
        public PayerBuilder phone(String phone) { this.phone = phone; return this; }
        public PayerBuilder pan(String pan) { this.pan = pan; return this; }
        public PayerBuilder aadhaarLast4(String aadhaarLast4) { this.aadhaarLast4 = aadhaarLast4; return this; }
        public PayerBuilder kycVerified(boolean kycVerified) { this.kycVerified = kycVerified; return this; }
        public PayerBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public PayerBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Payer build() {
            return new Payer(id, name, email, password, role, phone, pan, aadhaarLast4, kycVerified, createdAt, updatedAt);
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    public String getAadhaarLast4() { return aadhaarLast4; }
    public void setAadhaarLast4(String aadhaarLast4) { this.aadhaarLast4 = aadhaarLast4; }
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
