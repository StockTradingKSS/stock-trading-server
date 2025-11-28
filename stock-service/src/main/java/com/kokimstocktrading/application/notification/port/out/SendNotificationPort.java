package com.kokimstocktrading.application.notification.port.out;

import reactor.core.publisher.Mono;

/**
 * 알림 전송 포트
 */
public interface SendNotificationPort {

  /**
   * 카카오톡으로 알림 메시지 전송
   *
   * @param message 전송할 메시지
   * @return 전송 결과
   */
  Mono<Void> sendKakaoMessage(String message);
}
