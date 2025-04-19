package com.common.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;


@Configuration
public class EnvConfig {
    
    @Value("${env.file.directory:./}")
    private String envDirectory;

    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .directory(envDirectory)
                .ignoreIfMissing()
                .load();
    }
}
