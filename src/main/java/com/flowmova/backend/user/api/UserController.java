package com.flowmova.backend.user.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.user.application.GetCurrentUserProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final GetCurrentUserProfileService getCurrentUserProfileService;

    public UserController(GetCurrentUserProfileService getCurrentUserProfileService) {
        this.getCurrentUserProfileService = getCurrentUserProfileService;
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return UserProfileResponse.from(getCurrentUserProfileService.getCurrentUserProfile(authenticatedUser));
    }
}
