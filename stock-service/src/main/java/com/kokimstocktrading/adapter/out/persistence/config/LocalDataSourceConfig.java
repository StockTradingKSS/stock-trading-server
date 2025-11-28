package com.kokimstocktrading.adapter.out.persistence.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("local") // 로컬 환경에서만 활성화
public class LocalDataSourceConfig {

  @Value("${spring.datasource.url}")
  private String jdbcUrl;

  @Value("${spring.datasource.username}")
  private String username;

  @Value("${spring.datasource.password}")
  private String password;

  /**
   * 로컬 개발용 PostgreSQL DataSource
   */
  @Bean
  @Primary
  public HikariDataSource localDataSource() {
    log.info("로컬 PostgreSQL DataSource 생성");

    HikariDataSource hds = new HikariDataSource();
    hds.setJdbcUrl(jdbcUrl);
    hds.setUsername(username);
    hds.setPassword(password);
    hds.setDriverClassName("org.postgresql.Driver");

    // HikariCP 설정
    hds.setMaximumPoolSize(5);
    hds.setMinimumIdle(2);
    hds.setConnectionTimeout(20000);
    hds.setIdleTimeout(300000);
    hds.setMaxLifetime(1200000);
    hds.setPoolName("Local-HikariCP");

    log.info("로컬 PostgreSQL 연결 완료: {}", jdbcUrl);

    return hds;
  }
}
