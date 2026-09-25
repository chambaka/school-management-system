package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.BuildingRequest;
import tz.co.chambaka.school.management.dto.academic.BuildingResponse;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.service.BuildingService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/buildings")
@Tag(name = "Buildings")
public class BuildingController {

    private final BuildingService buildingService;
    private final TenantResolver tenantResolver;

    public BuildingController(BuildingService buildingService, TenantResolver tenantResolver) {
        this.buildingService = buildingService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize(Access.ACADEMIC_STAFF)
    public List<BuildingResponse> list() {
        return buildingService.list(tenantResolver.requireSchoolId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.BUILDING_MANAGE)
    public BuildingResponse create(@Valid @RequestBody BuildingRequest request) {
        return buildingService.create(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Access.BUILDING_MANAGE)
    public BuildingResponse update(@PathVariable Long id, @Valid @RequestBody BuildingRequest request) {
        return buildingService.update(tenantResolver.requireSchoolId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.BUILDING_MANAGE)
    public void delete(@PathVariable Long id) {
        buildingService.delete(tenantResolver.requireSchoolId(), id);
    }
}
