    package org.tc.mtracker.user.dto;

    import org.mapstruct.*;
    import org.tc.mtracker.user.User;

    @Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface UserMapper {
        void updateEntityFromDto(UpdateUserProfileDTO updateUserProfileDTO, @MappingTarget User user);

        UserResponseDTO toDto(User user);
    }
