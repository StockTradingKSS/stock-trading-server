package com.kokimstocktrading.application.user.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

public interface UpdateLastLoginPort {
    void updateLastLogin(UUID userId, LocalDateTime lastLogin);
}
