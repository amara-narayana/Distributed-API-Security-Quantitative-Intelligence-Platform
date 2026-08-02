package com.platform.config;

import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Wallet;
import org.hyperledger.fabric.gateway.Wallets;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Properties;

@Configuration
public class FabricConfig {

    @Value("${fabric.connectionProfilePath:classpath:connection-profile.json}")
    private String connectionProfilePath;

    @Value("${fabric.walletPath:./wallet}")
    private String walletPath;

    @Value("${fabric.channelName:auditchannel}")
    private String channelName;

    @Value("${fabric.chaincodeName:audit}")
    private String chaincodeName;

    @Bean
    public Gateway gateway() throws Exception {
        // Load wallet
        Wallet wallet = Wallets.newFileSystemWallet(Paths.get(walletPath));
        
        // In production, load actual credentials from secure storage
        // For development, this is a simplified setup
        String userId = "appUser";
        
        if (wallet.get(userId) == null) {
            // In real implementation, enroll user here
            System.out.println("User not enrolled. Please run enrollment process.");
        }

        Gateway.Builder builder = Gateway.createBuilder()
                .identity(wallet, userId);
        
        // Load connection profile
        Path networkConfigFile = Paths.get(connectionProfilePath.replace("classpath:", ""));
        if (networkConfigFile.toFile().exists()) {
            builder.networkConfig(networkConfigFile);
        }
        
        return builder.connect();
    }

    @Bean
    public CryptoSuite cryptoSuite() throws Exception {
        return CryptoSuiteFactory.getDefault().getCryptoSuite();
    }
}
