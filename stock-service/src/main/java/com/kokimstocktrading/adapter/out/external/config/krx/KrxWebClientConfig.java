package com.kokimstocktrading.adapter.out.external.config.krx;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * KRX API 전용 WebClient 설정 - 하루 몇 번만 호출하는 특성에 맞게 최적화 - 메모리 사용량 최소화 및 안정성 확보
 */
@Configuration
public class KrxWebClientConfig {

  @Value("${krx.api.base-url}")
  private String baseUrl;

  @Bean(name = "krxWebClient")
  public WebClient krxWebClient() {
    // 메모리 제한 설정 (KRX HTML 응답용)
    final int maxMemorySize = 512 * 1024; // 512KB (HTML 파싱용)
    final ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(maxMemorySize))
        .build();

    // Connection Pool 설정 (최소한으로 설정)
    ConnectionProvider connectionProvider = ConnectionProvider.builder("krx-connection-pool")
        .maxConnections(2)           // 최대 2개 연결 (동시 요청 없음)
        .maxIdleTime(Duration.ofSeconds(30))    // 30초 후 연결 해제
        .maxLifeTime(Duration.ofMinutes(5))     // 5분 후 연결 갱신
        .pendingAcquireTimeout(Duration.ofSeconds(10))
        .evictInBackground(Duration.ofSeconds(60))
        .build();

    // HTTP Client 설정
    HttpClient httpClient = HttpClient.create(connectionProvider)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)  // 연결 타임아웃 10초
        .responseTimeout(Duration.ofSeconds(15))              // 응답 타임아웃 15초
        .doOnConnected(conn ->
            conn.addHandlerLast(new ReadTimeoutHandler(15, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
        );

    return WebClient.builder()
        .baseUrl(baseUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(strategies)
        .build();
  }
}
