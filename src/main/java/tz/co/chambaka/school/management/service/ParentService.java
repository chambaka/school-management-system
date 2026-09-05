package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.parent.CreateParentRequest;
import tz.co.chambaka.school.management.dto.parent.LinkParentRequest;
import tz.co.chambaka.school.management.dto.parent.ParentResponse;
import tz.co.chambaka.school.management.dto.parent.StudentParentResponse;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Parent;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentParent;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.ParentRepository;
import tz.co.chambaka.school.management.repository.StudentParentRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ParentService {

    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;
    private final UserAccountService userAccountService;
    private final StudentService studentService;

    public ParentService(
            ParentRepository parentRepository,
            StudentParentRepository studentParentRepository,
            UserAccountService userAccountService,
            StudentService studentService
    ) {
        this.parentRepository = parentRepository;
        this.studentParentRepository = studentParentRepository;
        this.userAccountService = userAccountService;
        this.studentService = studentService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ParentResponse> list(Long schoolId, Pageable pageable) {
        return PageResponse.of(parentRepository.findBySchoolId(schoolId, pageable).map(this::toResponse));
    }

    @Transactional
    public ParentResponse create(Long schoolId, CreateParentRequest request) {
        User user = userAccountService.create(
                schoolId, request.name(), request.email(), request.password(), Role.PARENT, request.phone());
        Parent parent = new Parent();
        parent.setSchoolId(schoolId);
        parent.setUser(user);
        parent.setOccupation(request.occupation());
        parent.setAddress(request.address());
        return toResponse(parentRepository.save(parent));
    }

    @Transactional
    public StudentParentResponse link(Long schoolId, Long studentId, LinkParentRequest request) {
        Student student = studentService.require(schoolId, studentId);
        Parent parent = require(schoolId, request.parentId());
        if (studentParentRepository.existsByStudentIdAndParentId(student.getId(), parent.getId())) {
            throw new DuplicateResourceException("Parent is already linked to this student");
        }
        StudentParent link = new StudentParent();
        link.setSchoolId(schoolId);
        link.setStudent(student);
        link.setParent(parent);
        link.setRelationship(request.relationship());
        link.setPrimaryContact(request.primaryContact());
        return toLink(studentParentRepository.save(link));
    }

    @Transactional(readOnly = true)
    public List<StudentParentResponse> listByStudent(Long schoolId, Long studentId) {
        studentService.require(schoolId, studentId);
        return studentParentRepository.findByStudentId(studentId).stream().map(this::toLink).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentParentResponse> listByParent(Long parentId) {
        return studentParentRepository.findByParentId(parentId).stream().map(this::toLink).toList();
    }

    public Parent require(Long schoolId, Long id) {
        return parentRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Parent", id));
    }

    public Parent requireByUser(Long userId) {
        return parentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent profile not found for current user"));
    }

    public void assertLinked(Long parentUserId, Long studentId) {
        Parent parent = requireByUser(parentUserId);
        boolean linked = studentParentRepository.findByParentId(parent.getId()).stream()
                .anyMatch(link -> link.getStudent().getId().equals(studentId));
        if (!linked) {
            throw new ResourceNotFoundException("Student is not linked to this parent");
        }
    }

    private ParentResponse toResponse(Parent parent) {
        User user = parent.getUser();
        return new ParentResponse(
                parent.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                parent.getOccupation(),
                parent.getAddress(),
                user.isEnabled()
        );
    }

    private StudentParentResponse toLink(StudentParent link) {
        return new StudentParentResponse(
                link.getId(),
                link.getStudent().getId(),
                link.getStudent().getUser().getName(),
                link.getParent().getId(),
                link.getParent().getUser().getName(),
                link.getRelationship(),
                link.isPrimaryContact()
        );
    }
}
