package com.legalpay.domain.repository;

import com.legalpay.domain.entity.Contract;
import com.legalpay.domain.entity.Merchant;
import com.legalpay.domain.entity.Payer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {
    
    Page<Contract> findByMerchant(Merchant merchant, Pageable pageable);
    
    Page<Contract> findByPayer(Payer payer, Pageable pageable);
    
    Page<Contract> findByMerchantAndStatus(Merchant merchant, Contract.ContractStatus status, Pageable pageable);
}
