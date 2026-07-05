package com.flowmova.backend.shared.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Erreur de validation rattachee a un champ de requete.")
public record ApiFieldError(
        @Schema(description = "Nom du champ invalide.", example = "email")
        String field,
        @Schema(description = "Message de validation du champ.", example = "must not be blank")
        String message) {
}
