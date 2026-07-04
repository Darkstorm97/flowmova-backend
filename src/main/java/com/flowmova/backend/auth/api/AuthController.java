package com.flowmova.backend.auth.api;

import com.flowmova.backend.auth.application.LoginUserService;
import com.flowmova.backend.auth.application.RegisterUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Inscription, connexion et obtention du token JWT.")
public class AuthController {

    private final RegisterUserService registerUserService;
    private final LoginUserService loginUserService;

    public AuthController(RegisterUserService registerUserService, LoginUserService loginUserService) {
        this.registerUserService = registerUserService;
        this.loginUserService = loginUserService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Inscrire un utilisateur",
            description = "Cree un compte utilisateur actif. Endpoint public, aucun JWT requis.")
    public RegisterUserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return RegisterUserResponse.from(registerUserService.register(request.toCommand()));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Connecter un utilisateur",
            description = "Verifie l'email et le mot de passe puis retourne un access token JWT Bearer.")
    public LoginUserResponse login(@Valid @RequestBody LoginUserRequest request) {
        return LoginUserResponse.from(loginUserService.login(request.toCommand()));
    }
}
