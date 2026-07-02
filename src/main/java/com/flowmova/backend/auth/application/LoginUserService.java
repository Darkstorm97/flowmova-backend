package com.flowmova.backend.auth.application;

import com.flowmova.backend.auth.domain.AccessToken;
import com.flowmova.backend.auth.domain.AccessTokenGenerator;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.domain.UserStatus;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoginUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenGenerator accessTokenGenerator;

    public LoginUserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccessTokenGenerator accessTokenGenerator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenGenerator = accessTokenGenerator;
    }

    @Transactional(readOnly = true)
    public LoginUserResult login(LoginUserCommand command) {
        User user = userRepository.findByEmail(normalizeEmail(command.email()))
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is disabled");
        }

        AccessToken accessToken = accessTokenGenerator.generate(user);
        return new LoginUserResult(accessToken.value(), "Bearer", accessToken.expiresInSeconds());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
