package tz.co.chambaka.school.management.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class PasswordPolicy {

    public static final String SPECIAL_CHARS = "!@#$%^&*";
    public static final int MIN_LENGTH = 10;

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*]");

    private static final Set<String> COMMON = Set.of(
            "password", "password1", "password12", "password123", "password1234",
            "123456", "1234567", "12345678", "123456789", "1234567890",
            "qwerty", "qwerty123", "abc123", "111111", "123123",
            "admin", "admin123", "letmein", "welcome", "welcome1",
            "changeme", "secret", "passw0rd", "p@ssw0rd", "p@ssword",
            "password1!", "welcome1!",
            "changeme123", "changeme123!", "test1234", "root", "administrator"
    );

    private PasswordPolicy() {
    }

    public static Evaluation evaluate(String password, String email, String name) {
        String value = password == null ? "" : password;
        List<RuleResult> rules = List.of(
                rule("length", "Be at least 10 characters long", value.length() >= MIN_LENGTH),
                rule("upper", "Include at least one uppercase letter (A-Z)", UPPER.matcher(value).find()),
                rule("lower", "Include at least one lowercase letter (a-z)", LOWER.matcher(value).find()),
                rule("digit", "Include at least one number (0-9)", DIGIT.matcher(value).find()),
                rule("special", "Include at least one special character (" + SPECIAL_CHARS + ")",
                        SPECIAL.matcher(value).find()),
                rule("notIdentity", "Not contain your username or email address",
                        !containsIdentity(value, email, name)),
                rule("notCommon", "Not be a commonly used password", !isCommon(value))
        );
        long met = rules.stream().filter(RuleResult::met).count();
        boolean valid = !value.isEmpty() && met == rules.size();
        String firstError = rules.stream().filter(r -> !r.met()).findFirst().map(RuleResult::label).orElse(null);
        int strength = (int) Math.min(4, Math.round((met / (double) rules.size()) * 4));
        String strengthLabel = switch (strength) {
            case 0, 1 -> "Weak";
            case 2 -> "Fair";
            case 3 -> "Good";
            default -> "Strong";
        };
        return new Evaluation(rules, valid, firstError, (int) met, rules.size(),
                (int) Math.round(met * 100.0 / rules.size()), strength, strengthLabel);
    }

    public static void requireValid(String password, String email, String name) {
        Evaluation evaluation = evaluate(password, email, name);
        if (!evaluation.valid()) {
            throw new tz.co.chambaka.school.management.exception.BusinessException(
                    evaluation.firstError() == null
                            ? "Password does not meet requirements"
                            : evaluation.firstError());
        }
    }

    public static List<RuleDefinition> definitions() {
        Evaluation empty = evaluate("", "", "");
        List<RuleDefinition> defs = new ArrayList<>();
        for (RuleResult rule : empty.rules()) {
            defs.add(new RuleDefinition(rule.id(), rule.label()));
        }
        return defs;
    }

    private static RuleResult rule(String id, String label, boolean met) {
        return new RuleResult(id, label, met);
    }

    private static boolean isCommon(String password) {
        return COMMON.contains(password.toLowerCase(Locale.ROOT));
    }

    private static boolean containsIdentity(String password, String email, String name) {
        String pwd = password.toLowerCase(Locale.ROOT);
        if (pwd.isEmpty()) {
            return false;
        }
        if (email != null && !email.isBlank()) {
            String normalized = email.trim().toLowerCase(Locale.ROOT);
            if (pwd.contains(normalized)) {
                return true;
            }
            String local = normalized.split("@", 2)[0];
            if (local.length() >= 3 && pwd.contains(local)) {
                return true;
            }
        }
        if (name != null && name.trim().length() >= 3) {
            String normalizedName = name.trim().toLowerCase(Locale.ROOT);
            if (pwd.contains(normalizedName)) {
                return true;
            }
            for (String part : normalizedName.split("\\s+")) {
                if (part.length() >= 3 && pwd.contains(part)) {
                    return true;
                }
            }
        }
        return false;
    }

    public record RuleDefinition(String id, String label) {
    }

    public record RuleResult(String id, String label, boolean met) {
    }

    public record Evaluation(
            List<RuleResult> rules,
            boolean valid,
            String firstError,
            int metCount,
            int totalRules,
            int strengthPercent,
            int strength,
            String strengthLabel
    ) {
    }
}
