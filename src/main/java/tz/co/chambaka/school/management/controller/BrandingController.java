package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.school.BrandingResponse;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.service.SchoolService;
import tz.co.chambaka.school.management.web.RequestHosts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/branding")
@Tag(name = "White-label branding")
public class BrandingController {

    private final SchoolService schoolService;

    public BrandingController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Resolve school branding by slug")
    public BrandingResponse bySlug(@PathVariable String slug) {
        return schoolService.brandingBySlug(slug);
    }

    @GetMapping
    @Operation(summary = "Resolve school branding by custom domain")
    public ResponseEntity<BrandingResponse> byHost(@RequestParam String host) {
        try {
            return ResponseEntity.ok(schoolService.brandingByHost(host));
        } catch (ResourceNotFoundException ex) {
            if (RequestHosts.isLoopback(host)) {
                return ResponseEntity.noContent().build();
            }
            throw ex;
        }
    }
}
