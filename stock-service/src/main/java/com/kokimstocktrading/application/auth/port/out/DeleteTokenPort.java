package com.kokimstocktrading.application.auth.port.out;

public interface DeleteTokenPort {
    void deleteByToken(String token);
    void deleteAllByUserId(Long userId);
}
