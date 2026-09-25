package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.auth.AuthResponse;
import tz.co.chambaka.school.management.dto.auth.ChangePasswordRequest;
import tz.co.chambaka.school.management.dto.auth.ForgotPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.ForgotPasswordResponse;
import tz.co.chambaka.school.management.dto.auth.LoginRequest;
import tz.co.chambaka.school.management.dto.auth.LoginResponse;
import tz.co.chambaka.school.management.dto.auth.PasswordRulesResponse;
import tz.co.chambaka.school.management.dto.auth.RefreshTokenRequest;
import tz.co.chambaka.school.management.dto.auth.RegisterSchoolRequest;
import tz.co.chambaka.school.management.dto.auth.ResetPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.ResetTwoFactorResponse;
import tz.co.chambaka.school.management.dto.auth.SwitchSchoolRequest;
import tz.co.chambaka.school.management.dto.auth.UserProfileResponse;
import tz.co.chambaka.school.management.dto.auth.VerifyResetCodeRequest;
import tz.co.chambaka.school.management.dto.auth.VerifyResetCodeResponse;
import tz.co.chambaka.school.management.dto.auth.VerifyTwoFactorRequest;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.AuthService;
import tz.co.chambaka.school.management.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/login/2fa")
    @Operation(summary = "Complete sign-in with a Google Authenticator code")
    public LoginResponse verifyTwoFactor(@Valid @RequestBody VerifyTwoFactorRequest request) {
        return authService.verifyTwoFactor(request);
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

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke the current refresh token")
    public void logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(request);
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

    @PostMapping("/reset-2fa")
    @PreAuthorize(Access.ACCOUNT_UNLOCK)
    @Operation(summary = "Clear your Google Authenticator enrollment. Headmaster and school admin only.")
    public ResetTwoFactorResponse resetOwnTwoFactor(@CurrentUser UserPrincipal principal) {
        return authService.resetOwnTwoFactor(principal.getId());
    }

    @GetMapping("/password-rules")
    @Operation(summary = "Nexus-style password rules for the strength meter")
    public PasswordRulesResponse passwordRules() {
        return PasswordRulesResponse.current();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a 6-digit password reset code by SMS")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("password-reset HTTP POST /forgot-password body={}", request);
        ForgotPasswordResponse response = passwordResetService.requestReset(request);
        log.info("password-reset HTTP POST /forgot-password response message={} maskedPhone={} expiresInSeconds={} debugCode={}",
                response.message(), response.maskedPhone(), response.expiresInSeconds(), response.debugCode());
        return response;
    }

    @PostMapping("/forgot-password/verify")
    @Operation(summary = "Verify the SMS reset code and start a reset session")
    public VerifyResetCodeResponse verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        log.info("password-reset HTTP POST /forgot-password/verify body={}", request);
        VerifyResetCodeResponse response = passwordResetService.verifyCode(request);
        log.info("password-reset HTTP POST /forgot-password/verify response resetToken={} expiresInSeconds={}",
                response.resetToken(), response.expiresInSeconds());
        return response;
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Set a new password after verifying the reset code")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("password-reset HTTP POST /reset-password resetToken={} newPasswordLen={}",
                request.resetToken(),
                request.newPassword() == null ? 0 : request.newPassword().length());
        passwordResetService.resetPassword(request);
        log.info("password-reset HTTP POST /reset-password completed");
    }
}
