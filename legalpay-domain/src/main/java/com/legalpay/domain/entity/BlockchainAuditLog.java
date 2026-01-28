package com.legalpay.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores blockchain transaction records for audit trail
 * Maps application events to blockchain transaction hashes
 */
@Entity
@Table(name = "blockchain_audit_logs", indexes = {
    @Index(name = "idx_entity_id", columnList = "entityId"),
    @Index(name = "idx_tx_hash", columnList = "transactionHash"),
    @Index(name = "idx_event_type", columnList = "eventType"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
public class BlockchainAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Type of event logged to blockchain
     */
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    /**
     * ID of the entity (contract, payment, mandate)
     */
    @Column(nullable = false)
    private UUID entityId;

    /**
     * Type of entity
     */
    @Column(nullable = false, length = 50)
    private String entityType;

    /**
     * User who triggered the event
     */
    @Column(nullable = false)
    private UUID userId;

    /**
     * JSON metadata stored on blockchain
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * Blockchain network (e.g., "polygon-mumbai", "polygon-mainnet")
     */
    @Column(nullable = false, length = 50)
    private String network;

    /**
     * Transaction hash on blockchain
     */
    @Column(nullable = false, unique = true, length = 66)
    private String transactionHash;

    /**
     * Block number where transaction was mined
     */
    @Column
    private Long blockNumber;

    /**
     * Gas used for transaction
     */
    @Column
    private Long gasUsed;

    /**
     * Gas price in wei
     */
    @Column
    private String gasPrice;

    /**
     * Transaction cost in wei
     */
    @Column
    private String transactionCost;

    /**
     * Status of blockchain transaction
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    /**
     * Error message if transaction failed
     */
    @Column(length = 1000)
    private String errorMessage;

    /**
     * Number of retry attempts
     */
    @Column(nullable = false)
    private Integer retryCount = 0;

    /**
     * When the event was created in our system
     */
    @Column(nullable = false)
    private Instant createdAt;

    /**
     * When the blockchain transaction was confirmed
     */
    @Column
    private Instant confirmedAt;

    public enum EventType {
        CONTRACT_CREATED,
        CONTRACT_SIGNED,
        CONTRACT_ACTIVATED,
        PAYMENT_INITIATED,
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
        MANDATE_CREATED,
        MANDATE_ACTIVATED,
        MANDATE_CANCELLED,
        LEGAL_NOTICE_SENT,
        DISPUTE_RAISED,
        DISPUTE_RESOLVED
    }

    public enum TransactionStatus {
        PENDING,      // Transaction submitted to blockchain
        CONFIRMED,    // Transaction mined and confirmed
        FAILED,       // Transaction failed
        RETRY         // Queued for retry
    }

    // Constructors
    public BlockchainAuditLog() {
        this.createdAt = Instant.now();
        this.status = TransactionStatus.PENDING;
        this.retryCount = 0;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public Long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(Long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public Long getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(Long gasUsed) {
        this.gasUsed = gasUsed;
    }

    public String getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(String gasPrice) {
        this.gasPrice = gasPrice;
    }

    public String getTransactionCost() {
        return transactionCost;
    }

    public void setTransactionCost(String transactionCost) {
        this.transactionCost = transactionCost;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
}
