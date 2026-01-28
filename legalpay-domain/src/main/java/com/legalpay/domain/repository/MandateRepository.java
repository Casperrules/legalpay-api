package com.legalpay.domain.repository;

import com.legalpay.domain.entity.Mandate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MandateRepository extends JpaRepository<Mandate, UUID> {
    
    Optional<Mandate> findByContractId(UUID contractId);
    
    Optional<Mandate> findByGatewayMandateId(String gatewayMandateId);
}
