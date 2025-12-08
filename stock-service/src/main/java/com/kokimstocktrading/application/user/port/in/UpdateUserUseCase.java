package com.kokimstocktrading.application.user.port.in;

import com.kokimstocktrading.domain.user.User;
import java.util.UUID;

public interface UpdateUserUseCase {

  User updateUser(UUID id, UpdateUserCommand command);
}
