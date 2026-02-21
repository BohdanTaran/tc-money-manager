package org.tc.mtracker.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.user.dto.RequestUpdateUserEmailDTO;
import org.tc.mtracker.user.dto.ResponseUserProfileDTO;
import org.tc.mtracker.user.dto.UpdateUserProfileDTO;
import org.tc.mtracker.user.image.ValidImage;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController implements UserOperations {

    private final UserService userService;

    @Override
    public ResponseEntity<ResponseUserProfileDTO> updateMe(
            UpdateUserProfileDTO dto,
            MultipartFile avatar,
            Authentication auth
    ) {
        ResponseUserProfileDTO responseUserProfileDTO = userService.updateProfile(dto, avatar, auth);
        return ResponseEntity.ok(responseUserProfileDTO);
    }

    @Override
    public ResponseEntity<ResponseUserProfileDTO> updateEmail(
            RequestUpdateUserEmailDTO dto,
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
}
