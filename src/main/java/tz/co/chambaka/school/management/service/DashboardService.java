package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.dashboard.DashboardResponse;
import tz.co.chambaka.school.management.model.enums.ExamApprovalStatus;
import tz.co.chambaka.school.management.model.enums.InvoiceStatus;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.model.enums.StudentStatus;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.InvoiceRepository;
import tz.co.chambaka.school.management.repository.NoticeRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.TeacherRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final InvoiceRepository invoiceRepository;
    private final NoticeRepository noticeRepository;
    private final ExamRepository examRepository;

    public DashboardService(
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            InvoiceRepository invoiceRepository,
            NoticeRepository noticeRepository,
            ExamRepository examRepository
    ) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.invoiceRepository = invoiceRepository;
        this.noticeRepository = noticeRepository;
        this.examRepository = examRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse snapshot(Long schoolId, Role role) {
        long students = studentRepository.search(schoolId, null, false, StudentStatus.ARCHIVED, PageRequest.of(0, 1)).getTotalElements();
        long teachers = teacherRepository.search(schoolId, false, tz.co.chambaka.school.management.model.enums.TeacherStatus.ARCHIVED, PageRequest.of(0, 1)).getTotalElements();
        long invoices = invoiceRepository.countBySchoolId(schoolId);
        long notices = noticeRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId).size();
        var exams = examRepository.findBySchoolIdOrderByStartDateDesc(schoolId);
        long pending = exams.stream().filter(e -> e.getApprovalStatus() == ExamApprovalStatus.ENTERED
                || e.getApprovalStatus() == ExamApprovalStatus.VERIFIED).count();
        long unpublished = exams.stream().filter(e -> !e.isPublished()).count();
        BigDecimal outstanding = invoiceRepository.findBySchoolId(schoolId, PageRequest.of(0, 200)).stream()
                .filter(i -> i.getStatus() != InvoiceStatus.CANCELLED)
                .map(i -> i.getBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> tasks = new ArrayList<>();
        if (role == Role.HEADMASTER || role == Role.ACADEMIC_MASTER) {
            if (pending > 0) {
                tasks.add(pending + " exam(s) waiting in the approval chain");
            }
            if (unpublished > 0) {
                tasks.add(unpublished + " exam(s) not yet published");
            }
        }
        if (role == Role.ACCOUNTANT || role == Role.HEADMASTER) {
            tasks.add("Outstanding fees: " + outstanding + " TZS");
        }
        if (role == Role.TEACHER) {
            tasks.add("Mark today's register and enter pending grades");
        }
        return new DashboardResponse(students, teachers, invoices, notices, exams.size(), pending, unpublished, 0, outstanding, tasks);
    }
}
