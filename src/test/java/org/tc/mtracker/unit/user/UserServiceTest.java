package org.tc.mtracker.unit.user;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.tc.mtracker.currency.CurrencyCode;
import org.tc.mtracker.support.factory.EntityTestFactory;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserRepository;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.user.dto.RequestUpdateUserProfileDTO;
import org.tc.mtracker.user.dto.ResponseUserDTO;
import org.tc.mtracker.user.dto.UserMapper;
import org.tc.mtracker.utils.S3Service;
import org.tc.mtracker.utils.exceptions.UserNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldUpdateProfileAndReuseExistingAvatarId() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        user.setAvatarId("existing-avatar-id");
        RequestUpdateUserProfileDTO dto = new RequestUpdateUserProfileDTO("Updated User", CurrencyCode.EUR);
        MockMultipartFile avatar = new MockMultipartFile("avatar", "avatar.jpg", "image/jpeg", "avatar".getBytes());
        ResponseUserDTO response = new ResponseUserDTO(
                1L,
                user.getEmail(),
                "Updated User",
                CurrencyCode.EUR,
                "https://test-bucket.local/existing-avatar-id",
                true,
                LocalDateTime.now()
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            RequestUpdateUserProfileDTO updateDto = invocation.getArgument(0);
            User target = invocation.getArgument(1);
            target.setFullName(updateDto.fullName());
            target.setCurrencyCode(updateDto.currencyCode());
            return null;
        }).when(userMapper).updateEntityFromDto(dto, user);
        when(s3Service.generatePresignedUrl("existing-avatar-id")).thenReturn("https://test-bucket.local/existing-avatar-id");
        when(userMapper.toDto(user, "https://test-bucket.local/existing-avatar-id")).thenReturn(response);

        ResponseUserDTO result = userService.updateProfile(dto, avatar, user.getEmail());

        assertThat(result).isEqualTo(response);
        assertThat(user.getFullName()).isEqualTo("Updated User");
        assertThat(user.getCurrencyCode()).isEqualTo(CurrencyCode.EUR);
        verify(s3Service).saveFile("existing-avatar-id", avatar);
        verify(userRepository).save(user);
    }

    @Test
    void shouldReturnUserProfileWithGeneratedAvatarUrl() {
        User user = EntityTestFactory.user(1L, "user@example.com", true);
        user.setAvatarId("avatar-id");
        ResponseUserDTO response = new ResponseUserDTO(
                1L,
                user.getEmail(),
                user.getFullName(),
                user.getCurrencyCode(),
                "https://test-bucket.local/avatar-id",
                true,
                LocalDateTime.now()
        );

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(s3Service.generatePresignedUrl("avatar-id")).thenReturn("https://test-bucket.local/avatar-id");
        when(userMapper.toDto(user, "https://test-bucket.local/avatar-id")).thenReturn(response);

        ResponseUserDTO result = userService.getUser(user.getEmail());

        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldThrowWhenAuthenticatedUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentAuthenticatedUser("missing@example.com"))
                .isInstanceOf(UserNotFoundException.class);

        verifyNoInteractions(userMapper, s3Service);
    }
}
