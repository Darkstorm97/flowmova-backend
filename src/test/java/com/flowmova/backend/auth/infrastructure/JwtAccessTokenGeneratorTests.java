package com.flowmova.backend.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.auth.domain.InvalidAccessTokenException;
import com.flowmova.backend.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtAccessTokenGeneratorTests {

    private static final Instant NOW = Instant.now();
    private static final String ISSUER = "flowmova-test";
    private static final String SECRET = "test-secret-with-enough-length-for-hmac";

    private final Algorithm algorithm = Algorithm.HMAC256(SECRET);
    private final JwtAccessTokenGenerator tokenGenerator = new JwtAccessTokenGenerator(
            algorithm,
            Clock.fixed(NOW, ZoneOffset.UTC),
            ISSUER,
            43_200);

    @Test
    void validatesGeneratedToken() {
        User user = new User("jwt-user@flowmova.test", "$2a$10$placeholder", "Jwt", "User");

        AuthenticatedUser authenticatedUser = tokenGenerator.validate(tokenGenerator.generate(user).value());

        assertThat(authenticatedUser.userId()).isEqualTo(user.getId());
        assertThat(authenticatedUser.email()).isEqualTo("jwt-user@flowmova.test");
    }

    @Test
    void rejectsTokenWithInvalidSignature() {
        String token = tokenWith(Algorithm.HMAC256("other-secret"), ISSUER, UUID.randomUUID().toString(), "jwt-user@flowmova.test", Instant.now().plusSeconds(60));

        assertThatThrownBy(() -> tokenGenerator.validate(token))
                .isInstanceOf(InvalidAccessTokenException.class)
                .hasMessage("Access token is invalid or expired");
    }

    @Test
    void rejectsExpiredToken() {
        String token = tokenWith(algorithm, ISSUER, UUID.randomUUID().toString(), "jwt-user@flowmova.test", NOW.minusSeconds(60));

        assertThatThrownBy(() -> tokenGenerator.validate(token))
                .isInstanceOf(InvalidAccessTokenException.class)
                .hasMessage("Access token is invalid or expired");
    }

    @Test
    void rejectsTokenWithInvalidIssuer() {
        String token = tokenWith(algorithm, "other-issuer", UUID.randomUUID().toString(), "jwt-user@flowmova.test", Instant.now().plusSeconds(60));

        assertThatThrownBy(() -> tokenGenerator.validate(token))
                .isInstanceOf(InvalidAccessTokenException.class)
                .hasMessage("Access token is invalid or expired");
    }

    @Test
    void rejectsTokenMissingRequiredClaims() {
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withExpiresAt(Date.from(Instant.now().plusSeconds(60)))
                .sign(algorithm);

        assertThatThrownBy(() -> tokenGenerator.validate(token))
                .isInstanceOf(InvalidAccessTokenException.class)
                .hasMessage("Access token is missing required claims");
    }

    @Test
    void rejectsTokenWithInvalidUserIdClaim() {
        String token = tokenWith(algorithm, ISSUER, "not-a-uuid", "jwt-user@flowmova.test", Instant.now().plusSeconds(60));

        assertThatThrownBy(() -> tokenGenerator.validate(token))
                .isInstanceOf(InvalidAccessTokenException.class)
                .hasMessage("Access token contains invalid claims");
    }

    private String tokenWith(Algorithm tokenAlgorithm, String issuer, String userId, String email, Instant expiresAt) {
        return JWT.create()
                .withIssuer(issuer)
                .withClaim("userId", userId)
                .withClaim("email", email)
                .withExpiresAt(Date.from(expiresAt))
                .sign(tokenAlgorithm);
    }
}
