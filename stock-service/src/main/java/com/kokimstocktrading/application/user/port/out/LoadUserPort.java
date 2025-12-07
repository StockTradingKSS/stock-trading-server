package com.kokimstocktrading.application.user.port.out;

import com.kokimstocktrading.domain.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadUserPort {
    Optional<User> findById(UUID id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    boolean existsByUsername(String username);
}
