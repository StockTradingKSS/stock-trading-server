package com.kokimstocktrading.adapter.out.external.kiwoom;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KiwoomWebSocketClient extends WebSocketClient {

    private final String accessToken;
    private final Gson gson = new Gson();
    private boolean isConnected = false;
    private final CountDownLatch loginLatch = new CountDownLatch(1);
    @Setter
    private Sinks.Many<Map<String, Object>> messageSink;

    // 현재 구독 중인 그룹 관리
    private final Map<String, List<String>> subscribedGroups = new ConcurrentHashMap<>();
    private int currentGroupNo = 1;  // 그룹 번호 시작값

    public KiwoomWebSocketClient(URI serverUri, String accessToken) {
        super(serverUri);
        this.accessToken = accessToken;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.info("WebSocket 연결됨");
        sendLoginMessage();
    }

    @Override
    public void onMessage(String message) {
        try {
            log.debug("WebSocket 메시지 수신: {}", message);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = gson.fromJson(message, Map.class);
            String trnm = (String) response.get("trnm");

            if ("LOGIN".equals(trnm)) {
                double returnCode = ((Double) response.get("return_code"));
                if (returnCode != 0) {
                    log.error("로그인 실패: {}", response.get("return_msg"));
                    close();
                } else {
                    log.info("로그인 성공");
                    isConnected = true;
                    loginLatch.countDown();
                }
            } else if ("PING".equals(trnm)) {
                // PING 메시지 그대로 응답
                send(message);
            } else if ("REAL".equals(trnm)) {
                // 실시간 데이터 메시지 처리
                if (messageSink != null) {
                    messageSink.tryEmitNext(response);
                }
            } else if ("REG".equals(trnm) || "REMOVE".equals(trnm)) {
                double returnCode = ((Double) response.get("return_code"));
                if (returnCode != 0) {
                    log.error("{} 요청 실패: {}", trnm, response.get("return_msg"));
                } else {
                    log.info("{} 요청 성공", trnm);
                }
            }
        } catch (Exception e) {
            log.error("WebSocket 메시지 처리 중 오류: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("WebSocket 연결 종료: 코드={}, 이유={}, 원격={}", code, reason, remote);
        isConnected = false;
    }

    @Override
    public void onError(Exception ex) {
        log.error("WebSocket 오류: {}", ex.getMessage(), ex);
    }

    /**
     * 로그인 메시지 전송
     */
    private void sendLoginMessage() {
        JsonObject loginMessage = new JsonObject();
        loginMessage.addProperty("trnm", "LOGIN");
        loginMessage.addProperty("token", accessToken);
        send(gson.toJson(loginMessage));
        log.info("로그인 메시지 전송");
    }

    /**
     * 종목 실시간 시세 구독 등록
     *
     * @param stockCodes 종목 코드 목록
     * @return 그룹 번호
     */
    public String subscribeStocks(List<String> stockCodes) {
        // 연결 상태 확인 및 대기
        try {
            if (!isConnected && !loginLatch.await(5, TimeUnit.SECONDS)) {
                log.error("WebSocket 연결 또는 로그인 대기 시간 초과");
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("WebSocket 연결 대기 중 인터럽트 발생", e);
            return null;
        }

        String groupNo = String.valueOf(currentGroupNo++);

        JsonObject registerMessage = new JsonObject();
        registerMessage.addProperty("trnm", "REG");
        registerMessage.addProperty("grp_no", groupNo);
        registerMessage.addProperty("refresh", "1");  // 기존 등록 유지

        // 종목 코드 배열 생성
        JsonArray itemArray = new JsonArray();
        stockCodes.forEach(itemArray::add);

        // 실시간 항목 유형 설정 (0B: 주식체결)
        JsonArray typeArray = new JsonArray();
        typeArray.add("0B");

        // 데이터 객체 생성
        JsonObject dataObject = new JsonObject();
        dataObject.add("item", itemArray);
        dataObject.add("type", typeArray);

        // 데이터 배열에 추가
        JsonArray dataArray = new JsonArray();
        dataArray.add(dataObject);
        registerMessage.add("data", dataArray);

        // 메시지 전송
        send(gson.toJson(registerMessage));
        log.info("실시간 시세 구독 등록 메시지 전송. 그룹번호: {}, 종목: {}", groupNo, stockCodes);

        // 구독 정보 저장
        subscribedGroups.put(groupNo, stockCodes);

        return groupNo;
    }

    /**
     * 종목 실시간 시세 구독 해지
     *
     * @param groupNo 해지할 그룹 번호
     * @return 성공 여부
     */
    public boolean unsubscribeStocks(String groupNo) {
        if (!isConnected) {
            log.error("WebSocket 연결이 끊어진 상태에서 구독 해지 시도");
            return false;
        }

        JsonObject removeMessage = new JsonObject();
        removeMessage.addProperty("trnm", "REMOVE");
        removeMessage.addProperty("grp_no", groupNo);

        send(gson.toJson(removeMessage));
        log.info("실시간 시세 구독 해지 메시지 전송. 그룹번호: {}", groupNo);

        // 구독 정보 제거
        subscribedGroups.remove(groupNo);

        return true;
    }

    /**
     * 모든 그룹 구독 해지
     *
     * @return 성공 여부
     */
    public boolean unsubscribeAllGroups() {
        if (!isConnected) {
            log.error("WebSocket 연결이 끊어진 상태에서 모든 구독 해지 시도");
            return false;
        }

        boolean allSuccess = true;
        for (String groupNo : subscribedGroups.keySet()) {
            boolean success = unsubscribeStocks(groupNo);
            if (!success) {
                allSuccess = false;
            }
        }

        return allSuccess;
    }

    /**
     * 연결 상태 확인
     *
     * @return 연결 상태
     */
    public boolean isConnected() {
        return isConnected && isOpen();
    }
}
