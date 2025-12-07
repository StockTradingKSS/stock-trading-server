package com.kokimstocktrading.application.user.port.out;

import java.time.LocalDateTime;

public interface UpdateLastLoginPort {
    void updateLastLogin(Long userId, LocalDateTime lastLogin);
}
