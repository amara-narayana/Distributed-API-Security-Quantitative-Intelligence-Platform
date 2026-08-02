package com.platform.service;

import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class BlockchainAuditService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainAuditService.class);

    @Value("${fabric.config-path:./blockchain/connection-profile.json}")
    private String configPath;

    @Value("${fabric.network-id:test-network}")
    private String networkId;

    @Value("${fabric.channel:audit-channel}")
    private String channelName;

    @Value("${fabric.chaincode:audit-chaincode}")
    private String chaincodeName;

    private Gateway gateway;
    private Network network;
    private Contract contract;

    public void initialize() {
        try {
            log.info("Initializing Fabric Gateway connection...");

            Wallet wallet = Wallets.newInMemoryWallet();
            
            Path connectionProfile = Paths.get(configPath);
            if (!connectionProfile.toFile().exists()) {
                log.warn("Connection profile not found at {}. Blockchain features will be disabled.", configPath);
                return;
            }

            gateway = Gateway.builder()
                    .identity(wallet.get("appUser"))
                    .networkConfig(connectionProfile)
                    .connect();

            network = gateway.getNetwork(channelName);
            contract = network.getContract(chaincodeName);

            log.info("Successfully connected to Fabric network: {}", networkId);

        } catch (Exception e) {
            log.error("Failed to initialize Fabric Gateway: {}", e.getMessage());
            gateway = null;
            network = null;
            contract = null;
        }
    }

    public String storeAuditEntry(String entryId, String actor, String action, 
                                   String targetResource, String resultHash, String metadata) {
        if (contract == null) {
            log.warn("Fabric contract not initialized, skipping blockchain storage");
            return null;
        }

        try {
            log.info("Submitting StoreAuditEntry transaction to blockchain");

            byte[] result = contract.submitTransaction("StoreAuditEntry",
                    entryId, actor, action, targetResource, resultHash, metadata != null ? metadata : "");

            String txId = new String(result);
            log.info("Transaction submitted successfully: {}", txId);

            return txId;

        } catch (Exception e) {
            log.error("Failed to submit transaction to blockchain: {}", e.getMessage());
            throw new RuntimeException("Blockchain transaction failed", e);
        }
    }

    public String getAuditEntry(String entryId) {
        if (contract == null) {
            log.warn("Fabric contract not initialized");
            return null;
        }

        try {
            log.info("Evaluating GetAuditEntry transaction for: {}", entryId);

            byte[] result = contract.evaluateTransaction("GetAuditEntry", entryId);
            return new String(result);

        } catch (Exception e) {
            log.error("Failed to evaluate GetAuditEntry: {}", e.getMessage());
            return null;
        }
    }

    public boolean verifyTransaction(String txId) {
        if (contract == null) {
            return false;
        }

        try {
            log.info("Verifying blockchain transaction: {}", txId);
            
            byte[] result = contract.evaluateTransaction("GetHistory", txId);
            String history = new String(result);
            
            boolean valid = history != null && !history.isEmpty() && !history.equals("null");
            log.info("Transaction {} verification: {}", txId, valid ? "VALID" : "INVALID");
            
            return valid;

        } catch (Exception e) {
            log.error("Failed to verify transaction {}: {}", txId, e.getMessage());
            return false;
        }
    }

    public void close() {
        if (gateway != null) {
            try {
                gateway.close();
                log.info("Fabric Gateway connection closed");
            } catch (IOException e) {
                log.error("Error closing gateway: {}", e.getMessage());
            }
        }
    }
}
