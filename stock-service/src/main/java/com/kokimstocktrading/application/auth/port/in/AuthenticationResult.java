package com.kokimstocktrading.application.auth.port.in;

import com.kokimstocktrading.domain.user.Role;

public record AuthenticationResult(
    String token,
    Long userId,
    String username,
    Role role
) {}
