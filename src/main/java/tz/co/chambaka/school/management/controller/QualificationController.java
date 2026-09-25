package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.teacher.QualificationRequest;
import tz.co.chambaka.school.management.dto.teacher.QualificationResponse;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.service.QualificationService;
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
@RequestMapping("/api/v1/qualifications")
@Tag(name = "Qualifications")
public class QualificationController {

    private final QualificationService qualificationService;

    public QualificationController(QualificationService qualificationService) {
        this.qualificationService = qualificationService;
    }

    @GetMapping
    @PreAuthorize(Access.STAFF_OFFICERS)
    public List<QualificationResponse> list() {
        return qualificationService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public QualificationResponse create(@Valid @RequestBody QualificationRequest request) {
        return qualificationService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public QualificationResponse update(@PathVariable Long id, @Valid @RequestBody QualificationRequest request) {
        return qualificationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void delete(@PathVariable Long id) {
        qualificationService.delete(id);
    }
}
