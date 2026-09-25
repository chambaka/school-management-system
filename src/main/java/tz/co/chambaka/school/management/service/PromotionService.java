package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.student.EnrolmentHistoryResponse;
import tz.co.chambaka.school.management.dto.student.PromoteStudentsRequest;
import tz.co.chambaka.school.management.dto.student.PromotionPreviewResponse;
import tz.co.chambaka.school.management.dto.student.StudentResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.model.SchoolClass;
import tz.co.chambaka.school.management.model.Section;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentEnrolment;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.PromotionAction;
import tz.co.chambaka.school.management.model.enums.StudentStatus;
import tz.co.chambaka.school.management.repository.StudentEnrolmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PromotionService {

    private final StudentService studentService;
    private final AcademicYearService academicYearService;
    private final ClassService classService;
    private final SectionService sectionService;
    private final StudentEnrolmentRepository enrolmentRepository;

    public PromotionService(
            StudentService studentService,
            AcademicYearService academicYearService,
            ClassService classService,
            SectionService sectionService,
            StudentEnrolmentRepository enrolmentRepository
    ) {
        this.studentService = studentService;
        this.academicYearService = academicYearService;
        this.classService = classService;
        this.sectionService = sectionService;
        this.enrolmentRepository = enrolmentRepository;
    }

    @Transactional(readOnly = true)
    public List<PromotionPreviewResponse> preview(Long schoolId, PromoteStudentsRequest request) {
        if (request.action() == PromotionAction.PROMOTE && request.schoolClassId() == null) {
            throw new BusinessException("Choose the next class for promotion");
        }
        SchoolClass destinationClass = request.schoolClassId() == null ? null : classService.require(schoolId, request.schoolClassId());
        Section destinationSection = request.sectionId() == null ? null : sectionService.require(schoolId, request.sectionId());
        List<PromotionPreviewResponse> rows = new ArrayList<>();
        for (Long studentId : request.studentIds()) {
            Student student = studentService.require(schoolId, studentId);
            AcademicYear destinationYear = resolveYear(schoolId, student, request);
            rows.add(new PromotionPreviewResponse(
                    student.getId(),
                    student.getUser().getName(),
                    student.getAdmissionNo(),
                    request.action(),
                    student.getAcademicYear() == null ? null : student.getAcademicYear().getName(),
                    student.getSchoolClass() == null ? null : student.getSchoolClass().getName(),
                    student.getSection() == null ? null : student.getSection().getName(),
                    destinationYear == null ? null : destinationYear.getName(),
                    destinationClass == null ? (student.getSchoolClass() == null ? null : student.getSchoolClass().getName())
                            : destinationClass.getName(),
                    destinationSection == null ? (student.getSection() == null ? null : student.getSection().getName())
                            : destinationSection.getName()
            ));
        }
        return rows;
    }

    @Transactional
    public List<StudentResponse> promote(Long schoolId, PromoteStudentsRequest request) {
        if (request.action() == PromotionAction.PROMOTE && request.schoolClassId() == null) {
            throw new BusinessException("Choose the next class for promotion");
        }
        List<StudentResponse> updated = new ArrayList<>();
        for (Long studentId : request.studentIds()) {
            Student student = studentService.require(schoolId, studentId);
            AcademicYear destinationYear = resolveYear(schoolId, student, request);
            AcademicYear historyYear = student.getAcademicYear() != null ? student.getAcademicYear() : destinationYear;
            if (historyYear == null) {
                throw new BusinessException("Create an academic year before recording enrolment history");
            }
            StudentEnrolment history = new StudentEnrolment();
            history.setSchoolId(schoolId);
            history.setStudent(student);
            history.setAcademicYear(historyYear);
            history.setSchoolClass(student.getSchoolClass());
            history.setSection(student.getSection());
            history.setAction(request.action());
            history.setEffectiveDate(LocalDate.now());
            history.setNotes(request.notes());
            enrolmentRepository.save(history);
            switch (request.action()) {
                case GRADUATE -> studentService.setLifecycle(schoolId, studentId, StudentStatus.GRADUATED);
                case TRANSFER -> studentService.setLifecycle(schoolId, studentId, StudentStatus.TRANSFERRED);
                case REPEAT, PROMOTE -> {
                    if (destinationYear != null) {
                        student.setAcademicYear(destinationYear);
                    }
                    if (request.schoolClassId() != null) {
                        student.setSchoolClass(classService.require(schoolId, request.schoolClassId()));
                    }
                    if (request.sectionId() != null) {
                        student.setSection(sectionService.require(schoolId, request.sectionId()));
                    }
                    student.setStatus(StudentStatus.ACTIVE);
                    student.getUser().setEnabled(true);
                }
            }
            updated.add(studentService.get(schoolId, studentId));
        }
        return updated;
    }

    @Transactional(readOnly = true)
    public List<EnrolmentHistoryResponse> history(Long schoolId, Long studentId, Long academicYearId, PromotionAction action) {
        if (studentId != null) {
            studentService.require(schoolId, studentId);
        }
        return enrolmentRepository.search(schoolId, studentId, academicYearId, action)
                .stream()
                .map(PromotionService::toResponse)
                .toList();
    }

    private AcademicYear resolveYear(Long schoolId, Student student, PromoteStudentsRequest request) {
        if (request.academicYearId() != null) {
            return academicYearService.require(schoolId, request.academicYearId());
        }
        if (request.action() == PromotionAction.REPEAT) {
            return academicYearService.nextAfter(schoolId, student.getAcademicYear());
        }
        return student.getAcademicYear();
    }

    private static EnrolmentHistoryResponse toResponse(StudentEnrolment enrolment) {
        Student student = enrolment.getStudent();
        User user = student == null ? null : student.getUser();
        AcademicYear year = enrolment.getAcademicYear();
        SchoolClass schoolClass = enrolment.getSchoolClass();
        Section section = enrolment.getSection();
        return new EnrolmentHistoryResponse(
                enrolment.getId(),
                student == null ? null : student.getId(),
                user == null ? null : user.getName(),
                student == null ? null : student.getAdmissionNo(),
                enrolment.getAction(),
                enrolment.getEffectiveDate(),
                year == null ? null : year.getId(),
                year == null ? null : year.getName(),
                schoolClass == null ? null : schoolClass.getId(),
                schoolClass == null ? null : schoolClass.getName(),
                section == null ? null : section.getId(),
                section == null ? null : section.getName(),
                enrolment.getNotes()
        );
    }
}
