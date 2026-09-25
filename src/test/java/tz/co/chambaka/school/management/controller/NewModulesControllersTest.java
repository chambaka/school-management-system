package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.curriculum.CurriculumController;
import tz.co.chambaka.school.management.dto.academic.BuildingRequest;
import tz.co.chambaka.school.management.dto.teacher.QualificationRequest;
import tz.co.chambaka.school.management.service.BuildingService;
import tz.co.chambaka.school.management.service.QualificationService;
import tz.co.chambaka.school.management.curriculum.CurriculumService;
import tz.co.chambaka.school.management.curriculum.CurriculumTopicRequest;
import tz.co.chambaka.school.management.ledger.LedgerController;
import tz.co.chambaka.school.management.ledger.LedgerService;
import tz.co.chambaka.school.management.notification.NotificationPreferenceRequest;
import tz.co.chambaka.school.management.notification.NotificationSettingsController;
import tz.co.chambaka.school.management.notification.NotificationSettingsService;
import tz.co.chambaka.school.management.notification.NotificationTemplateRequest;
import tz.co.chambaka.school.management.permission.PermissionController;
import tz.co.chambaka.school.management.permission.PermissionService;
import tz.co.chambaka.school.management.push.PushController;
import tz.co.chambaka.school.management.push.PushService;
import tz.co.chambaka.school.management.push.PushSubscribeRequest;
import tz.co.chambaka.school.management.solver.TeacherAvailabilityController;
import tz.co.chambaka.school.management.solver.TeacherAvailabilityRequest;
import tz.co.chambaka.school.management.solver.TeacherAvailabilityService;
import tz.co.chambaka.school.management.support.Fixtures;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewModulesControllersTest {

    @Mock TenantResolver tenantResolver;
    @Mock CurriculumService curriculumService;
    @Mock LedgerService ledgerService;
    @Mock PermissionService permissionService;
    @Mock NotificationSettingsService settingsService;
    @Mock PushService pushService;
    @Mock TeacherAvailabilityService availabilityService;
    @Mock BuildingService buildingService;
    @Mock QualificationService qualificationService;

    @BeforeEach
    void school() {
        org.mockito.Mockito.lenient().when(tenantResolver.requireSchoolId()).thenReturn(1L);
    }

    @Test
    void wiresNewModuleEndpoints() {
        var head = Fixtures.principal(tz.co.chambaka.school.management.model.enums.Role.HEADMASTER);
        BuildingController buildings = new BuildingController(buildingService, tenantResolver);
        buildings.list();
        buildings.create(new BuildingRequest("Block A", null));
        buildings.update(3L, new BuildingRequest("Block B", "Science"));
        buildings.delete(3L);
        verify(buildingService).create(eq(1L), any());
        verify(buildingService).update(eq(1L), eq(3L), any());
        verify(buildingService).delete(1L, 3L);

        QualificationController qualifications = new QualificationController(qualificationService);
        qualifications.list();
        qualifications.create(new QualificationRequest("Diploma", 1));
        qualifications.update(4L, new QualificationRequest("Degree", 2));
        qualifications.delete(4L);
        verify(qualificationService).create(any());
        verify(qualificationService).update(eq(4L), any());
        verify(qualificationService).delete(4L);

        CurriculumController curriculum = new CurriculumController(curriculumService, tenantResolver);
        curriculum.list(null);
        curriculum.create(new CurriculumTopicRequest(1L, 1L, "Topic", "Obj", 1));
        curriculum.update(2L, new CurriculumTopicRequest(1L, 1L, "Topic", "Obj", 1));
        curriculum.delete(2L);
        verify(curriculumService).create(eq(1L), any());
        verify(curriculumService).update(eq(1L), eq(2L), any());
        verify(curriculumService).delete(1L, 2L);

        LedgerController ledger = new LedgerController(ledgerService, tenantResolver);
        ledger.student(1L);
        verify(ledgerService).list(1L, 1L);

        PermissionController permissions = new PermissionController(permissionService);
        when(permissionService.codesFor(head.getRole())).thenReturn(List.of("people.manage"));
        assertThat(permissions.mine(head)).contains("people.manage");

        NotificationSettingsController settings = new NotificationSettingsController(settingsService, tenantResolver);
        settings.templates();
        settings.saveTemplate(new NotificationTemplateRequest("ATTENDANCE", null, "S", "B"));
        settings.preferences(head);
        settings.savePreference(head, new NotificationPreferenceRequest("ATTENDANCE", true, false, true, false));
        verify(settingsService).savePreference(eq(head.getId()), any());

        PushController push = new PushController(pushService);
        when(pushService.publicKey()).thenReturn("key");
        assertThat(push.publicKey()).containsEntry("publicKey", "key");
        push.subscribe(head, new PushSubscribeRequest("e", "k", "a"));
        push.unsubscribe(head, "e");
        verify(pushService).unsubscribe(head.getId(), "e");

        TeacherAvailabilityController availability = new TeacherAvailabilityController(availabilityService, tenantResolver);
        availability.list(1L);
        availability.create(new TeacherAvailabilityRequest(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        verify(availabilityService).create(eq(1L), any());
    }
}
