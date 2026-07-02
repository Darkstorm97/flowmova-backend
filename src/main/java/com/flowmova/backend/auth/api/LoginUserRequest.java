package com.flowmova.backend.auth.api;

import com.flowmova.backend.auth.application.LoginUserCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginUserRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank String password) {

    public LoginUserCommand toCommand() {
        return new LoginUserCommand(email, password);
    }
}
