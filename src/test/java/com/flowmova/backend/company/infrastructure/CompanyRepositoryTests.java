package com.flowmova.backend.company.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CompanyRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void savesAndFindsCompanyByIdStatusAndName() {
        User owner = userRepository.save(new User(
                "company-owner@flowmova.test",
                "$2a$10$placeholder",
                "Company",
                "Owner"));

        Company company = companyRepository.save(new Company(
                "FlowMova Services",
                "Service operations",
                owner));

        assertThat(companyRepository.findById(company.getId())).contains(company);
        assertThat(companyRepository.findByStatus(CompanyStatus.DISABLED)).contains(company);
        assertThat(companyRepository.findByNameContainingIgnoreCase("flowmova")).contains(company);
        assertThat(company.getCreatedAt()).isNotNull();
        assertThat(company.getUpdatedAt()).isNotNull();
        assertThat(company.getCreatedBy()).isEqualTo(owner);
        assertThat(company.getVersion()).isNotNull();
    }
}
