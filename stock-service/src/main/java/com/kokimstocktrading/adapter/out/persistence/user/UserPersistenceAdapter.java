package com.kokimstocktrading.adapter.out.persistence.user;

import com.common.PersistenceAdapter;
import com.kokimstocktrading.application.user.port.out.DeleteUserPort;
import com.kokimstocktrading.application.user.port.out.LoadUserPort;
import com.kokimstocktrading.application.user.port.out.SaveUserPort;
import com.kokimstocktrading.application.user.port.out.UpdateLastLoginPort;
import com.kokimstocktrading.domain.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Persistence Adapter 모든 사용자 관련 출력 포트 구현
 */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class UserPersistenceAdapter implements SaveUserPort, LoadUserPort,
    DeleteUserPort, UpdateLastLoginPort {

  private final UserRepository userRepository;

  @Override
  @Transactional
  public User save(User user) {
    UserEntity entity = UserEntity.from(user);
    UserEntity saved = userRepository.save(entity);
    return saved.toDomain();
  }

  @Override
  public Optional<User> findById(UUID id) {
    return userRepository.findById(id)
        .map(UserEntity::toDomain);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username)
        .map(UserEntity::toDomain);
  }

  @Override
  public List<User> findAll() {
    return userRepository.findAll().stream()
        .map(UserEntity::toDomain)
        .toList();
  }

  @Override
  public boolean existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }

  @Override
  @Transactional
  public void deleteById(UUID id) {
    userRepository.deleteById(id);
  }

  @Override
  @Transactional
  public void updateLastLogin(UUID userId, LocalDateTime lastLogin) {
    userRepository.updateLastLogin(userId, lastLogin);
  }
}
