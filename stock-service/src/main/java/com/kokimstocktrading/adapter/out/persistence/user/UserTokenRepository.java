package com.kokimstocktrading.adapter.out.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * UserToken JPA 리포지토리
 */
@Repository
public interface UserTokenRepository extends JpaRepository<UserTokenEntity, Long> {

    Optional<UserTokenEntity> findByToken(String token);

    boolean existsByToken(String token);

    void deleteByToken(String token);

    void deleteAllByUserId(Long userId);

    /**
     * 만료된 토큰 삭제 (정리 작업)
     */
    @Modifying
    @Query("DELETE FROM UserTokenEntity t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
