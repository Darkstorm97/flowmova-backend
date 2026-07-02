package com.flowmova.backend.companyaccess.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CompanyUserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyUserRepository companyUserRepository;

    @Test
    void savesAndFindsCompanyUserAccess() {
        User user = userRepository.save(new User(
                "owner@flowmova.test",
                "$2a$10$placeholder",
                "Flow",
                "Owner"));
        UUID companyId = UUID.randomUUID();

        CompanyUser companyUser = companyUserRepository.save(new CompanyUser(companyId, user, CompanyRole.ADMIN));

        assertThat(userRepository.findByEmail("owner@flowmova.test")).contains(user);
        assertThat(userRepository.existsByEmail("owner@flowmova.test")).isTrue();
        assertThat(companyUserRepository.findByCompanyId(companyId)).containsExactly(companyUser);
        assertThat(companyUserRepository.findByUserId(user.getId())).containsExactly(companyUser);
        assertThat(companyUserRepository.findByCompanyIdAndUserId(companyId, user.getId())).contains(companyUser);
        assertThat(companyUserRepository.existsByCompanyIdAndUserId(companyId, user.getId())).isTrue();
    }
}
