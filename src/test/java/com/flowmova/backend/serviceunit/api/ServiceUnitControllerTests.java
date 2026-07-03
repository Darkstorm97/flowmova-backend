package com.flowmova.backend.serviceunit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmova.backend.auth.domain.AccessTokenGenerator;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.serviceunit.domain.ServiceUnitType;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationStatus;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationType;
import com.flowmova.backend.serviceunitlocation.infrastructure.ServiceUnitLocationRepository;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceUnitControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyUserRepository companyUserRepository;

    @Autowired
    private ServiceUnitRepository serviceUnitRepository;

    @Autowired
    private ServiceUnitLocationRepository serviceUnitLocationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessTokenGenerator accessTokenGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCreatesServiceUnitWithDefaultLocation() throws Exception {
        User admin = user("service-unit-admin");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        String response = mockMvc.perform(post("/api/companies/{companyId}/service-units", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " File principale ",
                                  "description": " File pour le comptoir ",
                                  "location": " Hall ",
                                  "type": "TICKET_QUEUE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.companyId").value(company.getId().toString()))
                .andExpect(jsonPath("$.name").value("File principale"))
                .andExpect(jsonPath("$.description").value("File pour le comptoir"))
                .andExpect(jsonPath("$.location").value("Hall"))
                .andExpect(jsonPath("$.type").value("TICKET_QUEUE"))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.defaultLocation.id").exists())
                .andExpect(jsonPath("$.defaultLocation.name").value("Principal"))
                .andExpect(jsonPath("$.defaultLocation.type").value("DEFAULT"))
                .andExpect(jsonPath("$.defaultLocation.defaultLocation").value(true))
                .andExpect(jsonPath("$.defaultLocation.publicAccessSlug").exists())
                .andExpect(jsonPath("$.defaultLocation.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(response);
        UUID serviceUnitId = UUID.fromString(responseJson.get("id").asText());
        String publicAccessSlug = responseJson.get("defaultLocation").get("publicAccessSlug").asText();

        ServiceUnit serviceUnit = serviceUnitRepository.findById(serviceUnitId).orElseThrow();
        ServiceUnitLocation defaultLocation = serviceUnitLocationRepository.findByPublicAccessSlug(publicAccessSlug)
                .orElseThrow();

        assertThat(serviceUnit.getCompany().getId()).isEqualTo(company.getId());
        assertThat(serviceUnit.getType()).isEqualTo(ServiceUnitType.TICKET_QUEUE);
        assertThat(serviceUnit.getStatus()).isEqualTo(ServiceUnitStatus.CLOSED);
        assertThat(serviceUnit.getCreatedBy().getId()).isEqualTo(admin.getId());
        assertThat(defaultLocation.getServiceUnit().getId()).isEqualTo(serviceUnitId);
        assertThat(defaultLocation.getType()).isEqualTo(ServiceUnitLocationType.DEFAULT);
        assertThat(defaultLocation.isDefaultLocation()).isTrue();
        assertThat(defaultLocation.getStatus()).isEqualTo(ServiceUnitLocationStatus.ACTIVE);
        assertThat(defaultLocation.getCreatedBy().getId()).isEqualTo(admin.getId());
    }

    @Test
    void rejectsServiceUnitCreationWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/companies/{companyId}/service-units", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unauthorized Queue",
                                  "type": "TICKET_QUEUE"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsServiceUnitCreationForEmployee() throws Exception {
        User employee = user("service-unit-employee");
        Company company = activeCompany(employee);
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(post("/api/companies/{companyId}/service-units", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Employee Queue",
                                  "type": "TICKET_QUEUE"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));
    }

    @Test
    void rejectsServiceUnitCreationWithoutNameOrType() throws Exception {
        User admin = user("service-unit-invalid");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post("/api/companies/{companyId}/service-units", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'type')]").exists());
    }

    @Test
    void rejectsServiceUnitCreationForDisabledCompany() throws Exception {
        User admin = user("service-unit-disabled-company");
        Company disabledCompany = companyRepository.save(new Company(
                "Disabled Service Unit Company",
                "Hidden service unit company",
                admin));
        companyUserRepository.save(new CompanyUser(disabledCompany.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post("/api/companies/{companyId}/service-units", disabledCompany.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Disabled Company Queue",
                                  "type": "TICKET_QUEUE"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Company not found"));
    }

    private User user(String emailPrefix) {
        return userRepository.save(new User(
                "%s.%s@flowmova.test".formatted(emailPrefix, UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Service",
                "Admin"));
    }

    private Company activeCompany(User owner) {
        Company company = new Company(
                "Service Unit API Company %s".formatted(UUID.randomUUID()),
                "Company for service unit API tests",
                owner);
        company.activate();
        return companyRepository.save(company);
    }
}
