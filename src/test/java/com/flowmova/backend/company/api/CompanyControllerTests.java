package com.flowmova.backend.company.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmova.backend.auth.domain.AccessTokenGenerator;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyBusinessType;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.domain.CompanyUserStatus;
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
                                  "description": " Demo company ",
                                  "currency": "usd",
                                  "businessType": "RESTAURANT",
                                  "addressLine1": " 123 Flow Street ",
                                  "addressLine2": " Suite 5 ",
                                  "city": " Montreal ",
                                  "region": " Quebec ",
                                  "postalCode": " H2X 1Y4 ",
                                  "country": " ca ",
                                  "latitude": 45.501689,
                                  "longitude": -73.567256
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("FlowMova Demo"))
                .andExpect(jsonPath("$.description").value("Demo company"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.businessType").value("RESTAURANT"))
                .andExpect(jsonPath("$.addressLine1").value("123 Flow Street"))
                .andExpect(jsonPath("$.addressLine2").value("Suite 5"))
                .andExpect(jsonPath("$.city").value("Montreal"))
                .andExpect(jsonPath("$.region").value("Quebec"))
                .andExpect(jsonPath("$.postalCode").value("H2X 1Y4"))
                .andExpect(jsonPath("$.country").value("CA"))
                .andExpect(jsonPath("$.latitude").value(45.501689))
                .andExpect(jsonPath("$.longitude").value(-73.567256))
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
        assertThat(company.getCurrency()).isEqualTo("USD");
        assertThat(company.getBusinessType()).isEqualTo(CompanyBusinessType.RESTAURANT);
        assertThat(company.getAddressLine1()).isEqualTo("123 Flow Street");
        assertThat(company.getAddressLine2()).isEqualTo("Suite 5");
        assertThat(company.getCity()).isEqualTo("Montreal");
        assertThat(company.getRegion()).isEqualTo("Quebec");
        assertThat(company.getPostalCode()).isEqualTo("H2X 1Y4");
        assertThat(company.getCountry()).isEqualTo("CA");
        assertThat(company.getLatitude()).isEqualByComparingTo(new BigDecimal("45.501689"));
        assertThat(company.getLongitude()).isEqualByComparingTo(new BigDecimal("-73.567256"));
        assertThat(company.getCreatedBy().getId()).isEqualTo(user.getId());
        assertThat(companyUser.getRole()).isEqualTo(CompanyRole.ADMIN);
        assertThat(companyUser.getStatus()).isEqualTo(CompanyUserStatus.ACTIVE);
    }

    @Test
    void defaultsCompanyCurrencyToCadWhenMissing() throws Exception {
        User user = userRepository.save(new User(
                "company-default-currency.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Company",
                "Currency"));
        String token = accessTokenGenerator.generate(user).value();

        mockMvc.perform(post("/api/companies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Default Currency Company"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("CAD"))
                .andExpect(jsonPath("$.businessType").value("OTHER"));
    }

    @Test
    void rejectsCompanyCreationWithUnsupportedBusinessType() throws Exception {
        User user = userRepository.save(new User(
                "company-invalid-business-type.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Company",
                "BusinessType"));
        String token = accessTokenGenerator.generate(user).value();

        mockMvc.perform(post("/api/companies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Invalid Business Type Company",
                                  "businessType": "MUSEUM"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Business type must be a supported company business type"));
    }

    @Test
    void rejectsCompanyCreationWithLatitudeOutOfRange() throws Exception {
        User user = userRepository.save(new User(
                "company-invalid-latitude.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Company",
                "Latitude"));
        String token = accessTokenGenerator.generate(user).value();

        mockMvc.perform(post("/api/companies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Invalid Latitude Company",
                                  "latitude": 91
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'latitude')]").exists());
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
    void rejectsCompanyCreationWithUnsupportedCurrency() throws Exception {
        User user = userRepository.save(new User(
                "company-invalid-currency.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Company",
                "Currency"));
        String token = accessTokenGenerator.generate(user).value();

        mockMvc.perform(post("/api/companies")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Invalid Currency Company",
                                  "currency": "ZZZ"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Currency must be a valid ISO 4217 code"));
    }

    @Test
    void updatesActiveCompanyWhenUserIsAdmin() throws Exception {
        User admin = userRepository.save(new User(
                "company-update-admin.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Company",
                "Admin"));
        Company company = activeCompany("Before Company", "Before description", admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put("/api/companies/{companyId}", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " Updated Company ",
                                  "description": " Updated description ",
                                  "currency": "eur",
                                  "businessType": "SERVICE",
                                  "addressLine1": " 456 Update Street ",
                                  "addressLine2": "",
                                  "city": " Paris ",
                                  "region": " Ile-de-France ",
                                  "postalCode": " 75001 ",
                                  "country": " fr ",
                                  "latitude": 48.856613,
                                  "longitude": 2.352222
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(company.getId().toString()))
                .andExpect(jsonPath("$.name").value("Updated Company"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.businessType").value("SERVICE"))
                .andExpect(jsonPath("$.addressLine1").value("456 Update Street"))
                .andExpect(jsonPath("$.addressLine2").doesNotExist())
                .andExpect(jsonPath("$.city").value("Paris"))
                .andExpect(jsonPath("$.region").value("Ile-de-France"))
                .andExpect(jsonPath("$.postalCode").value("75001"))
                .andExpect(jsonPath("$.country").value("FR"))
                .andExpect(jsonPath("$.latitude").value(48.856613))
                .andExpect(jsonPath("$.longitude").value(2.352222))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        Company updatedCompany = companyRepository.findById(company.getId()).orElseThrow();
        assertThat(updatedCompany.getName()).isEqualTo("Updated Company");
        assertThat(updatedCompany.getDescription()).isEqualTo("Updated description");
        assertThat(updatedCompany.getCurrency()).isEqualTo("EUR");
        assertThat(updatedCompany.getBusinessType()).isEqualTo(CompanyBusinessType.SERVICE);
        assertThat(updatedCompany.getAddressLine1()).isEqualTo("456 Update Street");
        assertThat(updatedCompany.getAddressLine2()).isNull();
        assertThat(updatedCompany.getCity()).isEqualTo("Paris");
        assertThat(updatedCompany.getRegion()).isEqualTo("Ile-de-France");
        assertThat(updatedCompany.getPostalCode()).isEqualTo("75001");
        assertThat(updatedCompany.getCountry()).isEqualTo("FR");
        assertThat(updatedCompany.getLatitude()).isEqualByComparingTo(new BigDecimal("48.856613"));
        assertThat(updatedCompany.getLongitude()).isEqualByComparingTo(new BigDecimal("2.352222"));
        assertThat(updatedCompany.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(updatedCompany.getUpdatedBy().getId()).isEqualTo(admin.getId());
    }

    @Test
    void rejectsCompanyUpdateWithoutJwt() throws Exception {
        mockMvc.perform(put("/api/companies/{companyId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Company"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsCompanyUpdateWhenUserIsNotAdmin() throws Exception {
        User owner = userRepository.save(new User(
                "company-update-owner.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Company",
                "Owner"));
        User employee = userRepository.save(new User(
                "company-update-employee.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Company",
                "Employee"));
        Company company = activeCompany("Employee Forbidden Company", "Visible", owner);
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(put("/api/companies/{companyId}", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Forbidden Update"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));
    }

    @Test
    void rejectsCompanyUpdateForDisabledCompany() throws Exception {
        User admin = userRepository.save(new User(
                "company-update-disabled.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Company",
                "Disabled"));
        Company disabledCompany = companyRepository.save(new Company("Disabled Update Company", "Hidden", admin));
        companyUserRepository.save(new CompanyUser(disabledCompany.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put("/api/companies/{companyId}", disabledCompany.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Still Hidden"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Company not found"));
    }

    @Test
    void rejectsCompanyUpdateWithUnsupportedBusinessType() throws Exception {
        User admin = userRepository.save(new User(
                "company-update-invalid-business.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Company",
                "InvalidBusiness"));
        Company company = activeCompany("Invalid Business Update Company", "Visible", admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put("/api/companies/{companyId}", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Invalid Business Update Company",
                                  "businessType": "MUSEUM"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Business type must be a supported company business type"));
    }

    @Test
    void searchesActiveCompaniesWithoutJwtUsingPaginationAndNameFilter() throws Exception {
        String uniquePrefix = "Public Search %s".formatted(UUID.randomUUID());
        User owner = userRepository.save(new User(
                "public-search-owner.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Public",
                "Owner"));

        Company alphaCompany = activeCompany(uniquePrefix + " Alpha Moving", "Visible alpha", owner);
        activeCompany(uniquePrefix + " Beta Moving", "Visible beta", owner);
        companyRepository.save(new Company(uniquePrefix + " Alpha Disabled", "Hidden disabled", owner));

        mockMvc.perform(get("/api/companies")
                        .param("q", uniquePrefix + " Alpha")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(alphaCompany.getId().toString()))
                .andExpect(jsonPath("$.items[0].name").value(uniquePrefix + " Alpha Moving"))
                .andExpect(jsonPath("$.items[0].currency").value("CAD"))
                .andExpect(jsonPath("$.items[0].businessType").value("OTHER"))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void searchesActiveCompaniesWithBusinessTypeAndLocationFilters() throws Exception {
        String uniquePrefix = "Public Location %s".formatted(UUID.randomUUID());
        User owner = userRepository.save(new User(
                "public-location-owner.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Public",
                "Location"));

        Company matchingCompany = activeCompany(
                uniquePrefix + " Bistro",
                "Visible bistro",
                "12 Rue Flow",
                null,
                "Montreal",
                "Quebec",
                "H2X 1Y4",
                "CA",
                new BigDecimal("45.501689"),
                new BigDecimal("-73.567256"),
                "CAD",
                CompanyBusinessType.RESTAURANT,
                owner);
        activeCompany(
                uniquePrefix + " Salon",
                "Visible salon",
                "20 Avenue Move",
                null,
                "Montreal",
                "Quebec",
                "H2X 1Y4",
                "CA",
                null,
                null,
                "CAD",
                CompanyBusinessType.HAIR_SALON,
                owner);
        activeCompany(
                uniquePrefix + " Toronto Bistro",
                "Wrong city",
                "44 King Street",
                null,
                "Toronto",
                "Ontario",
                "M5H 1A1",
                "CA",
                null,
                null,
                "CAD",
                CompanyBusinessType.RESTAURANT,
                owner);

        mockMvc.perform(get("/api/companies")
                        .param("q", uniquePrefix)
                        .param("businessType", "RESTAURANT")
                        .param("city", "montreal")
                        .param("region", "quebec")
                        .param("country", "ca")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "city,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(matchingCompany.getId().toString()))
                .andExpect(jsonPath("$.items[0].businessType").value("RESTAURANT"))
                .andExpect(jsonPath("$.items[0].city").value("Montreal"))
                .andExpect(jsonPath("$.items[0].region").value("Quebec"))
                .andExpect(jsonPath("$.items[0].country").value("CA"))
                .andExpect(jsonPath("$.items[0].latitude").value(45.501689))
                .andExpect(jsonPath("$.items[0].longitude").value(-73.567256))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void rejectsCompanySearchWithUnsupportedBusinessType() throws Exception {
        mockMvc.perform(get("/api/companies")
                        .param("businessType", "MUSEUM"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Business type must be a supported company business type"));
    }

    @Test
    void searchesActiveCompaniesExcludingDisabledCompanies() throws Exception {
        String uniquePrefix = "Public Disabled %s".formatted(UUID.randomUUID());
        User owner = userRepository.save(new User(
                "public-search-disabled.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Public",
                "Disabled"));

        activeCompany(uniquePrefix + " Visible Company", "Visible", owner);
        companyRepository.save(new Company(uniquePrefix + " Hidden Disabled Company", "Hidden", owner));

        mockMvc.perform(get("/api/companies")
                        .param("q", uniquePrefix)
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.name == '%s Visible Company')]".formatted(uniquePrefix)).exists())
                .andExpect(jsonPath("$.items[?(@.name == '%s Hidden Disabled Company')]".formatted(uniquePrefix)).doesNotExist());
    }

    @Test
    void getsActiveCompanyWithoutJwt() throws Exception {
        User owner = userRepository.save(new User(
                "public-detail-owner.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Public",
                "Detail"));
        Company company = activeCompany("Public Detail Company", "Visible detail", owner);

        mockMvc.perform(get("/api/companies/{companyId}", company.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(company.getId().toString()))
                .andExpect(jsonPath("$.name").value("Public Detail Company"))
                .andExpect(jsonPath("$.description").value("Visible detail"))
                .andExpect(jsonPath("$.currency").value("CAD"))
                .andExpect(jsonPath("$.businessType").value("OTHER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.role").doesNotExist());
    }

    @Test
    void hidesDisabledCompanyDetailPublicly() throws Exception {
        User owner = userRepository.save(new User(
                "public-detail-disabled.%s@flowmova.test".formatted(UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Public",
                "Disabled"));
        Company disabledCompany = companyRepository.save(new Company(
                "Disabled Detail Company",
                "Hidden detail",
                owner));

        mockMvc.perform(get("/api/companies/{companyId}", disabledCompany.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Company not found"));
    }

    @Test
    void returnsNotFoundForUnknownCompanyDetail() throws Exception {
        mockMvc.perform(get("/api/companies/{companyId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Company not found"));
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
                .andExpect(jsonPath("$.items[0].currency").value("CAD"))
                .andExpect(jsonPath("$.items[0].businessType").value("OTHER"))
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

    private Company activeCompany(
            String name,
            String description,
            String addressLine1,
            String addressLine2,
            String city,
            String region,
            String postalCode,
            String country,
            BigDecimal latitude,
            BigDecimal longitude,
            String currency,
            CompanyBusinessType businessType,
            User createdBy) {
        Company company = new Company(
                name,
                description,
                addressLine1,
                addressLine2,
                city,
                region,
                postalCode,
                country,
                latitude,
                longitude,
                currency,
                businessType,
                createdBy);
        company.activate();
        return companyRepository.save(company);
    }
}
