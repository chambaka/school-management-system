package tz.co.chambaka.school.management.dto.auth;

import tz.co.chambaka.school.management.security.PasswordPolicy;

import java.util.List;

public record PasswordRulesResponse(
        int minLength,
        String specialChars,
        List<PasswordPolicy.RuleDefinition> rules
) {
    public static PasswordRulesResponse current() {
        return new PasswordRulesResponse(
                PasswordPolicy.MIN_LENGTH,
                PasswordPolicy.SPECIAL_CHARS,
                PasswordPolicy.definitions());
    }
}
