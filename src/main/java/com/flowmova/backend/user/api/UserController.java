package com.flowmova.backend.user.api;

import com.flowmova.backend.auth.domain.AccessTokenValidator;
import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.auth.domain.InvalidAccessTokenException;
import com.flowmova.backend.user.application.GetCurrentUserProfileService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenValidator accessTokenValidator;
    private final GetCurrentUserProfileService getCurrentUserProfileService;

    public UserController(
            AccessTokenValidator accessTokenValidator,
            GetCurrentUserProfileService getCurrentUserProfileService) {
        this.accessTokenValidator = accessTokenValidator;
        this.getCurrentUserProfileService = getCurrentUserProfileService;
    }

    @GetMapping("/me")
    public UserProfileResponse me(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        AuthenticatedUser authenticatedUser = accessTokenValidator.validate(extractBearerToken(authorizationHeader));
        return UserProfileResponse.from(getCurrentUserProfileService.getCurrentUserProfile(authenticatedUser));
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new InvalidAccessTokenException("Missing bearer token");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new InvalidAccessTokenException("Missing bearer token");
        }

        return token;
    }
}
