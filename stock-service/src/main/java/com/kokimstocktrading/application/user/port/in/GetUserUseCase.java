package com.kokimstocktrading.application.user.port.in;

import com.kokimstocktrading.domain.user.User;
import java.util.List;
import java.util.UUID;

public interface GetUserUseCase {

  User getUserById(UUID id);

  User getUserByUsername(String username);

  List<User> getAllUsers();
}
