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

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findFirstByPhone(String phone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    Optional<User> findByIdAndSchoolId(Long id, Long schoolId);

    long countByRole(Role role);

    Page<User> findByRole(Role role, Pageable pageable);

    List<User> findByRole(Role role);

    List<User> findByTenantId(Long tenantId);

    List<User> findByTenantIdAndRoleOrderByNameAsc(Long tenantId, Role role);

    List<User> findBySchoolId(Long schoolId);

    List<User> findBySchoolIdIn(Collection<Long> schoolIds);

    Page<User> findBySchoolIdAndRole(Long schoolId, Role role, Pageable pageable);

    Page<User> findBySchoolIdAndRoleIn(Long schoolId, Collection<Role> roles, Pageable pageable);

    Page<User> findBySchoolIdAndRoleNot(Long schoolId, Role role, Pageable pageable);

    Optional<User> findByIdAndSchoolIdAndRole(Long id, Long schoolId, Role role);

    Optional<User> findByIdAndSchoolIdAndRoleIn(Long id, Long schoolId, Collection<Role> roles);
}
