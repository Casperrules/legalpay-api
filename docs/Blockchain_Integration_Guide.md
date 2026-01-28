# Blockchain Integration Guide - Polygon Audit Trail

## Overview

LegalPay uses **Polygon blockchain** for immutable audit trails. Every critical event (contract creation, signing, payments) is logged to a smart contract on Polygon, providing tamper-proof evidence for legal disputes.

---

## Why Polygon?

**Recommended blockchain for Indian fintech:**

- ✅ **Low Cost**: ₹0.01 - ₹0.10 per transaction (vs ₹1000+ on Ethereum)
- ✅ **Fast**: 2-second block time (vs 15 seconds on Ethereum)
- ✅ **Ethereum-Compatible**: Use standard Web3 tools
- ✅ **Proven**: Used by top Indian Web3 startups
- ✅ **Carbon Neutral**: PoS consensus

**Cost Comparison (per audit log entry):**

- Polygon Mumbai (testnet): FREE
- Polygon Mainnet: ₹0.05 - ₹0.20
- Ethereum Mainnet: ₹500 - ₹2000 (not viable)

---

## Architecture

### What's Logged to Blockchain

```
Contract Created → Blockchain Log (tx hash stored in DB)
Contract Signed → Blockchain Log
Payment Completed → Blockchain Log
Payment Failed → Blockchain Log
Mandate Created → Blockchain Log (future)
Legal Notice Sent → Blockchain Log (future)
```

### Data Flow

```
Application Event
  ↓
BlockchainService.logEvent()
  ↓
Submit to Polygon Smart Contract
  ↓
Store Transaction Hash in PostgreSQL
  ↓
Wait for Confirmation (background)
  ↓
Update DB with block number & gas used
```

### What's Stored Where

**On Blockchain (Immutable):**

- Event type (CONTRACT_CREATED, PAYMENT_COMPLETED, etc.)
- Entity ID (contract UUID)
- User ID (who triggered the action)
- Metadata JSON (amount, status, payment method, etc.)
- Timestamp (block timestamp)

**In Database (Queryable):**

- Transaction hash
- Block number
- Gas used & cost
- Confirmation status
- Retry count & errors

---

## Setup Instructions

### Step 1: Create Polygon Wallet

1. **Install MetaMask**
   - Go to https://metamask.io
   - Install browser extension
   - Create new wallet
   - **CRITICAL**: Save seed phrase securely (never share!)

2. **Add Polygon Mumbai Network (Testnet)**
   - MetaMask → Settings → Networks → Add Network
   - Network Name: `Polygon Mumbai Testnet`
   - RPC URL: `https://rpc-mumbai.maticvigil.com`
   - Chain ID: `80001`
   - Currency Symbol: `MATIC`
   - Block Explorer: `https://mumbai.polygonscan.com`

3. **Get Test MATIC (Free)**
   - Go to https://faucet.polygon.technology
   - Connect MetaMask
   - Request test MATIC (0.5 MATIC)
   - Wait 1-2 minutes for confirmation

### Step 2: Deploy Smart Contract

#### Option A: Using Remix IDE (Recommended for Testing)

1. **Open Remix**
   - Go to https://remix.ethereum.org

2. **Create Contract File**
   - Create new file: `AuditTrail.sol`
   - Copy contract from `/contracts/AuditTrail.sol`
   - Paste into Remix

3. **Compile Contract**
   - Compiler tab → Select Solidity 0.8.20
   - Click "Compile AuditTrail.sol"
   - Check for errors (should be none)

4. **Deploy to Mumbai**
   - Deploy tab → Environment: "Injected Provider - MetaMask"
   - Confirm MetaMask connection
   - Network: Polygon Mumbai (80001)
   - Click "Deploy"
   - Confirm in MetaMask (gas fee: ~0.001 MATIC)

5. **Copy Contract Address**
   - After deployment, copy contract address
   - Example: `0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb`
   - Save this for Step 3

6. **Verify on PolygonScan (Optional but Recommended)**
   - Go to https://mumbai.polygonscan.com/address/YOUR_CONTRACT_ADDRESS
   - Click "Verify and Publish"
   - Compiler: 0.8.20
   - Optimization: Yes (200 runs)
   - Paste contract source code
   - Verify

#### Option B: Using Hardhat (For Production)

```bash
# Install Hardhat
npm install --save-dev hardhat @nomicfoundation/hardhat-toolbox

# Initialize Hardhat project
npx hardhat

# Create deployment script
cat > scripts/deploy-audit-trail.js << 'EOF'
async function main() {
  const AuditTrail = await ethers.getContractFactory("AuditTrail");
  const auditTrail = await AuditTrail.deploy();
  await auditTrail.deployed();
  console.log("AuditTrail deployed to:", auditTrail.address);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
EOF

# Configure Hardhat (hardhat.config.js)
cat > hardhat.config.js << 'EOF'
require("@nomicfoundation/hardhat-toolbox");

module.exports = {
  solidity: "0.8.20",
  networks: {
    mumbai: {
      url: "https://rpc-mumbai.maticvigil.com",
      accounts: [process.env.BLOCKCHAIN_PRIVATE_KEY]
    },
    polygon: {
      url: "https://polygon-rpc.com",
      accounts: [process.env.BLOCKCHAIN_PRIVATE_KEY]
    }
  }
};
EOF

# Deploy to Mumbai
npx hardhat run scripts/deploy-audit-trail.js --network mumbai

# Deploy to Polygon Mainnet (production)
npx hardhat run scripts/deploy-audit-trail.js --network polygon
```

### Step 3: Configure Backend

1. **Export Private Key from MetaMask**
   - MetaMask → Account Details → Export Private Key
   - Enter password
   - **CRITICAL**: This key controls your wallet! Keep it secret!
   - Copy private key (64 hex characters)

2. **Update .env File**

```bash
# Blockchain Configuration
BLOCKCHAIN_ENABLED=true
BLOCKCHAIN_NETWORK=polygon-mumbai
BLOCKCHAIN_RPC_URL=https://rpc-mumbai.maticvigil.com
BLOCKCHAIN_PRIVATE_KEY=YOUR_64_CHARACTER_PRIVATE_KEY_HERE
BLOCKCHAIN_CONTRACT_ADDRESS=YOUR_DEPLOYED_CONTRACT_ADDRESS
BLOCKCHAIN_GAS_PRICE=1000000000
BLOCKCHAIN_GAS_LIMIT=300000
BLOCKCHAIN_CONFIRMATION_BLOCKS=5
```

3. **Restart Backend**

```bash
cd legalpay-api
mvn spring-boot:run
```

### Step 4: Test Blockchain Integration

1. **Create a Contract**
   - Login as merchant
   - Create new contract
   - Check logs for: `Blockchain event logged: CONTRACT_CREATED`

2. **Check Database**

```sql
SELECT * FROM blockchain_audit_logs
ORDER BY created_at DESC LIMIT 1;
```

Expected result:

- `event_type`: CONTRACT_CREATED
- `transaction_hash`: 0x... (66 characters)
- `status`: PENDING or CONFIRMED
- `network`: polygon-mumbai

3. **Verify on PolygonScan**
   - Go to https://mumbai.polygonscan.com/tx/YOUR_TX_HASH
   - Should see transaction with:
     - Status: Success
     - To: YOUR_CONTRACT_ADDRESS
     - Method: logEvent
     - Gas Used: ~80,000 - 120,000

4. **Check Smart Contract Events**
   - On PolygonScan → Logs tab
   - Should see `AuditEntryCreated` event
   - Event data includes entityId and eventType

---

## Production Deployment

### 1. Switch to Polygon Mainnet

**Get Real MATIC:**

- Buy MATIC from:
  - WazirX (Indian exchange): https://wazirx.com
  - CoinDCX: https://coindcx.com
  - Binance: https://binance.com
- Transfer to your MetaMask wallet (Polygon network)
- Need: ~10 MATIC for first month (₹500 at current prices)

**Deploy Contract to Mainnet:**

```bash
# Using Remix: Change network to "Polygon Mainnet" in MetaMask
# Using Hardhat:
npx hardhat run scripts/deploy-audit-trail.js --network polygon
```

**Update Production .env:**

```bash
BLOCKCHAIN_NETWORK=polygon-mainnet
BLOCKCHAIN_RPC_URL=https://polygon-rpc.com
BLOCKCHAIN_CONTRACT_ADDRESS=YOUR_MAINNET_CONTRACT_ADDRESS
BLOCKCHAIN_GAS_PRICE=50000000000  # 50 Gwei (adjust based on network)
```

### 2. Security Best Practices

**Private Key Management:**

- ✅ **DO**: Store in environment variable only
- ✅ **DO**: Use different wallets for test/production
- ✅ **DO**: Keep backup of seed phrase in safe
- ❌ **DON'T**: Commit to Git
- ❌ **DON'T**: Share with anyone
- ❌ **DON'T**: Store in application.yml

**Wallet Hygiene:**

- Create dedicated wallet for backend service
- Keep only necessary MATIC (50-100 MATIC)
- Monitor wallet balance (set up alerts)
- Rotate keys every 6 months

**Contract Security:**

- Verify contract on PolygonScan (makes it transparent)
- Audit contract code before mainnet deployment
- Test extensively on Mumbai before mainnet

### 3. Monitoring & Alerts

**Monitor Blockchain Transactions:**

```sql
-- Check transaction success rate
SELECT
  status,
  COUNT(*) as count,
  COUNT(*) * 100.0 / SUM(COUNT(*)) OVER() as percentage
FROM blockchain_audit_logs
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY status;

-- Check average gas cost
SELECT
  AVG(gas_used) as avg_gas,
  AVG(CAST(transaction_cost AS BIGINT)) as avg_cost_wei
FROM blockchain_audit_logs
WHERE status = 'CONFIRMED'
  AND created_at > NOW() - INTERVAL '7 days';

-- Find failed transactions needing retry
SELECT * FROM blockchain_audit_logs
WHERE status = 'FAILED'
  AND retry_count < 3
ORDER BY created_at ASC;
```

**Set Up Alerts:**

- Wallet balance < 10 MATIC → Top up needed
- Transaction failure rate > 5% → Check RPC endpoint
- Pending transactions > 100 → Check gas price
- Gas cost spike → Adjust BLOCKCHAIN_GAS_PRICE

---

## Cost Analysis

### Mumbai Testnet (Development)

- Transaction Cost: **FREE** (test MATIC from faucet)
- Transactions/Day: Unlimited
- Monthly Cost: **₹0**

### Polygon Mainnet (Production)

**Gas Usage per Event:**

- CONTRACT_CREATED: ~100,000 gas
- CONTRACT_SIGNED: ~80,000 gas
- PAYMENT_COMPLETED: ~90,000 gas

**Cost Calculation:**

```
1 MATIC = ₹50 (approximate)
1 MATIC = 10^18 wei
1 Gwei = 10^9 wei

Gas Price = 50 Gwei (standard)
Transaction Cost = Gas Used × Gas Price
                 = 100,000 × 50 × 10^9 wei
                 = 0.005 MATIC
                 = ₹0.25 per transaction

Monthly Volume: 10,000 transactions
Monthly Cost = 10,000 × ₹0.25 = ₹2,500

First Year Cost Estimate:
- Jan-Mar: 5,000 tx/month × ₹0.25 = ₹3,750
- Apr-Jun: 10,000 tx/month × ₹0.25 = ₹7,500
- Jul-Dec: 20,000 tx/month × ₹0.25 = ₹30,000
Total Year 1: ~₹45,000 (negligible vs ₹10L+ revenue target)
```

**Cost Optimization:**

- Batch events (future enhancement): Log 10 events in 1 tx → Save 90% on gas
- Off-peak transactions: Submit during low gas price periods
- RPC caching: Reduce RPC calls

---

## Troubleshooting

### Issue 1: Transaction Failing with "Insufficient Funds"

**Cause:** Wallet has no MATIC for gas fees

**Solution:**

```bash
# Check wallet balance
curl https://rpc-mumbai.maticvigil.com \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"eth_getBalance","params":["YOUR_WALLET_ADDRESS","latest"],"id":1}'

# Get test MATIC
# Go to https://faucet.polygon.technology
```

### Issue 2: "Transaction Underpriced"

**Cause:** Gas price too low

**Solution:**

```bash
# Increase gas price in .env
BLOCKCHAIN_GAS_PRICE=2000000000  # 2 Gwei → 5 Gwei or higher
```

### Issue 3: RPC Connection Failed

**Cause:** RPC endpoint down or rate-limited

**Solution:**

```bash
# Try alternative RPC endpoints

# Mumbai Testnet:
BLOCKCHAIN_RPC_URL=https://rpc-mumbai.matic.today
# or
BLOCKCHAIN_RPC_URL=https://matic-mumbai.chainstacklabs.com

# Polygon Mainnet:
BLOCKCHAIN_RPC_URL=https://polygon-rpc.com
# or
BLOCKCHAIN_RPC_URL=https://rpc-mainnet.matic.quiknode.pro
# or (Alchemy - requires free account)
BLOCKCHAIN_RPC_URL=https://polygon-mainnet.g.alchemy.com/v2/YOUR_API_KEY
```

### Issue 4: High Gas Costs on Mainnet

**Cause:** Network congestion

**Solution:**

```bash
# Check current gas price
curl https://gasstation-mainnet.matic.network/v2

# Adjust dynamically or wait for lower gas prices
# Consider batching transactions during off-peak hours (2-6 AM IST)
```

### Issue 5: Contract Not Verified on PolygonScan

**Impact:** Can't read events easily on block explorer

**Solution:**

1. Go to https://mumbai.polygonscan.com/address/YOUR_CONTRACT_ADDRESS#code
2. Click "Verify and Publish"
3. Compiler: `v0.8.20+commit.a1b79de6`
4. Optimization: `Yes` with 200 runs
5. Paste contract source from `/contracts/AuditTrail.sol`
6. Constructor arguments: (leave empty)
7. Click "Verify and Publish"

---

## API Reference

### BlockchainService Methods

**Log Event Asynchronously (Recommended):**

```java
CompletableFuture<BlockchainAuditLog> logEventAsync(
    EventType eventType,
    UUID entityId,
    String entityType,
    UUID userId,
    Map<String, Object> metadata
)
```

**Log Event Synchronously (Blocks until submitted):**

```java
BlockchainAuditLog logEvent(
    EventType eventType,
    UUID entityId,
    String entityType,
    UUID userId,
    Map<String, Object> metadata
)
```

**Get Audit Trail for Entity:**

```java
List<BlockchainAuditLog> getAuditTrail(UUID entityId)
```

**Check if Event Already Logged:**

```java
boolean hasEvent(UUID entityId, EventType eventType)
```

**Retry Failed Transactions:**

```java
void retryFailedTransactions(int maxRetries)
```

### Smart Contract Methods

**Log Event (Called by Backend):**

```solidity
function logEvent(
    EventType eventType,
    string memory entityId,
    string memory userId,
    string memory metadata
) public onlyOwner returns (uint256)
```

**Get Audit Entry:**

```solidity
function getEntry(uint256 entryId) public view returns (AuditEntry memory)
```

**Get Audit Trail for Entity:**

```solidity
function getAuditTrail(string memory entityId) public view returns (AuditEntry[] memory)
```

---

## Future Enhancements

1. **Batch Logging** (90% cost reduction):
   - Aggregate 10-50 events into single transaction
   - Submit batch every 5 minutes or when threshold reached

2. **IPFS Integration** (for large metadata):
   - Store detailed metadata in IPFS
   - Only store IPFS hash on blockchain
   - Reduces gas costs for complex data

3. **Zero-Knowledge Proofs** (privacy):
   - Prove contract signed without revealing signer identity
   - Use zk-SNARKs on Polygon zkEVM

4. **DAO Governance** (for dispute resolution):
   - Token holders vote on disputed transactions
   - On-chain governance for platform rules

5. **Cross-Chain Bridge** (future markets):
   - Bridge audit logs to Ethereum for international contracts
   - Support BSC for Southeast Asia expansion

---

## Resources

### Official Documentation

- Polygon Docs: https://docs.polygon.technology
- Web3j Java Library: https://docs.web3j.io
- Solidity Language: https://docs.soliditylang.org

### Blockchain Explorers

- Mumbai Testnet: https://mumbai.polygonscan.com
- Polygon Mainnet: https://polygonscan.com

### Faucets

- Polygon Mumbai Faucet: https://faucet.polygon.technology
- Alchemy Faucet: https://mumbaifaucet.com

### RPC Providers

- Polygon Public RPC: https://polygon-rpc.com (free, rate-limited)
- Alchemy: https://alchemy.com (10M requests/month free)
- Infura: https://infura.io (100K requests/day free)
- QuickNode: https://quicknode.com (paid)

### Indian Crypto Exchanges (to buy MATIC)

- WazirX: https://wazirx.com
- CoinDCX: https://coindcx.com
- ZebPay: https://zebpay.com

---

## Support

**Need Help?**

- Check logs: `logs/application.log`
- Database queries: See "Monitoring & Alerts" section
- Mumbai PolygonScan: Verify transactions
- Contract verified: Read events directly on explorer

**Common Questions:**

**Q: Do I need KYC to use Polygon?**
A: No. Blockchain wallets are pseudonymous. You only need KYC to buy MATIC from Indian exchanges.

**Q: Can users see blockchain transactions?**
A: Yes, all Polygon transactions are public. Anyone can view on PolygonScan. Metadata is visible but doesn't contain PII.

**Q: What if I lose my private key?**
A: Funds are lost permanently. Always keep seed phrase backup. Consider multi-sig wallet for production.

**Q: Can I change the contract after deployment?**
A: No, smart contracts are immutable. You can deploy a new contract and update BLOCKCHAIN_CONTRACT_ADDRESS.

**Q: What happens if Polygon blockchain goes down?**
A: Very unlikely (99.99% uptime). If it happens, audit logs queue in DB and retry automatically. Application continues working.

---

**Blockchain integration is now READY for production!** 🚀
