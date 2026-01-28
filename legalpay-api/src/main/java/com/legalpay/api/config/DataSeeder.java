package com.legalpay.api.config;

import com.legalpay.domain.entity.Merchant;
import com.legalpay.domain.entity.Payer;
import com.legalpay.domain.repository.MerchantRepository;
import com.legalpay.domain.repository.PayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seed database with sample data for local testing
 * Only runs in 'local' profile (not production)
 */
@Configuration
@Profile("local")
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner initDatabase(
            MerchantRepository merchantRepository,
            PayerRepository payerRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            log.info("🌱 Seeding database with sample data...");

            String defaultPassword = passwordEncoder.encode("password");

            // Create sample merchant
            if (merchantRepository.count() == 0) {
                Merchant merchant = Merchant.builder()
                        .id(java.util.UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
                        .businessName("Sample Merchant Ltd.")
                        .email("merchant@example.com")
                        .password(defaultPassword)
                        .role("MERCHANT")
                        .phone("9876543210")
                        .pan("ABCDE1234F")
                        .gstin("27ABCDE1234F1Z5")
                        .kycVerified(false)
                        .build();

                merchant = merchantRepository.save(merchant);
                log.info("✅ Created merchant: {} (ID: {})", merchant.getBusinessName(), merchant.getId());
                log.info("🔐 Merchant Login: email=merchant@example.com, password=password");
            }

            // Create sample payers
            if (payerRepository.count() == 0) {
                Payer payer1 = Payer.builder()
                        .id(java.util.UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa7"))
                        .name("Rajesh Kumar")
                        .email("rajesh@example.com")
                        .password(defaultPassword)
                        .role("PAYER")
                        .phone("9876543211")
                        .pan("XYZPQ5678K")
                        .aadhaarLast4("1234")
                        .kycVerified(false)
                        .build();

                Payer payer2 = Payer.builder()
                        .id(java.util.UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa8"))
                        .name("Priya Sharma")
                        .email("priya@example.com")
                        .password(defaultPassword)
                        .role("PAYER")
                        .phone("9876543212")
                        .pan("ABCPQ5678L")
                        .aadhaarLast4("5678")
                        .kycVerified(false)
                        .build();

                payerRepository.save(payer1);
                payerRepository.save(payer2);
                
                log.info("✅ Created 2 sample payers");
                log.info("🔐 Payer Login: email=rajesh@example.com, password=password");
            }

            log.info("🎉 Database seeding complete!");
            log.info("📊 Current data: {} merchants, {} payers", 
                    merchantRepository.count(), 
                    payerRepository.count());
        };
    }
}
