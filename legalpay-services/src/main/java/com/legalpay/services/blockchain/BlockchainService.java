package com.legalpay.services.blockchain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpay.domain.entity.BlockchainAuditLog;
import com.legalpay.domain.entity.BlockchainAuditLog.EventType;
import com.legalpay.domain.entity.BlockchainAuditLog.TransactionStatus;
import com.legalpay.domain.repository.BlockchainAuditLogRepository;
import com.legalpay.services.config.BlockchainConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for logging events to blockchain audit trail
 * Uses Polygon network for low-cost, fast transactions
 */
@Service
public class BlockchainService {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainService.class);

    private final BlockchainConfig blockchainConfig;
    private final BlockchainAuditLogRepository auditLogRepository;
    private final Web3j web3j;
    private final Credentials credentials;
    private final DefaultGasProvider gasProvider;
    private final ObjectMapper objectMapper;

    public BlockchainService(
            BlockchainConfig blockchainConfig,
            BlockchainAuditLogRepository auditLogRepository,
            Web3j web3j,
            Credentials credentials,
            DefaultGasProvider gasProvider,
            ObjectMapper objectMapper
    ) {
        this.blockchainConfig = blockchainConfig;
        this.auditLogRepository = auditLogRepository;
        this.web3j = web3j;
        this.credentials = credentials;
        this.gasProvider = gasProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * Log an event to blockchain asynchronously
     * Does not block the main transaction
     */
    @Async
    @Transactional
    public CompletableFuture<BlockchainAuditLog> logEventAsync(
            EventType eventType,
            UUID entityId,
            String entityType,
            UUID userId,
            Map<String, Object> metadata
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return logEvent(eventType, entityId, entityType, userId, metadata);
            } catch (Exception e) {
                logger.error("Async blockchain logging failed for entity {}: {}", 
                    entityId, e.getMessage(), e);
                return null;
            }
        });
    }

    /**
     * Log an event to blockchain synchronously
     */
    @Transactional
    public BlockchainAuditLog logEvent(
            EventType eventType,
            UUID entityId,
            String entityType,
            UUID userId,
            Map<String, Object> metadata
    ) {
        if (!blockchainConfig.isBlockchainEnabled()) {
            logger.warn("Blockchain is disabled. Event {} not logged for entity {}", 
                eventType, entityId);
            return null;
        }

        BlockchainAuditLog auditLog = new BlockchainAuditLog();
        auditLog.setEventType(eventType);
        auditLog.setEntityId(entityId);
        auditLog.setEntityType(entityType);
        auditLog.setUserId(userId);
        auditLog.setNetwork(blockchainConfig.getNetwork());
        auditLog.setStatus(TransactionStatus.PENDING);

        // Convert metadata to JSON
        try {
            String metadataJson = objectMapper.writeValueAsString(metadata);
            auditLog.setMetadata(metadataJson);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize metadata for blockchain logging", e);
            auditLog.setMetadata("{}");
        }

        // Save to DB first
        auditLog = auditLogRepository.save(auditLog);

        // Submit to blockchain
        try {
            String txHash = submitToBlockchain(eventType, entityId, userId, auditLog.getMetadata());
            auditLog.setTransactionHash(txHash);
            auditLog = auditLogRepository.save(auditLog);

            logger.info("Blockchain event logged: {} for entity {} - tx: {}", 
                eventType, entityId, txHash);

            // Confirm transaction in background
            confirmTransactionAsync(auditLog.getId(), txHash);

        } catch (Exception e) {
            logger.error("Failed to submit blockchain transaction for entity {}: {}", 
                entityId, e.getMessage(), e);
            auditLog.setStatus(TransactionStatus.FAILED);
            auditLog.setErrorMessage(e.getMessage());
            auditLog = auditLogRepository.save(auditLog);
        }

        return auditLog;
    }

    /**
     * Submit transaction to blockchain smart contract
     */
    private String submitToBlockchain(
            EventType eventType,
            UUID entityId,
            UUID userId,
            String metadata
    ) throws Exception {
        
        // Prepare function call to AuditTrail.logEvent()
        Function function = new Function(
            "logEvent",
            Arrays.asList(
                new Uint8(BigInteger.valueOf(eventType.ordinal())),  // EventType enum
                new Utf8String(entityId.toString()),                  // entityId
                new Utf8String(userId.toString()),                    // userId
                new Utf8String(metadata)                              // metadata JSON
            ),
            Collections.singletonList(new TypeReference<Uint256>() {}) // returns uint256
        );

        String encodedFunction = FunctionEncoder.encode(function);

        // Get nonce
        BigInteger nonce = web3j.ethGetTransactionCount(
            credentials.getAddress(),
            DefaultBlockParameterName.LATEST
        ).send().getTransactionCount();

        // Create and send raw transaction
        Long chainId = blockchainConfig.getNetwork().contains("mumbai") ? 80001L : 137L;
        
        RawTransactionManager txManager = new RawTransactionManager(
            web3j, 
            credentials,
            chainId
        );

        EthSendTransaction ethSendTransaction = txManager.sendTransaction(
            gasProvider.getGasPrice("logEvent"),
            gasProvider.getGasLimit("logEvent"),
            blockchainConfig.getContractAddress(),
            encodedFunction,
            BigInteger.ZERO // value in wei (0 for contract calls)
        );

        if (ethSendTransaction.hasError()) {
            throw new RuntimeException("Transaction failed: " + 
                ethSendTransaction.getError().getMessage());
        }

        return ethSendTransaction.getTransactionHash();
    }

    /**
     * Confirm transaction asynchronously and update audit log
     */
    @Async
    public void confirmTransactionAsync(UUID auditLogId, String txHash) {
        try {
            // Wait for transaction to be mined
            TransactionReceipt receipt = waitForTransactionReceipt(txHash);

            BlockchainAuditLog auditLog = auditLogRepository.findById(auditLogId)
                .orElseThrow(() -> new RuntimeException("Audit log not found: " + auditLogId));

            if (receipt.isStatusOK()) {
                auditLog.setStatus(TransactionStatus.CONFIRMED);
                auditLog.setBlockNumber(receipt.getBlockNumber().longValue());
                auditLog.setGasUsed(receipt.getGasUsed().longValue());
                
                // Calculate effective gas price from transaction
                BigInteger effectiveGasPrice = gasProvider.getGasPrice("logEvent");
                String gasPriceStr = effectiveGasPrice.toString();
                    
                auditLog.setGasPrice(gasPriceStr);
                
                // Calculate transaction cost
                BigInteger cost = receipt.getGasUsed().multiply(effectiveGasPrice);
                auditLog.setTransactionCost(cost.toString());
                auditLog.setConfirmedAt(Instant.now());

                logger.info("Blockchain transaction confirmed: {} - block: {}, gas: {}", 
                    txHash, receipt.getBlockNumber(), receipt.getGasUsed());
            } else {
                auditLog.setStatus(TransactionStatus.FAILED);
                auditLog.setErrorMessage("Transaction reverted on blockchain");
                logger.error("Blockchain transaction failed: {}", txHash);
            }

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            logger.error("Failed to confirm blockchain transaction {}: {}", 
                txHash, e.getMessage(), e);
        }
    }

    /**
     * Wait for transaction receipt (with timeout)
     */
    private TransactionReceipt waitForTransactionReceipt(String txHash) throws Exception {
        int attempts = 0;
        int maxAttempts = 40; // 40 attempts * 3 seconds = 2 minutes timeout

        while (attempts < maxAttempts) {
            EthGetTransactionReceipt receiptResponse = web3j
                .ethGetTransactionReceipt(txHash)
                .send();

            if (receiptResponse.getTransactionReceipt().isPresent()) {
                return receiptResponse.getTransactionReceipt().get();
            }

            Thread.sleep(3000); // Wait 3 seconds between attempts
            attempts++;
        }

        throw new RuntimeException("Transaction not mined after " + maxAttempts + " attempts");
    }

    /**
     * Get audit trail for an entity from blockchain
     */
    public List<BlockchainAuditLog> getAuditTrail(UUID entityId) {
        return auditLogRepository.findByEntityIdOrderByCreatedAtAsc(entityId);
    }

    /**
     * Check if an event has been logged for an entity
     */
    public boolean hasEvent(UUID entityId, EventType eventType) {
        return auditLogRepository.existsByEntityIdAndEventType(entityId, eventType);
    }

    /**
     * Retry failed transactions
     */
    @Transactional
    public void retryFailedTransactions(int maxRetries) {
        List<BlockchainAuditLog> failedLogs = auditLogRepository
            .findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                TransactionStatus.FAILED, 
                maxRetries
            );

        for (BlockchainAuditLog log : failedLogs) {
            try {
                logger.info("Retrying blockchain transaction for entity {}", log.getEntityId());
                
                String txHash = submitToBlockchain(
                    log.getEventType(),
                    log.getEntityId(),
                    log.getUserId(),
                    log.getMetadata()
                );

                log.setTransactionHash(txHash);
                log.setStatus(TransactionStatus.PENDING);
                log.setRetryCount(log.getRetryCount() + 1);
                log.setErrorMessage(null);
                auditLogRepository.save(log);

                confirmTransactionAsync(log.getId(), txHash);

            } catch (Exception e) {
                logger.error("Retry failed for audit log {}: {}", 
                    log.getId(), e.getMessage());
                log.setRetryCount(log.getRetryCount() + 1);
                log.setErrorMessage(e.getMessage());
                auditLogRepository.save(log);
            }
        }
    }
}
