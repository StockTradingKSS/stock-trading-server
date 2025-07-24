package com.kokimstocktrading.adapter.in.web.healthcheck;

import com.common.WebAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebAdapter
@RestController
@RequestMapping("/api/stock-trading")
@Slf4j
public class HealthCheckController {

    @GetMapping("/v1/health-check")
    public boolean healthCheck() {
        return true;
    }
}
