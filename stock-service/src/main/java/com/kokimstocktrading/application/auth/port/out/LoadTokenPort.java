package com.kokimstocktrading.application.auth.port.out;

import com.kokimstocktrading.domain.user.UserToken;

import java.util.Optional;

public interface LoadTokenPort {
    Optional<UserToken> findByToken(String token);
    boolean existsByToken(String token);
}
