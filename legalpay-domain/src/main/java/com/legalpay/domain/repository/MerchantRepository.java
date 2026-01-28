package com.legalpay.domain.repository;

import com.legalpay.domain.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository Pattern for Merchant
 */
@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    
    Optional<Merchant> findByEmail(String email);
    
    Optional<Merchant> findByVerificationToken(String token);
    
    Optional<Merchant> findByPasswordResetToken(String token);
    
    boolean existsByEmail(String email);
    
    boolean existsByPan(String pan);
}
