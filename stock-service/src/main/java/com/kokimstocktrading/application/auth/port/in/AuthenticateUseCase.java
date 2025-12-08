package com.kokimstocktrading.application.auth.port.in;

public interface AuthenticateUseCase {

  AuthenticationResult authenticate(LoginCommand command);
}
