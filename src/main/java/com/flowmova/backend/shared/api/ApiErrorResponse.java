package com.flowmova.backend.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Format standard des erreurs retournees par l'API FlowMova.")
public record ApiErrorResponse(
        @Schema(description = "Date et heure de production de l'erreur.", example = "2026-07-04T18:00:00Z")
        Instant timestamp,
        @Schema(description = "Statut HTTP numerique.", example = "400")
        int status,
        @Schema(description = "Libelle HTTP standard.", example = "Bad Request")
        String error,
        @Schema(description = "Code applicatif stable de l'erreur.", example = "VALIDATION_ERROR")
        String code,
        @Schema(description = "Message lisible expliquant l'erreur.", example = "Request validation failed")
        String message,
        @Schema(description = "Chemin HTTP ayant produit l'erreur.", example = "/api/auth/register")
        String path,
        @Schema(description = "Liste des erreurs de champs. Vide lorsqu'aucun champ precis n'est concerne.")
        List<ApiFieldError> fieldErrors) {

    public ApiErrorResponse(
            Instant timestamp,
            int status,
            String error,
            String code,
            String message,
            String path) {
        this(timestamp, status, error, code, message, path, List.of());
    }
}
