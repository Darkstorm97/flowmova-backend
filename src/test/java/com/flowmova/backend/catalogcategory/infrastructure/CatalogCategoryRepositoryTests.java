package com.flowmova.backend.catalogcategory.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowmova.backend.catalogcategory.domain.CatalogCategory;
import com.flowmova.backend.catalogcategory.domain.CatalogCategoryStatus;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CatalogCategoryRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CatalogCategoryRepository catalogCategoryRepository;

    @Test
    void savesListsAndChecksNameUniquenessByCompany() {
        User owner = userRepository.save(new User(
                "catalog-category-owner.%s@flowmova.test".formatted(UUID.randomUUID()),
                "$2a$10$placeholder",
                "Catalog",
                "Owner"));
        Company company = companyRepository.save(new Company(
                "Catalog Category Company",
                "Company with categories",
                owner));
        Company otherCompany = companyRepository.save(new Company(
                "Other Catalog Category Company",
                "Other company with categories",
                owner));

        CatalogCategory laterCategory = catalogCategoryRepository.save(new CatalogCategory(
                company,
                "Main Services",
                "Main company services",
                20,
                owner));
        CatalogCategory firstCategory = catalogCategoryRepository.save(new CatalogCategory(
                company,
                "Express Services",
                "Express company services",
                10,
                owner));
        catalogCategoryRepository.save(new CatalogCategory(
                otherCompany,
                "Main Services",
                "Same name in another company",
                10,
                owner));

        List<CatalogCategory> categories = catalogCategoryRepository
                .findByCompanyIdOrderByDisplayOrderAscNameAsc(company.getId());

        assertThat(categories).containsExactly(firstCategory, laterCategory);
        assertThat(catalogCategoryRepository.existsByCompanyIdAndNameIgnoreCase(
                company.getId(),
                "main services")).isTrue();
        assertThat(catalogCategoryRepository.existsByCompanyIdAndNameIgnoreCase(
                otherCompany.getId(),
                "express services")).isFalse();
        assertThat(firstCategory.getStatus()).isEqualTo(CatalogCategoryStatus.ACTIVE);
        assertThat(firstCategory.getCreatedAt()).isNotNull();
        assertThat(firstCategory.getUpdatedAt()).isNotNull();
        assertThat(firstCategory.getCreatedBy()).isEqualTo(owner);
        assertThat(firstCategory.getVersion()).isNotNull();
    }
}
