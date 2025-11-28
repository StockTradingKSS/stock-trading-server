package com.kokimstocktrading.adapter.in.web.stock;

import com.common.WebAdapter;
import com.kokimstocktrading.adapter.out.persistence.stock.StockEntity;
import com.kokimstocktrading.adapter.out.persistence.stock.StockRepository;
import com.kokimstocktrading.application.stock.port.in.RefreshStockUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebAdapter
@RestController
@RequestMapping("/api/stock")
@Tag(name = "Stock API", description = "Stock 관련 API")
@RequiredArgsConstructor
@Slf4j
public class RefreshStockController {

  private final RefreshStockUseCase refreshStockUseCase;
  private final StockRepository stockRepository;

  @PostMapping("/refresh")
  @Operation(summary = "종목 정보 갱신", description = "종목 정보를 외부 API로 받아와 갱신 합니다.")
  public boolean refreshStock() {
    return refreshStockUseCase.refreshStock();
  }

  @PostMapping("/test-upsert")
  @Operation(summary = "UPSERT 쿼리 테스트", description = "실제 UPSERT 쿼리가 어떻게 실행되는지 테스트합니다.")
  public String testUpsert() {
    log.info("=== UPSERT 쿼리 테스트 시작 ===");

    // 테스트용 주식 데이터 생성
    StockEntity stock1 = StockEntity.builder()
        .code("005930")
        .name("삼성전자")
        .listCount(1000L)
        .auditInfo("정기감사")
        .regDay("2024-01-01")
        .state("정상")
        .marketCode("KOSPI")
        .marketName("코스피")
        .upName("전기전자")
        .upSizeName("대형주")
        .companyClassName("제조업")
        .orderWarning("없음")
        .nxtEnable(true)
        .build();

    StockEntity stock2 = StockEntity.builder()
        .code("000660")
        .name("SK하이닉스")
        .listCount(500L)
        .auditInfo("정기감사")
        .regDay("2024-01-01")
        .state("정상")
        .marketCode("KOSPI")
        .marketName("코스피")
        .upName("전기전자")
        .upSizeName("대형주")
        .companyClassName("제조업")
        .orderWarning("없음")
        .nxtEnable(true)
        .build();

    log.info("=== 개별 UPSERT 테스트 ===");

    // 개별 UPSERT 실행
    stockRepository.upsertStock(stock1);
    stockRepository.upsertStock(stock2);

    log.info("=== 배치 UPSERT 테스트 ===");

    // 배치 UPSERT 실행
    String[] codes = {"005930", "000660"};
    String[] names = {"삼성전자", "SK하이닉스"};
    Long[] listCounts = {1000L, 500L};
    String[] auditInfos = {"정기감사", "정기감사"};
    String[] regDays = {"2024-01-01", "2024-01-01"};
    String[] states = {"정상", "정상"};
    String[] marketCodes = {"KOSPI", "KOSPI"};
    String[] marketNames = {"코스피", "코스피"};
    String[] upNames = {"전기전자", "전기전자"};
    String[] upSizeNames = {"대형주", "대형주"};
    String[] companyClassNames = {"제조업", "제조업"};
    String[] orderWarnings = {"없음", "없음"};
    Boolean[] nxtEnables = {true, true};

    stockRepository.batchUpsertStocks(
        codes, names, listCounts, auditInfos, regDays, states,
        marketCodes, marketNames, upNames, upSizeNames,
        companyClassNames, orderWarnings, nxtEnables
    );

    log.info("=== UPSERT 쿼리 테스트 완료 ===");
    return "UPSERT 테스트 완료 - 로그에서 실제 실행된 쿼리를 확인하세요.";
  }
}
