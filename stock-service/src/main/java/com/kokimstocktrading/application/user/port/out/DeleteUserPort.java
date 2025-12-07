package com.kokimstocktrading.application.user.port.out;

import java.util.UUID;

public interface DeleteUserPort {
    void deleteById(UUID id);
}
