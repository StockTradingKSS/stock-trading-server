package com.KimStock.adapter.in.web.kiwoom;

import com.KimStock.application.port.out.RequestStockOrderPort;
import com.KimStock.domain.model.type.TradeType;
import com.common.WebAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@WebAdapter
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kiwoom/orders")
@Tag(name = "Kiwoom Orders", description = "키움증권 주문 API")
public class KiwoomOrderController {

    private final RequestStockOrderPort requestStockOrderPort;

    @PostMapping("/buy")
    @Operation(summary = "매수 주문", description = "주식 매수 주문을 실행합니다.")
    public Mono<ResponseEntity<OrderResponse>> buyStock(@Valid @RequestBody OrderRequest request) {
        log.info("매수 주문 요청: {}", request);

        // 시장가 주문인 경우 단가를 0으로 설정
        double price = 0;
        if (request.tradeType() == TradeType.LIMIT) {
            if (request.price() == null || request.price() <= 0) {
                return Mono.error(new IllegalArgumentException("지정가 주문은 단가를 입력해야 합니다."));
            }
            price = request.price();
        }

        return requestStockOrderPort.requestBuyStock(
                        request.stockCode(),
                        request.quantity(),
                        price,
                        request.tradeType()
                )
                .map(orderResult -> {
                    log.info("매수 주문 성공: {}", orderResult.getOrderNo());
                    return ResponseEntity.ok(OrderResponse.from(orderResult));
                });
    }

    @PostMapping("/sell")
    @Operation(summary = "매도 주문", description = "주식 매도 주문을 실행합니다.")
    public Mono<ResponseEntity<OrderResponse>> sellStock(@Valid @RequestBody OrderRequest request) {
        log.info("매도 주문 요청: {}", request);

        // 시장가 주문인 경우 단가를 0으로 설정
        double price = 0;
        if (request.tradeType() == TradeType.LIMIT) {
            if (request.price() == null || request.price() <= 0) {
                return Mono.error(new IllegalArgumentException("지정가 주문은 단가를 입력해야 합니다."));
            }
            price = request.price();
        }

        return requestStockOrderPort.requestSellStock(
                        request.stockCode(),
                        request.quantity(),
                        price,
                        request.tradeType()
                )
                .map(orderResult -> {
                    log.info("매도 주문 성공: {}", orderResult.getOrderNo());
                    return ResponseEntity.ok(OrderResponse.from(orderResult));
                });
    }

    @PutMapping("/modify")
    @Operation(summary = "정정 주문", description = "기존 주문을 정정합니다.")
    public Mono<ResponseEntity<OrderResponse>> modifyOrder(@Valid @RequestBody OrderModifyRequest request) {
        log.info("정정 주문 요청: {}", request);

        if (request.price() == null || request.price() <= 0) {
            return Mono.error(new IllegalArgumentException("정정 주문은 단가를 입력해야 합니다."));
        }

        return requestStockOrderPort.requestModifyOrder(
                        request.orderNo(),
                        request.stockCode(),
                        request.quantity(),
                        request.price()
                )
                .map(orderResult -> {
                    log.info("정정 주문 성공: {}", orderResult.getOrderNo());
                    return ResponseEntity.ok(OrderResponse.from(orderResult));
                });
    }

    @PutMapping("/cancel")
    @Operation(summary = "취소 주문", description = "기존 주문을 취소합니다. quantity가 0이면 전량 취소됩니다.")
    public Mono<ResponseEntity<OrderResponse>> cancelOrder(@Valid @RequestBody OrderCancelRequest request) {
        log.info("취소 주문 요청: {}", request);

        Mono<OrderResponse> response;

        if (request.quantity() == null || request.quantity() == 0) {
            response = requestStockOrderPort.requestCancelOrder(request.orderNo(), request.stockCode())
                    .map(OrderResponse::from);
        } else {
            response = requestStockOrderPort.requestCancelOrder(request.orderNo(), request.stockCode(), request.quantity())
                    .map(OrderResponse::from);
        }

        return response.map(result -> {
            log.info("취소 주문 성공: {}", result.orderNo());
            return ResponseEntity.ok(result);
        });
    }
}
