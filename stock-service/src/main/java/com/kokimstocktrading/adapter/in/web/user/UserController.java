package com.kokimstocktrading.adapter.in.web.user;

import com.common.Authorize;
import com.common.WebAdapter;
import com.kokimstocktrading.application.user.port.in.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * User Management Controller
 * 사용자 CRUD 작업 처리
 */
@WebAdapter
@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "사용자 관리 API")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final GetUserUseCase getUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "사용자 등록", description = "새로운 사용자를 등록합니다")
    public UserResponse registerUser(@RequestBody RegisterUserRequest request) {
        log.info("신규 사용자 등록: {}", request.username());

        RegisterUserCommand command = new RegisterUserCommand(
            request.username(),
            request.password(),
            request.email(),
            request.name(),
            request.role()
        );

        return UserResponse.from(registerUserUseCase.registerUser(command));
    }

    @GetMapping("/{id}")
    @Authorize
    @SecurityRequirement(name = "bearer-token")
    @Operation(summary = "사용자 조회", description = "ID로 사용자를 조회합니다")
    public UserResponse getUser(@PathVariable UUID id) {
        log.info("사용자 조회: {}", id);

        return UserResponse.from(getUserUseCase.getUserById(id));
    }

    @GetMapping
    @Authorize(roles = {"ADMIN"})
    @SecurityRequirement(name = "bearer-token")
    @Operation(summary = "모든 사용자 조회", description = "모든 사용자를 조회합니다 (관리자 전용)")
    public List<UserResponse> getAllUsers() {
        log.info("전체 사용자 조회");

        return getUserUseCase.getAllUsers().stream()
            .map(UserResponse::from)
            .toList();
    }

    @PutMapping("/{id}")
    @Authorize
    @SecurityRequirement(name = "bearer-token")
    @Operation(summary = "사용자 수정", description = "사용자 정보를 수정합니다")
    public UserResponse updateUser(@PathVariable UUID id,
                                    @RequestBody UpdateUserRequest request) {
        log.info("사용자 정보 수정: {}", id);

        UpdateUserCommand command = new UpdateUserCommand(
            request.email(),
            request.name(),
            request.password()
        );

        return UserResponse.from(updateUserUseCase.updateUser(id, command));
    }

    @DeleteMapping("/{id}")
    @Authorize(roles = {"ADMIN"})
    @SecurityRequirement(name = "bearer-token")
    @Operation(summary = "사용자 삭제", description = "사용자를 삭제합니다 (관리자 전용)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        log.info("사용자 삭제: {}", id);

        deleteUserUseCase.deleteUser(id);
    }
}
