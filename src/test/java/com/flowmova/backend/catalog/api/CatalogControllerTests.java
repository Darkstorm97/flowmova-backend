package com.flowmova.backend.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    void adminUpdatesCatalog() throws Exception {
        User admin = user("catalog-update-admin");
        Company company = company(admin);
        CatalogCategory initialCategory = category(company, admin);
        CatalogCategory newCategory = category(company, admin);
        Catalog catalog = catalogRepository.save(new Catalog(
                company,
                initialCategory,
                "Old Catalog",
                "Old description",
                "https://cdn.flowmova.test/old.png",
                new BigDecimal("15.00"),
                admin));
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put("/api/companies/{companyId}/catalogs/{catalogId}", company.getId(), catalog.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogCategoryId": "%s",
                                  "name": " Updated Catalog ",
                                  "description": " Updated description ",
                                  "imageUrl": " https://cdn.flowmova.test/updated.png ",
                                  "priceAmount": 25.50
                                }
                                """.formatted(newCategory.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(catalog.getId().toString()))
                .andExpect(jsonPath("$.companyId").value(company.getId().toString()))
                .andExpect(jsonPath("$.catalogCategoryId").value(newCategory.getId().toString()))
                .andExpect(jsonPath("$.name").value("Updated Catalog"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.imageUrl").value("https://cdn.flowmova.test/updated.png"))
                .andExpect(jsonPath("$.priceAmount").value(25.50))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        Catalog updatedCatalog = catalogRepository.findById(catalog.getId()).orElseThrow();
        assertThat(updatedCatalog.getCatalogCategory().getId()).isEqualTo(newCategory.getId());
        assertThat(updatedCatalog.getName()).isEqualTo("Updated Catalog");
        assertThat(updatedCatalog.getPriceAmount()).isEqualByComparingTo("25.50");
        assertThat(updatedCatalog.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
        assertThat(updatedCatalog.getUpdatedBy().getId()).isEqualTo(admin.getId());
    }

    @Test
    void adminUpdatesCatalogWithBlankOptionalFields() throws Exception {
        User admin = user("catalog-update-blank");
        Company company = company(admin);
        CatalogCategory category = category(company, admin);
        Catalog catalog = catalogRepository.save(new Catalog(
                company,
                category,
                "Catalog With Optionals",
                "Description",
                "https://cdn.flowmova.test/catalog.png",
                new BigDecimal("12.00"),
                admin));
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put("/api/companies/{companyId}/catalogs/{catalogId}", company.getId(), catalog.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogCategoryId": "%s",
                                  "name": "Catalog Without Optionals",
                                  "description": " ",
                                  "imageUrl": " "
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.priceAmount").doesNotExist());
    }

    @Test
    void rejectsCatalogUpdateWithoutJwt() throws Exception {
        mockMvc.perform(put("/api/companies/{companyId}/catalogs/{catalogId}", UUID.randomUUID(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogCategoryId": "%s",
                                  "name": "Unauthorized Update"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsCatalogUpdateForEmployee() throws Exception {
        User employee = user("catalog-update-employee");
        Company company = company(employee);
        CatalogCategory category = category(company, employee);
        Catalog catalog = catalogRepository.save(new Catalog(
                company,
                category,
                "Employee Update Catalog",
                null,
                null,
                null,
                employee));
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(put("/api/companies/{companyId}/catalogs/{catalogId}", company.getId(), catalog.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogCategoryId": "%s",
                                  "name": "Employee Updated Catalog"
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));
    }

    @Test
    void rejectsCatalogUpdateWithCategoryFromAnotherCompany() throws Exception {
        User admin = user("catalog-update-wrong-category");
        Company company = company(admin);
        Company otherCompany = company(admin);
        CatalogCategory category = category(company, admin);
        CatalogCategory otherCategory = category(otherCompany, admin);
        Catalog catalog = catalogRepository.save(new Catalog(
                company,
                category,
                "Wrong Category Update Catalog",
                null,
                null,
                null,
                admin));
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put("/api/companies/{companyId}/catalogs/{catalogId}", company.getId(), catalog.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogCategoryId": "%s",
                                  "name": "Wrong Category Update Catalog"
                                }
                                """.formatted(otherCategory.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Catalog category is invalid"));
    }

    @Test
    void rejectsCatalogUpdateForUnknownCatalog() throws Exception {
        User admin = user("catalog-update-unknown");
        Company company = company(admin);
        CatalogCategory category = category(company, admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put("/api/companies/{companyId}/catalogs/{catalogId}", company.getId(), UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogCategoryId": "%s",
                                  "name": "Unknown Catalog"
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Catalog not found"));
    }

    @Test
    void rejectsCatalogUpdateWithoutNameOrCategory() throws Exception {
        User admin = user("catalog-update-invalid");
        Company company = company(admin);
        CatalogCategory category = category(company, admin);
        Catalog catalog = catalogRepository.save(new Catalog(
                company,
                category,
                "Invalid Update Catalog",
                null,
                null,
                null,
                admin));
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put("/api/companies/{companyId}/catalogs/{catalogId}", company.getId(), catalog.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "priceAmount": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'catalogCategoryId')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'priceAmount')]").exists());
    }

    @Test
    void adminArchivesCatalog() throws Exception {
        User admin = user("catalog-archive-admin");
        Company company = company(admin);
        CatalogCategory category = category(company, admin);
        Catalog catalog = catalogRepository.save(new Catalog(
                company,
                category,
                "Archived By API",
                "Catalog to archive",
                null,
                new BigDecimal("30.00"),
                admin));
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(delete("/api/companies/{companyId}/catalogs/{catalogId}", company.getId(), catalog.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(catalog.getId().toString()))
                .andExpect(jsonPath("$.companyId").value(company.getId().toString()))
                .andExpect(jsonPath("$.catalogCategoryId").value(category.getId().toString()))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        Catalog archivedCatalog = catalogRepository.findById(catalog.getId()).orElseThrow();
        assertThat(archivedCatalog.getStatus()).isEqualTo(CatalogStatus.ARCHIVED);
        assertThat(archivedCatalog.getUpdatedBy().getId()).isEqualTo(admin.getId());

        mockMvc.perform(get("/api/companies/{companyId}/catalogs", company.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(catalog.getId())).doesNotExist());
    }

    @Test
    void rejectsCatalogArchiveWithoutJwt() throws Exception {
        mockMvc.perform(delete("/api/companies/{companyId}/catalogs/{catalogId}", UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsCatalogArchiveForEmployee() throws Exception {
        User employee = user("catalog-archive-employee");
        Company company = company(employee);
        CatalogCategory category = category(company, employee);
        Catalog catalog = catalogRepository.save(new Catalog(
                company,
                category,
                "Employee Archive Catalog",
                null,
                null,
                null,
                employee));
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(delete("/api/companies/{companyId}/catalogs/{catalogId}", company.getId(), catalog.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));

        assertThat(catalogRepository.findById(catalog.getId()).orElseThrow().getStatus())
                .isEqualTo(CatalogStatus.ACTIVE);
    }

    @Test
    void listsActiveCatalogsWithoutJwtOrderedByCategoryAndName() throws Exception {
        User owner = user("catalog-list-public");
        Company company = company(owner);
        CatalogCategory secondCategory = catalogCategoryRepository.save(new CatalogCategory(
                company,
                "Z Services",
                "Second category",
                20,
                owner));
        CatalogCategory firstCategory = catalogCategoryRepository.save(new CatalogCategory(
                company,
                "A Services",
                "First category",
                10,
                owner));
        Catalog archived = catalogRepository.save(new Catalog(
                company,
                firstCategory,
                "Archived Catalog",
                "Hidden catalog",
                null,
                null,
                owner));
        archived.archive();
        catalogRepository.saveAndFlush(archived);
        Catalog alpha = catalogRepository.save(new Catalog(
                company,
                firstCategory,
                "Alpha Catalog",
                "Visible alpha",
                null,
                new BigDecimal("10.00"),
                owner));
        Catalog zeta = catalogRepository.save(new Catalog(
                company,
                secondCategory,
                "Zeta Catalog",
                "Visible zeta",
                null,
                null,
                owner));

        mockMvc.perform(get("/api/companies/{companyId}/catalogs", company.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(alpha.getId().toString()))
                .andExpect(jsonPath("$[0].catalogCategoryId").value(firstCategory.getId().toString()))
                .andExpect(jsonPath("$[0].name").value("Alpha Catalog"))
                .andExpect(jsonPath("$[0].priceAmount").value(10.00))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].id").value(zeta.getId().toString()))
                .andExpect(jsonPath("$[1].catalogCategoryId").value(secondCategory.getId().toString()))
                .andExpect(jsonPath("$[1].name").value("Zeta Catalog"))
                .andExpect(jsonPath("$[?(@.name == 'Archived Catalog')]").doesNotExist());
    }

    @Test
    void listsActiveCatalogsFilteredByCategoryWithoutJwt() throws Exception {
        User owner = user("catalog-list-filtered");
        Company company = company(owner);
        CatalogCategory targetCategory = category(company, owner);
        CatalogCategory otherCategory = category(company, owner);
        Catalog targetCatalog = catalogRepository.save(new Catalog(
                company,
                targetCategory,
                "Target Catalog",
                "Visible target",
                null,
                null,
                owner));
        catalogRepository.save(new Catalog(
                company,
                otherCategory,
                "Other Catalog",
                "Other category catalog",
                null,
                null,
                owner));

        mockMvc.perform(get("/api/companies/{companyId}/catalogs", company.getId())
                        .param("catalogCategoryId", targetCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(targetCatalog.getId().toString()))
                .andExpect(jsonPath("$[0].catalogCategoryId").value(targetCategory.getId().toString()))
                .andExpect(jsonPath("$[0].name").value("Target Catalog"));
    }

    @Test
    void rejectsCatalogListingForDisabledCompany() throws Exception {
        User owner = user("catalog-list-disabled-company");
        Company disabledCompany = companyRepository.save(new Company(
                "Disabled Catalog Company",
                "Hidden catalogs",
                owner));

        mockMvc.perform(get("/api/companies/{companyId}/catalogs", disabledCompany.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Company not found"));
    }

    @Test
    void rejectsCatalogListingWithCategoryFromAnotherCompany() throws Exception {
        User owner = user("catalog-list-wrong-category");
        Company company = company(owner);
        Company otherCompany = company(owner);
        CatalogCategory otherCategory = category(otherCompany, owner);

        mockMvc.perform(get("/api/companies/{companyId}/catalogs", company.getId())
                        .param("catalogCategoryId", otherCategory.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Catalog category is invalid"));
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
