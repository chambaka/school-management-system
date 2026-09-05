package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.auth.AuthResponse;
import tz.co.chambaka.school.management.dto.auth.ChangePasswordRequest;
import tz.co.chambaka.school.management.dto.auth.LoginRequest;
import tz.co.chambaka.school.management.dto.auth.RefreshTokenRequest;
import tz.co.chambaka.school.management.dto.auth.RegisterSchoolRequest;
import tz.co.chambaka.school.management.dto.auth.UserProfileResponse;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register-school")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Onboard a new school tenant and its first admin")
    public AuthResponse registerSchool(@Valid @RequestBody RegisterSchoolRequest request) {
        return authService.registerSchool(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue a new access token")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Current authenticated identity")
    public UserProfileResponse me(@CurrentUser UserPrincipal principal) {
        return authService.me(principal.getId());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@CurrentUser UserPrincipal principal, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getId(), request);
    }
}
