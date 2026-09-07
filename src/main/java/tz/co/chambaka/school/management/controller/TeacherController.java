package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.teacher.CreateTeacherRequest;
import tz.co.chambaka.school.management.dto.teacher.TeacherResponse;
import tz.co.chambaka.school.management.dto.teacher.UpdateTeacherRequest;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.StoredPhoto;
import tz.co.chambaka.school.management.service.TeacherService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/teachers")
@Tag(name = "Teachers")
public class TeacherController {

    private final TeacherService teacherService;
    private final TenantResolver tenantResolver;

    public TeacherController(TeacherService teacherService, TenantResolver tenantResolver) {
        this.teacherService = teacherService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public PageResponse<TeacherResponse> list(
            @RequestParam(defaultValue = "false") boolean archived,
            Pageable pageable
    ) {
        return teacherService.list(tenantResolver.requireSchoolId(), archived, pageable);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TEACHER')")
    public TeacherResponse me(@CurrentUser UserPrincipal principal) {
        return teacherService.get(tenantResolver.requireSchoolId(), teacherService.requireByUser(principal.getId()).getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public TeacherResponse get(@PathVariable Long id) {
        return teacherService.get(tenantResolver.requireSchoolId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherResponse create(@Valid @RequestBody CreateTeacherRequest request) {
        return teacherService.create(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTeacherRequest request) {
        return teacherService.update(tenantResolver.requireSchoolId(), id, request);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherResponse archive(@PathVariable Long id) {
        return teacherService.archive(tenantResolver.requireSchoolId(), id);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherResponse restore(@PathVariable Long id) {
        return teacherService.restore(tenantResolver.requireSchoolId(), id);
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherResponse uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return teacherService.uploadPhoto(tenantResolver.requireSchoolId(), id, file);
    }

    @DeleteMapping("/{id}/photo")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherResponse deletePhoto(@PathVariable Long id) {
        return teacherService.deletePhoto(tenantResolver.requireSchoolId(), id);
    }

    @GetMapping("/me/photo")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Resource> myPhoto(@CurrentUser UserPrincipal principal) {
        Long teacherId = teacherService.requireByUser(principal.getId()).getId();
        return toPhotoResponse(teacherService.photoFile(tenantResolver.requireSchoolId(), teacherId));
    }

    @GetMapping("/{id}/photo")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public ResponseEntity<Resource> photo(@CurrentUser UserPrincipal principal, @PathVariable Long id) {
        if (principal.getRole() == Role.TEACHER
                && !teacherService.requireByUser(principal.getId()).getId().equals(id)) {
            throw new AccessDeniedException("Not allowed to view this photo");
        }
        return toPhotoResponse(teacherService.photoFile(tenantResolver.requireSchoolId(), id));
    }

    private static ResponseEntity<Resource> toPhotoResponse(StoredPhoto photo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-cache")
                .body(new FileSystemResource(photo.path()));
    }
}
