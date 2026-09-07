package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.repository.CampusRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusServiceTest {

    @Mock
    private CampusRepository campusRepository;
    @InjectMocks
    private CampusService campusService;

    @Test
    void requirePrimaryAndSchoolWorkspace() {
        when(campusRepository.findFirstBySchoolIdAndPrimaryCampusTrue(1L)).thenReturn(Optional.of(Fixtures.campus()));
        assertThat(campusService.requirePrimary(1L).getId()).isEqualTo(20L);
        when(campusRepository.findFirstBySchoolIdAndPrimaryCampusTrue(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> campusService.requirePrimary(2L)).isInstanceOf(ResourceNotFoundException.class);

        when(campusRepository.findByIdAndSchoolId(20L, 1L)).thenReturn(Optional.of(Fixtures.campus()));
        assertThat(campusService.requireInSchool(20L, 1L).getSchoolId()).isEqualTo(1L);
        when(campusRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> campusService.requireInSchool(9L, 1L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
