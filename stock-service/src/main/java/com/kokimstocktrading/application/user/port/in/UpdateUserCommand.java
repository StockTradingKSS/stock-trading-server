package com.kokimstocktrading.application.user.port.in;

public record UpdateUserCommand(
    String email,
    String name,
    String password  // Optional - only update if provided
) {}
