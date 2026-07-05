package com.flowmova.backend.ticket.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.item.domain.Item;
import com.flowmova.backend.item.domain.ItemAvailability;
import com.flowmova.backend.item.domain.ItemStatus;
import com.flowmova.backend.item.infrastructure.ItemRepository;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.serviceunit.domain.TicketCreationGuardMode;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationStatus;
import com.flowmova.backend.serviceunitlocation.infrastructure.ServiceUnitLocationRepository;
import com.flowmova.backend.ticket.api.TicketResponse;
import com.flowmova.backend.ticket.domain.Ticket;
import com.flowmova.backend.ticket.domain.TicketLine;
import com.flowmova.backend.ticket.domain.TicketStatus;
import com.flowmova.backend.ticket.infrastructure.TicketRepository;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreateTicketService {

    private static final char[] ACCESS_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int ACCESS_CODE_LENGTH = 8;
    private static final List<TicketStatus> ACTIVE_TICKET_STATUSES = List.of(
            TicketStatus.CREATED,
            TicketStatus.RECEIVED);

    private final TicketRepository ticketRepository;
    private final ServiceUnitRepository serviceUnitRepository;
    private final ServiceUnitLocationRepository serviceUnitLocationRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public CreateTicketService(
            TicketRepository ticketRepository,
            ServiceUnitRepository serviceUnitRepository,
            ServiceUnitLocationRepository serviceUnitLocationRepository,
            ItemRepository itemRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.ticketRepository = ticketRepository;
        this.serviceUnitRepository = serviceUnitRepository;
        this.serviceUnitLocationRepository = serviceUnitLocationRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public TicketResponse create(UUID serviceUnitId, AuthenticatedUser authenticatedUser, CreateTicketCommand command) {
        ServiceUnit serviceUnit = serviceUnitRepository
                .findByIdAndCompanyStatusAndStatus(serviceUnitId, CompanyStatus.ACTIVE, ServiceUnitStatus.OPEN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service unit not found"));

        ServiceUnitLocation location = resolveLocation(serviceUnitId, command.locationId());
        User user = resolveUser(authenticatedUser);
        validateActiveTicketLimit(serviceUnit, user);
        String guestName = user == null ? requireGuestName(command.guestName()) : null;
        String accessCode = user == null ? generateAccessCode() : null;
        String accessCodeHash = accessCode == null ? null : passwordEncoder.encode(accessCode);

        Ticket ticket = new Ticket(
                nextTicketNumber(),
                user,
                guestName,
                accessCodeHash,
                serviceUnit,
                location,
                trimToNull(command.customerPhone()),
                trimToNull(command.notes()),
                serviceUnit.getCompany().getCurrency());

        for (CreateTicketLineCommand lineCommand : lines(command)) {
            Item item = itemRepository.findByIdAndServiceUnitId(lineCommand.itemId(), serviceUnitId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket line item is invalid"));
            if (item.getStatus() != ItemStatus.ACTIVE || item.getAvailability() != ItemAvailability.AVAILABLE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket line item is invalid");
            }
            ticket.addLine(new TicketLine(
                    item,
                    lineCommand.quantity(),
                    item.getPriceAmount(),
                    trimToNull(lineCommand.notes())));
        }

        return TicketResponse.from(ticketRepository.saveAndFlush(ticket), accessCode);
    }

    private ServiceUnitLocation resolveLocation(UUID serviceUnitId, UUID locationId) {
        if (locationId == null) {
            return serviceUnitLocationRepository.findByServiceUnitIdAndDefaultLocationTrueAndStatus(
                            serviceUnitId,
                            ServiceUnitLocationStatus.ACTIVE)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Default location not found"));
        }

        return serviceUnitLocationRepository.findByIdAndServiceUnitIdAndStatus(
                        locationId,
                        serviceUnitId,
                        ServiceUnitLocationStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location is invalid"));
    }

    private User resolveUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            return null;
        }
        return userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private void validateActiveTicketLimit(ServiceUnit serviceUnit, User user) {
        TicketCreationGuardMode guardMode = serviceUnit.getTicketCreationGuardMode();
        if (guardMode == TicketCreationGuardMode.NONE) {
            return;
        }

        if (guardMode == TicketCreationGuardMode.AUTHENTICATED_OR_GUEST_RECENT_ONE_OPEN_TICKET && user == null) {
            return;
        }

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        if (ticketRepository.existsByUserIdAndServiceUnitIdAndStatusIn(
                user.getId(),
                serviceUnit.getId(),
                ACTIVE_TICKET_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Active ticket already exists");
        }
    }

    private List<CreateTicketLineCommand> lines(CreateTicketCommand command) {
        return command.lines() == null ? List.of() : command.lines();
    }

    private String requireGuestName(String value) {
        String guestName = trimToNull(value);
        if (guestName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guest name is required");
        }
        return guestName;
    }

    private String nextTicketNumber() {
        Long value = jdbcTemplate.queryForObject("SELECT nextval('ticket_number_sequence')", Long.class);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ticket number could not be generated");
        }
        return "T-%06d".formatted(value);
    }

    private String generateAccessCode() {
        StringBuilder accessCode = new StringBuilder(ACCESS_CODE_LENGTH);
        for (int index = 0; index < ACCESS_CODE_LENGTH; index++) {
            accessCode.append(ACCESS_CODE_ALPHABET[secureRandom.nextInt(ACCESS_CODE_ALPHABET.length)]);
        }
        return accessCode.toString();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
