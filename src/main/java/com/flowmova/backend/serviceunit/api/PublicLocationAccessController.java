package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.serviceunit.application.GetPublicLocationAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/locations")
@Tag(name = "Public Location Access", description = "Acces public via lien ou QR code a un emplacement d'unite de service.")
public class PublicLocationAccessController {

    private final GetPublicLocationAccessService getPublicLocationAccessService;

    public PublicLocationAccessController(GetPublicLocationAccessService getPublicLocationAccessService) {
        this.getPublicLocationAccessService = getPublicLocationAccessService;
    }

    @GetMapping("/{publicAccessSlug}")
    @Operation(
            summary = "Consulter un emplacement public",
            description = "Retourne le contexte public d'un emplacement accessible par lien/QR code: entreprise, unite de service, emplacement et articles disponibles.")
    public PublicLocationAccessResponse get(@PathVariable String publicAccessSlug) {
        return getPublicLocationAccessService.get(publicAccessSlug);
    }
}
