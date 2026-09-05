package tz.co.chambaka.school.management.mapper;

import tz.co.chambaka.school.management.dto.school.BrandingResponse;
import tz.co.chambaka.school.management.dto.school.SchoolResponse;
import tz.co.chambaka.school.management.model.School;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SchoolMapper {

    SchoolResponse toResponse(School school);

    BrandingResponse toBranding(School school);
}
