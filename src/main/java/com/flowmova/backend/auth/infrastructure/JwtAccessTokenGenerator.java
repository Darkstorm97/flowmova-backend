package com.flowmova.backend.auth.infrastructure;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.flowmova.backend.auth.domain.AccessToken;
import com.flowmova.backend.auth.domain.AccessTokenGenerator;
import com.flowmova.backend.auth.domain.AccessTokenValidator;
import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.auth.domain.InvalidAccessTokenException;
import com.flowmova.backend.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenGenerator implements AccessTokenGenerator, AccessTokenValidator {

    private final Algorithm algorithm;
    private final Clock clock;
    private final JWTVerifier verifier;
    private final String issuer;
    private final long expiresInSeconds;

    @Autowired
    public JwtAccessTokenGenerator(
            @Value("${flowmova.auth.jwt.secret}") String secret,
            @Value("${flowmova.auth.jwt.issuer}") String issuer,
            @Value("${flowmova.auth.jwt.expires-in-seconds}") long expiresInSeconds) {
        this(Algorithm.HMAC256(secret), Clock.systemUTC(), issuer, expiresInSeconds);
    }

    JwtAccessTokenGenerator(Algorithm algorithm, Clock clock, String issuer, long expiresInSeconds) {
        this.algorithm = algorithm;
        this.clock = clock;
        this.verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .build();
        this.issuer = issuer;
        this.expiresInSeconds = expiresInSeconds;
    }

    @Override
    public AccessToken generate(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(expiresInSeconds);

        String token = JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getId().toString())
                .withClaim("userId", user.getId().toString())
                .withClaim("email", user.getEmail())
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .sign(algorithm);

        return new AccessToken(token, expiresInSeconds);
    }

    @Override
    public AuthenticatedUser validate(String token) {
        try {
            DecodedJWT decodedJwt = verifier.verify(token);
            String userId = decodedJwt.getClaim("userId").asString();
            String email = decodedJwt.getClaim("email").asString();

            if (userId == null || userId.isBlank() || email == null || email.isBlank()) {
                throw new InvalidAccessTokenException("Access token is missing required claims");
            }

            return new AuthenticatedUser(UUID.fromString(userId), email);
        } catch (InvalidAccessTokenException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new InvalidAccessTokenException("Access token contains invalid claims");
        } catch (JWTVerificationException exception) {
            throw new InvalidAccessTokenException("Access token is invalid or expired");
        }
    }
}
