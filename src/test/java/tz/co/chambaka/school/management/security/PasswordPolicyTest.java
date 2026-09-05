package tz.co.chambaka.school.management.security;

import tz.co.chambaka.school.management.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPasswordAndExposesRules() {
        PasswordPolicy.Evaluation evaluation = PasswordPolicy.evaluate("HaloCampus1!", "a@b.com", "Jo");
        assertThat(evaluation.valid()).isTrue();
        assertThat(evaluation.firstError()).isNull();
        assertThat(evaluation.strengthLabel()).isEqualTo("Strong");
        assertThat(evaluation.metCount()).isEqualTo(evaluation.totalRules());
        assertThat(PasswordPolicy.definitions()).hasSize(7);
        assertThat(PasswordPolicy.SPECIAL_CHARS).isEqualTo("!@#$%^&*");
        PasswordPolicy.requireValid("HaloCampus1!", "a@b.com", "Jo");
    }

    @Test
    void evaluatesMissingCharacterClassesAndLength() {
        assertThat(PasswordPolicy.evaluate(null, null, null).valid()).isFalse();
        assertThat(PasswordPolicy.evaluate("short1!A", "a@b.com", "Jo").firstError())
                .contains("at least 10");
        assertThat(PasswordPolicy.evaluate("helloworld1!", "a@b.com", "Jo").firstError())
                .contains("uppercase");
        assertThat(PasswordPolicy.evaluate("HELLOWORLD1!", "a@b.com", "Jo").firstError())
                .contains("lowercase");
        assertThat(PasswordPolicy.evaluate("HelloWorld!!", "a@b.com", "Jo").firstError())
                .contains("number");
        assertThat(PasswordPolicy.evaluate("HelloWorld1", "a@b.com", "Jo").firstError())
                .contains("special");
    }

    @Test
    void rejectsIdentityAndCommonPasswords() {
        assertThat(PasswordPolicy.evaluate("admin@example.comX1!", "admin@example.com", "Jo").valid())
                .isFalse();
        assertThat(PasswordPolicy.evaluate("adminCampus1!", "admin@example.com", "Jo").firstError())
                .contains("username or email");
        assertThat(PasswordPolicy.evaluate("Johnsmith1!", "ab@x.com", "John Smith").firstError())
                .contains("username or email");
        assertThat(PasswordPolicy.evaluate("adaLovelace1!", "ab@x.com", "Ada Lovelace").valid())
                .isFalse();
        assertThat(PasswordPolicy.evaluate("Password1!", "ab@x.com", "Jo").firstError())
                .contains("commonly used");
        assertThatThrownBy(() -> PasswordPolicy.requireValid("secret12", "a@b.com", "A"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void ignoresShortIdentityTokensAndBlankInputs() {
        assertThat(PasswordPolicy.evaluate("HaloCampus1!", "  ", "  ").valid()).isTrue();
        assertThat(PasswordPolicy.evaluate("HaloCampus1!", "ab@x.com", "Al").valid()).isTrue();
        assertThat(PasswordPolicy.evaluate("HaloCampus1!", null, null).valid()).isTrue();
        assertThat(PasswordPolicy.evaluate("", "admin@example.com", "Admin").strengthLabel())
                .isEqualTo("Weak");
        assertThat(PasswordPolicy.evaluate("ABCDEFGHIJ", "a@b.com", "Jo").strengthLabel())
                .isEqualTo("Fair");
        assertThat(PasswordPolicy.evaluate("Abcdefghij", "a@b.com", "Jo").strengthLabel())
                .isEqualTo("Good");
        assertThat(PasswordPolicy.evaluate("Abcdefgh1!", "a@b.com", "Jo").strengthLabel())
                .isEqualTo("Strong");
    }
}
