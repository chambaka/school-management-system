package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.admin.RemoveUserLinkRequest;
import tz.co.chambaka.school.management.dto.admin.RemoveUserResponse;
import tz.co.chambaka.school.management.dto.admin.SchoolUserResponse;
import tz.co.chambaka.school.management.dto.admin.SetUserEnabledRequest;
import tz.co.chambaka.school.management.dto.admin.SetUserEnabledResponse;
import tz.co.chambaka.school.management.dto.admin.UserLinksResponse;
import tz.co.chambaka.school.management.dto.auth.AdminResetPasswordRequest;
import tz.co.chambaka.school.management.dto.auth.AdminResetPasswordResponse;
import tz.co.chambaka.school.management.dto.auth.ResetTwoFactorResponse;
import tz.co.chambaka.school.management.dto.auth.UnlockAccountResponse;
import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    @PreAuthorize(Access.ACCOUNT_UNLOCK)
    @Operation(summary = "List login accounts. School officers see their school; platform admin sees the whole platform.")
    public PageResponse<SchoolUserResponse> list(
            @CurrentUser UserPrincipal principal,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) Role role,
            Pageable pageable
    ) {
        return authService.listSchoolUsers(schoolId, role, principal, pageable);
    }

    @GetMapping("/{id}/links")
    @PreAuthorize(Access.ACCOUNT_UNLOCK)
    @Operation(summary = "Records linked to this login: teacher, student, parent, allocations, and other school activity.")
    public UserLinksResponse links(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) Long schoolId
    ) {
        return authService.links(id, schoolId, principal);
    }

    @DeleteMapping("/{id}/links")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.ACCOUNT_UNLOCK)
    @Operation(summary = "Remove a subject allocation or parent–student link from this login.")
    public void removeLink(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) Long schoolId,
            @RequestBody RemoveUserLinkRequest request
    ) {
        authService.removeLink(id, schoolId, principal, request);
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize(Access.ACCOUNT_UNLOCK)
    public UnlockAccountResponse unlock(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) Long schoolId
    ) {
        return authService.unlock(id, schoolId, principal);
    }

    @PostMapping("/{id}/reset-2fa")
    @PreAuthorize(Access.ACCOUNT_UNLOCK)
    @Operation(summary = "Clear Google Authenticator for a user. Headmaster, school admin, or platform admin.")
    public ResetTwoFactorResponse resetTwoFactor(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) Long schoolId
    ) {
        return authService.resetTwoFactor(id, schoolId, principal);
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize(Access.ACCOUNT_UNLOCK)
    @Operation(summary = "Set a new password (or SMS a temporary one). Headmaster, school admin, or platform admin.")
    public AdminResetPasswordResponse resetPassword(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) Long schoolId,
            @RequestBody(required = false) @Valid AdminResetPasswordRequest request
    ) {
        return authService.resetPassword(id, schoolId, principal, request);
    }

    @PostMapping("/{id}/enabled")
    @PreAuthorize(Access.ACCOUNT_UNLOCK)
    @Operation(summary = "Enable or disable a login. Headmaster, school admin, academic master, or platform admin.")
    public SetUserEnabledResponse setEnabled(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) Long schoolId,
            @RequestBody SetUserEnabledRequest request
    ) {
        return authService.setEnabled(id, schoolId, principal, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Access.USER_REMOVE)
    @Operation(summary = "Delete a login completely. Headmaster or platform admin. Teacher, student, and parent profiles stay, but the login is removed.")
    public RemoveUserResponse remove(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) Long schoolId
    ) {
        return authService.remove(id, schoolId, principal);
    }
}
