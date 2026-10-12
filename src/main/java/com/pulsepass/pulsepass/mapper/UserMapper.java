package com.pulsepass.pulsepass.mapper;

import com.pulsepass.pulsepass.dto.response.UserResponse;
import com.pulsepass.pulsepass.entity.User;
import com.pulsepass.pulsepass.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id",        source = "user.id")
    @Mapping(target = "username",  source = "user.username")
    @Mapping(target = "email",     source = "user.email")
    @Mapping(target = "active",    source = "user.active")
    @Mapping(target = "firstName", source = "profile.firstName")
    @Mapping(target = "lastName",  source = "profile.lastName")
    @Mapping(target = "phone",     source = "profile.phone")
    @Mapping(target = "city",      source = "profile.city")
    @Mapping(target = "birthDate", source = "profile.birthDate")
    UserResponse toResponse(User user, UserProfile profile);
}
