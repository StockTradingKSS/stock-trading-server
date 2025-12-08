package com.kokimstocktrading.adapter.in.web.stock;

import com.common.Authorize;
import com.common.WebAdapter;
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

  @PostMapping("/refresh")
  @Authorize(roles = {"TRADER", "ADMIN"})
  @Operation(summary = "종목 정보 갱신", description = "종목 정보를 외부 API로 받아와 갱신 합니다.")
  public boolean refreshStock() {
    return refreshStockUseCase.refreshStock();
  }
}
