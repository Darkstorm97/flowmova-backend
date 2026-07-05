package com.flowmova.backend.shared.infrastructure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final String API_ERROR_SCHEMA = "#/components/schemas/ApiErrorResponse";

    @Bean
    OpenAPI flowMovaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FlowMova Backend API")
                        .version("0.0.1")
                        .description("API REST du backend FlowMova pour l'authentification, les entreprises, les catalogues, les unites de service, les emplacements, les articles et les tickets."))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addResponses("BadRequest", errorResponse("Bad Request", "Requete invalide ou erreur de validation."))
                        .addResponses("Unauthorized", errorResponse("Unauthorized", "JWT absent, invalide ou identifiants incorrects."))
                        .addResponses("Forbidden", errorResponse("Forbidden", "Acces refuse pour l'utilisateur courant."))
                        .addResponses("NotFound", errorResponse("Not Found", "Ressource inexistante ou non visible."))
                        .addResponses("Conflict", errorResponse("Conflict", "Conflit metier ou transition non autorisee."))
                        .addResponses("InternalServerError", errorResponse("Internal Server Error", "Erreur technique inattendue."))
                        .addSchemas("ApiErrorResponse", apiErrorResponseSchema())
                        .addSchemas("ApiFieldError", apiFieldErrorSchema()))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    OpenApiCustomizer commonErrorResponsesOpenApiCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
                pathItem.readOperations().forEach(operation -> operation.getResponses()
                        .addApiResponse("400", new ApiResponse().$ref("#/components/responses/BadRequest"))
                        .addApiResponse("401", new ApiResponse().$ref("#/components/responses/Unauthorized"))
                        .addApiResponse("403", new ApiResponse().$ref("#/components/responses/Forbidden"))
                        .addApiResponse("404", new ApiResponse().$ref("#/components/responses/NotFound"))
                        .addApiResponse("409", new ApiResponse().$ref("#/components/responses/Conflict"))
                        .addApiResponse("500", new ApiResponse().$ref("#/components/responses/InternalServerError"))));
    }

    private ApiResponse errorResponse(String description, String detail) {
        return new ApiResponse()
                .description("%s - %s".formatted(description, detail))
                .content(new Content()
                        .addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, new MediaType()
                                .schema(new Schema<>().$ref(API_ERROR_SCHEMA))));
    }

    @SuppressWarnings("unchecked")
    private Schema<?> apiErrorResponseSchema() {
        return new ObjectSchema()
                .description("Format standard des erreurs retournees par l'API FlowMova.")
                .addProperty("timestamp", new StringSchema()
                        .format("date-time")
                        .description("Date et heure de production de l'erreur.")
                        .example("2026-07-04T18:00:00Z"))
                .addProperty("status", new IntegerSchema()
                        .description("Statut HTTP numerique.")
                        .example(400))
                .addProperty("error", new StringSchema()
                        .description("Libelle HTTP standard.")
                        .example("Bad Request"))
                .addProperty("code", new StringSchema()
                        .description("Code applicatif stable de l'erreur.")
                        .example("VALIDATION_ERROR"))
                .addProperty("message", new StringSchema()
                        .description("Message lisible expliquant l'erreur.")
                        .example("Request validation failed"))
                .addProperty("path", new StringSchema()
                        .description("Chemin HTTP ayant produit l'erreur.")
                        .example("/api/auth/register"))
                .addProperty("fieldErrors", new ArraySchema()
                        .description("Liste des erreurs de champs. Vide lorsqu'aucun champ precis n'est concerne.")
                        .items(new Schema<>().$ref("#/components/schemas/ApiFieldError")));
    }

    @SuppressWarnings("unchecked")
    private Schema<?> apiFieldErrorSchema() {
        return new ObjectSchema()
                .description("Erreur de validation rattachee a un champ de requete.")
                .addProperty("field", new StringSchema()
                        .description("Nom du champ invalide.")
                        .example("email"))
                .addProperty("message", new StringSchema()
                        .description("Message de validation du champ.")
                        .example("must not be blank"));
    }
}
