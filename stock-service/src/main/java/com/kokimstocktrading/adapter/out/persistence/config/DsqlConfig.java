package com.kokimstocktrading.adapter.out.persistence.config;

import com.zaxxer.hikari.HikariDataSource;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.dsql.DsqlUtilities;
import software.amazon.awssdk.services.dsql.model.GenerateAuthTokenRequest;
import software.amazon.awssdk.regions.Region;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.function.Consumer;

@Slf4j
@Configuration
@EnableScheduling
public class DsqlConfig {

    @Value("${app.datasource.dsql.endpoint:3iabuhgpuyweeglhfr73mskaoq.dsql.ap-northeast-2.on.aws}")
    private String dsqlEndpoint;

    @Value("${app.datasource.dsql.user:admin}")
    private String dsqlUser;

    @Value("${app.datasource.dsql.database:postgres}")
    private String dsqlDatabase;

    @Value("${app.datasource.dsql.region:ap-northeast-2}")
    private String region;

    // AWS Credentials (환경변수에서 읽기)
    @Value("${DSQL_AWS_ACCESS_KEY_ID:}")
    private String awsAccessKey;

    @Value("${DSQL_AWS_SECRET_ACCESS_KEY:}")
    private String awsSecretKey;

    private HikariDataSource dataSource;
    private DsqlUtilities dsqlUtilities;

    /**
     * DSQL Utilities 초기화
     */
    private DsqlUtilities getDsqlUtilities() {
        if (dsqlUtilities == null) {
            try {
                if (!awsAccessKey.isEmpty() && !awsSecretKey.isEmpty()) {
                    // 명시적 credentials 사용
                    log.info("명시적 AWS Credentials 사용");
                    AwsBasicCredentials credentials = AwsBasicCredentials.create(awsAccessKey, awsSecretKey);
                    dsqlUtilities = DsqlUtilities.builder()
                            .region(Region.of(region))
                            .credentialsProvider(StaticCredentialsProvider.create(credentials))
                            .build();
                } else {
                    // 기본 credential chain 사용
                    log.info("기본 AWS Credential Chain 사용");
                    dsqlUtilities = DsqlUtilities.builder()
                            .region(Region.of(region))
                            .credentialsProvider(DefaultCredentialsProvider.create())
                            .build();
                }
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
        log.info("DSQL HikariCP DataSource 생성");
        
        HikariDataSource hds = new HikariDataSource();
        hds.setJdbcUrl(String.format("jdbc:postgresql://%s:5432/%s", dsqlEndpoint, dsqlDatabase));
        hds.setUsername(dsqlUser);
        hds.setPassword(""); // 초기에는 빈 패스워드, 토큰으로 업데이트
        
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
     * DSQL IAM 토큰 생성 및 설정
     */
    @Scheduled(fixedRateString = "${app.dsql.token.refresh-interval:600000}") // 10분마다 토큰 갱신
    public void generateToken() {
        try {
            log.info("DSQL 토큰 생성 시작 - Region: {}, User: {}", region, dsqlUser);
            
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
                log.info("DSQL 토큰 생성 완료");
            }
            
        } catch (Exception e) {
            log.error("DSQL 토큰 생성 실패", e);
            throw new RuntimeException("DSQL 토큰 생성 실패: " + e.getMessage(), e);
        }
    }
}
