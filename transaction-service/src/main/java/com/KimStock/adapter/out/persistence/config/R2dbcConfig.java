package com.KimStock.adapter.out.persistence.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.core.DatabaseClient;
import io.r2dbc.spi.ConnectionFactory;

@Configuration
@EnableR2dbcRepositories
@RequiredArgsConstructor
public class R2dbcConfig {

    private final ConnectionFactory connectionFactory;

    @Bean
    public DatabaseClient databaseClient() {
        return DatabaseClient.create(connectionFactory);
    }

    @Bean
    public PostgresInitializer postgresInitializer(R2dbcEntityTemplate template) {
        return new PostgresInitializer(template);
    }
}
