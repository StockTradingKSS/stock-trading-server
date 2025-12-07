package com.kokimstocktrading.application.user.port.in;

import com.kokimstocktrading.domain.user.User;

public interface UpdateUserUseCase {
    User updateUser(Long id, UpdateUserCommand command);
}
