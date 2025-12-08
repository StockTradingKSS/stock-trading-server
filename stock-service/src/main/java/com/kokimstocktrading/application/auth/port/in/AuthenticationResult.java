package com.kokimstocktrading.application.auth.port.in;

import com.kokimstocktrading.domain.user.Role;
import java.util.UUID;

public record AuthenticationResult(
    String token,
    UUID userId,
    String username,
    Role role
) {

}
