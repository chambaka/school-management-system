package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.AuditActionSetting;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditActionSettingRepository extends JpaRepository<AuditActionSetting, AuditAction> {
}
