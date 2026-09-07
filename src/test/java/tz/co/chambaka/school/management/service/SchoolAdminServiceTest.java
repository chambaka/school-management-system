package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.admin.CreateSchoolAdminRequest;
import tz.co.chambaka.school.management.dto.admin.UpdateSchoolAdminRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.UserRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserAccountService userAccountService;
    @Mock
    private CampusService campusService;
    @InjectMocks
    private SchoolAdminService service;

    @Test
    void listGetCreateUpdate() {
        User admin = Fixtures.user(5L, Role.ADMIN);
        when(userRepository.findBySchoolIdAndRole(1L, Role.ADMIN, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(admin)));
        assertThat(service.list(1L, PageRequest.of(0, 10)).totalElements()).isEqualTo(1);

        when(userRepository.findByIdAndSchoolIdAndRole(5L, 1L, Role.ADMIN)).thenReturn(Optional.of(admin));
        assertThat(service.get(1L, 5L).email()).isEqualTo(admin.getEmail());

        when(userAccountService.create(1L, "Asha", "asha@x.com", "HaloCampus1!", Role.ADMIN, "07"))
                .thenReturn(admin);
        when(campusService.requirePrimary(1L)).thenReturn(Fixtures.campus());
        var created = service.create(1L, new CreateSchoolAdminRequest("Asha", "asha@x.com", "HaloCampus1!", "07"));
        assertThat(created.id()).isEqualTo(5L);
        assertThat(admin.getCampusId()).isEqualTo(Fixtures.CAMPUS_ID);

        service.update(1L, 5L, new UpdateSchoolAdminRequest("New", "08", false));
        assertThat(admin.getName()).isEqualTo("New");
        assertThat(admin.isEnabled()).isFalse();
    }

    @Test
    void createRejectsWeakPasswordAndMissingAdmin() {
        assertThatThrownBy(() -> service.create(1L, new CreateSchoolAdminRequest("Asha", "asha@x.com", "weak", null)))
                .isInstanceOf(BusinessException.class);
        when(userRepository.findByIdAndSchoolIdAndRole(9L, 1L, Role.ADMIN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
