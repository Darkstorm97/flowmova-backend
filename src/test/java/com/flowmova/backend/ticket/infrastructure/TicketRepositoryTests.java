package com.flowmova.backend.ticket.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.flowmova.backend.ticket.domain.TicketLine;
import com.flowmova.backend.ticket.domain.TicketStatus;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TicketRepositoryTests {

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

    @Test
    void savesGuestTicketWithLinesAndDefaultQuantity() {
        Fixture fixture = fixture("guest-ticket");
        Ticket ticket = new Ticket(
                "T-100001",
                null,
                "Guest User",
                "$2a$10$guest-access-code-hash",
                fixture.serviceUnit(),
                fixture.location(),
                "+15145550100",
                "Window seat",
                fixture.company().getCurrency());
        ticket.addLine(new TicketLine(
                fixture.item(),
                null,
                fixture.item().getPriceAmount(),
                "No onions"));

        Ticket savedTicket = ticketRepository.saveAndFlush(ticket);

        assertThat(savedTicket.getStatus()).isEqualTo(TicketStatus.CREATED);
        assertThat(savedTicket.getUser()).isNull();
        assertThat(savedTicket.getGuestName()).isEqualTo("Guest User");
        assertThat(savedTicket.getGuestAccessCodeHash()).isEqualTo("$2a$10$guest-access-code-hash");
        assertThat(savedTicket.getCustomerPhone()).isEqualTo("+15145550100");
        assertThat(savedTicket.getCurrency()).isEqualTo("CAD");
        assertThat(savedTicket.getTotalAmount()).isEqualByComparingTo("9.99");
        assertThat(savedTicket.getClosedAt()).isNull();
        assertThat(savedTicket.getCreatedAt()).isNotNull();
        assertThat(savedTicket.getUpdatedAt()).isNotNull();
        assertThat(savedTicket.getVersion()).isNotNull();

        TicketLine savedLine = ticketLineRepository.findByTicketId(savedTicket.getId()).getFirst();
        assertThat(savedLine.getItem().getId()).isEqualTo(fixture.item().getId());
        assertThat(savedLine.getQuantity()).isEqualTo(1);
        assertThat(savedLine.getUnitPriceAmount()).isEqualByComparingTo("9.99");
        assertThat(savedLine.getLineTotalAmount()).isEqualByComparingTo("9.99");
        assertThat(savedLine.getNotes()).isEqualTo("No onions");
    }

    @Test
    void savesAuthenticatedTicketAndFiltersTickets() {
        Fixture fixture = fixture("auth-ticket");
        Ticket authenticatedTicket = new Ticket(
                "T-100002",
                fixture.user(),
                null,
                null,
                fixture.serviceUnit(),
                fixture.location(),
                null,
                null,
                fixture.company().getCurrency());
        authenticatedTicket.addLine(new TicketLine(
                fixture.item(),
                3,
                fixture.item().getPriceAmount(),
                null));
        authenticatedTicket.markTreated();
        authenticatedTicket = ticketRepository.saveAndFlush(authenticatedTicket);

        Ticket guestTicket = new Ticket(
                "T-100003",
                null,
                "Other Guest",
                "$2a$10$other-guest-code-hash",
                fixture.serviceUnit(),
                fixture.location(),
                null,
                null,
                fixture.company().getCurrency());
        guestTicket = ticketRepository.saveAndFlush(guestTicket);

        assertThat(authenticatedTicket.getTotalAmount()).isEqualByComparingTo("29.97");
        assertThat(authenticatedTicket.getStatus()).isEqualTo(TicketStatus.TREATED);
        assertThat(authenticatedTicket.getGuestAccessCodeHash()).isNull();
        assertThat(ticketRepository.findByTicketNumber("T-100002")).contains(authenticatedTicket);
        assertThat(ticketRepository.findByServiceUnitIdOrderByCreatedAtDesc(fixture.serviceUnit().getId()))
                .contains(authenticatedTicket, guestTicket);
        assertThat(ticketRepository.findByServiceUnitIdAndStatusOrderByCreatedAtDesc(
                fixture.serviceUnit().getId(),
                TicketStatus.TREATED))
                .containsExactly(authenticatedTicket);
        assertThat(ticketRepository.findByUserIdOrderByCreatedAtDesc(fixture.user().getId()))
                .containsExactly(authenticatedTicket);
        assertThat(ticketRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                fixture.user().getId(),
                TicketStatus.TREATED))
                .containsExactly(authenticatedTicket);
    }

    @Test
    void preparesTicketStatusTransitions() {
        Fixture fixture = fixture("ticket-transition");
        Ticket ticket = new Ticket(
                "T-100004",
                fixture.user(),
                null,
                null,
                fixture.serviceUnit(),
                fixture.location(),
                null,
                null,
                fixture.company().getCurrency());

        ticket.markReceived();
        ticket.markTreated();
        ticket.confirmCustomerTreatment();
        ticket.close();
        ticket = ticketRepository.saveAndFlush(ticket);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(ticket.getClosedAt()).isNotNull();
    }

    @Test
    void rejectsInvalidTicketRelationsAndLines() {
        Fixture fixture = fixture("ticket-invalid");
        Fixture otherFixture = fixture("ticket-invalid-other");

        assertThatThrownBy(() -> new Ticket(
                "T-100005",
                null,
                null,
                "$2a$10$missing-guest-name",
                fixture.serviceUnit(),
                fixture.location(),
                null,
                null,
                fixture.company().getCurrency()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Guest ticket requires guest name and access code hash");

        assertThatThrownBy(() -> new Ticket(
                "T-100006",
                fixture.user(),
                null,
                "$2a$10$auth-code-not-allowed",
                fixture.serviceUnit(),
                fixture.location(),
                null,
                null,
                fixture.company().getCurrency()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authenticated ticket must not have guest access code hash");

        assertThatThrownBy(() -> new Ticket(
                "T-100007",
                fixture.user(),
                null,
                null,
                fixture.serviceUnit(),
                otherFixture.location(),
                null,
                null,
                fixture.company().getCurrency()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Location must belong to the same service unit as ticket");

        assertThatThrownBy(() -> new TicketLine(fixture.item(), 0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ticket line quantity must be greater than or equal to 1");

        Ticket ticket = new Ticket(
                "T-100008",
                fixture.user(),
                null,
                null,
                fixture.serviceUnit(),
                fixture.location(),
                null,
                null,
                fixture.company().getCurrency());
        assertThatThrownBy(() -> ticket.addLine(new TicketLine(otherFixture.item(), 1, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Item must belong to the same service unit as ticket");
    }

    @Test
    void rejectsDuplicateTicketNumberAtDatabaseLevel() {
        Fixture fixture = fixture("ticket-duplicate");
        ticketRepository.saveAndFlush(new Ticket(
                "T-100009",
                fixture.user(),
                null,
                null,
                fixture.serviceUnit(),
                fixture.location(),
                null,
                null,
                fixture.company().getCurrency()));

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(new Ticket(
                "T-100009",
                fixture.user(),
                null,
                null,
                fixture.serviceUnit(),
                fixture.location(),
                null,
                null,
                fixture.company().getCurrency())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Fixture fixture(String prefix) {
        User user = userRepository.save(new User(
                "%s.%s@flowmova.test".formatted(prefix, UUID.randomUUID()),
                "$2a$10$placeholder",
                "Ticket",
                "Owner"));
        Company company = new Company(
                "Ticket Company %s".formatted(UUID.randomUUID()),
                "Company with tickets",
                user);
        company.activate();
        company = companyRepository.save(company);
        CatalogCategory category = catalogCategoryRepository.save(new CatalogCategory(
                company,
                "Ticket Category %s".formatted(UUID.randomUUID()),
                null,
                0,
                user));
        Catalog catalog = catalogRepository.save(new Catalog(
                company,
                category,
                "Ticket Offer %s".formatted(UUID.randomUUID()),
                null,
                null,
                new BigDecimal("9.99"),
                user));
        ServiceUnit serviceUnit = new ServiceUnit(
                company,
                "Ticket Queue %s".formatted(UUID.randomUUID()),
                null,
                null,
                null,
                user);
        serviceUnit.open(user);
        serviceUnit = serviceUnitRepository.save(serviceUnit);
        ServiceUnitLocation location = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                user));
        Item item = itemRepository.save(new Item(
                serviceUnit,
                catalog,
                catalog.getPriceAmount(),
                ItemAvailability.AVAILABLE,
                1,
                0));
        return new Fixture(user, company, serviceUnit, location, item);
    }

    private record Fixture(
            User user,
            Company company,
            ServiceUnit serviceUnit,
            ServiceUnitLocation location,
            Item item) {
    }
}
