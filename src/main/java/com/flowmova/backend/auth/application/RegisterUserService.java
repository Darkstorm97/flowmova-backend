package com.flowmova.backend.auth.application;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegisterUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterUserCommand command) {
        String normalizedEmail = normalizeEmail(command.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw emailAlreadyExists();
        }

        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(command.password()),
                command.firstName().trim(),
                command.lastName().trim());

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            throw emailAlreadyExists();
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException emailAlreadyExists() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }
}
