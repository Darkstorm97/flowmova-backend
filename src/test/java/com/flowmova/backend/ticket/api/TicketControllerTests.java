package com.flowmova.backend.ticket.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.flowmova.backend.item.domain.Item;
import com.flowmova.backend.item.domain.ItemAvailability;
import com.flowmova.backend.item.infrastructure.ItemRepository;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationType;
import com.flowmova.backend.serviceunitlocation.infrastructure.ServiceUnitLocationRepository;
import com.flowmova.backend.ticket.domain.Ticket;
import com.flowmova.backend.ticket.domain.TicketStatus;
import com.flowmova.backend.ticket.infrastructure.TicketLineRepository;
import com.flowmova.backend.ticket.infrastructure.TicketRepository;
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
class TicketControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CatalogCategoryRepository catalogCategoryRepository;

    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private ServiceUnitRepository serviceUnitRepository;

    @Autowired
    private ServiceUnitLocationRepository serviceUnitLocationRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketLineRepository ticketLineRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessTokenGenerator accessTokenGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void guestCreatesTicketWithDefaultLocationAndAccessCode() throws Exception {
        Fixture fixture = fixture("guest-create-ticket");

        String response = mockMvc.perform(post("/api/service-units/{serviceUnitId}/tickets", fixture.serviceUnit().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": " Alice Client ",
                                  "customerPhone": " +1 514 555 0000 ",
                                  "notes": " Besoin d'aide ",
                                  "lines": [
                                    {
                                      "itemId": "%s",
                                      "quantity": null,
                                      "notes": " Premier choix "
                                    }
                                  ]
                                }
                                """.formatted(fixture.item().getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ticketNumber").value(org.hamcrest.Matchers.matchesPattern("T-\\d{6,}")))
                .andExpect(jsonPath("$.accessCode").value(org.hamcrest.Matchers.matchesPattern("[A-Z2-9]{8}")))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.guestName").value("Alice Client"))
                .andExpect(jsonPath("$.customerPhone").value("+1 514 555 0000"))
                .andExpect(jsonPath("$.serviceUnitId").value(fixture.serviceUnit().getId().toString()))
                .andExpect(jsonPath("$.locationId").value(fixture.defaultLocation().getId().toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.totalAmount").value(4.5))
                .andExpect(jsonPath("$.lines[0].itemId").value(fixture.item().getId().toString()))
                .andExpect(jsonPath("$.lines[0].quantity").value(1))
                .andExpect(jsonPath("$.lines[0].unitPriceAmount").value(4.5))
                .andExpect(jsonPath("$.lines[0].lineTotalAmount").value(4.5))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(response);
        Ticket ticket = ticketRepository.findByTicketNumber(responseJson.get("ticketNumber").asText()).orElseThrow();

        assertThat(ticket.getUser()).isNull();
        assertThat(ticket.getGuestAccessCodeHash()).isNotEqualTo(responseJson.get("accessCode").asText());
        assertThat(passwordEncoder.matches(responseJson.get("accessCode").asText(), ticket.getGuestAccessCodeHash()))
                .isTrue();
        assertThat(ticketLineRepository.findByTicketId(ticket.getId())).hasSize(1);
    }

    @Test
    void authenticatedUserCreatesTicketWithoutGuestAccessCode() throws Exception {
        Fixture fixture = fixture("auth-create-ticket");
        User customer = user("ticket-customer");
        String token = accessTokenGenerator.generate(customer).value();

        String response = mockMvc.perform(post("/api/service-units/{serviceUnitId}/tickets", fixture.serviceUnit().getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locationId": "%s",
                                  "customerPhone": "5145551111",
                                  "lines": []
                                }
                                """.formatted(fixture.defaultLocation().getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessCode").doesNotExist())
                .andExpect(jsonPath("$.userId").value(customer.getId().toString()))
                .andExpect(jsonPath("$.guestName").doesNotExist())
                .andExpect(jsonPath("$.lines").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Ticket ticket = ticketRepository.findByTicketNumber(objectMapper.readTree(response).get("ticketNumber").asText())
                .orElseThrow();

        assertThat(ticket.getUser().getId()).isEqualTo(customer.getId());
        assertThat(ticket.getGuestAccessCodeHash()).isNull();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CREATED);
        assertThat(ticketLineRepository.findByTicketId(ticket.getId())).isEmpty();
    }

    @Test
    void rejectsGuestTicketWithoutGuestName() throws Exception {
        Fixture fixture = fixture("guest-name-required");

        mockMvc.perform(post("/api/service-units/{serviceUnitId}/tickets", fixture.serviceUnit().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lines": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Guest name is required"));
    }

    @Test
    void rejectsTicketCreationWhenServiceUnitIsClosed() throws Exception {
        Fixture fixture = fixture("closed-ticket");
        fixture.serviceUnit().close(fixture.owner());
        serviceUnitRepository.saveAndFlush(fixture.serviceUnit());

        mockMvc.perform(post("/api/service-units/{serviceUnitId}/tickets", fixture.serviceUnit().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": "Closed Client"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Service unit not found"));
    }

    @Test
    void rejectsLocationFromAnotherServiceUnit() throws Exception {
        Fixture fixture = fixture("invalid-location");
        Fixture otherFixture = fixture("invalid-location-other");

        mockMvc.perform(post("/api/service-units/{serviceUnitId}/tickets", fixture.serviceUnit().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": "Bad Location",
                                  "locationId": "%s"
                                }
                                """.formatted(otherFixture.defaultLocation().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Location is invalid"));
    }

    @Test
    void rejectsInvalidLineQuantityBeforePersistence() throws Exception {
        Fixture fixture = fixture("invalid-quantity");

        mockMvc.perform(post("/api/service-units/{serviceUnitId}/tickets", fixture.serviceUnit().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": "Bad Quantity",
                                  "lines": [
                                    {
                                      "itemId": "%s",
                                      "quantity": 0
                                    }
                                  ]
                                }
                                """.formatted(fixture.item().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'lines[0].quantity')]").exists());
    }

    @Test
    void rejectsUnavailableLineItem() throws Exception {
        Fixture fixture = fixture("unavailable-item");
        fixture.item().configure(new BigDecimal("4.50"), ItemAvailability.UNAVAILABLE, null, 0);
        itemRepository.saveAndFlush(fixture.item());

        mockMvc.perform(post("/api/service-units/{serviceUnitId}/tickets", fixture.serviceUnit().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": "Unavailable Item",
                                  "lines": [
                                    {
                                      "itemId": "%s"
                                    }
                                  ]
                                }
                                """.formatted(fixture.item().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Ticket line item is invalid"));
    }

    private Fixture fixture(String emailPrefix) {
        User owner = user(emailPrefix);
        Company company = new Company(
                "Ticket Company %s".formatted(UUID.randomUUID()),
                "Company for ticket tests",
                "USD",
                owner);
        company.activate();
        company = companyRepository.save(company);

        CatalogCategory category = catalogCategoryRepository.save(new CatalogCategory(
                company,
                "Ticket Category %s".formatted(UUID.randomUUID()),
                null,
                0,
                owner));
        Catalog catalog = catalogRepository.save(new Catalog(
                company,
                category,
                "Counter Service",
                null,
                null,
                new BigDecimal("4.50"),
                owner));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Ticket Queue",
                null,
                null,
                null,
                owner));
        serviceUnit.open(owner);
        serviceUnit = serviceUnitRepository.saveAndFlush(serviceUnit);
        ServiceUnitLocation defaultLocation = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                owner));
        Item item = itemRepository.saveAndFlush(new Item(
                serviceUnit,
                catalog,
                new BigDecimal("4.50"),
                ItemAvailability.AVAILABLE,
                null,
                0));

        return new Fixture(owner, company, serviceUnit, defaultLocation, item);
    }

    private User user(String emailPrefix) {
        return userRepository.save(new User(
                "%s.%s@flowmova.test".formatted(emailPrefix, UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Ticket",
                "User"));
    }

    private record Fixture(
            User owner,
            Company company,
            ServiceUnit serviceUnit,
            ServiceUnitLocation defaultLocation,
            Item item) {
    }
}
