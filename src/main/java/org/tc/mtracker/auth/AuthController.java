package org.tc.mtracker.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.auth.dto.*;
import org.tc.mtracker.security.JwtResponseDTO;

import java.net.URI;

@RestController
@RequestMapping(value = "/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController implements AuthOperations {

    private final AuthService authService;

    @Override
    public ResponseEntity<AuthResponseDTO> register(
            @ValidImage
            @RequestPart(name = "avatar", required = false) MultipartFile avatar
            AuthRequestDTO authRequestDTO,
            MultipartFile avatar
    ) {
        AuthResponseDTO authResponseDTO = authService.registerUser(authRequestDTO, avatar);
        URI location = URI.create("/api/v1/users/" + authResponseDTO.id());
        return ResponseEntity.created(location).body(authResponseDTO);
    }

    @Override
    public ResponseEntity<JwtResponseDTO> login(
            LoginRequestDto loginRequestDto
    ) {
        JwtResponseDTO jwt = authService.login(loginRequestDto);
        return ResponseEntity.ok(jwt);
    }

    @Override
    public ResponseEntity<Void> sendResetPasswordToken(
            String email
    ) {
        authService.sendTokenToResetPassword(email);
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<JwtResponseDTO> resetPassword(
            String token,
            ResetPasswordDTO resetPasswordDTO
    ) {
        JwtResponseDTO response = authService.resetPassword(token, resetPasswordDTO);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<JwtResponseDTO> verifyToken(
            String token
    ) {
        JwtResponseDTO jwt = authService.verifyToken(token);
        return ResponseEntity.ok(jwt);
    }

    @Override
    public ResponseEntity<JwtResponseDTO> refreshToken(
            RefreshTokenRequest request
    ) {
        JwtResponseDTO jwt = authService.refreshToken(request);
        return ResponseEntity.ok(jwt);
    }
}
