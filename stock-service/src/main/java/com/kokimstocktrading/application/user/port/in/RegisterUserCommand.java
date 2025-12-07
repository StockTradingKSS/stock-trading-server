package com.kokimstocktrading.application.user.port.in;

import com.kokimstocktrading.domain.user.Role;

public record RegisterUserCommand(
    String username,
    String password,
    String email,
    String name,
    Role role
) {}
