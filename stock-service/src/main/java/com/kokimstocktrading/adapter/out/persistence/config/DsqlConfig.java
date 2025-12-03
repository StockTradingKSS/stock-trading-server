package com.kokimstocktrading.adapter.out.persistence.config;

import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dsql.DsqlUtilities;
import software.amazon.awssdk.services.dsql.model.GenerateAuthTokenRequest;

@Slf4j
@Configuration
@EnableScheduling
@Profile("prod") // 프로덕션 환경에서만 활성화
public class DsqlConfig {

  @Value("${app.datasource.dsql.endpoint}")
  private String dsqlEndpoint;

  @Value("${app.datasource.dsql.user}")
  private String dsqlUser;

  @Value("${app.datasource.dsql.database}")
  private String dsqlDatabase;

  @Value("${app.datasource.dsql.region}")
  private String region;

  private HikariDataSource dataSource;
  private DsqlUtilities dsqlUtilities;

  /**
   * DSQL Utilities 초기화 - IAM Role 사용
   */
  private DsqlUtilities getDsqlUtilities() {
    if (dsqlUtilities == null) {
      try {
        log.info("ECS Task Role을 사용한 DSQL 연결 초기화");
        dsqlUtilities = DsqlUtilities.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create()) // ECS Task Role 자동 사용
            .build();
        log.info("DSQL Utilities 초기화 완료 - Region: {}", region);
      } catch (Exception e) {
        log.error("DsqlUtilities 초기화 실패", e);
        throw new RuntimeException("DsqlUtilities 초기화 실패", e);
      }
    }
    return dsqlUtilities;
  }

  /**
   * DSQL용 DataSource Bean - HikariCP 사용
   */
  @Bean
  @Primary
  public HikariDataSource dsqlDataSource() {
    log.info("DSQL HikariCP DataSource 생성 (프로덕션)");

    HikariDataSource hds = new HikariDataSource();
    hds.setJdbcUrl(String.format("jdbc:postgresql://%s:5432/%s", dsqlEndpoint, dsqlDatabase));
    hds.setUsername(dsqlUser);
    hds.setPassword(""); // 초기에는 빈 패스워드, IAM 토큰으로 업데이트

    // HikariCP 설정
    hds.setMaximumPoolSize(10);
    hds.setMinimumIdle(5);
    hds.setConnectionTimeout(30000);
    hds.setIdleTimeout(600000);
    hds.setMaxLifetime(1800000); // 30분 (토큰 만료 전)
    hds.setLeakDetectionThreshold(60000);
    hds.setPoolName("DSQL-HikariCP");

    // DSQL 전용 설정
    hds.addDataSourceProperty("sslmode", "verify-full");
    hds.addDataSourceProperty("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
    hds.addDataSourceProperty("sslNegotiation", "direct");
    hds.addDataSourceProperty("currentSchema", "stock_trading");

    // 예외 처리 클래스 설정
    hds.setExceptionOverrideClassName(DsqlExceptionOverride.class.getName());

    this.dataSource = hds;

    // 초기 토큰 생성
    generateToken();

    return hds;
  }

  /**
   * DSQL IAM 토큰 생성 및 설정 - ECS Task Role 사용
   */
  @Scheduled(fixedRateString = "${app.dsql.token.refresh-interval:600000}") // 10분마다 토큰 갱신
  public void generateToken() {
    try {
      DsqlUtilities utilities = getDsqlUtilities();

      final Consumer<GenerateAuthTokenRequest.Builder> requester = builder -> builder
          .hostname(dsqlEndpoint)
          .region(Region.of(region))
          .expiresIn(Duration.ofMillis(1800000)); // 30분

      String token;
      if ("admin".equals(dsqlUser)) {
        token = utilities.generateDbConnectAdminAuthToken(requester);
      } else {
        token = utilities.generateDbConnectAuthToken(requester);
      }

      if (dataSource != null) {
        dataSource.setPassword(token);
      }

    } catch (Exception e) {
      log.error("DSQL 토큰 생성 실패", e);
      throw new RuntimeException("DSQL 토큰 생성 실패: " + e.getMessage(), e);
    }
  }
}
