package com.kokimstocktrading.adapter.out.persistence.user;

import com.common.PersistenceAdapter;
import com.kokimstocktrading.application.auth.port.out.DeleteTokenPort;
import com.kokimstocktrading.application.auth.port.out.LoadTokenPort;
import com.kokimstocktrading.application.auth.port.out.SaveTokenPort;
import com.kokimstocktrading.domain.user.UserToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * UserToken Persistence Adapter
 * 토큰 관련 출력 포트 구현
 */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class UserTokenPersistenceAdapter implements SaveTokenPort, LoadTokenPort, DeleteTokenPort {

    private final UserTokenRepository userTokenRepository;

    @Override
    @Transactional
    public UserToken save(UserToken token) {
        UserTokenEntity entity = UserTokenEntity.from(token);
        UserTokenEntity saved = userTokenRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<UserToken> findByToken(String token) {
        return userTokenRepository.findByToken(token)
            .map(UserTokenEntity::toDomain);
    }

    @Override
    public boolean existsByToken(String token) {
        return userTokenRepository.existsByToken(token);
    }

    @Override
    @Transactional
    public void deleteByToken(String token) {
        userTokenRepository.deleteByToken(token);
    }

    @Override
    @Transactional
    public void deleteAllByUserId(UUID userId) {
        userTokenRepository.deleteAllByUserId(userId);
    }
}
