package com.flowmova.backend.company.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmova.backend.auth.domain.AccessTokenGenerator;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.domain.CompanyUserStatus;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
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
class CompanyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyUserRepository companyUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessTokenGenerator accessTokenGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsActiveCompanyAndAssociatesCreatorAsAdmin() throws Exception {
        User user = userRepository.save(new User(
                "company-create.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Company",
                "Creator"));
        String token = accessTokenGenerator.generate(user).value();

        String response = mockMvc.perform(post("/api/companies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " FlowMova Demo ",
                                  "description": " Demo company "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("FlowMova Demo"))
                .andExpect(jsonPath("$.description").value("Demo company"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(response);
        UUID createdCompanyId = UUID.fromString(responseJson.get("id").asText());
        Company company = companyRepository.findById(createdCompanyId).orElseThrow();
        CompanyUser companyUser = companyUserRepository.findByCompanyIdAndUserId(createdCompanyId, user.getId()).orElseThrow();

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(company.getCreatedBy().getId()).isEqualTo(user.getId());
        assertThat(companyUser.getRole()).isEqualTo(CompanyRole.ADMIN);
        assertThat(companyUser.getStatus()).isEqualTo(CompanyUserStatus.ACTIVE);
    }

    @Test
    void rejectsCompanyCreationWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "FlowMova Demo"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void rejectsCompanyCreationWithoutName() throws Exception {
        User user = userRepository.save(new User(
                "company-invalid.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Company",
                "Invalid"));
        String token = accessTokenGenerator.generate(user).value();

        mockMvc.perform(post("/api/companies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "description": "No name"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());
    }

    @Test
    void listsCurrentUserCompaniesWithPaginationAndRoles() throws Exception {
        User user = userRepository.save(new User(
                "company-list.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Company",
                "Lister"));
        User otherUser = userRepository.save(new User(
                "other-company-list.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Other",
                "Lister"));
        String token = accessTokenGenerator.generate(user).value();

        Company alphaCompany = activeCompany("Alpha Company", "Visible alpha", user);
        Company betaCompany = activeCompany("Beta Company", "Visible beta", user);
        Company inactiveMembershipCompany = activeCompany("Hidden Membership", "Inactive membership", user);
        Company otherUserCompany = activeCompany("Other User Company", "Other user", otherUser);

        companyUserRepository.save(new CompanyUser(betaCompany.getId(), user, CompanyRole.EMPLOYEE));
        companyUserRepository.save(new CompanyUser(alphaCompany.getId(), user, CompanyRole.ADMIN));
        CompanyUser inactiveMembership = new CompanyUser(inactiveMembershipCompany.getId(), user, CompanyRole.ADMIN);
        inactiveMembership.deactivate();
        companyUserRepository.save(inactiveMembership);
        companyUserRepository.save(new CompanyUser(otherUserCompany.getId(), otherUser, CompanyRole.ADMIN));

        mockMvc.perform(get("/api/users/me/companies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(alphaCompany.getId().toString()))
                .andExpect(jsonPath("$.items[0].name").value("Alpha Company"))
                .andExpect(jsonPath("$.items[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void rejectsCurrentUserCompaniesWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/users/me/companies"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private Company activeCompany(String name, String description, User createdBy) {
        Company company = new Company(name, description, createdBy);
        company.activate();
        return companyRepository.save(company);
    }
}
