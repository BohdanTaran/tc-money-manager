package org.tc.mtracker.user.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.user.dto.RequestUpdateUserProfileDTO;
import org.tc.mtracker.user.dto.ResponseUserDTO;

@RestController
@RequiredArgsConstructor
@Validated
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<ResponseUserDTO> updateMe(
            RequestUpdateUserProfileDTO dto,
            MultipartFile avatar,
            Authentication auth
    ) {
        ResponseUserDTO responseUserProfileDTO = userService.updateProfile(dto, avatar, auth.getName());
        return ResponseEntity.ok()
                .body(responseUserProfileDTO);
    }

    @Override
    public ResponseEntity<ResponseUserDTO> getUserProfile(Authentication auth) {
        return ResponseEntity.ok(userService.getUser(auth.getName()));
    }
}
