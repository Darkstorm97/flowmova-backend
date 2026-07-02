package com.flowmova.backend.auth.infrastructure;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.flowmova.backend.auth.domain.AccessToken;
import com.flowmova.backend.auth.domain.AccessTokenGenerator;
import com.flowmova.backend.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenGenerator implements AccessTokenGenerator {

    private final Algorithm algorithm;
    private final Clock clock;
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
}
