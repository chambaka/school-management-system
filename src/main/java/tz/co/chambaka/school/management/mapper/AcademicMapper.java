package tz.co.chambaka.school.management.mapper;

import tz.co.chambaka.school.management.dto.academic.AcademicYearResponse;
import tz.co.chambaka.school.management.dto.academic.ClassroomResponse;
import tz.co.chambaka.school.management.dto.academic.DepartmentResponse;
import tz.co.chambaka.school.management.dto.academic.SubjectResponse;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.model.Classroom;
import tz.co.chambaka.school.management.model.Department;
import tz.co.chambaka.school.management.model.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AcademicMapper {

    AcademicYearResponse toYear(AcademicYear year);

    SubjectResponse toSubject(Subject subject);

    DepartmentResponse toDepartment(Department department);

    @Mapping(target = "buildingId", source = "site.id")
    @Mapping(target = "building", expression = "java(classroom.getSite() != null ? classroom.getSite().getName() : classroom.getBuilding())")
    ClassroomResponse toClassroom(Classroom classroom);
}
