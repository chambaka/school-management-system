package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByIdAndSchoolId(Long id, Long schoolId);

    long countByRole(Role role);

    List<User> findByRole(Role role);

    List<User> findByTenantId(Long tenantId);

    List<User> findBySchoolId(Long schoolId);

    List<User> findBySchoolIdIn(Collection<Long> schoolIds);

    Page<User> findBySchoolIdAndRole(Long schoolId, Role role, Pageable pageable);

    Optional<User> findByIdAndSchoolIdAndRole(Long id, Long schoolId, Role role);
}
