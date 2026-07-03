package com.flowmova.backend.serviceunitlocation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationStatus;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationType;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ServiceUnitLocationRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ServiceUnitRepository serviceUnitRepository;

    @Autowired
    private ServiceUnitLocationRepository serviceUnitLocationRepository;

    @Test
    void savesListsAndFindsServiceUnitLocations() {
        User owner = userRepository.save(new User(
                "service-unit-location-owner.%s@flowmova.test".formatted(UUID.randomUUID()),
                "$2a$10$placeholder",
                "Location",
                "Owner"));
        Company company = new Company(
                "Location Company",
                "Company with service unit locations",
                owner);
        company.activate();
        company = companyRepository.save(company);
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Restaurant Queue",
                "Queue with locations",
                "Main room",
                null,
                owner));

        ServiceUnitLocation defaultLocation = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                "Default access point",
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                owner));
        ServiceUnitLocation tableTwo = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Table 2",
                "Dining room table",
                ServiceUnitLocationType.CUSTOM,
                false,
                "loc-%s".formatted(UUID.randomUUID()),
                owner));
        ServiceUnitLocation archived = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Old Table",
                "Archived location",
                ServiceUnitLocationType.CUSTOM,
                false,
                "loc-%s".formatted(UUID.randomUUID()),
                owner));
        archived.archive(owner);
        serviceUnitLocationRepository.flush();

        assertThat(serviceUnitLocationRepository.findByServiceUnitIdOrderByDefaultLocationDescNameAsc(serviceUnit.getId()))
                .containsExactly(defaultLocation, archived, tableTwo);
        assertThat(serviceUnitLocationRepository.findByServiceUnitIdAndDefaultLocationTrueAndStatus(
                serviceUnit.getId(),
                ServiceUnitLocationStatus.ACTIVE))
                .contains(defaultLocation);
        assertThat(serviceUnitLocationRepository.findByPublicAccessSlug(defaultLocation.getPublicAccessSlug()))
                .contains(defaultLocation);
        assertThat(serviceUnitLocationRepository.findByPublicAccessSlugAndStatus(
                archived.getPublicAccessSlug(),
                ServiceUnitLocationStatus.ACTIVE))
                .isEmpty();

        assertThat(defaultLocation.getType()).isEqualTo(ServiceUnitLocationType.DEFAULT);
        assertThat(defaultLocation.isDefaultLocation()).isTrue();
        assertThat(defaultLocation.getStatus()).isEqualTo(ServiceUnitLocationStatus.ACTIVE);
        assertThat(archived.getStatus()).isEqualTo(ServiceUnitLocationStatus.ARCHIVED);
        assertThat(archived.getUpdatedBy()).isEqualTo(owner);
        assertThat(defaultLocation.getCreatedAt()).isNotNull();
        assertThat(defaultLocation.getUpdatedAt()).isNotNull();
        assertThat(defaultLocation.getCreatedBy()).isEqualTo(owner);
        assertThat(defaultLocation.getVersion()).isNotNull();
    }

    @Test
    void enforcesSingleActiveDefaultLocationPerServiceUnit() {
        User owner = userRepository.save(new User(
                "service-unit-location-default.%s@flowmova.test".formatted(UUID.randomUUID()),
                "$2a$10$placeholder",
                "Location",
                "Owner"));
        Company company = new Company(
                "Default Location Company",
                "Company with default location",
                owner);
        company.activate();
        company = companyRepository.save(company);
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Default Queue",
                "Queue with default location",
                null,
                null,
                owner));
        serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Principal",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                owner));

        serviceUnitLocationRepository.save(new ServiceUnitLocation(
                serviceUnit,
                "Second Default",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                owner));

        assertThatThrownBy(() -> serviceUnitLocationRepository.flush())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
