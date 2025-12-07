package com.kokimstocktrading.application.user.port.out;

import com.kokimstocktrading.domain.user.User;

public interface SaveUserPort {
    User save(User user);
}
