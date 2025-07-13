package com.KimStock.adapter.out.persistence.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

@Slf4j
public class PostgresInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.info("JPA will handle table creation automatically");
    }
}
