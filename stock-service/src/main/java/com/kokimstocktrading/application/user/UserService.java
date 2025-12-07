package com.kokimstocktrading.application.user;

import com.common.UseCase;
import com.kokimstocktrading.application.user.port.in.*;
import com.kokimstocktrading.application.user.port.out.*;
import com.kokimstocktrading.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User Service
 * 모든 사용자 관련 유스케이스 구현
 */
@UseCase
@Slf4j
@RequiredArgsConstructor
public class UserService implements RegisterUserUseCase, GetUserUseCase,
                                     UpdateUserUseCase, DeleteUserUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final DeleteUserPort deleteUserPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User registerUser(RegisterUserCommand command) {
        log.info("신규 사용자 등록: {}", command.username());

        if (loadUserPort.existsByUsername(command.username())) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다: " + command.username());
        }

        User user = User.builder()
            .username(command.username())
            .password(passwordEncoder.encode(command.password()))
            .email(command.email())
            .name(command.name())
            .role(command.role())
            .createdAt(LocalDateTime.now())
            .build();

        User savedUser = saveUserPort.save(user);
        log.info("사용자 등록 완료: {}", savedUser.getUsername());
        return savedUser;
    }

    @Override
    public User getUserById(UUID id) {
        return loadUserPort.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));
    }

    @Override
    public User getUserByUsername(String username) {
        return loadUserPort.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
    }

    @Override
    public List<User> getAllUsers() {
        return loadUserPort.findAll();
    }

    @Override
    public User updateUser(UUID id, UpdateUserCommand command) {
        log.info("사용자 정보 수정: {}", id);

        User existingUser = loadUserPort.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));

        User.UserBuilder builder = User.builder()
            .id(existingUser.getId())
            .username(existingUser.getUsername())
            .email(command.email() != null ? command.email() : existingUser.getEmail())
            .name(command.name() != null ? command.name() : existingUser.getName())
            .role(existingUser.getRole())
            .createdAt(existingUser.getCreatedAt())
            .lastLogin(existingUser.getLastLogin());

        // 비밀번호가 제공된 경우에만 업데이트
        if (command.password() != null && !command.password().isBlank()) {
            builder.password(passwordEncoder.encode(command.password()));
        } else {
            builder.password(existingUser.getPassword());
        }

        User updatedUser = saveUserPort.save(builder.build());
        log.info("사용자 정보 수정 완료: {}", updatedUser.getUsername());
        return updatedUser;
    }

    @Override
    public void deleteUser(UUID id) {
        log.info("사용자 삭제: {}", id);

        loadUserPort.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));

        deleteUserPort.deleteById(id);
        log.info("사용자 삭제 완료: {}", id);
    }
}
