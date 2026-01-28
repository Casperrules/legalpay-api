// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title AuditTrail
 * @dev Immutable audit log for LegalPay platform events
 * 
 * Deployed on Polygon (Mumbai Testnet for development, Mainnet for production)
 * 
 * Events logged:
 * - Contract creation
 * - Contract signing (eSign completion)
 * - Payment completion
 * - Mandate creation
 * - Legal notice initiation
 * 
 * Each event is permanently recorded on blockchain with timestamp,
 * providing tamper-proof audit trail for legal disputes.
 */
contract AuditTrail {
    
    // Event types
    enum EventType {
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
    
    // Audit log entry
    struct AuditEntry {
        uint256 id;
        EventType eventType;
        string entityId;          // UUID of contract/payment/mandate
        string userId;            // UUID of user who triggered action
        string metadata;          // JSON metadata (amounts, status, etc.)
        uint256 timestamp;
        address submitter;        // Ethereum address of backend service
    }
    
    // Storage
    mapping(uint256 => AuditEntry) public auditEntries;
    mapping(string => uint256[]) public entryIdsByEntityId;
    uint256 public totalEntries;
    
    // Owner (LegalPay backend service)
    address public owner;
    
    // Events
    event AuditEntryCreated(
        uint256 indexed entryId,
        EventType indexed eventType,
        string entityId,
        uint256 timestamp
    );
    
    // Modifiers
    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can log audit entries");
        _;
    }
    
    constructor() {
        owner = msg.sender;
        totalEntries = 0;
    }
    
    /**
     * @dev Log a new audit entry
     * @param eventType Type of event
     * @param entityId UUID of the entity (contract/payment/mandate)
     * @param userId UUID of the user
     * @param metadata JSON string with additional data
     */
    function logEvent(
        EventType eventType,
        string memory entityId,
        string memory userId,
        string memory metadata
    ) public onlyOwner returns (uint256) {
        uint256 entryId = totalEntries;
        
        auditEntries[entryId] = AuditEntry({
            id: entryId,
            eventType: eventType,
            entityId: entityId,
            userId: userId,
            metadata: metadata,
            timestamp: block.timestamp,
            submitter: msg.sender
        });
        
        entryIdsByEntityId[entityId].push(entryId);
        totalEntries++;
        
        emit AuditEntryCreated(entryId, eventType, entityId, block.timestamp);
        
        return entryId;
    }
    
    /**
     * @dev Get audit entry by ID
     */
    function getEntry(uint256 entryId) public view returns (AuditEntry memory) {
        require(entryId < totalEntries, "Entry does not exist");
        return auditEntries[entryId];
    }
    
    /**
     * @dev Get all entry IDs for an entity
     */
    function getEntryIdsByEntity(string memory entityId) public view returns (uint256[] memory) {
        return entryIdsByEntityId[entityId];
    }
    
    /**
     * @dev Get audit trail for an entity
     */
    function getAuditTrail(string memory entityId) public view returns (AuditEntry[] memory) {
        uint256[] memory entryIds = entryIdsByEntityId[entityId];
        AuditEntry[] memory trail = new AuditEntry[](entryIds.length);
        
        for (uint256 i = 0; i < entryIds.length; i++) {
            trail[i] = auditEntries[entryIds[i]];
        }
        
        return trail;
    }
    
    /**
     * @dev Transfer ownership (in case of backend key rotation)
     */
    function transferOwnership(address newOwner) public onlyOwner {
        require(newOwner != address(0), "Invalid address");
        owner = newOwner;
    }
}
