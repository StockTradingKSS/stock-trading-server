package com.KimStock.adapter.out.persistence.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.KimStock.adapter.out.persistence")
@EntityScan(basePackages = {
        "com.KimStock.adapter.out.persistence"
})
@EnableTransactionManagement
public class JpaConfig {
}
