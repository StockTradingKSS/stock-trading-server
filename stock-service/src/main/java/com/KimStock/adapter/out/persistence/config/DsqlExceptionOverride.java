package com.KimStock.adapter.out.persistence.config;

import com.zaxxer.hikari.SQLExceptionOverride;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;

/**
 * DSQL 연결에서 발생하는 특정 예외들을 처리하여 연결 풀에서 제거하지 않도록 함
 */
@Slf4j
public class DsqlExceptionOverride implements SQLExceptionOverride {

    @java.lang.Override
    public SQLExceptionOverride.Override adjudicate(SQLException ex) {
        log.debug("DSQL 예외 처리: {}", ex.getMessage());
        log.debug("SQL State: {}", ex.getSQLState());

        String sqlState = ex.getSQLState();

        // DSQL에서 재시도 가능한 예외들
        if ("0C000".equalsIgnoreCase(sqlState) || "0C001".equalsIgnoreCase(sqlState)
                || (sqlState != null && sqlState.matches("0A\\d{3}"))) {
            log.info("재시도 가능한 DSQL 예외 감지: {}", sqlState);
            return SQLExceptionOverride.Override.DO_NOT_EVICT;
        }

        // 기타 경우에는 기본 처리 방식 사용
        return Override.CONTINUE_EVICT;
    }
} 
