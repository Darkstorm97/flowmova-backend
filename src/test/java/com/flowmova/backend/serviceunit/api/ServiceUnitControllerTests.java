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
import com.flowmova.backend.catalog.domain.Catalog;
import com.flowmova.backend.catalog.infrastructure.CatalogRepository;
import com.flowmova.backend.catalogcategory.domain.CatalogCategory;
import com.flowmova.backend.catalogcategory.infrastructure.CatalogCategoryRepository;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.item.domain.Item;
import com.flowmova.backend.item.domain.ItemAvailability;
import com.flowmova.backend.item.domain.ItemStatus;
import com.flowmova.backend.item.infrastructure.ItemRepository;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.ServiceUnitCreationEntryMode;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.serviceunit.domain.ServiceUnitType;
import com.flowmova.backend.serviceunit.domain.TicketCreationGuardMode;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationStatus;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationType;
import com.flowmova.backend.serviceunitlocation.infrastructure.ServiceUnitLocationRepository;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import jakarta.persistence.EntityManager;
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
    private CatalogCategoryRepository catalogCategoryRepository;

    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ServiceUnitRepository serviceUnitRepository;

    @Autowired
    private ServiceUnitLocationRepository serviceUnitLocationRepository;

    @Autowired
    private EntityManager entityManager;

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
                .andExpect(jsonPath("$.ticketCreationGuardMode").value("NONE"))
                .andExpect(jsonPath("$.creationEntryMode").value("PUBLIC_AND_QR"))
                .andExpect(jsonPath("$.allowTicketWithoutItems").value(true))
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
        assertThat(serviceUnit.getCreationEntryMode()).isEqualTo(ServiceUnitCreationEntryMode.PUBLIC_AND_QR);
        assertThat(serviceUnit.isTicketWithoutItemsAllowed()).isTrue();
        assertThat(serviceUnit.getCreatedBy().getId()).isEqualTo(admin.getId());
        assertThat(defaultLocation.getServiceUnit().getId()).isEqualTo(serviceUnitId);
        assertThat(defaultLocation.getType()).isEqualTo(ServiceUnitLocationType.DEFAULT);
        assertThat(defaultLocation.isDefaultLocation()).isTrue();
        assertThat(defaultLocation.getStatus()).isEqualTo(ServiceUnitLocationStatus.ACTIVE);
        assertThat(defaultLocation.getCreatedBy().getId()).isEqualTo(admin.getId());
    }

    @Test
    void adminCreatesServiceUnitThatRequiresItems() throws Exception {
        User admin = user("service-unit-requires-items");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        String response = mockMvc.perform(post("/api/companies/{companyId}/service-units", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Commande avec article",
                                  "type": "TICKET_QUEUE",
                                  "allowTicketWithoutItems": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allowTicketWithoutItems").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ServiceUnit serviceUnit = serviceUnitRepository.findById(
                        UUID.fromString(objectMapper.readTree(response).get("id").asText()))
                .orElseThrow();
        assertThat(serviceUnit.isTicketWithoutItemsAllowed()).isFalse();
    }

    @Test
    void adminCreatesServiceUnitWithQrOnlyCreationEntryMode() throws Exception {
        User admin = user("service-unit-qr-only");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        String response = mockMvc.perform(post("/api/companies/{companyId}/service-units", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "QR queue",
                                  "type": "TICKET_QUEUE",
                                  "creationEntryMode": "QR_ONLY"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creationEntryMode").value("QR_ONLY"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ServiceUnit serviceUnit = serviceUnitRepository.findById(
                        UUID.fromString(objectMapper.readTree(response).get("id").asText()))
                .orElseThrow();
        assertThat(serviceUnit.getCreationEntryMode()).isEqualTo(ServiceUnitCreationEntryMode.QR_ONLY);
    }

    @Test
    void adminCreatesServiceUnitWithTicketCreationGuardMode() throws Exception {
        User admin = user("service-unit-active-ticket-limit");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        String response = mockMvc.perform(post("/api/companies/{companyId}/service-units", company.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Anti spam queue",
                                  "type": "TICKET_QUEUE",
                                  "ticketCreationGuardMode": "AUTHENTICATED_ONLY_ONE_OPEN_TICKET"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCreationGuardMode").value("AUTHENTICATED_ONLY_ONE_OPEN_TICKET"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ServiceUnit serviceUnit = serviceUnitRepository.findById(
                        UUID.fromString(objectMapper.readTree(response).get("id").asText()))
                .orElseThrow();
        assertThat(serviceUnit.getTicketCreationGuardMode())
                .isEqualTo(TicketCreationGuardMode.AUTHENTICATED_ONLY_ONE_OPEN_TICKET);
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
        ServiceUnitLocation tableLocation = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Table 4",
                "Public table",
                ServiceUnitLocationType.CUSTOM,
                false,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));
        Catalog availableCatalog = catalog(company, admin, "Visible Sandwich", new BigDecimal("8.50"));
        Catalog unavailableCatalog = catalog(company, admin, "Unavailable Sandwich", new BigDecimal("9.50"));
        Item availableItem = itemRepository.saveAndFlush(new Item(
                serviceUnit,
                availableCatalog,
                new BigDecimal("8.00"),
                ItemAvailability.AVAILABLE,
                12,
                1));
        itemRepository.saveAndFlush(new Item(
                serviceUnit,
                unavailableCatalog,
                new BigDecimal("9.00"),
                ItemAvailability.UNAVAILABLE,
                5,
                2));

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
                .andExpect(jsonPath("$.defaultLocation.status").value("ACTIVE"))
                .andExpect(jsonPath("$.locations.length()").value(2))
                .andExpect(jsonPath("$.locations[0].id").value(defaultLocation.getId().toString()))
                .andExpect(jsonPath("$.locations[1].id").value(tableLocation.getId().toString()))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(availableItem.getId().toString()))
                .andExpect(jsonPath("$.items[0].catalog.name").value("Visible Sandwich"))
                .andExpect(jsonPath("$.items[0].priceAmount").value(8.00))
                .andExpect(jsonPath("$.items[0].availability").value("AVAILABLE"));
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
    void guestGetsPublicLocationAccess() throws Exception {
        User admin = user("public-location-access-admin");
        Company company = activeCompany(admin);
        ServiceUnit serviceUnit = new ServiceUnit(
                company,
                "Public Access Queue",
                "Queue accessed by public slug",
                "Counter",
                null,
                admin);
        serviceUnit.open(admin);
        serviceUnit = serviceUnitRepository.save(serviceUnit);
        ServiceUnitLocation location = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Table 12",
                "Public table",
                ServiceUnitLocationType.CUSTOM,
                false,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));
        Catalog availableCatalog = catalog(company, admin, "Visible Coffee", new BigDecimal("3.50"));
        Catalog archivedCatalog = catalog(company, admin, "Archived Coffee", new BigDecimal("4.50"));
        archivedCatalog.archive(admin);
        catalogRepository.saveAndFlush(archivedCatalog);
        Item availableItem = itemRepository.saveAndFlush(new Item(
                serviceUnit,
                availableCatalog,
                new BigDecimal("3.25"),
                ItemAvailability.AVAILABLE,
                50,
                1));
        itemRepository.saveAndFlush(new Item(
                serviceUnit,
                archivedCatalog,
                new BigDecimal("4.25"),
                ItemAvailability.AVAILABLE,
                20,
                2));

        mockMvc.perform(get("/api/public/locations/{publicAccessSlug}", location.getPublicAccessSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company.id").value(company.getId().toString()))
                .andExpect(jsonPath("$.company.name").value(company.getName()))
                .andExpect(jsonPath("$.company.status").value("ACTIVE"))
                .andExpect(jsonPath("$.serviceUnit.id").value(serviceUnit.getId().toString()))
                .andExpect(jsonPath("$.serviceUnit.companyId").value(company.getId().toString()))
                .andExpect(jsonPath("$.serviceUnit.name").value("Public Access Queue"))
                .andExpect(jsonPath("$.serviceUnit.status").value("OPEN"))
                .andExpect(jsonPath("$.location.id").value(location.getId().toString()))
                .andExpect(jsonPath("$.location.serviceUnitId").value(serviceUnit.getId().toString()))
                .andExpect(jsonPath("$.location.name").value("Table 12"))
                .andExpect(jsonPath("$.location.publicAccessSlug").value(location.getPublicAccessSlug()))
                .andExpect(jsonPath("$.location.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(availableItem.getId().toString()))
                .andExpect(jsonPath("$.items[0].catalog.name").value("Visible Coffee"))
                .andExpect(jsonPath("$.items[0].priceAmount").value(3.25))
                .andExpect(jsonPath("$.items[0].availability").value("AVAILABLE"));
    }

    @Test
    void guestCannotGetPublicLocationForClosedServiceUnit() throws Exception {
        User admin = user("public-location-closed-unit");
        Company company = activeCompany(admin);
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Closed Public Queue",
                null,
                null,
                null,
                admin));
        ServiceUnitLocation location = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));

        mockMvc.perform(get("/api/public/locations/{publicAccessSlug}", location.getPublicAccessSlug()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Public location not found"));
    }

    @Test
    void guestCannotGetPublicLocationForArchivedServiceUnit() throws Exception {
        User admin = user("public-location-archived-unit");
        Company company = activeCompany(admin);
        ServiceUnit serviceUnit = new ServiceUnit(
                company,
                "Archived Public Queue",
                null,
                null,
                null,
                admin);
        serviceUnit.archive(admin);
        serviceUnit = serviceUnitRepository.save(serviceUnit);
        ServiceUnitLocation location = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));

        mockMvc.perform(get("/api/public/locations/{publicAccessSlug}", location.getPublicAccessSlug()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Public location not found"));
    }

    @Test
    void guestCannotGetArchivedPublicLocation() throws Exception {
        User admin = user("public-location-archived-location");
        Company company = activeCompany(admin);
        ServiceUnit serviceUnit = new ServiceUnit(
                company,
                "Archived Location Queue",
                null,
                null,
                null,
                admin);
        serviceUnit.open(admin);
        serviceUnit = serviceUnitRepository.save(serviceUnit);
        ServiceUnitLocation location = new ServiceUnitLocation(
                serviceUnit,
                "Archived Place",
                null,
                ServiceUnitLocationType.CUSTOM,
                false,
                "loc-%s".formatted(UUID.randomUUID()),
                admin);
        location.archive(admin);
        location = serviceUnitLocationRepository.save(location);

        mockMvc.perform(get("/api/public/locations/{publicAccessSlug}", location.getPublicAccessSlug()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Public location not found"));
    }

    @Test
    void guestCannotGetPublicLocationForDisabledCompany() throws Exception {
        User admin = user("public-location-disabled-company");
        Company disabledCompany = companyRepository.save(new Company(
                "Disabled Public Location Company",
                "Hidden public location company",
                admin));
        ServiceUnit serviceUnit = new ServiceUnit(
                disabledCompany,
                "Disabled Company Public Queue",
                null,
                null,
                null,
                admin);
        serviceUnit.open(admin);
        serviceUnit = serviceUnitRepository.save(serviceUnit);
        ServiceUnitLocation location = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));

        mockMvc.perform(get("/api/public/locations/{publicAccessSlug}", location.getPublicAccessSlug()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Public location not found"));
    }

    @Test
    void guestCannotGetUnknownPublicLocation() throws Exception {
        mockMvc.perform(get("/api/public/locations/{publicAccessSlug}", "loc-unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Public location not found"));
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
    void adminClosesOpenServiceUnit() throws Exception {
        User admin = user("service-unit-close-admin");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = new ServiceUnit(
                company,
                "Open To Close Queue",
                null,
                null,
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

        mockMvc.perform(post(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/close",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceUnit.getId().toString()))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.defaultLocation.id").value(defaultLocation.getId().toString()));

        ServiceUnit closedServiceUnit = serviceUnitRepository.findById(serviceUnit.getId()).orElseThrow();
        assertThat(closedServiceUnit.getStatus()).isEqualTo(ServiceUnitStatus.CLOSED);
        assertThat(closedServiceUnit.getUpdatedBy().getId()).isEqualTo(admin.getId());
    }

    @Test
    void rejectsServiceUnitCloseWhenNotOpen() throws Exception {
        User admin = user("service-unit-close-not-open");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Already Closed Queue",
                null,
                null,
                null,
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/close",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Service unit must be OPEN to be closed"));
    }

    @Test
    void rejectsServiceUnitCloseForEmployee() throws Exception {
        User employee = user("service-unit-close-employee");
        Company company = activeCompany(employee);
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        ServiceUnit serviceUnit = new ServiceUnit(
                company,
                "Employee Close Queue",
                null,
                null,
                null,
                employee);
        serviceUnit.open(employee);
        serviceUnit = serviceUnitRepository.save(serviceUnit);
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(post(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/close",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));
    }

    @Test
    void adminArchivesClosedServiceUnit() throws Exception {
        User admin = user("service-unit-archive-closed-admin");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Closed To Archive Queue",
                null,
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
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/archive",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceUnit.getId().toString()))
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.defaultLocation.id").value(defaultLocation.getId().toString()));

        ServiceUnit archivedServiceUnit = serviceUnitRepository.findById(serviceUnit.getId()).orElseThrow();
        assertThat(archivedServiceUnit.getStatus()).isEqualTo(ServiceUnitStatus.ARCHIVED);
        assertThat(archivedServiceUnit.getUpdatedBy().getId()).isEqualTo(admin.getId());
    }

    @Test
    void adminArchivesOpenServiceUnitAndItIsHiddenPublicly() throws Exception {
        User admin = user("service-unit-archive-open-admin");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = new ServiceUnit(
                company,
                "Open To Archive Queue",
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
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/archive",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(get(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}",
                        company.getId(),
                        serviceUnit.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Service unit not found"));
    }

    @Test
    void rejectsServiceUnitArchiveWhenAlreadyArchived() throws Exception {
        User admin = user("service-unit-archive-already");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = new ServiceUnit(
                company,
                "Already Archived Queue",
                null,
                null,
                null,
                admin);
        serviceUnit.archive(admin);
        serviceUnit = serviceUnitRepository.save(serviceUnit);
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/archive",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Service unit is already archived"));
    }

    @Test
    void rejectsServiceUnitArchiveForEmployee() throws Exception {
        User employee = user("service-unit-archive-employee");
        Company company = activeCompany(employee);
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Employee Archive Queue",
                null,
                null,
                null,
                employee));
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(post(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/archive",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));
    }

    @Test
    void adminCreatesServiceUnitLocation() throws Exception {
        User admin = user("service-unit-location-create-admin");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Location Create Queue",
                null,
                null,
                null,
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        String response = mockMvc.perform(post(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/locations",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " Table 12 ",
                                  "description": " Pres de la fenetre "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.serviceUnitId").value(serviceUnit.getId().toString()))
                .andExpect(jsonPath("$.name").value("Table 12"))
                .andExpect(jsonPath("$.description").value("Pres de la fenetre"))
                .andExpect(jsonPath("$.type").value("CUSTOM"))
                .andExpect(jsonPath("$.defaultLocation").value(false))
                .andExpect(jsonPath("$.publicAccessSlug").value(org.hamcrest.Matchers.matchesPattern("loc-[a-z0-9]{12,32}")))
                .andExpect(jsonPath("$.publicUrl").value(org.hamcrest.Matchers.containsString("/public/locations/loc-")))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(response);
        ServiceUnitLocation location = serviceUnitLocationRepository
                .findById(UUID.fromString(responseJson.get("id").asText()))
                .orElseThrow();

        assertThat(location.getServiceUnit().getId()).isEqualTo(serviceUnit.getId());
        assertThat(location.getType()).isEqualTo(ServiceUnitLocationType.CUSTOM);
        assertThat(location.isDefaultLocation()).isFalse();
        assertThat(location.getCreatedBy().getId()).isEqualTo(admin.getId());
    }

    @Test
    void rejectsServiceUnitLocationCreationForEmployee() throws Exception {
        User employee = user("service-unit-location-create-employee");
        Company company = activeCompany(employee);
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Location Employee Queue",
                null,
                null,
                null,
                employee));
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(post(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/locations",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Table employee"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));
    }

    @Test
    void rejectsServiceUnitLocationCreationWithoutName() throws Exception {
        User admin = user("service-unit-location-create-invalid");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Location Invalid Queue",
                null,
                null,
                null,
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/locations",
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
    void adminListsServiceUnitLocationsWithPagination() throws Exception {
        User admin = user("service-unit-location-list-admin");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        Company otherCompany = activeCompany(admin);
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Location List Queue",
                null,
                null,
                null,
                admin));
        ServiceUnit otherServiceUnit = serviceUnitRepository.save(new ServiceUnit(
                otherCompany,
                "Other Location Queue",
                null,
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
        serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Table 2",
                "Second table",
                ServiceUnitLocationType.CUSTOM,
                false,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));
        serviceUnitLocationRepository.save(new ServiceUnitLocation(
                otherServiceUnit,
                "Other Table",
                null,
                ServiceUnitLocationType.CUSTOM,
                false,
                "loc-%s".formatted(UUID.randomUUID()),
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(get(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/locations",
                        company.getId(),
                        serviceUnit.getId())
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].id").value(defaultLocation.getId().toString()))
                .andExpect(jsonPath("$.items[0].defaultLocation").value(true))
                .andExpect(jsonPath("$.items[0].publicUrl").value(org.hamcrest.Matchers.containsString("/public/locations/loc-")))
                .andExpect(jsonPath("$.items[?(@.serviceUnitId == '%s')]".formatted(otherServiceUnit.getId())).doesNotExist());
    }

    @Test
    void rejectsServiceUnitLocationListForAnotherCompanyServiceUnit() throws Exception {
        User admin = user("service-unit-location-list-other-company");
        Company company = activeCompany(admin);
        Company otherCompany = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit otherServiceUnit = serviceUnitRepository.save(new ServiceUnit(
                otherCompany,
                "Other Company Location Queue",
                null,
                null,
                null,
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(get(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/locations",
                        company.getId(),
                        otherServiceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Service unit not found"));
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
                                  "location": " Updated location ",
                                  "ticketCreationGuardMode": "AUTHENTICATED_OR_GUEST_RECENT_ONE_OPEN_TICKET",
                                  "creationEntryMode": "QR_ONLY",
                                  "allowTicketWithoutItems": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(serviceUnit.getId().toString()))
                .andExpect(jsonPath("$.companyId").value(company.getId().toString()))
                .andExpect(jsonPath("$.name").value("Updated Queue"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.location").value("Updated location"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.ticketCreationGuardMode").value("AUTHENTICATED_OR_GUEST_RECENT_ONE_OPEN_TICKET"))
                .andExpect(jsonPath("$.creationEntryMode").value("QR_ONLY"))
                .andExpect(jsonPath("$.allowTicketWithoutItems").value(false))
                .andExpect(jsonPath("$.defaultLocation.id").value(defaultLocation.getId().toString()));

        serviceUnitRepository.flush();
        entityManager.clear();

        ServiceUnit updatedServiceUnit = serviceUnitRepository.findById(serviceUnit.getId()).orElseThrow();
        assertThat(updatedServiceUnit.getName()).isEqualTo("Updated Queue");
        assertThat(updatedServiceUnit.getDescription()).isEqualTo("Updated description");
        assertThat(updatedServiceUnit.getLocation()).isEqualTo("Updated location");
        assertThat(updatedServiceUnit.getStatus()).isEqualTo(ServiceUnitStatus.OPEN);
        assertThat(updatedServiceUnit.getTicketCreationGuardMode())
                .isEqualTo(TicketCreationGuardMode.AUTHENTICATED_OR_GUEST_RECENT_ONE_OPEN_TICKET);
        assertThat(updatedServiceUnit.getCreationEntryMode()).isEqualTo(ServiceUnitCreationEntryMode.QR_ONLY);
        assertThat(updatedServiceUnit.isTicketWithoutItemsAllowed()).isFalse();
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

    @Test
    void adminAssociatesCatalogToServiceUnitAsItem() throws Exception {
        User admin = user("service-unit-item-admin");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        Catalog catalog = catalog(company, admin, "Coffee", new BigDecimal("3.50"));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Cafe Counter",
                null,
                null,
                null,
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        String response = mockMvc.perform(post(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/items",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogId": "%s",
                                  "availability": "AVAILABLE",
                                  "configuredQuantity": 25,
                                  "displayOrder": 3
                                }
                                """.formatted(catalog.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.serviceUnitId").value(serviceUnit.getId().toString()))
                .andExpect(jsonPath("$.catalog.id").value(catalog.getId().toString()))
                .andExpect(jsonPath("$.catalog.companyId").value(company.getId().toString()))
                .andExpect(jsonPath("$.catalog.name").value("Coffee"))
                .andExpect(jsonPath("$.priceAmount").value(3.50))
                .andExpect(jsonPath("$.availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.configuredQuantity").value(25))
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.displayOrder").value(3))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID itemId = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        Item item = itemRepository.findById(itemId).orElseThrow();

        assertThat(item.getServiceUnit().getId()).isEqualTo(serviceUnit.getId());
        assertThat(item.getCatalog().getId()).isEqualTo(catalog.getId());
        assertThat(item.getPriceAmount()).isEqualByComparingTo("3.50");
        assertThat(item.getAvailability()).isEqualTo(ItemAvailability.AVAILABLE);
        assertThat(item.getConfiguredQuantity()).isEqualTo(25);
        assertThat(item.getDisplayOrder()).isEqualTo(3);
        assertThat(item.getStatus()).isEqualTo(ItemStatus.ACTIVE);
    }

    @Test
    void adminListsServiceUnitItemsIncludingUnavailableItems() throws Exception {
        User admin = user("service-unit-item-list-admin");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        Catalog coffee = catalog(company, admin, "Admin Coffee", new BigDecimal("3.50"));
        Catalog tea = catalog(company, admin, "Admin Tea", new BigDecimal("2.75"));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Item Admin Queue",
                null,
                null,
                null,
                admin));
        itemRepository.save(new Item(
                serviceUnit,
                coffee,
                new BigDecimal("3.50"),
                ItemAvailability.AVAILABLE,
                10,
                2));
        itemRepository.save(new Item(
                serviceUnit,
                tea,
                new BigDecimal("2.75"),
                ItemAvailability.UNAVAILABLE,
                0,
                1));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(get(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/items",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].catalog.name").value("Admin Tea"))
                .andExpect(jsonPath("$[0].availability").value("UNAVAILABLE"))
                .andExpect(jsonPath("$[1].catalog.name").value("Admin Coffee"))
                .andExpect(jsonPath("$[1].availability").value("AVAILABLE"));
    }

    @Test
    void adminAssociatesCatalogToServiceUnitWithItemPriceOverride() throws Exception {
        User admin = user("service-unit-item-price");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        Catalog catalog = catalog(company, admin, "Tea", new BigDecimal("2.75"));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Tea Counter",
                null,
                null,
                null,
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/items",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogId": "%s",
                                  "priceAmount": 2.25,
                                  "availability": "UNAVAILABLE",
                                  "configuredQuantity": 0,
                                  "displayOrder": 8
                                }
                                """.formatted(catalog.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priceAmount").value(2.25))
                .andExpect(jsonPath("$.availability").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.configuredQuantity").value(0))
                .andExpect(jsonPath("$.displayOrder").value(8));
    }

    @Test
    void rejectsDuplicateCatalogAssociationToServiceUnit() throws Exception {
        User admin = user("service-unit-item-duplicate");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        Catalog catalog = catalog(company, admin, "Duplicate Offer", null);
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Duplicate Item Queue",
                null,
                null,
                null,
                admin));
        itemRepository.saveAndFlush(new Item(
                serviceUnit,
                catalog,
                null,
                ItemAvailability.AVAILABLE,
                null,
                0));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/items",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogId": "%s"
                                }
                                """.formatted(catalog.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Catalog is already associated with service unit"));
    }

    @Test
    void rejectsCatalogAssociationForEmployee() throws Exception {
        User employee = user("service-unit-item-employee");
        Company company = activeCompany(employee);
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        Catalog catalog = catalog(company, employee, "Employee Offer", null);
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Employee Item Queue",
                null,
                null,
                null,
                employee));
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(post(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/items",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogId": "%s"
                                }
                                """.formatted(catalog.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));
    }

    @Test
    void rejectsCatalogAssociationFromAnotherCompany() throws Exception {
        User admin = user("service-unit-item-other-company");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        Company otherCompany = activeCompany(admin);
        Catalog otherCatalog = catalog(otherCompany, admin, "Other Company Offer", null);
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Wrong Catalog Queue",
                null,
                null,
                null,
                admin));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(post(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/items",
                        company.getId(),
                        serviceUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catalogId": "%s"
                                }
                                """.formatted(otherCatalog.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Catalog is invalid"));
    }

    @Test
    void adminUpdatesServiceUnitItemConfiguration() throws Exception {
        User admin = user("service-unit-item-update-admin");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Configurable Item Queue",
                null,
                null,
                null,
                admin));
        Catalog catalog = catalog(company, admin, "Configurable Offer", new BigDecimal("15.00"));
        Item item = itemRepository.saveAndFlush(new Item(
                serviceUnit,
                catalog,
                new BigDecimal("12.00"),
                ItemAvailability.AVAILABLE,
                20,
                1));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/items/{itemId}",
                        company.getId(),
                        serviceUnit.getId(),
                        item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "priceAmount": 10.50,
                                  "availability": "UNAVAILABLE",
                                  "configuredQuantity": 5,
                                  "displayOrder": 9
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(item.getId().toString()))
                .andExpect(jsonPath("$.serviceUnitId").value(serviceUnit.getId().toString()))
                .andExpect(jsonPath("$.catalog.id").value(catalog.getId().toString()))
                .andExpect(jsonPath("$.priceAmount").value(10.50))
                .andExpect(jsonPath("$.availability").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.configuredQuantity").value(5))
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.displayOrder").value(9))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(updatedItem.getPriceAmount()).isEqualByComparingTo("10.50");
        assertThat(updatedItem.getAvailability()).isEqualTo(ItemAvailability.UNAVAILABLE);
        assertThat(updatedItem.getConfiguredQuantity()).isEqualTo(5);
        assertThat(updatedItem.getReservedQuantity()).isZero();
        assertThat(updatedItem.getDisplayOrder()).isEqualTo(9);
    }

    @Test
    void adminClearsServiceUnitItemOptionalPriceAndQuantityLimit() throws Exception {
        User admin = user("service-unit-item-update-clear");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Clear Item Queue",
                null,
                null,
                null,
                admin));
        Catalog catalog = catalog(company, admin, "Clearable Offer", new BigDecimal("7.00"));
        Item item = itemRepository.saveAndFlush(new Item(
                serviceUnit,
                catalog,
                new BigDecimal("6.00"),
                ItemAvailability.UNAVAILABLE,
                3,
                2));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/items/{itemId}",
                        company.getId(),
                        serviceUnit.getId(),
                        item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "priceAmount": null,
                                  "availability": "AVAILABLE",
                                  "configuredQuantity": null,
                                  "displayOrder": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceAmount").doesNotExist())
                .andExpect(jsonPath("$.availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.configuredQuantity").doesNotExist())
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.displayOrder").value(0));

        Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(updatedItem.getPriceAmount()).isNull();
        assertThat(updatedItem.getConfiguredQuantity()).isNull();
        assertThat(updatedItem.getReservedQuantity()).isZero();
    }

    @Test
    void rejectsServiceUnitItemUpdateForEmployee() throws Exception {
        User employee = user("service-unit-item-update-employee");
        Company company = activeCompany(employee);
        companyUserRepository.save(new CompanyUser(company.getId(), employee, CompanyRole.EMPLOYEE));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Employee Item Update Queue",
                null,
                null,
                null,
                employee));
        Catalog catalog = catalog(company, employee, "Employee Update Offer", null);
        Item item = itemRepository.saveAndFlush(new Item(
                serviceUnit,
                catalog,
                null,
                ItemAvailability.AVAILABLE,
                null,
                0));
        String token = accessTokenGenerator.generate(employee).value();

        mockMvc.perform(put(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/items/{itemId}",
                        company.getId(),
                        serviceUnit.getId(),
                        item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availability": "UNAVAILABLE",
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company admin role is required"));
    }

    @Test
    void rejectsServiceUnitItemUpdateForAnotherServiceUnit() throws Exception {
        User admin = user("service-unit-item-update-other-unit");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Expected Item Queue",
                null,
                null,
                null,
                admin));
        ServiceUnit otherServiceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Other Item Queue",
                null,
                null,
                null,
                admin));
        Catalog catalog = catalog(company, admin, "Other Unit Offer", null);
        Item otherItem = itemRepository.saveAndFlush(new Item(
                otherServiceUnit,
                catalog,
                null,
                ItemAvailability.AVAILABLE,
                null,
                0));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/items/{itemId}",
                        company.getId(),
                        serviceUnit.getId(),
                        otherItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availability": "UNAVAILABLE",
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Item not found"));
    }

    @Test
    void rejectsServiceUnitItemUpdateWithoutRequiredFields() throws Exception {
        User admin = user("service-unit-item-update-invalid");
        Company company = activeCompany(admin);
        companyUserRepository.save(new CompanyUser(company.getId(), admin, CompanyRole.ADMIN));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Invalid Item Update Queue",
                null,
                null,
                null,
                admin));
        Catalog catalog = catalog(company, admin, "Invalid Update Offer", null);
        Item item = itemRepository.saveAndFlush(new Item(
                serviceUnit,
                catalog,
                null,
                ItemAvailability.AVAILABLE,
                null,
                0));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(put(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/items/{itemId}",
                        company.getId(),
                        serviceUnit.getId(),
                        item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "priceAmount": -1,
                                  "configuredQuantity": -1,
                                  "displayOrder": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'priceAmount')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'availability')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'configuredQuantity')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'displayOrder')]").exists());
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

    private Catalog catalog(Company company, User creator, String name, BigDecimal priceAmount) {
        CatalogCategory category = catalogCategoryRepository.save(new CatalogCategory(
                company,
                "Item Category %s".formatted(UUID.randomUUID()),
                null,
                0,
                creator));
        return catalogRepository.save(new Catalog(
                company,
                category,
                name,
                null,
                null,
                priceAmount,
                creator));
    }
}
