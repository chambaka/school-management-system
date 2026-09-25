package tz.co.chambaka.school.management.curriculum;

import tz.co.chambaka.school.management.security.Access;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/curriculum")
@Tag(name = "Curriculum")
public class CurriculumController {

    private final CurriculumService curriculumService;
    private final TenantResolver tenantResolver;

    public CurriculumController(CurriculumService curriculumService, TenantResolver tenantResolver) {
        this.curriculumService = curriculumService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize(Access.CURRICULUM)
    public List<CurriculumTopicResponse> list(@RequestParam(required = false) Long subjectId) {
        return curriculumService.list(tenantResolver.requireSchoolId(), subjectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.CURRICULUM)
    public CurriculumTopicResponse create(@Valid @RequestBody CurriculumTopicRequest request) {
        return curriculumService.create(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Access.CURRICULUM)
    public CurriculumTopicResponse update(@PathVariable Long id, @Valid @RequestBody CurriculumTopicRequest request) {
        return curriculumService.update(tenantResolver.requireSchoolId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.CURRICULUM)
    public void delete(@PathVariable Long id) {
        curriculumService.delete(tenantResolver.requireSchoolId(), id);
    }
}
