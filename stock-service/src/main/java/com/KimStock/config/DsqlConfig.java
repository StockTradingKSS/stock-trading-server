package com.KimStock.config;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.dsql.DsqlUtilities;
import software.amazon.awssdk.services.dsql.model.GenerateAuthTokenRequest;
import software.amazon.awssdk.regions.Region;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class DsqlConfig {

    @Value("${app.datasource.dsql.endpoint:3iabuhgpuyweeglhfr73mskaoq.dsql.ap-northeast-2.on.aws}")
    private String dsqlEndpoint;

    @Value("${app.datasource.dsql.user:admin}")
    private String dsqlUser;

    @Value("${app.datasource.dsql.database:stock-trading-db}")
    private String dsqlDatabase;

    @Value("${app.datasource.dsql.region:ap-northeast-2}")
    private String region;

    // AWS Credentials (환경변수에서 읽기)
    @Value("${AWS_ACCESS_KEY_ID:}")
    private String awsAccessKey;

    @Value("${AWS_SECRET_ACCESS_KEY:}")
    private String awsSecretKey;

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
     * DSQL IAM 토큰 생성
     */
    public String generateDsqlToken() {
        try {
            GenerateAuthTokenRequest tokenRequest = GenerateAuthTokenRequest.builder()
                    .hostname(dsqlEndpoint)
                    .region(Region.of(region))
                    .build();

            DsqlUtilities utilities = getDsqlUtilities();
            
            if ("admin".equals(dsqlUser)) {
                return utilities.generateDbConnectAdminAuthToken(tokenRequest);
            } else {
                return utilities.generateDbConnectAuthToken(tokenRequest);
            }
        } catch (Exception e) {
            log.error("DSQL 토큰 생성 실패", e);
            throw new RuntimeException("DSQL 토큰 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * DSQL에 연결하는 Connection 생성
     */
    public Connection getDsqlConnection() throws SQLException {
        try {
            log.debug("DSQL 연결 시도 - Endpoint: {}, User: {}, Database: {}", 
                    dsqlEndpoint, dsqlUser, dsqlDatabase);

            String password = generateDsqlToken();

            Properties props = new Properties();
            props.setProperty("user", dsqlUser);
            props.setProperty("password", password);
            props.setProperty("sslmode", "verify-full");
            props.setProperty("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
            props.setProperty("sslNegotiation", "direct");
            // DSQL에서 스키마 사용을 위한 설정
            props.setProperty("currentSchema", "stock_trading");

            String url = String.format("jdbc:postgresql://%s:5432/%s", dsqlEndpoint, dsqlDatabase);
            
            Connection conn = DriverManager.getConnection(url, props);
            
            // 연결 후 스키마 설정
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET search_path TO stock_trading, public");
            }
            
            log.debug("DSQL 연결 성공");
            return conn;

        } catch (Exception e) {
            log.error("DSQL 연결 실패", e);
            throw new SQLException("DSQL 연결 실패: " + e.getMessage(), e);
        }
    }

    /**
     * DSQL용 DataSource Bean - HikariCP 없이 간단한 구현
     */
    @Bean
    @Primary
    public DataSource dsqlDataSource() {
        return new DsqlDataSource();
    }

    /**
     * DSQL 전용 DataSource 구현체
     * HikariCP 대신 단순한 연결 방식 사용
     */
    private class DsqlDataSource implements DataSource {

        @Override
        public Connection getConnection() throws SQLException {
            return getDsqlConnection();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getDsqlConnection();
        }

        @Override
        public java.io.PrintWriter getLogWriter() throws SQLException {
            return null;
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) throws SQLException {
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getLogger("DsqlDataSource");
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isAssignableFrom(getClass())) {
                return iface.cast(this);
            }
            throw new SQLException("DataSource does not wrap " + iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isAssignableFrom(getClass());
        }
    }
}
