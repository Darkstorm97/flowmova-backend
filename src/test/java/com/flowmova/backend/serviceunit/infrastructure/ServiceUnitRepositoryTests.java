package com.flowmova.backend.serviceunit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.serviceunit.domain.ServiceUnitType;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ServiceUnitRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ServiceUnitRepository serviceUnitRepository;

    @Test
    void savesListsAndFiltersServiceUnits() {
        User owner = userRepository.save(new User(
                "service-unit-owner.%s@flowmova.test".formatted(UUID.randomUUID()),
                "$2a$10$placeholder",
                "Service",
                "Owner"));
        Company activeCompany = new Company(
                "Active Service Company",
                "Company with service units",
                owner);
        activeCompany.activate();
        activeCompany = companyRepository.save(activeCompany);
        Company disabledCompany = companyRepository.save(new Company(
                "Disabled Service Company",
                "Hidden company with service units",
                owner));

        ServiceUnit frontDesk = serviceUnitRepository.save(new ServiceUnit(
                activeCompany,
                "Front Desk",
                "Main queue",
                "Lobby",
                Map.of("maxOpenTickets", 25),
                owner));
        frontDesk.open(owner);
        ServiceUnit counter = serviceUnitRepository.save(new ServiceUnit(
                activeCompany,
                "Counter",
                "Secondary queue",
                "Counter A",
                null,
                owner));
        ServiceUnit archived = serviceUnitRepository.save(new ServiceUnit(
                activeCompany,
                "Old Queue",
                "Archived queue",
                null,
                null,
                owner));
        archived.archive(owner);
        ServiceUnit disabledCompanyUnit = serviceUnitRepository.save(new ServiceUnit(
                disabledCompany,
                "Disabled Company Queue",
                "Should not be public",
                null,
                null,
                owner));
        disabledCompanyUnit.open(owner);
        serviceUnitRepository.flush();

        assertThat(serviceUnitRepository.findByCompanyIdOrderByNameAsc(activeCompany.getId()))
                .containsExactly(counter, frontDesk, archived);
        assertThat(serviceUnitRepository.findByCompanyIdAndStatusOrderByNameAsc(
                activeCompany.getId(),
                ServiceUnitStatus.OPEN))
                .containsExactly(frontDesk);
        assertThat(serviceUnitRepository.findByCompanyIdAndCompanyStatusAndStatusOrderByNameAsc(
                activeCompany.getId(),
                CompanyStatus.ACTIVE,
                ServiceUnitStatus.OPEN))
                .containsExactly(frontDesk);
        assertThat(serviceUnitRepository.findByCompanyIdAndCompanyStatusAndStatusOrderByNameAsc(
                disabledCompany.getId(),
                CompanyStatus.ACTIVE,
                ServiceUnitStatus.OPEN))
                .isEmpty();

        assertThat(frontDesk.getType()).isEqualTo(ServiceUnitType.TICKET_QUEUE);
        assertThat(frontDesk.getStatus()).isEqualTo(ServiceUnitStatus.OPEN);
        assertThat(frontDesk.getSettings()).containsEntry("maxOpenTickets", 25);
        assertThat(counter.getSettings()).isEmpty();
        assertThat(frontDesk.getCreatedAt()).isNotNull();
        assertThat(frontDesk.getUpdatedAt()).isNotNull();
        assertThat(frontDesk.getCreatedBy()).isEqualTo(owner);
        assertThat(frontDesk.getUpdatedBy()).isEqualTo(owner);
        assertThat(frontDesk.getVersion()).isNotNull();
    }
}
