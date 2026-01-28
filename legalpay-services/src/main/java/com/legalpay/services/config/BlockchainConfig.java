package com.legalpay.services.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

/**
 * Configuration for blockchain integration using Web3j
 * Connects to Polygon (Mumbai testnet or Mainnet)
 */
@Configuration
public class BlockchainConfig {

    @Value("${blockchain.enabled:true}")
    private boolean blockchainEnabled;

    @Value("${blockchain.network:polygon-mumbai}")
    private String network;

    @Value("${blockchain.rpc-url}")
    private String rpcUrl;

    @Value("${blockchain.private-key}")
    private String privateKey;

    @Value("${blockchain.contract-address}")
    private String contractAddress;

    @Value("${blockchain.gas-price:1000000000}")
    private Long gasPrice;

    @Value("${blockchain.gas-limit:300000}")
    private Long gasLimit;

    @Value("${blockchain.confirmation-blocks:5}")
    private Integer confirmationBlocks;

    @Bean
    public Web3j web3j() {
        if (!blockchainEnabled) {
            return null;
        }
        return Web3j.build(new HttpService(rpcUrl));
    }

    @Bean
    public Credentials credentials() {
        if (!blockchainEnabled || privateKey == null || privateKey.isEmpty()) {
            return null;
        }
        return Credentials.create(privateKey);
    }

    @Bean
    public DefaultGasProvider gasProvider() {
        return new DefaultGasProvider() {
            @Override
            public java.math.BigInteger getGasPrice(String contractFunc) {
                return java.math.BigInteger.valueOf(gasPrice);
            }

            @Override
            public java.math.BigInteger getGasLimit(String contractFunc) {
                return java.math.BigInteger.valueOf(gasLimit);
            }
        };
    }

    public boolean isBlockchainEnabled() {
        return blockchainEnabled;
    }

    public String getNetwork() {
        return network;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public Integer getConfirmationBlocks() {
        return confirmationBlocks;
    }
}
