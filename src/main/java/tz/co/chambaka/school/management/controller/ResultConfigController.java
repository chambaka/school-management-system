package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.GradingBandRequest;
import tz.co.chambaka.school.management.dto.academic.GradingBandResponse;
import tz.co.chambaka.school.management.dto.academic.ResultWeightRequest;
import tz.co.chambaka.school.management.dto.academic.ResultWeightResponse;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.service.ResultConfigService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/result-config")
@Tag(name = "Result config")
public class ResultConfigController {

    private final ResultConfigService resultConfigService;
    private final TenantResolver tenantResolver;

    public ResultConfigController(ResultConfigService resultConfigService, TenantResolver tenantResolver) {
        this.resultConfigService = resultConfigService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping("/weights")
    @PreAuthorize(Access.EXAM_MANAGE)
    public List<ResultWeightResponse> weights(@RequestParam Long academicYearId) {
        return resultConfigService.listWeights(tenantResolver.requireSchoolId(), academicYearId);
    }

    @PostMapping("/weights")
    @PreAuthorize(Access.EXAM_MANAGE)
    public ResultWeightResponse saveWeight(@Valid @RequestBody ResultWeightRequest request) {
        return resultConfigService.saveWeight(tenantResolver.requireSchoolId(), request);
    }

    @GetMapping("/grading-bands")
    @PreAuthorize(Access.GRADE_ENTER)
    public List<GradingBandResponse> bands() {
        return resultConfigService.listBands(tenantResolver.requireSchoolId());
    }

    @PutMapping("/grading-bands")
    @PreAuthorize(Access.EXAM_MANAGE)
    public List<GradingBandResponse> replaceBands(@Valid @RequestBody List<GradingBandRequest> request) {
        return resultConfigService.replaceBands(tenantResolver.requireSchoolId(), request);
    }
}
