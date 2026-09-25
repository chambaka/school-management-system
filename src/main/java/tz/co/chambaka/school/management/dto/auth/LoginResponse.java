package tz.co.chambaka.school.management.dto.auth;

public record LoginResponse(
        boolean twoFactorRequired,
        boolean setupRequired,
        String pendingToken,
        String totpSecret,
        String otpauthUri,
        String accessToken,
        String refreshToken,
        Long expiresIn,
        String tokenType,
        UserProfileResponse user
) {

    public static LoginResponse authenticated(AuthResponse tokens) {
        return new LoginResponse(
                false,
                false,
                null,
                null,
                null,
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.expiresIn(),
                tokens.tokenType(),
                tokens.user());
    }

    public static LoginResponse challenge(
            String pendingToken,
            boolean setupRequired,
            String totpSecret,
            String otpauthUri
    ) {
        return new LoginResponse(
                true,
                setupRequired,
                pendingToken,
                totpSecret,
                otpauthUri,
                null,
                null,
                null,
                null,
                null);
    }
}
