package com.flowmova.backend.catalogcategory.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowmova.backend.auth.domain.AccessTokenGenerator;
import com.flowmova.backend.catalogcategory.domain.CatalogCategory;
import com.flowmova.backend.catalogcategory.domain.CatalogCategoryStatus;
import com.flowmova.backend.catalogcategory.infrastructure.CatalogCategoryRepository;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
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
class CatalogCategoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyUserRepository companyUserRepository;

    @Autowired
    private CatalogCategoryRepository catalogCategoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessTokenGenerator accessTokenGenerator;

    @Test
    void adminCreatesCatalogCategory() throws Exception {
        User admin = user("catalog-category-admin");
        Company company = company(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post("/api/companies/{companyId}/catalog-categories", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " Main Services ",
                                  "description": " Core offers ",
                                  "displayOrder": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.companyId").value(company.getId().toString()))
                .andExpect(jsonPath("$.name").value("Main Services"))
                .andExpect(jsonPath("$.description").value("Core offers"))
                .andExpect(jsonPath("$.displayOrder").value(10))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        assertThat(catalogCategoryRepository.existsByCompanyIdAndNameIgnoreCase(
                company.getId(),
                "main services")).isTrue();
    }

    @Test
    void defaultsDisplayOrderAndBlankDescriptionWhenAdminCreatesCatalogCategory() throws Exception {
        User admin = user("catalog-category-default");
        Company company = company(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post("/api/companies/{companyId}/catalog-categories", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Default Services",
                                  "description": " "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.displayOrder").value(0));
    }

    @Test
    void rejectsCatalogCategoryCreationWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/companies/{companyId}/catalog-categories", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Main Services"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsCatalogCategoryCreationForEmployee() throws Exception {
        User employee = user("catalog-category-employee");
        Company company = company(employee);
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(post("/api/companies/{companyId}/catalog-categories", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Employee Services"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));
    }

    @Test
    void rejectsDuplicateCatalogCategoryNameInCompany() throws Exception {
        User admin = user("catalog-category-duplicate");
        Company company = company(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        catalogCategoryRepository.save(new CatalogCategory(company, "Main Services", null, 0, admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post("/api/companies/{companyId}/catalog-categories", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "main services"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Catalog category name already exists"));
    }

    @Test
    void rejectsCatalogCategoryCreationWithoutName() throws Exception {
        User admin = user("catalog-category-invalid");
        Company company = company(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post("/api/companies/{companyId}/catalog-categories", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "displayOrder": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());
    }

    @Test
    void createsCatalogCategoryWithActiveStatus() throws Exception {
        User admin = user("catalog-category-status");
        Company company = company(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post("/api/companies/{companyId}/catalog-categories", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Status Services"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        CatalogCategory category = catalogCategoryRepository
                .findByCompanyIdOrderByDisplayOrderAscNameAsc(company.getId())
                .getFirst();
        assertThat(category.getStatus()).isEqualTo(CatalogCategoryStatus.ACTIVE);
    }

    private User user(String emailPrefix) {
        return userRepository.save(new User(
                "%s.%s@flowmova.test".formatted(emailPrefix, UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Catalog",
                "Admin"));
    }

    private Company company(User owner) {
        Company company = new Company(
                "Catalog Category API Company %s".formatted(UUID.randomUUID()),
                "Company for catalog category API tests",
                owner);
        company.activate();
        return companyRepository.save(company);
    }
}
