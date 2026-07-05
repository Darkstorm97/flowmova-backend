package com.flowmova.backend.shared.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesOpenApiJsonWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("FlowMova Backend API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse").exists())
                .andExpect(jsonPath("$.components.responses.BadRequest.content.application/json.schema.$ref").value("#/components/schemas/ApiErrorResponse"))
                .andExpect(jsonPath("$.paths./api/auth/register.post.responses.400.$ref").value("#/components/responses/BadRequest"))
                .andExpect(jsonPath("$.paths./api/auth/register.post.responses.401.$ref").value("#/components/responses/Unauthorized"))
                .andExpect(jsonPath("$.paths./api/auth/register.post.responses.403.$ref").value("#/components/responses/Forbidden"))
                .andExpect(jsonPath("$.paths./api/auth/register.post.responses.404.$ref").value("#/components/responses/NotFound"))
                .andExpect(jsonPath("$.paths./api/auth/register.post.responses.409.$ref").value("#/components/responses/Conflict"))
                .andExpect(jsonPath("$.paths./api/auth/register.post.responses.500.$ref").value("#/components/responses/InternalServerError"));
    }

    @Test
    void exposesSwaggerUiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }
}
