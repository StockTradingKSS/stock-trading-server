package com.kokimstocktrading.application.user.port.in;

import com.kokimstocktrading.domain.user.User;

public interface RegisterUserUseCase {
    User registerUser(RegisterUserCommand command);
}
