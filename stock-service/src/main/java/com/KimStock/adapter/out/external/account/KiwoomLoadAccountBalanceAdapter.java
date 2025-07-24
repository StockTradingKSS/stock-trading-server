package com.KimStock.adapter.out.external.account;

import com.KimStock.adapter.out.external.error.ClientErrorHandler;
import com.KimStock.adapter.out.external.kiwoom.auth.KiwoomAuthAdapter;
import com.KimStock.application.port.out.LoadAccountBalancePort;
import com.KimStock.domain.model.AccountBalance;
import com.KimStock.domain.model.HoldingStock;
import com.common.ExternalSystemAdapter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@ExternalSystemAdapter
@Slf4j
public class KiwoomLoadAccountBalanceAdapter implements LoadAccountBalancePort {
    private final WebClient webClient;
    private final KiwoomAuthAdapter kiwoomAuthAdapter;
    private final ClientErrorHandler clientErrorHandler;

    public KiwoomLoadAccountBalanceAdapter(
            @Qualifier("kiwoomWebClient") WebClient kiwoomWebClient,
            KiwoomAuthAdapter kiwoomAuthAdapter, ClientErrorHandler clientErrorHandler) {

        this.webClient = kiwoomWebClient;
        this.kiwoomAuthAdapter = kiwoomAuthAdapter;
        this.clientErrorHandler = clientErrorHandler;
    }

    @Override
    public Mono<AccountBalance> loadAccountBalance() {
        AccountBalanceRequest request = AccountBalanceRequest.getDefaultRequest();

        return kiwoomAuthAdapter.getValidToken()
                .flatMap(token -> loadAccountBalanceApi(token, request))
                .onErrorResume(e -> {
                    log.error("[Load Account Balance Error] : {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    private Mono<AccountBalance> loadAccountBalanceApi(String token, AccountBalanceRequest request) {
        return webClient.post()
                .uri("/api/dostk/acnt")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("api-id", "kt00018")
                .header("cont-yn", "N")
                .header("next-key", "")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(this::handleAccountBalanceResponse);
    }

    private Mono<AccountBalance> handleAccountBalanceResponse(ClientResponse response) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(AccountBalanceResponse.class)
                    .map(AccountBalanceResponse::mapToAccountBalance)
                    .doOnSuccess(balance -> log.info("Account balance API call successful {}", balance));
        }

        return clientErrorHandler.handleErrorResponse(response, "Account balance request failed.");
    }

    public record AccountBalanceRequest(
            @JsonProperty("qry_tp") String queryType,  // 조회구분 (1:합산, 2:개별)
            @JsonProperty("dmst_stex_tp") String domesticStockExchangeType  // 국내거래소구분 (KRX:한국거래소, NXT:넥스트트레이드)
    ) {
        public static AccountBalanceRequest getDefaultRequest() {
            return new AccountBalanceRequest("1", "KRX");
        }
    }

    public record AccountBalanceResponse(
            @JsonProperty("tot_pur_amt") String totalPurchaseAmount,  // 총매입금액
            @JsonProperty("tot_evlt_amt") String totalEvaluationAmount,  // 총평가금액
            @JsonProperty("tot_evlt_pl") String totalEvaluationProfitLoss,  // 총평가손익금액
            @JsonProperty("tot_prft_rt") String totalProfitRate,  // 총수익률(%)
            @JsonProperty("prsm_dpst_aset_amt") String estimatedDepositAsset,  // 추정예탁자산
            @JsonProperty("tot_loan_amt") String totalLoanAmount,  // 총대출금
            @JsonProperty("tot_crd_loan_amt") String totalCreditLoanAmount,  // 총융자금액
            @JsonProperty("tot_crd_ls_amt") String totalCreditLeaseAmount,  // 총대주금액
            @JsonProperty("acnt_evlt_remn_indv_tot") List<StockHoldingResponse> stockHoldings,  // 계좌평가잔고개별합산
            @JsonProperty("return_code") int returnCode,
            @JsonProperty("return_msg") String returnMessage
    ) {
        public AccountBalance mapToAccountBalance() {
            List<HoldingStock> holdingStocks = stockHoldings == null
                    ? List.of()
                    : stockHoldings.stream()
                    .map(StockHoldingResponse::mapToHoldingStock)
                    .collect(Collectors.toList());

            return AccountBalance.builder()
                    .totalPurchaseAmount(parseStringToLong(totalPurchaseAmount))
                    .totalEvaluationAmount(parseStringToLong(totalEvaluationAmount))
                    .totalEvaluationProfitLoss(parseStringToLong(totalEvaluationProfitLoss))
                    .totalProfitRate(parseStringToDouble(totalProfitRate))
                    .estimatedDepositAsset(parseStringToLong(estimatedDepositAsset))
                    .totalLoanAmount(parseStringToLong(totalLoanAmount))
                    .totalCreditLoanAmount(parseStringToLong(totalCreditLoanAmount))
                    .totalCreditLeaseAmount(parseStringToLong(totalCreditLeaseAmount))
                    .holdingStocks(holdingStocks)
                    .build();
        }

        private static long parseStringToLong(String value) {
            if (value == null || value.isEmpty()) {
                return 0;
            }
            return Long.parseLong(value.replaceAll("[^0-9-]", ""));
        }

        private static double parseStringToDouble(String value) {
            if (value == null || value.isEmpty()) {
                return 0.0;
            }
            return Double.parseDouble(value.replaceAll("[^0-9.-]", ""));
        }

        public record StockHoldingResponse(
                @JsonProperty("stk_cd") String stockCode,  // 종목번호
                @JsonProperty("stk_nm") String stockName,  // 종목명
                @JsonProperty("evltv_prft") String evaluationProfit,  // 평가손익
                @JsonProperty("prft_rt") String profitRate,  // 수익률(%)
                @JsonProperty("pur_pric") String purchasePrice,  // 매입가
                @JsonProperty("pred_close_pric") String previousClosePrice,  // 전일종가
                @JsonProperty("rmnd_qty") String remainQuantity,  // 보유수량
                @JsonProperty("trde_able_qty") String tradeableQuantity,  // 매매가능수량
                @JsonProperty("cur_prc") String currentPrice,  // 현재가
                @JsonProperty("pred_buyq") String previousBuyQuantity,  // 전일매수수량
                @JsonProperty("pred_sellq") String previousSellQuantity,  // 전일매도수량
                @JsonProperty("tdy_buyq") String todayBuyQuantity,  // 금일매수수량
                @JsonProperty("tdy_sellq") String todaySellQuantity,  // 금일매도수량
                @JsonProperty("pur_amt") String purchaseAmount,  // 매입금액
                @JsonProperty("pur_cmsn") String purchaseCommission,  // 매입수수료
                @JsonProperty("evlt_amt") String evaluationAmount,  // 평가금액
                @JsonProperty("sell_cmsn") String sellCommission,  // 평가수수료
                @JsonProperty("tax") String tax,  // 세금
                @JsonProperty("sum_cmsn") String totalCommission,  // 수수료합
                @JsonProperty("poss_rt") String possessionRate,  // 보유비중(%)
                @JsonProperty("crd_tp") String creditType,  // 신용구분
                @JsonProperty("crd_tp_nm") String creditTypeName,  // 신용구분명
                @JsonProperty("crd_loan_dt") String creditLoanDate  // 대출일
        ) {
            public HoldingStock mapToHoldingStock() {
                return HoldingStock.builder()
                        .code(stockCode)
                        .name(stockName)
                        .quantity(parseStringToInt(remainQuantity))
                        .tradeableQuantity(parseStringToInt(tradeableQuantity))
                        .purchasePrice(parseStringToLong(purchaseAmount))
                        .avgPrice(parseStringToInt(purchasePrice))
                        .currentPrice(parseStringToLong(currentPrice))
                        .evaluationAmount(parseStringToLong(evaluationAmount))
                        .profitLoss(parseStringToLong(evaluationProfit))
                        .profitLossRate(parseStringToDouble(profitRate))
                        .possessionRate(parseStringToDouble(possessionRate))
                        .creditType(creditType)
                        .creditTypeName(creditTypeName)
                        .creditLoanDate(creditLoanDate)
                        .build();
            }

            private static int parseStringToInt(String value) {
                if (value == null || value.isEmpty()) {
                    return 0;
                }
                return Integer.parseInt(value.replaceAll("[^0-9-]", ""));
            }

            private static long parseStringToLong(String value) {
                if (value == null || value.isEmpty()) {
                    return 0;
                }
                return Long.parseLong(value.replaceAll("[^0-9-]", ""));
            }

            private static double parseStringToDouble(String value) {
                if (value == null || value.isEmpty()) {
                    return 0.0;
                }
                return Double.parseDouble(value.replaceAll("[^0-9.-]", ""));
            }
        }
    }
}
