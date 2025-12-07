package com.kokimstocktrading.application.auth.port.in;

public record LoginCommand(
    String username,
    String password
) {}
