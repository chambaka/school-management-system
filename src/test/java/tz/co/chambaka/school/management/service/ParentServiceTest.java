package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.parent.CreateParentRequest;
import tz.co.chambaka.school.management.dto.parent.LinkParentRequest;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Parent;
import tz.co.chambaka.school.management.model.StudentParent;
import tz.co.chambaka.school.management.model.enums.RelationshipType;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.ParentRepository;
import tz.co.chambaka.school.management.repository.StudentParentRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentServiceTest {

    @Mock
    private ParentRepository parentRepository;
    @Mock
    private StudentParentRepository studentParentRepository;
    @Mock
    private UserAccountService userAccountService;
    @Mock
    private StudentService studentService;
    @InjectMocks
    private ParentService service;

    @Test
    void listCreateLinkAndLookups() {
        Parent parent = Fixtures.parent();
        when(parentRepository.findBySchoolId(1L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(parent)));
        assertThat(service.list(1L, PageRequest.of(0, 10)).content()).hasSize(1);

        when(userAccountService.create(1L, "Mama", "m@x.com", "pw", Role.PARENT, "07"))
                .thenReturn(Fixtures.user(5L, Role.PARENT));
        when(parentRepository.save(any(Parent.class))).thenAnswer(inv -> {
            Parent saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        assertThat(service.create(1L, new CreateParentRequest("Mama", "m@x.com", "pw", "07", "Trader", "addr"))
                .occupation()).isEqualTo("Trader");

        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(parentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(parent));
        when(studentParentRepository.existsByStudentIdAndParentId(1L, 1L)).thenReturn(false);
        when(studentParentRepository.save(any(StudentParent.class))).thenAnswer(inv -> {
            StudentParent saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        assertThat(service.link(1L, 1L, new LinkParentRequest(1L, RelationshipType.MOTHER, true)).parentName())
                .isEqualTo("User PARENT");

        StudentParent link = new StudentParent();
        link.setId(1L);
        link.setStudent(Fixtures.student());
        link.setParent(parent);
        link.setRelationship(RelationshipType.MOTHER);
        when(studentParentRepository.findByStudentId(1L)).thenReturn(List.of(link));
        when(studentParentRepository.findByParentId(1L)).thenReturn(List.of(link));
        assertThat(service.listByStudent(1L, 1L)).hasSize(1);
        assertThat(service.listByParent(1L)).hasSize(1);

        when(parentRepository.findByUserId(5L)).thenReturn(Optional.of(parent));
        service.assertLinked(5L, 1L);
        assertThat(service.requireByUser(5L)).isSameAs(parent);
    }

    @Test
    void errors() {
        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(parentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(Fixtures.parent()));
        when(studentParentRepository.existsByStudentIdAndParentId(1L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.link(1L, 1L, new LinkParentRequest(1L, RelationshipType.FATHER, false)))
                .isInstanceOf(DuplicateResourceException.class);
        when(parentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
        when(parentRepository.findByUserId(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireByUser(8L)).isInstanceOf(ResourceNotFoundException.class);
        when(parentRepository.findByUserId(5L)).thenReturn(Optional.of(Fixtures.parent()));
        when(studentParentRepository.findByParentId(1L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.assertLinked(5L, 1L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
