package com.legalpay.domain.repository;

import com.legalpay.domain.entity.BlockchainAuditLog;
import com.legalpay.domain.entity.BlockchainAuditLog.EventType;
import com.legalpay.domain.entity.BlockchainAuditLog.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlockchainAuditLogRepository extends JpaRepository<BlockchainAuditLog, UUID> {

    /**
     * Find blockchain record by transaction hash
     */
    Optional<BlockchainAuditLog> findByTransactionHash(String transactionHash);

    /**
     * Find all blockchain records for an entity (contract, payment, etc.)
     */
    List<BlockchainAuditLog> findByEntityIdOrderByCreatedAtAsc(UUID entityId);

    /**
     * Find all records by entity type
     */
    List<BlockchainAuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType);

    /**
     * Find all records by event type
     */
    List<BlockchainAuditLog> findByEventTypeOrderByCreatedAtDesc(EventType eventType);

    /**
     * Find all records by user
     */
    List<BlockchainAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find pending/failed transactions for retry
     */
    List<BlockchainAuditLog> findByStatusInOrderByCreatedAtAsc(List<TransactionStatus> statuses);

    /**
     * Find failed transactions with retry count below threshold
     */
    List<BlockchainAuditLog> findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
        TransactionStatus status, 
        Integer maxRetries
    );

    /**
     * Check if an event has been logged for an entity
     */
    boolean existsByEntityIdAndEventType(UUID entityId, EventType eventType);

    /**
     * Count total blockchain transactions
     */
    long countByStatus(TransactionStatus status);
}
