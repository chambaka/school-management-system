package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.auth.AuthResponse;
import tz.co.chambaka.school.management.dto.auth.ChangePasswordRequest;
import tz.co.chambaka.school.management.dto.auth.ForgotPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.ForgotPasswordResponse;
import tz.co.chambaka.school.management.dto.auth.LoginRequest;
import tz.co.chambaka.school.management.dto.auth.PasswordRulesResponse;
import tz.co.chambaka.school.management.dto.auth.RefreshTokenRequest;
import tz.co.chambaka.school.management.dto.auth.RegisterSchoolRequest;
import tz.co.chambaka.school.management.dto.auth.ResetPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.SwitchSchoolRequest;
import tz.co.chambaka.school.management.dto.auth.UserProfileResponse;
import tz.co.chambaka.school.management.dto.auth.VerifyResetCodeRequest;
import tz.co.chambaka.school.management.dto.auth.VerifyResetCodeResponse;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.AuthService;
import tz.co.chambaka.school.management.service.PasswordResetService;
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
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register-school")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Onboard a new organization and its tenant admin. Schools are added later.")
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

    @PostMapping("/switch-school")
    @Operation(summary = "Switch the active school and campus for a tenant or platform admin")
    public AuthResponse switchSchool(@CurrentUser UserPrincipal principal, @Valid @RequestBody SwitchSchoolRequest request) {
        return authService.switchSchool(principal.getId(), request);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@CurrentUser UserPrincipal principal, @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getId(), request);
    }

    @GetMapping("/password-rules")
    @Operation(summary = "Nexus-style password rules for the strength meter")
    public PasswordRulesResponse passwordRules() {
        return PasswordRulesResponse.current();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a 6-digit password reset code")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return passwordResetService.requestReset(request);
    }

    @PostMapping("/forgot-password/verify")
    @Operation(summary = "Verify the email reset code and start a reset session")
    public VerifyResetCodeResponse verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        return passwordResetService.verifyCode(request);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Set a new password after verifying the reset code")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
    }
}
