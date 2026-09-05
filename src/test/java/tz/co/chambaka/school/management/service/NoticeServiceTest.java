package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.notice.NoticeRequest;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Notice;
import tz.co.chambaka.school.management.model.enums.NoticeAudience;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.NoticeRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClassService classService;
    @InjectMocks
    private NoticeService service;

    @Test
    void createUpdateList() {
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(userRepository.findById(2L)).thenReturn(Optional.of(Fixtures.user(2L, Role.ADMIN)));
        when(noticeRepository.save(any(Notice.class))).thenAnswer(inv -> {
            Notice saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        NoticeRequest req = new NoticeRequest("Hi", "Body", NoticeAudience.ALL, 1L, true, Instant.now(), null);
        assertThat(service.create(1L, req, 2L).createdByName()).isEqualTo("User ADMIN");

        Notice notice = notice();
        when(noticeRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(notice));
        NoticeRequest update = new NoticeRequest("Bye", "X", NoticeAudience.STUDENTS, null, false, null, Instant.now());
        assertThat(service.update(1L, 1L, update).schoolClassId()).isNull();
        assertThat(notice.getTitle()).isEqualTo("Bye");

        when(noticeRepository.findBySchoolIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notice));
        assertThat(service.listForAdmin(1L)).hasSize(1);

        when(noticeRepository.findBySchoolIdAndPublishedTrueAndAudienceInOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(List.of(notice));
        service.listForAudience(1L, Role.TEACHER);
        service.listForAudience(1L, Role.STUDENT);
        service.listForAudience(1L, Role.PARENT);
        service.listForAudience(1L, Role.ADMIN);
        verify(noticeRepository, org.mockito.Mockito.times(4))
                .findBySchoolIdAndPublishedTrueAndAudienceInOrderByCreatedAtDesc(eq(1L), any());
    }

    @Test
    void createWithoutClassOrCreator() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        when(noticeRepository.save(any(Notice.class))).thenAnswer(inv -> inv.getArgument(0));
        NoticeRequest req = new NoticeRequest("Hi", "Body", NoticeAudience.ALL, null, false, null, null);
        assertThat(service.create(1L, req, 9L).createdByName()).isNull();
        when(noticeRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private Notice notice() {
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setTitle("Hi");
        notice.setContent("Body");
        notice.setAudience(NoticeAudience.ALL);
        notice.setPublished(true);
        notice.setSchoolClass(Fixtures.schoolClass());
        notice.setCreatedBy(Fixtures.user(2L, Role.ADMIN));
        return notice;
    }
}
