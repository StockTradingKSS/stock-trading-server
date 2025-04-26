package com.KimStock;

import com.common.config.EnvConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(EnvConfig.class)
public class TransactionServiceApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TransactionServiceApplication.class);
        app.addInitializers(new EnvConfig.DotenvInitializer());
        app.run(args);
    }
}
