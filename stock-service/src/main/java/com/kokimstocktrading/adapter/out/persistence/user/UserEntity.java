package com.kokimstocktrading.adapter.out.persistence.user;

import com.kokimstocktrading.domain.user.Role;
import com.kokimstocktrading.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * User JPA 엔티티
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username", unique = true),
    @Index(name = "idx_email", columnList = "email")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "username", unique = true, nullable = false, length = 50)
  private String username;

  @Column(name = "password", nullable = false, length = 100)
  private String password;

  @Column(name = "email", nullable = false, length = 100)
  private String email;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private Role role;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "last_login")
  private LocalDateTime lastLogin;

  /**
   * 정적 팩토리 메서드: 도메인 -> 엔티티
   */
  public static UserEntity from(User user) {
    return UserEntity.builder()
        .id(user.getId())
        .username(user.getUsername())
        .password(user.getPassword())
        .email(user.getEmail())
        .name(user.getName())
        .role(user.getRole())
        .createdAt(user.getCreatedAt())
        .lastLogin(user.getLastLogin())
        .build();
  }

  /**
   * 도메인 변환: 엔티티 -> 도메인
   */
  public User toDomain() {
    return User.builder()
        .id(this.id)
        .username(this.username)
        .password(this.password)
        .email(this.email)
        .name(this.name)
        .role(this.role)
        .createdAt(this.createdAt)
        .lastLogin(this.lastLogin)
        .build();
  }
}
