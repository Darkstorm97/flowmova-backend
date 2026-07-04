package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.serviceunit.application.GetPublicLocationAccessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/locations")
public class PublicLocationAccessController {

    private final GetPublicLocationAccessService getPublicLocationAccessService;

    public PublicLocationAccessController(GetPublicLocationAccessService getPublicLocationAccessService) {
        this.getPublicLocationAccessService = getPublicLocationAccessService;
    }

    @GetMapping("/{publicAccessSlug}")
    public PublicLocationAccessResponse get(@PathVariable String publicAccessSlug) {
        return getPublicLocationAccessService.get(publicAccessSlug);
    }
}
