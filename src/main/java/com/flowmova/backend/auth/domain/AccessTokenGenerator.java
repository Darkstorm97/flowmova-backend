package com.flowmova.backend.auth.domain;

import com.flowmova.backend.user.domain.User;

public interface AccessTokenGenerator {

    AccessToken generate(User user);
}
