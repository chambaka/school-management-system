package tz.co.chambaka.school.management.mapper;

import tz.co.chambaka.school.management.dto.auth.UserProfileResponse;
import tz.co.chambaka.school.management.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserProfileResponse toProfile(User user);
}
