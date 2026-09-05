package tz.co.chambaka.school.management.mapper;

import tz.co.chambaka.school.management.dto.campus.CampusResponse;
import tz.co.chambaka.school.management.model.Campus;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CampusMapper {

    CampusResponse toResponse(Campus campus);
}
