package com.kokimstocktrading.application.auth.port.out;

import com.kokimstocktrading.domain.user.UserToken;

public interface SaveTokenPort {

  UserToken save(UserToken token);
}
