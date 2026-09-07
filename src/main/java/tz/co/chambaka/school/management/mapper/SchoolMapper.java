package tz.co.chambaka.school.management.mapper;

import tz.co.chambaka.school.management.dto.school.BrandingResponse;
import tz.co.chambaka.school.management.dto.school.SchoolResponse;
import tz.co.chambaka.school.management.model.School;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SchoolMapper {

    @Mapping(target = "tenantName", ignore = true)
    SchoolResponse toResponse(School school);

    BrandingResponse toBranding(School school);
}
