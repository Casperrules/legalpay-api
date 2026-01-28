package com.legalpay.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.legalpay")
@EntityScan("com.legalpay.domain.entity")
@EnableJpaRepositories("com.legalpay.domain.repository")
public class LegalPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegalPayApplication.class, args);
    }
}
