package com.flowmova.backend.ticket.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.flowmova.backend.company.domain.CompanyOperationalStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.item.domain.Item;
import com.flowmova.backend.item.domain.ItemAvailability;
import com.flowmova.backend.item.infrastructure.ItemRepository;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.TicketCreationGuardMode;
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
    private CompanyUserRepository companyUserRepository;

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
    void rejectsTicketCreationWhenCompanyIsOperationallyClosed() throws Exception {
        Fixture fixture = fixture("closed-company-ticket");
        fixture.company().update(
                fixture.company().getName(),
                fixture.company().getDescription(),
                fixture.company().getImageUrl(),
                fixture.company().getAddressLine1(),
                fixture.company().getAddressLine2(),
                fixture.company().getCity(),
                fixture.company().getRegion(),
                fixture.company().getPostalCode(),
                fixture.company().getCountry(),
                fixture.company().getLatitude(),
                fixture.company().getLongitude(),
                fixture.company().getCurrency(),
                fixture.company().getBusinessType(),
                CompanyOperationalStatus.CLOSED,
                fixture.owner());
        companyRepository.saveAndFlush(fixture.company());

        mockMvc.perform(post("/api/service-units/{serviceUnitId}/tickets", fixture.serviceUnit().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": "Alice Client"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Company is closed"));
    }

    @Test
    void rejectsGuestTicketWhenServiceUnitRequiresAuthenticatedUserWithoutActiveTicket() throws Exception {
        Fixture fixture = fixture("guest-active-ticket-limit");
        fixture.serviceUnit().setTicketCreationGuardMode(TicketCreationGuardMode.AUTHENTICATED_ONLY_ONE_OPEN_TICKET);
        serviceUnitRepository.saveAndFlush(fixture.serviceUnit());

        mockMvc.perform(post("/api/service-units/{serviceUnitId}/tickets", fixture.serviceUnit().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": "Alice Client"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void rejectsAuthenticatedTicketWhenUserAlreadyHasActiveTicketInSameServiceUnit() throws Exception {
        Fixture fixture = fixture("auth-active-ticket-limit");
        fixture.serviceUnit().setTicketCreationGuardMode(TicketCreationGuardMode.AUTHENTICATED_ONLY_ONE_OPEN_TICKET);
        serviceUnitRepository.saveAndFlush(fixture.serviceUnit());
        User customer = user("active-ticket-customer");
        Ticket activeTicket = saveAuthenticatedTicket("T-ACTIVE-%s".formatted(shortToken()), customer, fixture);
        activeTicket.markReceived();
        ticketRepository.saveAndFlush(activeTicket);
        String token = accessTokenGenerator.generate(customer).value();

        mockMvc.perform(post("/api/service-units/{serviceUnitId}/tickets", fixture.serviceUnit().getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lines": []
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Active ticket already exists"));
    }

    @Test
    void guestCreatesTicketWhenGuardModeUsesGuestRecentTickets() throws Exception {
        Fixture fixture = fixture("guest-recent-ticket-mode");
        fixture.serviceUnit().setTicketCreationGuardMode(
                TicketCreationGuardMode.AUTHENTICATED_OR_GUEST_RECENT_ONE_OPEN_TICKET);
        serviceUnitRepository.saveAndFlush(fixture.serviceUnit());

        mockMvc.perform(post("/api/service-units/{serviceUnitId}/tickets", fixture.serviceUnit().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": "Alice Client",
                                  "lines": []
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.guestName").value("Alice Client"))
                .andExpect(jsonPath("$.accessCode").exists());
    }

    @Test
    void rejectsAuthenticatedTicketWithActiveTicketWhenGuardModeUsesGuestRecentTickets() throws Exception {
        Fixture fixture = fixture("auth-recent-ticket-mode");
        fixture.serviceUnit().setTicketCreationGuardMode(
                TicketCreationGuardMode.AUTHENTICATED_OR_GUEST_RECENT_ONE_OPEN_TICKET);
        serviceUnitRepository.saveAndFlush(fixture.serviceUnit());
        User customer = user("recent-ticket-customer");
        Ticket activeTicket = saveAuthenticatedTicket("T-RECENT-%s".formatted(shortToken()), customer, fixture);
        activeTicket.markReceived();
        ticketRepository.saveAndFlush(activeTicket);
        String token = accessTokenGenerator.generate(customer).value();

        mockMvc.perform(post("/api/service-units/{serviceUnitId}/tickets", fixture.serviceUnit().getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lines": []
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Active ticket already exists"));
    }

    @Test
    void authenticatedUserCreatesTicketWhenPreviousTicketIsTreatedOrInAnotherServiceUnit() throws Exception {
        Fixture fixture = fixture("auth-treated-ticket-limit");
        fixture.serviceUnit().setTicketCreationGuardMode(TicketCreationGuardMode.AUTHENTICATED_ONLY_ONE_OPEN_TICKET);
        serviceUnitRepository.saveAndFlush(fixture.serviceUnit());
        Fixture otherFixture = otherFixture("auth-treated-ticket-limit-other", fixture.owner());
        otherFixture.serviceUnit().setTicketCreationGuardMode(TicketCreationGuardMode.AUTHENTICATED_ONLY_ONE_OPEN_TICKET);
        serviceUnitRepository.saveAndFlush(otherFixture.serviceUnit());
        User customer = user("treated-ticket-customer");
        Ticket treatedTicket = saveAuthenticatedTicket("T-TREATED-%s".formatted(shortToken()), customer, fixture);
        treatedTicket.markTreated();
        ticketRepository.saveAndFlush(treatedTicket);
        saveAuthenticatedTicket("T-OTHER-%s".formatted(shortToken()), customer, otherFixture);
        String token = accessTokenGenerator.generate(customer).value();

        mockMvc.perform(post("/api/service-units/{serviceUnitId}/tickets", fixture.serviceUnit().getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lines": []
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(customer.getId().toString()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void guestGetsTicketWithTicketNumberAndAccessCode() throws Exception {
        Fixture fixture = fixture("guest-get-ticket");
        JsonNode createdTicket = createGuestTicket(fixture);

        mockMvc.perform(post("/api/tickets/guest-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketNumber": "%s",
                                  "accessCode": "%s"
                                }
                                """.formatted(
                                        createdTicket.get("ticketNumber").asText(),
                                        createdTicket.get("accessCode").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.accessCode").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.ticketNumber").value(createdTicket.get("ticketNumber").asText()))
                .andExpect(jsonPath("$.guestName").value("Alice Client"))
                .andExpect(jsonPath("$.customerPhone").value("+1 514 555 0000"))
                .andExpect(jsonPath("$.serviceUnitId").value(fixture.serviceUnit().getId().toString()))
                .andExpect(jsonPath("$.locationId").value(fixture.defaultLocation().getId().toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.totalAmount").value(4.5))
                .andExpect(jsonPath("$.lines[0].itemId").value(fixture.item().getId().toString()));
    }

    @Test
    void rejectsGuestTicketAccessWithInvalidCode() throws Exception {
        Fixture fixture = fixture("guest-invalid-code");
        JsonNode createdTicket = createGuestTicket(fixture);

        mockMvc.perform(post("/api/tickets/guest-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketNumber": "%s",
                                  "accessCode": "BADCODE1"
                                }
                                """.formatted(createdTicket.get("ticketNumber").asText())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Ticket access is invalid"));
    }

    @Test
    void rejectsGuestTicketAccessWithTicketNumberOnly() throws Exception {
        Fixture fixture = fixture("guest-number-only");
        JsonNode createdTicket = createGuestTicket(fixture);

        mockMvc.perform(post("/api/tickets/guest-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketNumber": "%s"
                                }
                                """.formatted(createdTicket.get("ticketNumber").asText())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'accessCode')]").exists());
    }

    @Test
    void guestCancelsCreatedTicketWithTicketNumberAndAccessCode() throws Exception {
        Fixture fixture = fixture("guest-cancel-created");
        JsonNode createdTicket = createGuestTicket(fixture);

        mockMvc.perform(patch("/api/tickets/guest-access/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketNumber": "%s",
                                  "accessCode": "%s"
                                }
                                """.formatted(
                                        createdTicket.get("ticketNumber").asText(),
                                        createdTicket.get("accessCode").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.accessCode").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.ticketNumber").value(createdTicket.get("ticketNumber").asText()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Ticket ticket = ticketRepository.findByTicketNumber(createdTicket.get("ticketNumber").asText()).orElseThrow();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
    }

    @Test
    void guestCancelsReceivedTicketWithTicketNumberAndAccessCode() throws Exception {
        Fixture fixture = fixture("guest-cancel-received");
        Ticket ticket = saveGuestTicket("T-GUEST-CANCEL-%s".formatted(shortToken()), fixture);
        ticket.markReceived();
        ticket = ticketRepository.saveAndFlush(ticket);

        mockMvc.perform(patch("/api/tickets/guest-access/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketNumber": "%s",
                                  "accessCode": "ACCESS01"
                                }
                                """.formatted(ticket.getTicketNumber())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketNumber").value(ticket.getTicketNumber()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void rejectsGuestTicketCancellationWithInvalidCode() throws Exception {
        Fixture fixture = fixture("guest-cancel-invalid-code");
        JsonNode createdTicket = createGuestTicket(fixture);

        mockMvc.perform(patch("/api/tickets/guest-access/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketNumber": "%s",
                                  "accessCode": "BADCODE1"
                                }
                                """.formatted(createdTicket.get("ticketNumber").asText())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Ticket access is invalid"));
    }

    @Test
    void rejectsGuestTicketCancellationWhenAlreadyTreated() throws Exception {
        Fixture fixture = fixture("guest-cancel-treated");
        Ticket ticket = saveGuestTicket("T-GUEST-TREATED-%s".formatted(shortToken()), fixture);
        ticket.markTreated();
        ticket = ticketRepository.saveAndFlush(ticket);

        mockMvc.perform(patch("/api/tickets/guest-access/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketNumber": "%s",
                                  "accessCode": "ACCESS01"
                                }
                                """.formatted(ticket.getTicketNumber())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Ticket transition is invalid"));
    }

    @Test
    void guestConfirmsTreatedTicketWithTicketNumberAndAccessCode() throws Exception {
        Fixture fixture = fixture("guest-confirm-treated");
        Ticket ticket = saveGuestTicket("T-GUEST-CONFIRM-%s".formatted(shortToken()), fixture);
        ticket.markTreated();
        ticket = ticketRepository.saveAndFlush(ticket);

        mockMvc.perform(patch("/api/tickets/guest-access/confirm-treatment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketNumber": "%s",
                                  "accessCode": "ACCESS01"
                                }
                                """.formatted(ticket.getTicketNumber())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.accessCode").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.ticketNumber").value(ticket.getTicketNumber()))
                .andExpect(jsonPath("$.status").value("CUSTOMER_CONFIRMED"));

        assertThat(ticketRepository.findById(ticket.getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.CUSTOMER_CONFIRMED);
    }

    @Test
    void rejectsGuestTreatmentConfirmationWithInvalidCode() throws Exception {
        Fixture fixture = fixture("guest-confirm-invalid-code");
        Ticket ticket = saveGuestTicket("T-GUEST-CONFIRM-BAD-%s".formatted(shortToken()), fixture);
        ticket.markTreated();
        ticket = ticketRepository.saveAndFlush(ticket);

        mockMvc.perform(patch("/api/tickets/guest-access/confirm-treatment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketNumber": "%s",
                                  "accessCode": "BADCODE1"
                                }
                                """.formatted(ticket.getTicketNumber())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Ticket access is invalid"));
    }

    @Test
    void rejectsGuestTreatmentConfirmationWhenTicketIsNotTreated() throws Exception {
        Fixture fixture = fixture("guest-confirm-created");
        Ticket ticket = saveGuestTicket("T-GUEST-CONFIRM-CREATED-%s".formatted(shortToken()), fixture);

        mockMvc.perform(patch("/api/tickets/guest-access/confirm-treatment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketNumber": "%s",
                                  "accessCode": "ACCESS01"
                                }
                                """.formatted(ticket.getTicketNumber())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Ticket transition is invalid"));
    }

    @Test
    void rejectsGuestTreatmentConfirmationWithTicketNumberOnly() throws Exception {
        Fixture fixture = fixture("guest-confirm-number-only");
        Ticket ticket = saveGuestTicket("T-GUEST-CONFIRM-NO-CODE-%s".formatted(shortToken()), fixture);
        ticket.markTreated();
        ticket = ticketRepository.saveAndFlush(ticket);

        mockMvc.perform(patch("/api/tickets/guest-access/confirm-treatment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketNumber": "%s"
                                }
                                """.formatted(ticket.getTicketNumber())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'accessCode')]").exists());
    }

    @Test
    void authenticatedUserListsOwnTicketsWithPaginationStatusAndTicketNumberSearch() throws Exception {
        Fixture fixture = fixture("my-tickets");
        User customer = user("my-tickets-customer");
        User otherCustomer = user("my-tickets-other-customer");
        String token = accessTokenGenerator.generate(customer).value();
        String uniqueToken = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String treatedTicketNumber = "T-ME-%s".formatted(uniqueToken);

        saveAuthenticatedTicket("T-ME-HIDDEN-%s".formatted(uniqueToken), customer, fixture);
        Ticket treatedTicket = saveAuthenticatedTicket(treatedTicketNumber, customer, fixture);
        treatedTicket.markTreated();
        ticketRepository.saveAndFlush(treatedTicket);
        saveAuthenticatedTicket("T-OTHER-%s".formatted(uniqueToken), otherCustomer, fixture);
        saveGuestTicket("T-GUEST-%s".formatted(uniqueToken), fixture);

        mockMvc.perform(get("/api/users/me/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("status", "TREATED")
                        .param("ticketNumber", uniqueToken.toLowerCase())
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "ticketNumber,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(treatedTicket.getId().toString()))
                .andExpect(jsonPath("$.items[0].ticketNumber").value(treatedTicketNumber))
                .andExpect(jsonPath("$.items[0].accessCode").doesNotExist())
                .andExpect(jsonPath("$.items[0].guestName").doesNotExist())
                .andExpect(jsonPath("$.items[0].userId").value(customer.getId().toString()))
                .andExpect(jsonPath("$.items[0].status").value("TREATED"))
                .andExpect(jsonPath("$.items[0].serviceUnitId").value(fixture.serviceUnit().getId().toString()))
                .andExpect(jsonPath("$.items[?(@.ticketNumber == 'T-OTHER-%s')]".formatted(uniqueToken)).doesNotExist())
                .andExpect(jsonPath("$.items[?(@.ticketNumber == 'T-GUEST-%s')]".formatted(uniqueToken)).doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void authenticatedUserListsOwnTicketsWithDefaultPagination() throws Exception {
        Fixture fixture = fixture("my-tickets-default");
        User customer = user("my-tickets-default-customer");
        String token = accessTokenGenerator.generate(customer).value();
        String uniqueToken = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Ticket firstTicket = saveAuthenticatedTicket("T-ME-FIRST-%s".formatted(uniqueToken), customer, fixture);
        Ticket secondTicket = saveAuthenticatedTicket("T-ME-SECOND-%s".formatted(uniqueToken), customer, fixture);

        mockMvc.perform(get("/api/users/me/tickets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("ticketNumber", "T-ME-")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[?(@.id == '%s')]".formatted(firstTicket.getId())).exists())
                .andExpect(jsonPath("$.items[?(@.id == '%s')]".formatted(secondTicket.getId())).exists())
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void rejectsCurrentUserTicketsWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/users/me/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void authenticatedUserCancelsOwnCreatedTicket() throws Exception {
        Fixture fixture = fixture("cancel-own-created");
        User customer = user("cancel-own-created-customer");
        String token = accessTokenGenerator.generate(customer).value();
        Ticket ticket = saveAuthenticatedTicket("T-CANCEL-CREATED-%s".formatted(shortToken()), customer, fixture);

        mockMvc.perform(patch("/api/users/me/tickets/{ticketId}/cancel", ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId().toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(ticketRepository.findById(ticket.getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.CANCELLED);
    }

    @Test
    void authenticatedUserCancelsOwnReceivedTicket() throws Exception {
        Fixture fixture = fixture("cancel-own-received");
        User customer = user("cancel-own-received-customer");
        String token = accessTokenGenerator.generate(customer).value();
        Ticket ticket = saveAuthenticatedTicket("T-CANCEL-RECEIVED-%s".formatted(shortToken()), customer, fixture);
        ticket.markReceived();
        ticket = ticketRepository.saveAndFlush(ticket);

        mockMvc.perform(patch("/api/users/me/tickets/{ticketId}/cancel", ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void rejectsOwnTicketCancellationWhenAlreadyTreated() throws Exception {
        Fixture fixture = fixture("cancel-own-treated");
        User customer = user("cancel-own-treated-customer");
        String token = accessTokenGenerator.generate(customer).value();
        Ticket ticket = saveAuthenticatedTicket("T-CANCEL-TREATED-%s".formatted(shortToken()), customer, fixture);
        ticket.markTreated();
        ticket = ticketRepository.saveAndFlush(ticket);

        mockMvc.perform(patch("/api/users/me/tickets/{ticketId}/cancel", ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Ticket transition is invalid"));
    }

    @Test
    void rejectsCancellationOfAnotherUsersTicket() throws Exception {
        Fixture fixture = fixture("cancel-other-user");
        User owner = user("cancel-other-owner");
        User otherUser = user("cancel-other-user");
        String token = accessTokenGenerator.generate(otherUser).value();
        Ticket ticket = saveAuthenticatedTicket("T-CANCEL-OTHER-%s".formatted(shortToken()), owner, fixture);

        mockMvc.perform(patch("/api/users/me/tickets/{ticketId}/cancel", ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Ticket not found"));
    }

    @Test
    void rejectsCurrentUserTicketCancellationWithoutJwt() throws Exception {
        mockMvc.perform(patch("/api/users/me/tickets/{ticketId}/cancel", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void authenticatedUserConfirmsOwnTreatedTicket() throws Exception {
        Fixture fixture = fixture("confirm-own-treated");
        User customer = user("confirm-own-treated-customer");
        String token = accessTokenGenerator.generate(customer).value();
        Ticket ticket = saveAuthenticatedTicket("T-CONFIRM-TREATED-%s".formatted(shortToken()), customer, fixture);
        ticket.markTreated();
        ticket = ticketRepository.saveAndFlush(ticket);

        mockMvc.perform(patch("/api/users/me/tickets/{ticketId}/confirm-treatment", ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId().toString()))
                .andExpect(jsonPath("$.accessCode").doesNotExist())
                .andExpect(jsonPath("$.userId").value(customer.getId().toString()))
                .andExpect(jsonPath("$.status").value("CUSTOMER_CONFIRMED"));

        assertThat(ticketRepository.findById(ticket.getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.CUSTOMER_CONFIRMED);
    }

    @Test
    void rejectsOwnTreatmentConfirmationWhenTicketIsNotTreated() throws Exception {
        Fixture fixture = fixture("confirm-own-created");
        User customer = user("confirm-own-created-customer");
        String token = accessTokenGenerator.generate(customer).value();
        Ticket ticket = saveAuthenticatedTicket("T-CONFIRM-CREATED-%s".formatted(shortToken()), customer, fixture);

        mockMvc.perform(patch("/api/users/me/tickets/{ticketId}/confirm-treatment", ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Ticket transition is invalid"));
    }

    @Test
    void rejectsTreatmentConfirmationOfAnotherUsersTicket() throws Exception {
        Fixture fixture = fixture("confirm-other-user");
        User owner = user("confirm-other-owner");
        User otherUser = user("confirm-other-user");
        String token = accessTokenGenerator.generate(otherUser).value();
        Ticket ticket = saveAuthenticatedTicket("T-CONFIRM-OTHER-%s".formatted(shortToken()), owner, fixture);
        ticket.markTreated();
        ticket = ticketRepository.saveAndFlush(ticket);

        mockMvc.perform(patch("/api/users/me/tickets/{ticketId}/confirm-treatment", ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Ticket not found"));
    }

    @Test
    void rejectsCurrentUserTreatmentConfirmationWithoutJwt() throws Exception {
        mockMvc.perform(patch("/api/users/me/tickets/{ticketId}/confirm-treatment", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void employeeListsServiceUnitTicketsWithPaginationStatusAndTicketNumberSearch() throws Exception {
        Fixture fixture = fixture("unit-tickets");
        User employee = user("unit-tickets-employee");
        companyUserRepository.save(new CompanyUser(fixture.company().getId(), employee, CompanyRole.EMPLOYEE));
        String token = accessTokenGenerator.generate(employee).value();
        String uniqueToken = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String createdTicketNumber = "T-UNIT-%s".formatted(uniqueToken);

        Ticket createdTicket = saveGuestTicket(createdTicketNumber, fixture);
        saveAuthenticatedTicket("T-UNIT-AUTH-%s".formatted(uniqueToken), employee, fixture);
        Ticket treatedTicket = saveGuestTicket("T-UNIT-TREATED-%s".formatted(uniqueToken), fixture);
        treatedTicket.markTreated();
        ticketRepository.saveAndFlush(treatedTicket);
        saveGuestTicket("T-OTHER-UNIT-%s".formatted(uniqueToken), otherFixture("unit-tickets-other", fixture.owner()));

        mockMvc.perform(get(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets",
                        fixture.company().getId(),
                        fixture.serviceUnit().getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("status", "CREATED")
                        .param("ticketNumber", uniqueToken.toLowerCase())
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "ticketNumber,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[?(@.id == '%s')]".formatted(createdTicket.getId())).exists())
                .andExpect(jsonPath("$.items[?(@.ticketNumber == 'T-UNIT-AUTH-%s')]".formatted(uniqueToken)).exists())
                .andExpect(jsonPath("$.items[?(@.ticketNumber == 'T-UNIT-TREATED-%s')]".formatted(uniqueToken)).doesNotExist())
                .andExpect(jsonPath("$.items[?(@.ticketNumber == 'T-OTHER-UNIT-%s')]".formatted(uniqueToken)).doesNotExist())
                .andExpect(jsonPath("$.items[0].accessCode").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void adminListsServiceUnitTicketsWithoutFilters() throws Exception {
        Fixture fixture = fixture("unit-tickets-admin");
        User admin = user("unit-tickets-admin-user");
        companyUserRepository.save(new CompanyUser(fixture.company().getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();
        String uniqueToken = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Ticket guestTicket = saveGuestTicket("T-UNIT-GUEST-%s".formatted(uniqueToken), fixture);
        Ticket authTicket = saveAuthenticatedTicket("T-UNIT-AUTH-%s".formatted(uniqueToken), admin, fixture);

        mockMvc.perform(get(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets",
                        fixture.company().getId(),
                        fixture.serviceUnit().getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '%s')]".formatted(guestTicket.getId())).exists())
                .andExpect(jsonPath("$.items[?(@.id == '%s')]".formatted(authTicket.getId())).exists())
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void employeeListsServiceUnitTicketsFilteredByLocation() throws Exception {
        Fixture fixture = fixture("unit-tickets-location");
        User employee = user("unit-tickets-location-employee");
        companyUserRepository.save(new CompanyUser(fixture.company().getId(), employee, CompanyRole.EMPLOYEE));
        String token = accessTokenGenerator.generate(employee).value();
        ServiceUnitLocation customLocation = customLocation(fixture, "Table 12");
        String uniqueToken = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Ticket customLocationTicket = saveGuestTicket(
                "T-LOCATION-CREATED-%s".formatted(uniqueToken),
                fixture,
                customLocation);
        saveGuestTicket("T-LOCATION-DEFAULT-%s".formatted(uniqueToken), fixture);
        Ticket treatedCustomLocationTicket = saveGuestTicket(
                "T-LOCATION-TREATED-%s".formatted(uniqueToken),
                fixture,
                customLocation);
        treatedCustomLocationTicket.markTreated();
        ticketRepository.saveAndFlush(treatedCustomLocationTicket);

        mockMvc.perform(get(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets",
                        fixture.company().getId(),
                        fixture.serviceUnit().getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("locationId", customLocation.getId().toString())
                        .param("status", "CREATED")
                        .param("ticketNumber", uniqueToken.toLowerCase())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(customLocationTicket.getId().toString()))
                .andExpect(jsonPath("$.items[0].locationId").value(customLocation.getId().toString()))
                .andExpect(jsonPath("$.items[?(@.ticketNumber == 'T-LOCATION-DEFAULT-%s')]"
                        .formatted(uniqueToken)).doesNotExist())
                .andExpect(jsonPath("$.items[?(@.ticketNumber == 'T-LOCATION-TREATED-%s')]"
                        .formatted(uniqueToken)).doesNotExist())
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void rejectsServiceUnitTicketListForLocationFromAnotherServiceUnit() throws Exception {
        Fixture fixture = fixture("unit-tickets-invalid-location");
        Fixture otherFixture = fixture("unit-tickets-invalid-location-other");
        User admin = user("unit-tickets-invalid-location-admin");
        companyUserRepository.save(new CompanyUser(fixture.company().getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(get(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets",
                        fixture.company().getId(),
                        fixture.serviceUnit().getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("locationId", otherFixture.defaultLocation().getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Location is invalid"));
    }

    @Test
    void rejectsServiceUnitTicketListForNonMember() throws Exception {
        Fixture fixture = fixture("unit-tickets-non-member");
        User outsider = user("unit-tickets-outsider");
        String token = accessTokenGenerator.generate(outsider).value();

        mockMvc.perform(get(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets",
                        fixture.company().getId(),
                        fixture.serviceUnit().getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company member role is required"));
    }

    @Test
    void rejectsServiceUnitTicketListForAnotherCompanyServiceUnit() throws Exception {
        Fixture fixture = fixture("unit-tickets-company");
        Fixture otherFixture = fixture("unit-tickets-other-company");
        User admin = user("unit-tickets-company-admin");
        companyUserRepository.save(new CompanyUser(fixture.company().getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();

        mockMvc.perform(get(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets",
                        fixture.company().getId(),
                        otherFixture.serviceUnit().getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Service unit not found"));
    }

    @Test
    void rejectsServiceUnitTicketListWithoutJwt() throws Exception {
        mockMvc.perform(get(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets",
                        UUID.randomUUID(),
                        UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void employeeChangesTicketStatusFromCreatedToReceived() throws Exception {
        Fixture fixture = fixture("ticket-status-received");
        User employee = user("ticket-status-received-employee");
        companyUserRepository.save(new CompanyUser(fixture.company().getId(), employee, CompanyRole.EMPLOYEE));
        String token = accessTokenGenerator.generate(employee).value();
        Ticket ticket = saveGuestTicket("T-STATUS-RECEIVED-%s".formatted(shortToken()), fixture);

        mockMvc.perform(patch(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets/{ticketId}/status",
                        fixture.company().getId(),
                        fixture.serviceUnit().getId(),
                        ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RECEIVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId().toString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.closedAt").doesNotExist());

        assertThat(ticketRepository.findById(ticket.getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.RECEIVED);
    }

    @Test
    void adminCanSkipReceivedAndMarkTicketTreated() throws Exception {
        Fixture fixture = fixture("ticket-status-treated");
        User admin = user("ticket-status-treated-admin");
        companyUserRepository.save(new CompanyUser(fixture.company().getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();
        Ticket ticket = saveAuthenticatedTicket("T-STATUS-TREATED-%s".formatted(shortToken()), admin, fixture);

        mockMvc.perform(patch(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets/{ticketId}/status",
                        fixture.company().getId(),
                        fixture.serviceUnit().getId(),
                        ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "TREATED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TREATED"));

        assertThat(ticketRepository.findById(ticket.getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.TREATED);
    }

    @Test
    void employeeClosesTreatedTicket() throws Exception {
        Fixture fixture = fixture("ticket-status-close-treated");
        User employee = user("ticket-status-close-treated-employee");
        companyUserRepository.save(new CompanyUser(fixture.company().getId(), employee, CompanyRole.EMPLOYEE));
        String token = accessTokenGenerator.generate(employee).value();
        Ticket ticket = saveGuestTicket("T-STATUS-CLOSE-TREATED-%s".formatted(shortToken()), fixture);
        ticket.markTreated();
        ticket = ticketRepository.saveAndFlush(ticket);

        mockMvc.perform(patch(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets/{ticketId}/status",
                        fixture.company().getId(),
                        fixture.serviceUnit().getId(),
                        ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CLOSED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").exists());

        Ticket closedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
        assertThat(closedTicket.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(closedTicket.getClosedAt()).isNotNull();
    }

    @Test
    void adminClosesCustomerConfirmedTicket() throws Exception {
        Fixture fixture = fixture("ticket-status-closed");
        User admin = user("ticket-status-closed-admin");
        companyUserRepository.save(new CompanyUser(fixture.company().getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();
        Ticket ticket = saveGuestTicket("T-STATUS-CLOSED-%s".formatted(shortToken()), fixture);
        ticket.markTreated();
        ticket.confirmCustomerTreatment();
        ticket = ticketRepository.saveAndFlush(ticket);

        mockMvc.perform(patch(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets/{ticketId}/status",
                        fixture.company().getId(),
                        fixture.serviceUnit().getId(),
                        ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CLOSED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").exists());
    }

    @Test
    void rejectsChangingClosedTicket() throws Exception {
        Fixture fixture = fixture("ticket-status-closed-final");
        User admin = user("ticket-status-closed-final-admin");
        companyUserRepository.save(new CompanyUser(fixture.company().getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();
        Ticket ticket = saveGuestTicket("T-STATUS-CLOSED-FINAL-%s".formatted(shortToken()), fixture);
        ticket.markTreated();
        ticket.close();
        ticket = ticketRepository.saveAndFlush(ticket);

        mockMvc.perform(patch(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets/{ticketId}/status",
                        fixture.company().getId(),
                        fixture.serviceUnit().getId(),
                        ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "TREATED"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Ticket transition is invalid"));
    }

    @Test
    void rejectsInvalidTicketStatusTransition() throws Exception {
        Fixture fixture = fixture("ticket-status-invalid");
        User admin = user("ticket-status-invalid-admin");
        companyUserRepository.save(new CompanyUser(fixture.company().getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();
        Ticket ticket = saveGuestTicket("T-STATUS-INVALID-%s".formatted(shortToken()), fixture);

        mockMvc.perform(patch(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets/{ticketId}/status",
                        fixture.company().getId(),
                        fixture.serviceUnit().getId(),
                        ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CLOSED"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Ticket transition is invalid"));
    }

    @Test
    void rejectsCustomerConfirmedStatusFromCompanyTeam() throws Exception {
        Fixture fixture = fixture("ticket-status-customer-confirmed");
        User admin = user("ticket-status-customer-confirmed-admin");
        companyUserRepository.save(new CompanyUser(fixture.company().getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();
        Ticket ticket = saveGuestTicket("T-STATUS-CUSTOMER-%s".formatted(shortToken()), fixture);
        ticket.markTreated();
        ticket = ticketRepository.saveAndFlush(ticket);

        mockMvc.perform(patch(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets/{ticketId}/status",
                        fixture.company().getId(),
                        fixture.serviceUnit().getId(),
                        ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CUSTOMER_CONFIRMED"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Ticket transition is invalid"));
    }

    @Test
    void rejectsTicketStatusChangeForNonMember() throws Exception {
        Fixture fixture = fixture("ticket-status-non-member");
        User outsider = user("ticket-status-outsider");
        String token = accessTokenGenerator.generate(outsider).value();
        Ticket ticket = saveGuestTicket("T-STATUS-NONMEM-%s".formatted(shortToken()), fixture);

        mockMvc.perform(patch(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets/{ticketId}/status",
                        fixture.company().getId(),
                        fixture.serviceUnit().getId(),
                        ticket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RECEIVED"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Company member role is required"));
    }

    @Test
    void rejectsTicketStatusChangeForAnotherServiceUnit() throws Exception {
        Fixture fixture = fixture("ticket-status-unit");
        Fixture otherFixture = otherFixture("ticket-status-other-unit", fixture.owner());
        User admin = user("ticket-status-unit-admin");
        companyUserRepository.save(new CompanyUser(fixture.company().getId(), admin, CompanyRole.ADMIN));
        String token = accessTokenGenerator.generate(admin).value();
        Ticket otherTicket = saveGuestTicket("T-STATUS-OTHER-%s".formatted(shortToken()), otherFixture);

        mockMvc.perform(patch(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets/{ticketId}/status",
                        fixture.company().getId(),
                        fixture.serviceUnit().getId(),
                        otherTicket.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RECEIVED"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Ticket not found"));
    }

    @Test
    void rejectsTicketStatusChangeWithoutJwt() throws Exception {
        mockMvc.perform(patch(
                        "/api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets/{ticketId}/status",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RECEIVED"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
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

    private JsonNode createGuestTicket(Fixture fixture) throws Exception {
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
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """.formatted(fixture.item().getId())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response);
    }

    private String shortToken() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Ticket saveAuthenticatedTicket(String ticketNumber, User user, Fixture fixture) {
        return ticketRepository.saveAndFlush(new Ticket(
                ticketNumber,
                user,
                null,
                null,
                fixture.serviceUnit(),
                fixture.defaultLocation(),
                null,
                null,
                fixture.company().getCurrency()));
    }

    private Ticket saveGuestTicket(String ticketNumber, Fixture fixture) {
        return saveGuestTicket(ticketNumber, fixture, fixture.defaultLocation());
    }

    private Ticket saveGuestTicket(String ticketNumber, Fixture fixture, ServiceUnitLocation location) {
        return ticketRepository.saveAndFlush(new Ticket(
                ticketNumber,
                null,
                "Guest Client",
                passwordEncoder.encode("ACCESS01"),
                fixture.serviceUnit(),
                location,
                null,
                null,
                fixture.company().getCurrency()));
    }

    private ServiceUnitLocation customLocation(Fixture fixture, String name) {
        return serviceUnitLocationRepository.saveAndFlush(new ServiceUnitLocation(
                fixture.serviceUnit(),
                name,
                null,
                ServiceUnitLocationType.CUSTOM,
                false,
                "loc-%s".formatted(UUID.randomUUID()),
                fixture.owner()));
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

    private Fixture otherFixture(String emailPrefix, User owner) {
        Company company = new Company(
                "Other Ticket Company %s".formatted(UUID.randomUUID()),
                "Other company for ticket tests",
                "USD",
                owner);
        company.activate();
        company = companyRepository.save(company);

        CatalogCategory category = catalogCategoryRepository.save(new CatalogCategory(
                company,
                "Other Ticket Category %s".formatted(UUID.randomUUID()),
                null,
                0,
                owner));
        Catalog catalog = catalogRepository.save(new Catalog(
                company,
                category,
                "Other Counter Service",
                null,
                null,
                new BigDecimal("4.50"),
                owner));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Other Ticket Queue",
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
