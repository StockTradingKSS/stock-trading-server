package com.kokimstocktrading.adapter.out.external.config.kiwoom;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class KiwoomWebClientConfig {

  @Value("${kiwoom.api.base-url}")
  private String baseUrl;

  @Bean(name = "kiwoomWebClient")
  public WebClient kiwoomInvestmentWebClient() {
    // 메모리 제한 설정 확장 (기본값: 256KB)
    final int size = 16 * 1024 * 1024; // 16MB
    final ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
        .build();

    // Connection Pool 설정
    ConnectionProvider connectionProvider = ConnectionProvider.builder(
            "kiwoom-investment-connection-pool")
        .maxConnections(500)
        .maxIdleTime(Duration.ofSeconds(60))
        .maxLifeTime(Duration.ofMinutes(10))
        .pendingAcquireTimeout(Duration.ofSeconds(60))
        .evictInBackground(Duration.ofSeconds(120))
        .build();

    // HTTP Client 설정
    HttpClient httpClient = HttpClient.create(connectionProvider)
        .responseTimeout(Duration.ofSeconds(30));

    ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

    return WebClient.builder()
        .clientConnector(connector)
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader("charset", "UTF-8")
        .exchangeStrategies(strategies)
        .build();
  }
}
