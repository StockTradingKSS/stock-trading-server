package com.kokimstocktrading.application.auth.port.out;

import java.util.UUID;

public interface DeleteTokenPort {
    void deleteByToken(String token);
    void deleteAllByUserId(UUID userId);
}
