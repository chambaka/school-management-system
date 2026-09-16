package tz.co.chambaka.school.management.dto.academic;

import tz.co.chambaka.school.management.model.enums.AssessmentComponent;
import tz.co.chambaka.school.management.model.enums.ExamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExamRequest(
        @NotNull Long academicYearId,
        Long academicTermId,
        @NotNull Long schoolClassId,
        @NotBlank String name,
        @NotNull ExamType examType,
        AssessmentComponent assessmentComponent,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
    public ExamRequest(Long academicYearId, Long schoolClassId, String name, ExamType examType,
                       LocalDate startDate, LocalDate endDate) {
        this(academicYearId, null, schoolClassId, name, examType, null, startDate, endDate);
    }
}
