package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.student.PromoteStudentsRequest;
import tz.co.chambaka.school.management.dto.student.StudentResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentEnrolment;
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

    @Transactional
    public List<StudentResponse> promote(Long schoolId, PromoteStudentsRequest request) {
        if (request.action() == PromotionAction.PROMOTE && request.schoolClassId() == null) {
            throw new BusinessException("Choose the next class for promotion");
        }
        List<StudentResponse> updated = new ArrayList<>();
        for (Long studentId : request.studentIds()) {
            Student student = studentService.require(schoolId, studentId);
            StudentEnrolment history = new StudentEnrolment();
            history.setSchoolId(schoolId);
            history.setStudent(student);
            history.setAcademicYear(student.getAcademicYear());
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
                    Long yearId = request.academicYearId() != null ? request.academicYearId()
                            : (student.getAcademicYear() != null ? student.getAcademicYear().getId() : null);
                    if (yearId != null) {
                        student.setAcademicYear(academicYearService.require(schoolId, yearId));
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
}
