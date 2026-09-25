package tz.co.chambaka.school.management.dto.academic;

import tz.co.chambaka.school.management.model.enums.AssessmentComponent;
import tz.co.chambaka.school.management.model.enums.ExamApprovalStatus;
import tz.co.chambaka.school.management.model.enums.ExamType;

import java.time.LocalDate;
import java.util.List;

public record ExamResponse(
        Long id,
        Long academicYearId,
        String academicYearName,
        boolean currentYear,
        Long academicTermId,
        String academicTermName,
        Long schoolClassId,
        String schoolClassName,
        String name,
        ExamType examType,
        AssessmentComponent assessmentComponent,
        ExamApprovalStatus approvalStatus,
        LocalDate startDate,
        LocalDate endDate,
        boolean published,
        boolean scheduleLocked,
        String rejectionNote,
        List<String> subjects,
        List<String> teachers,
        boolean hasSchedule,
        boolean gradesEntered
) {
    public ExamResponse(
            Long id,
            Long academicYearId,
            Long academicTermId,
            String academicTermName,
            Long schoolClassId,
            String schoolClassName,
            String name,
            ExamType examType,
            AssessmentComponent assessmentComponent,
            ExamApprovalStatus approvalStatus,
            LocalDate startDate,
            LocalDate endDate,
            boolean published,
            boolean scheduleLocked
    ) {
        this(id, academicYearId, null, false, academicTermId, academicTermName, schoolClassId, schoolClassName, name,
                examType, assessmentComponent, approvalStatus, startDate, endDate, published, scheduleLocked, null,
                List.of(), List.of(), false, false);
    }
}
