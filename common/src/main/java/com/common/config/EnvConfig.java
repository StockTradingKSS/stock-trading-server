package com.common.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

public class EnvConfig {
    public static class DotenvInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        private static final Logger log = LoggerFactory.getLogger(DotenvInitializer.class);

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            String envDirectory = environment.getProperty("env.file.directory", "./env");

            log.info("Initializing environment variables from: {}", envDirectory);

            Dotenv dotenv = Dotenv.configure()
                    .directory(envDirectory)
                    .load();

            Properties props = new Properties();
            dotenv.entries().forEach(entry -> props.put(entry.getKey(), entry.getValue()));

            PropertiesPropertySource propertySource = new PropertiesPropertySource("dotenvProperties", props);
            environment.getPropertySources().addFirst(propertySource);

            log.info("Environment variables loaded successfully");
        }
    }
}
