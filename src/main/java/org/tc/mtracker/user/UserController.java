package org.tc.mtracker.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.user.dto.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController implements UserProfileAPI {

    private final UserService userService;
    private final UserChangePasswordService userChangePasswordService;

    @Override
    public ResponseEntity<UserProfileResponseDTO> updateMe(
            UpdateUserProfileRequestDTO dto,
            MultipartFile avatar,
            Authentication auth
    ) {
        UserProfileResponseDTO responseUserProfileDTO = userService.updateProfile(dto, avatar, auth);
        return ResponseEntity.ok(responseUserProfileDTO);
    }

    @Override
    public ResponseEntity<Void> updatePassword(UpdateUserPasswordRequestDTO dto,
                                               Authentication auth) {
        userChangePasswordService.changeUserPassword(dto, auth);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<UserProfileResponseDTO> updateEmail(
            UpdateUserEmailRequestDTO dto,
            Authentication auth
    ) {
        userService.updateEmail(dto, auth);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> verifyEmail(String token) {
        userService.verifyEmailUpdate(token);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<UserResponseDTO> getUserProfile(Authentication auth) {
        return ResponseEntity.ok(userService.getUser(auth));
    }
}
