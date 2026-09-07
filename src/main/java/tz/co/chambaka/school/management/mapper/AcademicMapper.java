package tz.co.chambaka.school.management.mapper;

import tz.co.chambaka.school.management.dto.academic.AcademicYearResponse;
import tz.co.chambaka.school.management.dto.academic.DepartmentResponse;
import tz.co.chambaka.school.management.dto.academic.SubjectResponse;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.model.Department;
import tz.co.chambaka.school.management.model.Subject;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AcademicMapper {

    AcademicYearResponse toYear(AcademicYear year);

    SubjectResponse toSubject(Subject subject);

    DepartmentResponse toDepartment(Department department);
}
