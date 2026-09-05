package tz.co.chambaka.school.management.dto.school;

public record BrandingResponse(
        Long id,
        String name,
        String slug,
        String logoUrl,
        String faviconUrl,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String timezone,
        String locale,
        String currency,
        String country
) {
}
