package com.kokimstocktrading.application.monitoring.mock;

import com.kokimstocktrading.application.realtime.out.SubscribeRealTimeQuotePort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class RealTimeQuotePortConfig {

  @Bean
  @Primary
  public SubscribeRealTimeQuotePort subscribeRealTimeQuotePort() {
    return new MockRealTimeQuoteAdapter();
  }
}
