package com.KimStock.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava;
import org.springframework.boot.system.JavaVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executors;

@Slf4j
@Configuration
@ConditionalOnJava(JavaVersion.TWENTY_ONE)
public class VirtualThreadsConfig {

    /**
     * Virtual Threads를 사용하는 JDBC 전용 스케줄러
     * WebFlux에서 JDBC 호출 시 블로킹 문제 해결
     */
    @Bean("jdbcScheduler")
    public Scheduler jdbcScheduler() {
        log.info("Virtual Threads 기반 JDBC 스케줄러 생성");
        
        return Schedulers.fromExecutor(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    /**
     * 일반적인 블로킹 작업용 Virtual Threads 스케줄러
     */
    @Bean("blockingScheduler")
    public Scheduler blockingScheduler() {
        log.info("Virtual Threads 기반 블로킹 작업 스케줄러 생성");
        
        return Schedulers.fromExecutor(
            Executors.newVirtualThreadPerTaskExecutor()
        );
    }
}
