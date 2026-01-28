package com.legalpay.domain.repository;

import com.legalpay.domain.entity.Payer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayerRepository extends JpaRepository<Payer, UUID> {
    
    Optional<Payer> findByEmail(String email);
    
    Optional<Payer> findByVerificationToken(String token);
    
    Optional<Payer> findByPasswordResetToken(String token);
    
    boolean existsByEmail(String email);
}
