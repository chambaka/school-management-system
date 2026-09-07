package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.parent.CreateParentRequest;
import tz.co.chambaka.school.management.dto.parent.LinkParentRequest;
import tz.co.chambaka.school.management.dto.parent.ParentResponse;
import tz.co.chambaka.school.management.dto.parent.StudentParentResponse;
import tz.co.chambaka.school.management.dto.parent.UpdateParentRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Parent;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentParent;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.ParentStatus;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.model.enums.StudentStatus;
import tz.co.chambaka.school.management.repository.ParentRepository;
import tz.co.chambaka.school.management.repository.StudentParentRepository;
import tz.co.chambaka.school.management.sms.PhoneNumbers;
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
    public PageResponse<ParentResponse> list(Long schoolId, boolean archived, Pageable pageable) {
        return PageResponse.of(parentRepository
                .search(schoolId, archived, ParentStatus.ARCHIVED, pageable)
                .map(this::toResponse));
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
        parent.setStatus(ParentStatus.ACTIVE);
        return toResponse(parentRepository.save(parent));
    }

    @Transactional(readOnly = true)
    public ParentResponse get(Long schoolId, Long id) {
        return toResponse(require(schoolId, id));
    }

    @Transactional
    public ParentResponse update(Long schoolId, Long id, UpdateParentRequest request) {
        Parent parent = require(schoolId, id);
        User user = parent.getUser();
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.phone() != null) {
            user.setPhone(PhoneNumbers.persist(request.phone()));
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.occupation() != null) {
            parent.setOccupation(request.occupation());
        }
        if (request.address() != null) {
            parent.setAddress(request.address());
        }
        return toResponse(parent);
    }

    @Transactional
    public ParentResponse archive(Long schoolId, Long id) {
        return setStatus(schoolId, id, ParentStatus.ARCHIVED);
    }

    @Transactional
    public ParentResponse restore(Long schoolId, Long id) {
        return setStatus(schoolId, id, ParentStatus.ACTIVE);
    }

    @Transactional
    public StudentParentResponse link(Long schoolId, Long studentId, LinkParentRequest request) {
        Student student = studentService.require(schoolId, studentId);
        Parent parent = require(schoolId, request.parentId());
        if (StudentService.statusOf(student) == StudentStatus.ARCHIVED) {
            throw new BusinessException("Restore this student first");
        }
        if (statusOf(parent) == ParentStatus.ARCHIVED) {
            throw new BusinessException("Restore this parent first");
        }
        if (studentParentRepository.existsByStudentId(student.getId())) {
            throw new DuplicateResourceException("This student already has a parent");
        }
        StudentParent link = new StudentParent();
        link.setSchoolId(schoolId);
        link.setStudent(student);
        link.setParent(parent);
        link.setRelationship(request.relationship());
        link.setPrimaryContact(request.primaryContact());
        return toLink(studentParentRepository.save(link));
    }

    @Transactional
    public void unlink(Long schoolId, Long studentId, Long parentId) {
        studentService.require(schoolId, studentId);
        require(schoolId, parentId);
        StudentParent link = studentParentRepository.findByStudentIdAndParentId(studentId, parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent is not linked to this student"));
        studentParentRepository.delete(link);
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

    @Transactional(readOnly = true)
    public List<StudentParentResponse> listChildren(Long schoolId, Long parentId) {
        require(schoolId, parentId);
        return listByParent(parentId);
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
                user.isEnabled(),
                statusOf(parent),
                studentParentRepository.findByParentId(parent.getId()).stream().map(this::toLink).toList()
        );
    }

    private ParentResponse setStatus(Long schoolId, Long id, ParentStatus status) {
        Parent parent = require(schoolId, id);
        parent.setStatus(status);
        parent.getUser().setEnabled(status == ParentStatus.ACTIVE);
        return toResponse(parent);
    }

    static ParentStatus statusOf(Parent parent) {
        return parent.getStatus() == null ? ParentStatus.ACTIVE : parent.getStatus();
    }

    private StudentParentResponse toLink(StudentParent link) {
        return new StudentParentResponse(
                link.getId(),
                link.getStudent().getId(),
                link.getStudent().getUser().getName(),
                link.getStudent().getAdmissionNo(),
                link.getParent().getId(),
                link.getParent().getUser().getName(),
                link.getRelationship(),
                link.isPrimaryContact()
        );
    }
}
