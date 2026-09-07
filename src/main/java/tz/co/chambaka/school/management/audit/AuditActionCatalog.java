package tz.co.chambaka.school.management.audit;

import tz.co.chambaka.school.management.model.enums.AuditAction;

import java.util.Arrays;
import java.util.List;

public final class AuditActionCatalog {

    public static final String GROUP_AUTH = "Auth";
    public static final String GROUP_FINANCE = "Finance";
    public static final String GROUP_HTTP = "HTTP trail";

    private AuditActionCatalog() {
    }

    public record Meta(AuditAction action, String group, String label, String description) {
    }

    public static List<Meta> all() {
        return Arrays.stream(AuditAction.values()).map(AuditActionCatalog::meta).toList();
    }

    public static Meta meta(AuditAction action) {
        return switch (action) {
            case CREATE -> new Meta(action, GROUP_HTTP, "Create", "POST requests that create records");
            case UPDATE -> new Meta(action, GROUP_HTTP, "Update", "PUT and PATCH requests that change records");
            case DELETE -> new Meta(action, GROUP_HTTP, "Delete", "DELETE requests that remove records");
            case LOGIN -> new Meta(action, GROUP_AUTH, "Login", "Successful sign-in");
            case LOGIN_FAILED -> new Meta(action, GROUP_AUTH, "Login failed", "Rejected credentials");
            case TOKEN_REFRESH -> new Meta(action, GROUP_AUTH, "Token refresh", "Refresh token rotated");
            case PASSWORD_CHANGE -> new Meta(action, GROUP_AUTH, "Password change", "Signed-in password change");
            case PASSWORD_RESET_REQUESTED -> new Meta(action, GROUP_AUTH, "Password reset requested", "Reset code issued");
            case PASSWORD_RESET -> new Meta(action, GROUP_AUTH, "Password reset", "Password reset completed");
            case REGISTER_SCHOOL -> new Meta(action, GROUP_AUTH, "Register school", "School registration (legacy)");
            case REGISTER_TENANT -> new Meta(action, GROUP_AUTH, "Register organization", "New tenant and tenant admin");
            case FEE_CREATED -> new Meta(action, GROUP_FINANCE, "Fee created", "Fee structure created");
            case FEE_LISTED -> new Meta(action, GROUP_FINANCE, "Fee listed", "Fee structures listed");
            case INVOICE_GENERATED -> new Meta(action, GROUP_FINANCE, "Invoice generated", "Invoices generated for a class");
            case INVOICE_LISTED -> new Meta(action, GROUP_FINANCE, "Invoice listed", "Invoices listed");
            case INVOICE_VIEWED -> new Meta(action, GROUP_FINANCE, "Invoice viewed", "A single invoice opened");
            case PAYMENT_RECORDED -> new Meta(action, GROUP_FINANCE, "Payment posted", "A payment was recorded against an invoice");
            case PAYMENT_LISTED -> new Meta(action, GROUP_FINANCE, "Payment listed", "Payments listed for an invoice");
            case PAYMENT_VIEWED -> new Meta(action, GROUP_FINANCE, "Payment viewed", "A single payment opened");
            case BALANCE_VIEWED -> new Meta(action, GROUP_FINANCE, "Balance viewed", "Student balance opened");
            case ACCESS -> new Meta(action, GROUP_HTTP, "Access", "Read or other HTTP access (finance GETs and failures)");
            case ACCESS_DENIED -> new Meta(action, GROUP_HTTP, "Access denied", "HTTP 401 and 403");
            case ERROR -> new Meta(action, GROUP_HTTP, "Error", "HTTP 5xx failures");
        };
    }
}
