package com.flowmova.backend.serviceunit.api;

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

    @Test
    void adminGetsDefaultPublicLink() throws Exception {
        User admin = user("service-unit-link-admin");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Public Link Queue",
                "Queue with public link",
                null,
                null,
                admin));
        ServiceUnitLocation defaultLocation = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(get(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}/default-public-link",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceUnitId").value(serviceUnit.getId().toString()))
                .andExpect(jsonPath("$.locationId").value(defaultLocation.getId().toString()))
                .andExpect(jsonPath("$.publicAccessSlug").value(defaultLocation.getPublicAccessSlug()))
                .andExpect(jsonPath("$.publicUrl")
                        .value("http://localhost:3000/public/locations/%s".formatted(defaultLocation.getPublicAccessSlug())));
    }

    @Test
    void rejectsDefaultPublicLinkWithoutJwt() throws Exception {
        mockMvc.perform(get(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}/default-public-link",
                        UUID.randomUUID(),
                        UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsDefaultPublicLinkForEmployee() throws Exception {
        User employee = user("service-unit-link-employee");
        Company company = activeCompany(employee);
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Employee Link Queue",
                null,
                null,
                null,
                employee));
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(get(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}/default-public-link",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));
    }

    @Test
    void rejectsDefaultPublicLinkWhenDefaultLocationIsMissing() throws Exception {
        User admin = user("service-unit-link-missing-default");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Missing Default Queue",
                null,
                null,
                null,
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(get(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}/default-public-link",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Default location not found"));
    }

    @Test
    void adminOpensServiceUnit() throws Exception {
        User admin = user("service-unit-open-admin");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Openable Queue",
                "Queue ready to open",
                null,
                null,
                admin));
        ServiceUnitLocation defaultLocation = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}/open",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceUnit.getId().toString()))
                .andExpect(jsonPath("$.companyId").value(company.getId().toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.defaultLocation.id").value(defaultLocation.getId().toString()))
                .andExpect(jsonPath("$.defaultLocation.status").value("ACTIVE"));

        ServiceUnit openedServiceUnit = serviceUnitRepository.findById(serviceUnit.getId()).orElseThrow();
        assertThat(openedServiceUnit.getStatus()).isEqualTo(ServiceUnitStatus.OPEN);
        assertThat(openedServiceUnit.getUpdatedBy().getId()).isEqualTo(admin.getId());
    }

    @Test
    void rejectsServiceUnitOpenWithoutJwt() throws Exception {
        mockMvc.perform(post(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}/open",
                        UUID.randomUUID(),
                        UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsServiceUnitOpenForEmployee() throws Exception {
        User employee = user("service-unit-open-employee");
        Company company = activeCompany(employee);
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Employee Open Queue",
                null,
                null,
                null,
                employee));
        serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                employee));
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(post(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}/open",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));
    }

    @Test
    void rejectsServiceUnitOpenWhenServiceUnitIsNotClosed() throws Exception {
        User admin = user("service-unit-open-not-closed");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = new ServiceUnit(
                company,
                "Already Open Queue",
                null,
                null,
                null,
                admin);
        serviceUnit.open(admin);
        serviceUnit = serviceUnitRepository.save(serviceUnit);
        serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}/open",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Service unit must be CLOSED to be opened"));
    }

    @Test
    void rejectsServiceUnitOpenWhenDefaultLocationIsMissing() throws Exception {
        User admin = user("service-unit-open-missing-default");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Missing Default Open Queue",
                null,
                null,
                null,
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}/open",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Default location not found"));
    }

    @Test
    void guestListsOpenServiceUnitsForActiveCompany() throws Exception {
        User admin = user("service-unit-list-admin");
        Company company = activeCompany(admin);
        ServiceUnit openServiceUnit = new ServiceUnit(
                company,
                "Open Queue",
                "Visible queue",
                "Front desk",
                null,
                admin);
        openServiceUnit.open(admin);
        openServiceUnit = serviceUnitRepository.save(openServiceUnit);
        ServiceUnitLocation defaultLocation = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                openServiceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));

        ServiceUnit closedServiceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Closed Queue",
                "Hidden closed queue",
                null,
                null,
                admin));
        serviceUnitLocationRepository.save(new ServiceUnitLocation(
                closedServiceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));

        ServiceUnit archivedServiceUnit = new ServiceUnit(
                company,
                "Archived Queue",
                "Hidden archived queue",
                null,
                null,
                admin);
        archivedServiceUnit.archive(admin);
        archivedServiceUnit = serviceUnitRepository.save(archivedServiceUnit);
        serviceUnitLocationRepository.save(new ServiceUnitLocation(
                archivedServiceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));

        mockMvc.perform(get("/api/companies/{companyId}/service-units", company.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(openServiceUnit.getId().toString()))
                .andExpect(jsonPath("$[0].companyId").value(company.getId().toString()))
                .andExpect(jsonPath("$[0].name").value("Open Queue"))
                .andExpect(jsonPath("$[0].description").value("Visible queue"))
                .andExpect(jsonPath("$[0].location").value("Front desk"))
                .andExpect(jsonPath("$[0].type").value("TICKET_QUEUE"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].defaultLocation.id").value(defaultLocation.getId().toString()))
                .andExpect(jsonPath("$[0].defaultLocation.status").value("ACTIVE"));
    }

    @Test
    void guestListsNoServiceUnitForDisabledCompany() throws Exception {
        User admin = user("service-unit-list-disabled-company");
        Company disabledCompany = companyRepository.save(new Company(
                "Disabled Service Unit List Company",
                "Hidden service unit list company",
                admin));
        ServiceUnit serviceUnit = new ServiceUnit(
                disabledCompany,
                "Hidden Queue",
                null,
                null,
                null,
                admin);
        serviceUnit.open(admin);
        serviceUnitRepository.save(serviceUnit);

        mockMvc.perform(get("/api/companies/{companyId}/service-units", disabledCompany.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Company not found"));
    }

    @Test
    void guestGetsOpenServiceUnit() throws Exception {
        User admin = user("service-unit-get-admin");
        Company company = activeCompany(admin);
        ServiceUnit serviceUnit = new ServiceUnit(
                company,
                "Detail Open Queue",
                "Visible detail queue",
                "Counter",
                null,
                admin);
        serviceUnit.open(admin);
        serviceUnit = serviceUnitRepository.save(serviceUnit);
        ServiceUnitLocation defaultLocation = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));

        mockMvc.perform(get(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}",
                        company.getId(),
                        serviceUnit.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceUnit.getId().toString()))
                .andExpect(jsonPath("$.companyId").value(company.getId().toString()))
                .andExpect(jsonPath("$.name").value("Detail Open Queue"))
                .andExpect(jsonPath("$.description").value("Visible detail queue"))
                .andExpect(jsonPath("$.location").value("Counter"))
                .andExpect(jsonPath("$.type").value("TICKET_QUEUE"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.defaultLocation.id").value(defaultLocation.getId().toString()))
                .andExpect(jsonPath("$.defaultLocation.status").value("ACTIVE"));
    }

    @Test
    void guestCannotGetClosedServiceUnit() throws Exception {
        User admin = user("service-unit-get-closed");
        Company company = activeCompany(admin);
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Closed Detail Queue",
                "Hidden detail queue",
                null,
                null,
                admin));
        serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));

        mockMvc.perform(get(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}",
                        company.getId(),
                        serviceUnit.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Service unit not found"));
    }

    @Test
    void guestCannotGetArchivedServiceUnit() throws Exception {
        User admin = user("service-unit-get-archived");
        Company company = activeCompany(admin);
        ServiceUnit serviceUnit = new ServiceUnit(
                company,
                "Archived Detail Queue",
                "Hidden archived detail queue",
                null,
                null,
                admin);
        serviceUnit.archive(admin);
        serviceUnit = serviceUnitRepository.save(serviceUnit);
        serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));

        mockMvc.perform(get(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}",
                        company.getId(),
                        serviceUnit.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Service unit not found"));
    }

    @Test
    void guestCannotGetOpenServiceUnitForDisabledCompany() throws Exception {
        User admin = user("service-unit-get-disabled-company");
        Company disabledCompany = companyRepository.save(new Company(
                "Disabled Service Unit Detail Company",
                "Hidden service unit detail company",
                admin));
        ServiceUnit serviceUnit = new ServiceUnit(
                disabledCompany,
                "Disabled Company Detail Queue",
                null,
                null,
                null,
                admin);
        serviceUnit.open(admin);
        serviceUnit = serviceUnitRepository.save(serviceUnit);
        serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));

        mockMvc.perform(get(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}",
                        disabledCompany.getId(),
                        serviceUnit.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Company not found"));
    }

    @Test
    void adminListsServiceUnitsWithPagination() throws Exception {
        User admin = user("service-unit-admin-list");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        Company otherCompany = activeCompany(admin);

        ServiceUnit archivedServiceUnit = new ServiceUnit(
                company,
                "Archive Queue",
                "Archived admin queue",
                null,
                null,
                admin);
        archivedServiceUnit.archive(admin);
        archivedServiceUnit = serviceUnitRepository.save(archivedServiceUnit);
        serviceUnitLocationRepository.save(new ServiceUnitLocation(
                archivedServiceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));

        ServiceUnit closedServiceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Closed Queue",
                "Closed admin queue",
                null,
                null,
                admin));
        ServiceUnitLocation closedDefaultLocation = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                closedServiceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));

        ServiceUnit openServiceUnit = new ServiceUnit(
                company,
                "Open Queue",
                "Open admin queue",
                null,
                null,
                admin);
        openServiceUnit.open(admin);
        serviceUnitRepository.save(openServiceUnit);

        ServiceUnit otherCompanyServiceUnit = new ServiceUnit(
                otherCompany,
                "Other Company Queue",
                null,
                null,
                null,
                admin);
        otherCompanyServiceUnit.open(admin);
        serviceUnitRepository.save(otherCompanyServiceUnit);

        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(get("/api/companies/{companyId}/admin/service-units", company.getId())
                        .param("page", "0")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items[0].id").value(archivedServiceUnit.getId().toString()))
                .andExpect(jsonPath("$.items[0].status").value("ARCHIVED"))
                .andExpect(jsonPath("$.items[1].id").value(closedServiceUnit.getId().toString()))
                .andExpect(jsonPath("$.items[1].status").value("CLOSED"))
                .andExpect(jsonPath("$.items[1].defaultLocation.id").value(closedDefaultLocation.getId().toString()));
    }

    @Test
    void adminFiltersServiceUnitsByStatus() throws Exception {
        User admin = user("service-unit-admin-filter");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit closedServiceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Closed Filter Queue",
                null,
                null,
                null,
                admin));
        ServiceUnit openServiceUnit = new ServiceUnit(
                company,
                "Open Filter Queue",
                null,
                null,
                null,
                admin);
        openServiceUnit.open(admin);
        serviceUnitRepository.save(openServiceUnit);
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(get("/api/companies/{companyId}/admin/service-units", company.getId())
                        .param("status", "CLOSED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].id").value(closedServiceUnit.getId().toString()))
                .andExpect(jsonPath("$.items[0].status").value("CLOSED"));
    }

    @Test
    void rejectsAdminServiceUnitListWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/companies/{companyId}/admin/service-units", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsAdminServiceUnitListForEmployee() throws Exception {
        User employee = user("service-unit-admin-list-employee");
        Company company = activeCompany(employee);
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(get("/api/companies/{companyId}/admin/service-units", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));
    }

    @Test
    void rejectsAdminServiceUnitListForDisabledCompany() throws Exception {
        User admin = user("service-unit-admin-list-disabled-company");
        Company disabledCompany = companyRepository.save(new Company(
                "Disabled Admin Service Unit List Company",
                "Hidden admin service unit list company",
                admin));
        companyUserRepository.save(new CompanyUser(disabledCompany.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(get("/api/companies/{companyId}/admin/service-units", disabledCompany.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Company not found"));
    }

    @Test
    void adminUpdatesServiceUnitWithoutChangingStatusOrDefaultLocation() throws Exception {
        User admin = user("service-unit-update-admin");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = new ServiceUnit(
                company,
                "Old Queue",
                "Old description",
                "Old location",
                null,
                admin);
        serviceUnit.open(admin);
        serviceUnit = serviceUnitRepository.save(serviceUnit);
        ServiceUnitLocation defaultLocation = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " Updated Queue ",
                                  "description": " Updated description ",
                                  "location": " Updated location "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceUnit.getId().toString()))
                .andExpect(jsonPath("$.companyId").value(company.getId().toString()))
                .andExpect(jsonPath("$.name").value("Updated Queue"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.location").value("Updated location"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.defaultLocation.id").value(defaultLocation.getId().toString()));

        ServiceUnit updatedServiceUnit = serviceUnitRepository.findById(serviceUnit.getId()).orElseThrow();
        assertThat(updatedServiceUnit.getName()).isEqualTo("Updated Queue");
        assertThat(updatedServiceUnit.getDescription()).isEqualTo("Updated description");
        assertThat(updatedServiceUnit.getLocation()).isEqualTo("Updated location");
        assertThat(updatedServiceUnit.getStatus()).isEqualTo(ServiceUnitStatus.OPEN);
        assertThat(updatedServiceUnit.getUpdatedBy().getId()).isEqualTo(admin.getId());
    }

    @Test
    void adminUpdatesArchivedServiceUnit() throws Exception {
        User admin = user("service-unit-update-archived");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = new ServiceUnit(
                company,
                "Archived Old Queue",
                null,
                null,
                null,
                admin);
        serviceUnit.archive(admin);
        serviceUnit = serviceUnitRepository.save(serviceUnit);
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Archived Updated Queue",
                                  "description": "",
                                  "location": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Archived Updated Queue"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.location").doesNotExist())
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.defaultLocation").doesNotExist());
    }

    @Test
    void rejectsServiceUnitUpdateWithoutJwt() throws Exception {
        mockMvc.perform(put(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}",
                        UUID.randomUUID(),
                        UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unauthorized Queue"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsServiceUnitUpdateForEmployee() throws Exception {
        User employee = user("service-unit-update-employee");
        Company company = activeCompany(employee);
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Employee Update Queue",
                null,
                null,
                null,
                employee));
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(put(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Employee Updated Queue"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));
    }

    @Test
    void rejectsServiceUnitUpdateWithoutName() throws Exception {
        User admin = user("service-unit-update-invalid");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Invalid Update Queue",
                null,
                null,
                null,
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());
    }

    @Test
    void rejectsServiceUnitUpdateForDisabledCompany() throws Exception {
        User admin = user("service-unit-update-disabled-company");
        Company disabledCompany = companyRepository.save(new Company(
                "Disabled Service Unit Update Company",
                "Hidden service unit update company",
                admin));
        companyUserRepository.save(new CompanyUser(disabledCompany.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                disabledCompany,
                "Disabled Company Update Queue",
                null,
                null,
                null,
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}",
                        disabledCompany.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Disabled Company Updated Queue"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Company not found"));
    }

    @Test
    void rejectsServiceUnitUpdateForAnotherCompanyServiceUnit() throws Exception {
        User admin = user("service-unit-update-other-company");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        Company otherCompany = activeCompany(admin);
        ServiceUnit otherCompanyServiceUnit = serviceUnitRepository.save(new ServiceUnit(
                otherCompany,
                "Other Company Update Queue",
                null,
                null,
                null,
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}",
                        company.getId(),
                        otherCompanyServiceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Wrong Company Updated Queue"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Service unit not found"));
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
