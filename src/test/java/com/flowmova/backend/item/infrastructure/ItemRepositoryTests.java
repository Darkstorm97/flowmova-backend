package com.flowmova.backend.item.infrastructure;

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
import com.flowmova.backend.item.domain.ItemStatus;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ItemRepositoryTests {

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
    private ItemRepository itemRepository;

    @Test
    void savesListsAndFiltersItems() {
        User owner = userRepository.save(new User(
                "item-owner.%s@flowmova.test".formatted(UUID.randomUUID()),
                "$2a$10$placeholder",
                "Item",
                "Owner"));
        Company company = companyRepository.save(new Company(
                "Item Company",
                "Company with service unit items",
                owner));
        CatalogCategory category = catalogCategoryRepository.save(new CatalogCategory(
                company,
                "Services",
                "Service catalogs",
                10,
                owner));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Main Counter",
                "Counter queue",
                "Lobby",
                null,
                owner));

        Catalog basic = catalogRepository.save(new Catalog(
                company,
                category,
                "Basic Service",
                "Basic catalog",
                null,
                new BigDecimal("25.00"),
                owner));
        Catalog premium = catalogRepository.save(new Catalog(
                company,
                category,
                "Premium Service",
                "Premium catalog",
                null,
                new BigDecimal("50.00"),
                owner));
        Catalog hidden = catalogRepository.save(new Catalog(
                company,
                category,
                "Hidden Service",
                "Hidden catalog",
                null,
                null,
                owner));

        Item available = itemRepository.save(new Item(
                serviceUnit,
                premium,
                new BigDecimal("45.00"),
                ItemAvailability.AVAILABLE,
                5,
                20));
        Item unavailable = itemRepository.save(new Item(
                serviceUnit,
                basic,
                null,
                ItemAvailability.UNAVAILABLE,
                null,
                10));
        Item archived = new Item(
                serviceUnit,
                hidden,
                null,
                ItemAvailability.AVAILABLE,
                null,
                30);
        archived.archive();
        archived = itemRepository.save(archived);
        itemRepository.flush();

        List<Item> allItems = itemRepository.findByServiceUnitIdOrderByDisplayOrderAscCatalogNameAsc(serviceUnit.getId());
        List<Item> activeAvailableItems = itemRepository
                .findByServiceUnitIdAndStatusAndAvailabilityOrderByDisplayOrderAscCatalogNameAsc(
                        serviceUnit.getId(),
                        ItemStatus.ACTIVE,
                        ItemAvailability.AVAILABLE);

        assertThat(allItems).containsExactly(unavailable, available, archived);
        assertThat(activeAvailableItems).containsExactly(available);
        assertThat(itemRepository.findByServiceUnitIdAndCatalogId(serviceUnit.getId(), premium.getId()))
                .contains(available);
        assertThat(itemRepository.existsByServiceUnitIdAndCatalogId(serviceUnit.getId(), premium.getId()))
                .isTrue();
        assertThat(available.getStatus()).isEqualTo(ItemStatus.ACTIVE);
        assertThat(available.getAvailability()).isEqualTo(ItemAvailability.AVAILABLE);
        assertThat(available.getPriceAmount()).isEqualByComparingTo("45.00");
        assertThat(available.getConfiguredQuantity()).isEqualTo(5);
        assertThat(available.getReservedQuantity()).isZero();
        assertThat(available.getDisplayOrder()).isEqualTo(20);
        assertThat(available.getCreatedAt()).isNotNull();
        assertThat(available.getUpdatedAt()).isNotNull();
        assertThat(available.getVersion()).isNotNull();
    }

    @Test
    void rejectsDuplicateCatalogInSameServiceUnit() {
        User owner = userRepository.save(new User(
                "item-duplicate.%s@flowmova.test".formatted(UUID.randomUUID()),
                "$2a$10$placeholder",
                "Item",
                "Duplicate"));
        Company company = companyRepository.save(new Company(
                "Duplicate Item Company",
                "Company with duplicate item test",
                owner));
        CatalogCategory category = catalogCategoryRepository.save(new CatalogCategory(
                company,
                "Services",
                null,
                0,
                owner));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Duplicate Counter",
                null,
                null,
                null,
                owner));
        Catalog catalog = catalogRepository.save(new Catalog(
                company,
                category,
                "Reusable Catalog",
                null,
                null,
                null,
                owner));

        itemRepository.saveAndFlush(new Item(
                serviceUnit,
                catalog,
                null,
                ItemAvailability.AVAILABLE,
                null,
                0));

        assertThatThrownBy(() -> itemRepository.saveAndFlush(new Item(
                serviceUnit,
                catalog,
                null,
                ItemAvailability.AVAILABLE,
                null,
                1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsCatalogFromAnotherCompany() {
        User owner = userRepository.save(new User(
                "item-company.%s@flowmova.test".formatted(UUID.randomUUID()),
                "$2a$10$placeholder",
                "Item",
                "Company"));
        Company company = companyRepository.save(new Company(
                "Item Same Company",
                null,
                owner));
        Company otherCompany = companyRepository.save(new Company(
                "Item Other Company",
                null,
                owner));
        CatalogCategory otherCategory = catalogCategoryRepository.save(new CatalogCategory(
                otherCompany,
                "Other Services",
                null,
                0,
                owner));
        ServiceUnit serviceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Same Company Counter",
                null,
                null,
                null,
                owner));
        Catalog otherCatalog = catalogRepository.save(new Catalog(
                otherCompany,
                otherCategory,
                "Other Catalog",
                null,
                null,
                null,
                owner));

        assertThatThrownBy(() -> new Item(
                serviceUnit,
                otherCatalog,
                null,
                ItemAvailability.AVAILABLE,
                null,
                0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Catalog must belong to the same company as service unit");
    }
}
