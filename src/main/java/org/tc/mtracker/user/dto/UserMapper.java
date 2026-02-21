package org.tc.mtracker.user.dto;

import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserAvatarService;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class UserMapper {
    @Autowired
    protected UserAvatarService userAvatarService;
    public abstract void updateEntityFromDto(UpdateUserProfileRequestDTO dto, @MappingTarget User user);

    public abstract UserProfileResponseDTO toUserProfileResponseDTO(User user);

    public abstract UserResponseDTO toDto(User user);

    @AfterMapping
    protected void setAvatarUrl(User user, @MappingTarget UserProfileResponseDTO.UserProfileResponseDTOBuilder dto) {
        dto.avatarUrl(userAvatarService.generateAvatarUrl(user));
    }
}