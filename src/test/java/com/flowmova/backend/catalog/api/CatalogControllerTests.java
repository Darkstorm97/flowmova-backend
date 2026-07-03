package com.flowmova.backend.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowmova.backend.auth.domain.AccessTokenGenerator;
import com.flowmova.backend.catalog.domain.Catalog;
import com.flowmova.backend.catalog.domain.CatalogStatus;
import com.flowmova.backend.catalog.infrastructure.CatalogRepository;
import com.flowmova.backend.catalogcategory.domain.CatalogCategory;
import com.flowmova.backend.catalogcategory.infrastructure.CatalogCategoryRepository;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.math.BigDecimal;
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
class CatalogControllerTests {

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
    private CatalogRepository catalogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessTokenGenerator accessTokenGenerator;

    @Test
    void adminCreatesCatalog() throws Exception {
        User admin = user("catalog-admin");
        Company company = company(admin);
        CatalogCategory category = category(company, admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post("/api/companies/{companyId}/catalogs", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogCategoryId": "%s",
                                  "name": " Standard Cleaning ",
                                  "description": " Regular cleaning offer ",
                                  "imageUrl": " https://cdn.flowmova.test/catalogs/standard-cleaning.png ",
                                  "priceAmount": 49.99
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.companyId").value(company.getId().toString()))
                .andExpect(jsonPath("$.catalogCategoryId").value(category.getId().toString()))
                .andExpect(jsonPath("$.name").value("Standard Cleaning"))
                .andExpect(jsonPath("$.description").value("Regular cleaning offer"))
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.flowmova.test/catalogs/standard-cleaning.png"))
                .andExpect(jsonPath("$.priceAmount").value(49.99))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        Catalog catalog = catalogRepository.findByCompanyIdOrderByNameAsc(company.getId()).getFirst();
        assertThat(catalog.getName()).isEqualTo("Standard Cleaning");
        assertThat(catalog.getPriceAmount()).isEqualByComparingTo(new BigDecimal("49.99"));
        assertThat(catalog.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
        assertThat(catalog.getCreatedBy().getId()).isEqualTo(admin.getId());
    }

    @Test
    void adminCreatesCatalogWithoutOptionalFields() throws Exception {
        User admin = user("catalog-minimal");
        Company company = company(admin);
        CatalogCategory category = category(company, admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post("/api/companies/{companyId}/catalogs", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogCategoryId": "%s",
                                  "name": "Minimal Catalog",
                                  "description": " ",
                                  "imageUrl": " "
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.priceAmount").doesNotExist())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void rejectsCatalogCreationWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/companies/{companyId}/catalogs", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogCategoryId": "%s",
                                  "name": "Unauthorized Catalog"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsCatalogCreationForEmployee() throws Exception {
        User employee = user("catalog-employee");
        Company company = company(employee);
        CatalogCategory category = category(company, employee);
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(post("/api/companies/{companyId}/catalogs", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogCategoryId": "%s",
                                  "name": "Employee Catalog"
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));
    }

    @Test
    void rejectsCatalogCreationWithCategoryFromAnotherCompany() throws Exception {
        User admin = user("catalog-wrong-category");
        Company company = company(admin);
        Company otherCompany = company(admin);
        CatalogCategory otherCategory = category(otherCompany, admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post("/api/companies/{companyId}/catalogs", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogCategoryId": "%s",
                                  "name": "Wrong Category Catalog"
                                }
                                """.formatted(otherCategory.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Catalog category is invalid"));
    }

    @Test
    void rejectsCatalogCreationWithoutNameOrCategory() throws Exception {
        User admin = user("catalog-invalid");
        Company company = company(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post("/api/companies/{companyId}/catalogs", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'catalogCategoryId')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());
    }

    @Test
    void rejectsCatalogCreationWithNegativePrice() throws Exception {
        User admin = user("catalog-negative-price");
        Company company = company(admin);
        CatalogCategory category = category(company, admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post("/api/companies/{companyId}/catalogs", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogCategoryId": "%s",
                                  "name": "Negative Price Catalog",
                                  "priceAmount": -1
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'priceAmount')]").exists());
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
                "Catalog API Company %s".formatted(UUID.randomUUID()),
                "Company for catalog API tests",
                owner);
        company.activate();
        return companyRepository.save(company);
    }

    private CatalogCategory category(Company company, User owner) {
        return catalogCategoryRepository.save(new CatalogCategory(
                company,
                "Catalog API Category %s".formatted(UUID.randomUUID()),
                "Category for catalog API tests",
                0,
                owner));
    }
}
