package com.flowmova.backend.user.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.api.CurrentUserCompanyResponse;
import com.flowmova.backend.company.application.ListCurrentUserCompaniesService;
import com.flowmova.backend.shared.api.PageResponse;
import com.flowmova.backend.user.application.GetCurrentUserProfileService;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final GetCurrentUserProfileService getCurrentUserProfileService;
    private final ListCurrentUserCompaniesService listCurrentUserCompaniesService;

    public UserController(
            GetCurrentUserProfileService getCurrentUserProfileService,
            ListCurrentUserCompaniesService listCurrentUserCompaniesService) {
        this.getCurrentUserProfileService = getCurrentUserProfileService;
        this.listCurrentUserCompaniesService = listCurrentUserCompaniesService;
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return UserProfileResponse.from(getCurrentUserProfileService.getCurrentUserProfile(authenticatedUser));
    }

    @GetMapping("/me/companies")
    public PageResponse<CurrentUserCompanyResponse> myCompanies(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            Pageable pageable) {
        return PageResponse.from(listCurrentUserCompaniesService.listCompanies(authenticatedUser, pageable));
    }
}
