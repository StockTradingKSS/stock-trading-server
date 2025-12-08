package com.kokimstocktrading.application.user.port.in;

import java.util.UUID;

public interface DeleteUserUseCase {

  void deleteUser(UUID id);
}
