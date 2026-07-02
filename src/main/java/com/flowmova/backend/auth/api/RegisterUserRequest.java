package com.flowmova.backend.auth.api;

import com.flowmova.backend.auth.application.RegisterUserCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName) {

    public RegisterUserCommand toCommand() {
        return new RegisterUserCommand(email, password, firstName, lastName);
    }
}
