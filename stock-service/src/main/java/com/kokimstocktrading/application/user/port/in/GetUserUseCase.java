package com.kokimstocktrading.application.user.port.in;

import com.kokimstocktrading.domain.user.User;

import java.util.List;

public interface GetUserUseCase {
    User getUserById(Long id);
    User getUserByUsername(String username);
    List<User> getAllUsers();
}
