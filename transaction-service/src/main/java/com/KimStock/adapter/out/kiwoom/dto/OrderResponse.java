package com.KimStock.adapter.out.kiwoom.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderResponse(
        @JsonProperty("ord_no") String orderNo,                  // 주문번호
        @JsonProperty("dmst_stex_tp") String marketType,         // 국내거래소구분
        @JsonProperty("base_orig_ord_no") String baseOriginalOrderNo,  // 모주문번호 (정정/취소 시)
        @JsonProperty("mdfy_qty") String modifyQuantity,         // 정정수량 (정정 시)
        @JsonProperty("cncl_qty") String cancelQuantity,         // 취소수량 (취소 시)
        @JsonProperty("return_code") Integer returnCode,         // 응답 코드 (0: 정상)
        @JsonProperty("return_msg") String returnMessage         // 응답 메시지
) {
    /**
     * 응답이 정상인지 확인
     * @return 정상 여부
     */
    public boolean isSuccess() {
        return returnCode != null && returnCode == 0;
    }
}